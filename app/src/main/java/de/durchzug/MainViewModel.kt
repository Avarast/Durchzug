package de.durchzug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val api = WeatherApi()

    var tempIn by mutableStateOf("21")
    var rhIn by mutableStateOf("60")
    var query by mutableStateOf("")
    var places by mutableStateOf<List<Place>>(emptyList())
    var place by mutableStateOf<Place?>(null)
    var weather by mutableStateOf<Weather?>(null)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val result: Ventilation.Result?
        get() {
            val t = tempIn.replace(',', '.').toDoubleOrNull() ?: return null
            val rh = rhIn.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..100.0 } ?: return null
            val w = weather ?: return null
            return Ventilation.calculate(t, rh, w.temp, w.humidity, w.wind)
        }

    fun search() = launchSafe {
        places = api.searchPlaces(query.trim())
        if (places.isEmpty()) error = "Kein Ort gefunden"
    }

    fun select(p: Place) {
        place = p
        places = emptyList()
        refreshWeather()
    }

    fun refreshWeather() {
        val p = place ?: return
        launchSafe { weather = api.currentWeather(p.lat, p.lon) }
    }

    private fun launchSafe(block: suspend () -> Unit) {
        if (BuildConfig.OWM_API_KEY.isBlank()) {
            error = "Kein OpenWeatherMap-API-Key in local.properties hinterlegt"
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = "Fehler: ${e.message}"
            }
            loading = false
        }
    }
}
