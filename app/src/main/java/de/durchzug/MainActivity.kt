package de.durchzug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Surface { App() } } }
    }
}

private fun Double.fmt(decimals: Int = 1) = String.format(Locale.GERMANY, "%.${decimals}f", this)

private val timestampFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)

@Composable
fun App(vm: MainViewModel = viewModel()) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(vm, onBack = { showSettings = false })
    } else {
        MainScreen(vm, onOpenSettings = { showSettings = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
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
            vm.place?.let {
                Text("Standort: ${it.label}", fontWeight = FontWeight.SemiBold)
            } ?: TextButton(onClick = onOpenSettings) { Text("Standort in den Einstellungen festlegen") }

            Section("Innen") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.tempIn, vm::updateTempIn, "Temperatur (°C)", Modifier.weight(1f))
                    NumberField(vm.rhIn, vm::updateRhIn, "rel. Feuchte (%)", Modifier.weight(1f))
                }
            }

            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (vm.place != null) {
                Section("Außen (OpenWeatherMap)") {
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
                    FilledTonalButton(onClick = { vm.refreshWeather() }, enabled = !vm.loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Aktualisieren")
                    }
                }
            }

            vm.result?.let { ResultCard(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Standort") {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                label = { Text("Ort suchen") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                trailingIcon = {
                    TextButton(onClick = { vm.search() }, enabled = vm.query.isNotBlank()) { Text("Suchen") }
                },
            )
            vm.places.forEach { p ->
                Text(p.label, Modifier.fillMaxWidth().clickable { vm.select(p); onBack() }.padding(vertical = 10.dp))
            }
            vm.place?.let { Text("Gewählt: ${it.label}", fontWeight = FontWeight.SemiBold) }
            }

            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
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
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
