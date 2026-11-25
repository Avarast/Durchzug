package de.durchzug

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            vm.result?.let { RecommendationCard(it) }

            Section("Innen") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WheelPicker(tempInValues, vm.tempIn, { vm.tempIn = it }, "Temperatur", decimals = 1, unit = "°C", Modifier.weight(1f))
                    WheelPicker(rhValues, vm.rhIn, { vm.rhIn = it }, "rel. Feuchte", decimals = 0, unit = "%", Modifier.weight(1f))
                }
            }

            OutdoorSection(vm, onOpenSettings)
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
        WheelPicker(tempOutValues, vm.tempOut, { vm.tempOut = it }, "Temperatur", decimals = 1, unit = "°C", Modifier.weight(1f))
        WheelPicker(rhValues, vm.rhOut, { vm.rhOut = it }, "rel. Feuchte", decimals = 0, unit = "%", Modifier.weight(1f))
    }
    Text("Wind", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val winds = Ventilation.Wind.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        winds.forEachIndexed { i, wind ->
            SegmentedButton(
                selected = vm.windOut == wind,
                onClick = { vm.windOut = wind },
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
        val wind = Ventilation.Wind.fromMs(w.wind).label.lowercase()
        Text("${w.temp.fmt()} °C · ${w.humidity.fmt(0)} % rel. Feuchte · Wind $wind (${w.wind.fmt()} m/s)")
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
private fun RecommendationCard(r: Ventilation.Result) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = if (r.worthIt) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    ElevatedCard(
        onClick = { expanded = !expanded },
        colors = colors,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Empfehlung", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Details ausblenden" else "Details anzeigen",
                )
            }
            Text(
                if (r.worthIt) "Lüften lohnt sich!" else "Lüften nicht empfehlenswert",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (r.worthIt) {
                Text("${r.minutes} Minuten Stoßlüften", style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalContentColor.current.copy(alpha = 0.3f))
                Text("Absolute Feuchte innen: ${r.ahIn.fmt()} g/m³")
                Text("Absolute Feuchte außen: ${r.ahOut.fmt()} g/m³")
                Text("Rel. Feuchte nach dem Lüften: ${r.rhAfter.fmt(0)} %")
                if (!r.worthIt) {
                    Text("Die Außenluft enthält mehr Wasser als die Raumluft – Lüften würde die Feuchte im Raum erhöhen.")
                }
            }
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
