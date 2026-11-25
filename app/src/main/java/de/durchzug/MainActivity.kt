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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Surface { App() } } }
    }
}

private fun Double.fmt(decimals: Int = 1) = String.format(Locale.GERMANY, "%.${decimals}f", this)

@Composable
fun App(vm: MainViewModel = viewModel()) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Durchzug", style = MaterialTheme.typography.headlineMedium)

        Section("Innen") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.tempIn, { vm.tempIn = it }, "Temperatur (°C)", Modifier.weight(1f))
                NumberField(vm.rhIn, { vm.rhIn = it }, "rel. Feuchte (%)", Modifier.weight(1f))
            }
        }

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
                Text(p.label, Modifier.fillMaxWidth().clickable { vm.select(p) }.padding(vertical = 10.dp))
            }
            vm.place?.let { Text("Gewählt: ${it.label}", fontWeight = FontWeight.SemiBold) }
        }

        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        vm.weather?.let { w ->
            Section("Außen (OpenWeatherMap)") {
                Text("${w.temp.fmt()} °C · ${w.humidity.fmt(0)} % rel. Feuchte · Wind ${w.wind.fmt()} m/s")
                if (w.description.isNotBlank()) Text(w.description.replaceFirstChar { it.uppercase() })
                TextButton(onClick = { vm.refreshWeather() }) { Text("Aktualisieren") }
            }
        }

        vm.result?.let { ResultCard(it) }
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
