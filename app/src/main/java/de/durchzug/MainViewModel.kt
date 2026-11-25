package de.durchzug

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val api = WeatherApi()
    private val storage = Storage(app)

    var tempIn by mutableStateOf(storage.tempIn)
        private set
    var rhIn by mutableStateOf(storage.rhIn)
        private set
    var query by mutableStateOf("")
    var places by mutableStateOf<List<Place>>(emptyList())
    var place by mutableStateOf(storage.place)
        private set
    var weather by mutableStateOf(storage.weather)
        private set
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val result: Ventilation.Result?
        get() {
            val t = tempIn.replace(',', '.').toDoubleOrNull() ?: return null
            val rh = rhIn.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..100.0 } ?: return null
            val w = weather ?: return null
            return Ventilation.calculate(t, rh, w.temp, w.humidity, w.wind)
        }

    fun updateTempIn(value: String) {
        tempIn = value
        storage.tempIn = value
    }

    fun updateRhIn(value: String) {
        rhIn = value
        storage.rhIn = value
    }

    fun search() = launchSafe {
        places = api.searchPlaces(query.trim())
        if (places.isEmpty()) error = "Kein Ort gefunden"
    }

    fun select(p: Place) {
        place = p
        storage.place = p
        weather = null
        storage.weather = null
        places = emptyList()
        refreshWeather()
    }

    fun refreshWeather() {
        val p = place ?: return
        launchSafe {
            val w = api.currentWeather(p.lat, p.lon)
            if (place == p) {
                weather = w
                storage.weather = w
            }
        }
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
