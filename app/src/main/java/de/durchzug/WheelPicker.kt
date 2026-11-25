package de.durchzug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val tempInValues = (-20..80).map { it * 0.5 }
internal val tempOutValues = (-60..90).map { it * 0.5 }
internal val rhValues = (0..100).map { it.toDouble() }

@Composable
internal fun WheelPicker(
    values: List<Double>,
    value: Double,
    onChange: (Double) -> Unit,
    label: String,
    decimals: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val targetIndex = values.indices.minBy { abs(values[it] - value) }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex)
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
        if (!state.isScrollInProgress && targetIndex != centerIndex) {
            state.scrollToItem(targetIndex)
        }
    }
    // Report the selected value once a user scroll has settled.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.drop(1).filter { !it }.collect {
            val i = centerIndex.coerceIn(values.indices)
            if (i != currentTarget) currentOnChange(values[i])
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
                    val text = if (selected && i == targetIndex && value != values[i]) value.fmtCompact() else values[i].fmt(decimals)
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
                WheelInput(value.fmtCompact(), unit, itemHeight) { input ->
                    editing = false
                    input.parseDecimal()?.let { onChange(it.coerceIn(values.first(), values.last())) }
                }
            }
        }
    }
}

@Composable
private fun WheelInput(initial: String, unit: String, height: Dp, onDone: (String) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val currentOnDone by rememberUpdatedState(onDone)
    fun finish() {
        if (finished) return
        finished = true
        currentOnDone(text.text)
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
