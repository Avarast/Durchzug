package de.durchzug

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val api = WeatherApi()
    private val storage = Storage(app)

    var tempIn by storage.tempIn
    var rhIn by storage.rhIn
    var tempOut by storage.tempOut
    var rhOut by storage.rhOut
    var windOut by storage.windOut
    var manualOut by storage.manualOut
        private set
    var place by storage.place
        private set
    var weather by storage.weather
        private set

    var query by mutableStateOf("")
    var places by mutableStateOf<List<Place>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var weatherError by mutableStateOf<String?>(null)
        private set

    val result: Ventilation.Result?
        get() = if (manualOut) {
            Ventilation.calculate(tempIn, rhIn, tempOut, rhOut, windOut.ms)
        } else {
            weather?.let { Ventilation.calculate(tempIn, rhIn, it.temp, it.humidity, it.wind) }
        }

    fun updateManualOut(manual: Boolean) {
        // Start manual input from the last weather data if no own values exist yet.
        val w = weather
        if (manual && !storage.hasManualOut && w != null) {
            tempOut = (w.temp * 2).roundToInt() / 2.0
            rhOut = w.humidity
            windOut = Ventilation.Wind.fromMs(w.wind)
        }
        manualOut = manual
    }

    fun search() = launchSafe({ searching = it }, { searchError = it }) {
        places = api.searchPlaces(query.trim())
        if (places.isEmpty()) searchError = "Kein Ort gefunden"
    }

    fun select(p: Place) {
        place = p
        weather = null
        places = emptyList()
        refreshWeather()
    }

    fun refreshWeather() {
        val p = place ?: return
        launchSafe({ refreshing = it }, { weatherError = it }) {
            val w = api.currentWeather(p.lat, p.lon)
            if (place == p) weather = w
        }
    }

    private fun launchSafe(
        setLoading: (Boolean) -> Unit,
        setError: (String?) -> Unit,
        block: suspend () -> Unit,
    ) {
        if (BuildConfig.OWM_API_KEY.isBlank()) {
            setError("Kein OpenWeatherMap-API-Key in local.properties hinterlegt")
            return
        }
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            try {
                block()
            } catch (e: Exception) {
                setError("Fehler: ${e.message}")
            }
            setLoading(false)
        }
    }
}
