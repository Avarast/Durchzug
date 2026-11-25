package de.durchzug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
                    WheelPicker(tempValues, vm.tempIn, vm::updateTempIn, "Temperatur", decimals = 1, unit = "°C", Modifier.weight(1f))
                    WheelPicker(rhValues, vm.rhIn, vm::updateRhIn, "rel. Feuchte", decimals = 0, unit = "%", Modifier.weight(1f))
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

private val tempValues = (-20..80).map { it * 0.5 }
private val rhValues = (0..100).map { it.toDouble() }

@Composable
private fun WheelPicker(
    values: List<Double>,
    value: String,
    onChange: (String) -> Unit,
    label: String,
    decimals: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val parsed = value.replace(',', '.').toDoubleOrNull()
    val targetIndex = parsed?.let { p -> values.indices.minBy { abs(values[it] - p) } }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex ?: 0)
    val centerIndex by remember {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
        }
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val currentTarget by rememberUpdatedState(targetIndex)
    val currentOnChange by rememberUpdatedState(onChange)
    var editing by remember { mutableStateOf(false) }

    // Follow typed input when the wheel is idle.
    LaunchedEffect(targetIndex) {
        if (targetIndex != null && !state.isScrollInProgress && targetIndex != centerIndex) {
            state.scrollToItem(targetIndex)
        }
    }
    // Report the selected value once a user scroll has settled.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.drop(1).filter { !it }.collect {
            val i = centerIndex.coerceIn(values.indices)
            if (i != currentTarget) currentOnChange(values[i].fmt(decimals))
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { centerIndex }.drop(1).collect {
            if (state.isScrollInProgress) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Box(Modifier.fillMaxWidth().height(itemHeight * 3), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.fillMaxWidth().height(itemHeight),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {}
            LazyColumn(
                state = state,
                flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(vertical = itemHeight),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(values.size) { i ->
                    val selected = i == centerIndex
                    // Show the exact typed value if it lies between two wheel steps.
                    val text = if (selected && i == targetIndex && parsed != values[i]) value else values[i].fmt(decimals)
                    Box(
                        Modifier.fillMaxWidth().height(itemHeight).clickable(
                            onClickLabel = if (selected) "Wert eingeben" else "Wert auswählen",
                        ) {
                            if (selected) editing = true
                            else scope.launch { state.animateScrollToItem(i) }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$text $unit",
                            style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
            if (editing) {
                WheelInput(value, unit, itemHeight) { input ->
                    editing = false
                    val number = input?.replace(',', '.')?.toDoubleOrNull() ?: return@WheelInput
                    val min = values.first()
                    val max = values.last()
                    onChange(if (number in min..max) input else number.coerceIn(min, max).fmt(decimals))
                }
            }
        }
    }
}

@Composable
private fun WheelInput(initial: String, unit: String, height: Dp, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    val currentOnDone by rememberUpdatedState(onDone)
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (finished) return
        finished = true
        currentOnDone(text.text.trim())
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        Modifier.fillMaxWidth().height(height),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finish() }),
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (hadFocus && !it.isFocused) finish()
                    hadFocus = hadFocus || it.isFocused
                },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(IntrinsicSize.Min)) { inner() }
                    Text(
                        " $unit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            },
        )
    }
}
