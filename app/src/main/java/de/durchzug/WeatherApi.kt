package de.durchzug

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val name: String, val country: String, val state: String?, val lat: Double, val lon: Double) {
    val label get() = listOfNotNull(name, state, country).joinToString(", ")
}

data class Weather(
    val temp: Double,
    val humidity: Double,
    val wind: Double,
    val description: String,
    val fetchedAt: Long = System.currentTimeMillis(),
)

class HttpException(val code: Int) : Exception("HTTP $code")

class WeatherApi(private val apiKey: String = BuildConfig.OWM_API_KEY) {

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            if (code != 200) throw HttpException(code)
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun searchPlaces(query: String): List<Place> {
        val q = URLEncoder.encode(query, "UTF-8")
        val arr = JSONArray(get("https://api.openweathermap.org/geo/1.0/direct?q=$q&limit=5&appid=$apiKey"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val german = o.optJSONObject("local_names")?.optString("de")?.takeIf { s -> s.isNotBlank() }
            Place(
                name = german ?: o.getString("name"),
                country = o.optString("country"),
                state = o.optString("state").takeIf { s -> s.isNotBlank() },
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
            )
        }
    }

    suspend fun currentWeather(lat: Double, lon: Double): Weather {
        val o = JSONObject(get("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&lang=de&appid=$apiKey"))
        val main = o.getJSONObject("main")
        return Weather(
            temp = main.getDouble("temp"),
            humidity = main.getDouble("humidity"),
            wind = o.optJSONObject("wind")?.optDouble("speed", 0.0) ?: 0.0,
            description = o.optJSONArray("weather")?.optJSONObject(0)?.optString("description") ?: "",
        )
    }
}
