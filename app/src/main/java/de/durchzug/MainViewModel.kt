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
    var searching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var weatherError by mutableStateOf<String?>(null)
        private set

    var manualOut by mutableStateOf(storage.manualOut)
        private set
    var tempOut by mutableStateOf(storage.tempOut ?: "10")
        private set
    var rhOut by mutableStateOf(storage.rhOut ?: "70")
        private set
    var windOut by mutableStateOf(storage.windOut ?: Ventilation.Wind.LIGHT)
        private set

    val result: Ventilation.Result?
        get() {
            val t = tempIn.parse() ?: return null
            val rh = rhIn.parse()?.takeIf { it in 0.0..100.0 } ?: return null
            return if (manualOut) {
                val tOut = tempOut.parse() ?: return null
                val rhO = rhOut.parse()?.takeIf { it in 0.0..100.0 } ?: return null
                Ventilation.calculate(t, rh, tOut, rhO, windOut.ms)
            } else {
                val w = weather ?: return null
                Ventilation.calculate(t, rh, w.temp, w.humidity, w.wind)
            }
        }

    private fun String.parse() = replace(',', '.').toDoubleOrNull()

    fun updateManualOut(manual: Boolean) {
        // Start manual input from the last weather data if no own values exist yet.
        val w = weather
        if (manual && storage.tempOut == null && w != null) {
            updateTempOut((Math.round(w.temp * 2) / 2.0).fmt(1))
            updateRhOut(w.humidity.fmt(0))
            updateWindOut(Ventilation.Wind.fromMs(w.wind))
        }
        manualOut = manual
        storage.manualOut = manual
    }

    fun updateTempOut(value: String) {
        tempOut = value
        storage.tempOut = value
    }

    fun updateRhOut(value: String) {
        rhOut = value
        storage.rhOut = value
    }

    fun updateWindOut(value: Ventilation.Wind) {
        windOut = value
        storage.windOut = value
    }

    fun updateTempIn(value: String) {
        tempIn = value
        storage.tempIn = value
    }

    fun updateRhIn(value: String) {
        rhIn = value
        storage.rhIn = value
    }

    fun search() = launchSafe({ searching = it }, { searchError = it }) {
        places = api.searchPlaces(query.trim())
        if (places.isEmpty()) searchError = "Kein Ort gefunden"
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
        launchSafe({ refreshing = it }, { weatherError = it }) {
            val w = api.currentWeather(p.lat, p.lon)
            if (place == p) {
                weather = w
                storage.weather = w
            }
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
