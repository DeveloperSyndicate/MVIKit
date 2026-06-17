package io.mvikit.compose.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.core.MviViewModelRegistry
import io.mvikit.core.ViewModel
import io.mvikit.core.debug.MviKitTimeTravelRecorder
import io.mvikit.core.debug.TimeTravelRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A floating, draggable development overlay for monitoring state changes and scrubbing timeline history.
 *
 * Implements premium dark HSL aesthetics, reflective property tree inspection, state diff highlights,
 * and play/pause replay controls using styled unicode symbols to remain dependency-free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTravelDebugOverlay(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset(20f, 20f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // Consumes pointer input globally for drag bounds
    ) {
        if (!isExpanded) {
            // Floating minimized pill
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .size(width = 80.dp, height = 40.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xE61E1E24), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF00E6FF), RoundedCornerShape(20.dp))
                    .clickable { isExpanded = true }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛠️ MVI",
                    color = Color(0xFF00E6FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            // Premium Expanded Floating Card
            var selectedViewModel by remember { mutableStateOf<ViewModel<*, *>?>(null) }
            val activeViewModels = remember { MviViewModelRegistry.getActiveWrappers() }

            if (selectedViewModel == null && activeViewModels.isNotEmpty()) {
                selectedViewModel = activeViewModels.first()
            }

            Card(
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .width(360.dp)
                    .height(520.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x3300E6FF), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xF21E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header (Drag Area Indicator & Minimize Button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141419))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏱️ MVIKit Time-Traveler",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text(
                                text = "✕",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ViewModel Selector
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Button(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E2E38),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedViewModel?.let { it::class.simpleName?.replace("_GeneratedWrapper", "") } ?: "Select ViewModel",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .width(340.dp)
                                .background(Color(0xFF2E2E38))
                        ) {
                            activeViewModels.forEach { vm ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = vm::class.simpleName?.replace("_GeneratedWrapper", "") ?: "ViewModel",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        selectedViewModel = vm
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedViewModel?.let { vm ->
                        // Automatically register for diagnostics if not already
                        LaunchedEffect(vm) {
                            MviKitTimeTravelRecorder.startRecording(vm)
                        }

                        // Debug content for active ViewModel
                        TimeTravelControlPanel(vm)
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Active ViewModels Found.\nOpen a screen to register.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTravelControlPanel(viewModel: ViewModel<*, *>) {
    val timeline = remember(viewModel) { MviKitTimeTravelRecorder.getTimeline(viewModel) }
    val initialState = remember(viewModel) { MviKitTimeTravelRecorder.getInitialState(viewModel) }

    // Re-trigger reading whenever timeline updates
    var updateCounter by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel) {
        viewModel.state.collect {
            updateCounter++
        }
    }

    val currentTimeline = remember(updateCounter, viewModel) {
        MviKitTimeTravelRecorder.getTimeline(viewModel)
    }

    var sliderPosition by remember { mutableStateOf(0f) }
    val maxStep = currentTimeline.size

    // Keep slider inside bounds when timeline grows
    LaunchedEffect(maxStep) {
        if (sliderPosition > maxStep) {
            sliderPosition = maxStep.toFloat()
        }
    }

    // Determine current state based on scrubbing index
    val activeState = remember(sliderPosition, currentTimeline, initialState) {
        val index = sliderPosition.roundToInt()
        if (index == 0) {
            initialState ?: viewModel.state.value
        } else {
            currentTimeline[index - 1].stateAfter
        }
    }

    // Apply scrubbing override state
    LaunchedEffect(activeState) {
        viewModel.overrideState(activeState)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Timeline Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History Step: ${sliderPosition.roundToInt()} / $maxStep",
                color = Color(0xFF00E6FF),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            if (sliderPosition.roundToInt() < maxStep) {
                Text(
                    text = "TIME TRAVEL ACTIVE",
                    color = Color.Yellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..(if (maxStep > 0) maxStep.toFloat() else 1f),
            steps = if (maxStep > 1) maxStep - 1 else 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E6FF),
                activeTrackColor = Color(0xFF00E6FF),
                inactiveTrackColor = Color.DarkGray
            )
        )

        // Control Buttons (Step Back, Step Forward, Live State, Replay)
        val scope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (sliderPosition > 0f) sliderPosition -= 1f },
                enabled = sliderPosition > 0f
            ) {
                Text(
                    text = "◀",
                    color = if (sliderPosition > 0f) Color.White else Color.DarkGray,
                    fontSize = 16.sp
                )
            }

            IconButton(
                onClick = { if (sliderPosition < maxStep) sliderPosition += 1f },
                enabled = sliderPosition < maxStep
            ) {
                Text(
                    text = "▶",
                    color = if (sliderPosition < maxStep) Color.White else Color.DarkGray,
                    fontSize = 16.sp
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        sliderPosition = 0f
                        delay(200)
                        for (i in 1..maxStep) {
                            sliderPosition = i.toFloat()
                            delay(400)
                        }
                    }
                },
                enabled = maxStep > 0
            ) {
                Text(
                    text = "🔁",
                    color = if (maxStep > 0) Color(0xFF00FF66) else Color.DarkGray,
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = { sliderPosition = maxStep.toFloat() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E6FF)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Live State", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // State property tree inspector
        Text(
            text = "State properties:",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
            fontFamily = FontFamily.Monospace
        )

        val properties = remember(activeState) { inspectState(activeState) }
        val liveState = viewModel.state.value
        val liveProperties = remember(liveState) { inspectState(liveState) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF141419), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(properties.entries.toList()) { entry ->
                val propName = entry.key
                val propVal = entry.value
                val isChanged = liveProperties[propName] != propVal

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = propName,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = propVal.toString(),
                        color = if (isChanged) Color.Yellow else Color(0xFF00E6FF),
                        fontSize = 10.sp,
                        fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun inspectState(obj: Any?): Map<String, Any?> {
    if (obj == null) return emptyMap()
    val fieldsMap = mutableMapOf<String, Any?>()
    var currentClass: Class<*>? = obj::class.java
    while (currentClass != null && currentClass != Any::class.java) {
        for (field in currentClass.declaredFields) {
            // Ignore compiler-generated properties like companion classes or delegates
            if (field.name.startsWith("$") || field.name == "serialVersionUID") continue
            field.isAccessible = true
            fieldsMap[field.name] = try {
                field.get(obj)
            } catch (e: Exception) {
                "Error"
            }
        }
        currentClass = currentClass.superclass
    }
    return fieldsMap
}
