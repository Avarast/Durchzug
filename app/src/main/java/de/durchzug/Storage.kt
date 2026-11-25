package de.durchzug

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("durchzug", Context.MODE_PRIVATE)

    var place: Place?
        get() = prefs.getString(KEY_PLACE, null)?.let { runCatching { placeFromJson(JSONObject(it)) }.getOrNull() }
        set(value) = prefs.edit { putString(KEY_PLACE, value?.let { placeToJson(it).toString() }) }

    var weather: Weather?
        get() = prefs.getString(KEY_WEATHER, null)?.let { runCatching { weatherFromJson(JSONObject(it)) }.getOrNull() }
        set(value) = prefs.edit { putString(KEY_WEATHER, value?.let { weatherToJson(it).toString() }) }

    private fun placeToJson(p: Place) = JSONObject()
        .put("name", p.name).put("country", p.country).put("state", p.state)
        .put("lat", p.lat).put("lon", p.lon)

    private fun placeFromJson(o: JSONObject) = Place(
        name = o.getString("name"),
        country = o.getString("country"),
        state = o.optString("state").takeIf { it.isNotBlank() },
        lat = o.getDouble("lat"),
        lon = o.getDouble("lon"),
    )

    private fun weatherToJson(w: Weather) = JSONObject()
        .put("temp", w.temp).put("humidity", w.humidity).put("wind", w.wind)
        .put("description", w.description).put("fetchedAt", w.fetchedAt)

    private fun weatherFromJson(o: JSONObject) = Weather(
        temp = o.getDouble("temp"),
        humidity = o.getDouble("humidity"),
        wind = o.getDouble("wind"),
        description = o.optString("description"),
        fetchedAt = o.getLong("fetchedAt"),
    )

    private companion object {
        const val KEY_PLACE = "place"
        const val KEY_WEATHER = "weather"
    }
}
