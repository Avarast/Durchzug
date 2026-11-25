package de.durchzug

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Observable Compose state that is written to [SharedPreferences] on every change. */
class Pref<T>(initial: T, private val save: (T) -> Unit) : ReadWriteProperty<Any?, T> {
    private var state by mutableStateOf(initial)
    val value get() = state
    override fun getValue(thisRef: Any?, property: KProperty<*>) = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state = value
        save(value)
    }
}

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("durchzug", Context.MODE_PRIVATE)

    val place = pref("place", null, { placeFromJson(JSONObject(it)) }, { it?.let(::placeToJson)?.toString() })
    val weather = pref("weather", null, { weatherFromJson(JSONObject(it)) }, { it?.let(::weatherToJson)?.toString() })

    val tempIn = double("tempIn", 21.0)
    val rhIn = double("rhIn", 60.0)

    val manualOut = pref("manualOut", false, String::toBooleanStrict, Boolean::toString)
    val tempOut = double("tempOut", 10.0)
    val rhOut = double("rhOut", 70.0)
    val windOut = pref("windOut", Ventilation.Wind.LIGHT, Ventilation.Wind::valueOf, { it.name })

    val autoRefresh = pref("autoRefresh", false, String::toBooleanStrict, Boolean::toString)

    val themeMode = pref("themeMode", ThemeMode.SYSTEM, ThemeMode::valueOf, { it.name })
    val pureBlack = pref("pureBlack", false, String::toBooleanStrict, Boolean::toString)

    /** Whether manual outdoor values have been entered before. */
    val hasManualOut get() = prefs.contains("tempOut")

    private fun double(key: String, default: Double) =
        pref(key, default, { it.replace(',', '.').toDouble() }, Double::toString)

    private fun <T> pref(key: String, default: T, read: (String) -> T, write: (T) -> String?): Pref<T> {
        // Older versions stored some values as non-string types, so read via toString().
        val stored = prefs.all[key]?.toString()?.let { runCatching { read(it) }.getOrNull() }
        return Pref(stored ?: default) { value -> prefs.edit { putString(key, write(value)) } }
    }

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
}
