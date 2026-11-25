package de.durchzug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Durchzug") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Innen") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WheelPicker(tempInValues, vm.tempIn, vm::updateTempIn, "Temperatur", decimals = 1, unit = "°C", Modifier.weight(1f))
                    WheelPicker(rhValues, vm.rhIn, vm::updateRhIn, "rel. Feuchte", decimals = 0, unit = "%", Modifier.weight(1f))
                }
            }

            OutdoorSection(vm, onOpenSettings)

            vm.result?.let { ResultCard(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutdoorSection(vm: MainViewModel, onOpenSettings: () -> Unit) {
    Section("Außen") {
        val modes = listOf("Wetterdienst", "Manuell")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = vm.manualOut == (i == 1),
                    onClick = { vm.updateManualOut(i == 1) },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(label) }
            }
        }

        val place = vm.place
        when {
            vm.manualOut -> ManualOutdoor(vm)
            place == null -> OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Place, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Standort festlegen")
            }
            else -> WeatherOutdoor(vm, place, onOpenSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualOutdoor(vm: MainViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WheelPicker(tempOutValues, vm.tempOut, vm::updateTempOut, "Temperatur", decimals = 1, unit = "°C", Modifier.weight(1f))
        WheelPicker(rhValues, vm.rhOut, vm::updateRhOut, "rel. Feuchte", decimals = 0, unit = "%", Modifier.weight(1f))
    }
    Text("Wind", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val winds = Ventilation.Wind.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        winds.forEachIndexed { i, wind ->
            SegmentedButton(
                selected = vm.windOut == wind,
                onClick = { vm.updateWindOut(wind) },
                shape = SegmentedButtonDefaults.itemShape(i, winds.size),
                icon = {},
            ) { Text(wind.label, maxLines = 1) }
        }
    }
}

@Composable
private fun WeatherOutdoor(vm: MainViewModel, place: Place, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(place.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Edit, contentDescription = "Standort ändern")
        }
    }
    val w = vm.weather
    if (w != null) {
        Text("${w.temp.fmt()} °C · ${w.humidity.fmt(0)} % rel. Feuchte · Wind ${w.wind.fmt()} m/s")
        if (w.description.isNotBlank()) Text(w.description.replaceFirstChar { it.uppercase() })
        Text(
            "Stand: ${timestampFormat.format(Date(w.fetchedAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text("Noch keine Wetterdaten vorhanden.")
    }
    if (vm.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
    vm.weatherError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    FilledTonalButton(onClick = { vm.refreshWeather() }, enabled = !vm.refreshing) {
        Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Aktualisieren")
    }
}

@Composable
private fun ResultCard(r: Ventilation.Result) {
    val color = if (r.worthIt) Color(0xFF2E7D32) else Color(0xFFC62828)
    Section("Ergebnis") {
        Text(
            if (r.worthIt) "Lüften lohnt sich!" else "Lüften nicht empfehlenswert",
            color = color,
            style = MaterialTheme.typography.titleLarge,
        )
        Text("Absolute Feuchte innen: ${r.ahIn.fmt()} g/m³")
        Text("Absolute Feuchte außen: ${r.ahOut.fmt()} g/m³")
        Text("Rel. Feuchte nach dem Lüften: ${r.rhAfter.fmt(0)} %", fontWeight = FontWeight.SemiBold)
        if (r.worthIt) {
            Text("Empfohlene Lüftungsdauer: ${r.minutes} Minuten (Stoßlüften)", fontWeight = FontWeight.SemiBold)
        } else {
            Text("Die Außenluft enthält mehr Wasser als die Raumluft – Lüften würde die Feuchte im Raum erhöhen.")
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private val Ventilation.Wind.label
    get() = when (this) {
        Ventilation.Wind.CALM -> "Still"
        Ventilation.Wind.LIGHT -> "Leicht"
        Ventilation.Wind.MODERATE -> "Mäßig"
        Ventilation.Wind.STRONG -> "Stark"
    }
