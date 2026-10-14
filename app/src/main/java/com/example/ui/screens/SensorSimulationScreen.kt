package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Drum
import com.example.model.FaultType
import com.example.model.SensorHealth
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorSimulationScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedDrumId by viewModel.simulationDrumId.collectAsState()

    val drum = drums.find { it.id == selectedDrumId } ?: drums.firstOrNull()
    val reading = drum?.let { sensorReadings[it.id] }

    var customConsumeText by remember { mutableStateOf("15") }
    var customRefillText by remember { mutableStateOf("30") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("simulation_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Drum Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SELECT TARGET SEALER DRUM FOR SIMULATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drums.forEach { d ->
                            val isSelected = d.id == selectedDrumId
                            OutlinedButton(
                                onClick = { viewModel.setSimulationDrum(d.id) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) IndustrialNavy else Color.Transparent,
                                    contentColor = if (isSelected) Color.White else TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) IndustrialNavy else CardBorder
                                ),
                                modifier = Modifier.weight(1f).testTag("select_sim_drum_${d.id}")
                            ) {
                                Text(d.id, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        if (drum != null) {
            // Live Cross-Section Diagram with Geometric Calibration
            item {
                FollowerPlateCrossSectionDiagram(
                    drumCapacityKg = drum.nominalCapacityKg,
                    currentMassKg = drum.currentMassKg,
                    usableTravelCm = drum.usableTravelCm,
                    sensorOffsetCm = drum.sensorOffsetCm,
                    measuredDistanceCm = reading?.rawDistanceCm ?: drum.theoreticalDistanceCm,
                    estimatedMassKg = reading?.estimatedMassKg ?: drum.currentMassKg
                )
            }

            // Real-Time Simulation Master Controls
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SIMULATION ENGINE STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                )
                                Text(
                                    text = if (settings.isSimulationRunning) "RUNNING (TICKS ACTIVE)" else "PAUSED (STATIC)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (settings.isSimulationRunning) StatusNormal else StatusLow
                                    )
                                )
                            }

                            Button(
                                onClick = { viewModel.toggleSimulation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (settings.isSimulationRunning) StatusLow else StatusNormal
                                ),
                                modifier = Modifier.testTag("btn_toggle_sim")
                            ) {
                                Icon(
                                    imageVector = if (settings.isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (settings.isSimulationRunning) "Pause Engine" else "Start Engine")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = CardBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Simulation Rate Slider
                        Text(
                            text = "Line Consumption Rate: ${settings.simulationRateKgPerMin} kg/min",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Slider(
                            value = settings.simulationRateKgPerMin.toFloat(),
                            onValueChange = {
                                val rounded = (it * 10).roundToInt() / 10.0
                                viewModel.updateSettings(settings.copy(simulationRateKgPerMin = rounded))
                            },
                            valueRange = 0.5f..10.0f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth().testTag("slider_sim_rate")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Update Interval Selector
                        Text(
                            text = "Laser Telemetry Sample Rate: ${settings.simulationIntervalSec}s",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 5, 10).forEach { interval ->
                                val isSelected = settings.simulationIntervalSec == interval
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(simulationIntervalSec = interval)) },
                                    label = { Text("${interval}s interval") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Manual Material Injection & Refill Testing Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MANUAL MATERIAL DISPENSE & REFILL ACTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.consumeMaterial(drum.id, 5.0) },
                                modifier = Modifier.weight(1f).testTag("sim_consume_5")
                            ) {
                                Text("−5 kg")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.consumeMaterial(drum.id, 10.0) },
                                modifier = Modifier.weight(1f).testTag("sim_consume_10")
                            ) {
                                Text("−10 kg")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.refillDrum(drum.id, 20.0) },
                                modifier = Modifier.weight(1f).testTag("sim_refill_20")
                            ) {
                                Text("+20 kg")
                            }
                            Button(
                                onClick = { viewModel.resetDrum(drum.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                                modifier = Modifier.weight(1f).testTag("sim_reset_drum")
                            ) {
                                Text("Reset")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom quantity row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customConsumeText,
                                onValueChange = { customConsumeText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Consume (kg)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val amt = customConsumeText.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) viewModel.consumeMaterial(drum.id, amt)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }

            // Sensor Fault Injection Matrix
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SENSOR FAULT INJECTION LAB",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                )
                                Text(
                                    text = "Current State: ${drum.faultType.name}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (drum.faultType == FaultType.NONE) StatusNormal else StatusFault
                                    )
                                )
                            }
                            SensorHealthBadge(drum.sensorHealth, drum.faultType)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Test how automated safety algorithms and stock estimators respond when hardware sensors fail:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FaultOptionRow(
                                title = "Normal / Healthy",
                                description = "Clean optical laser measurement with calibrated ±0.15 cm variance.",
                                isSelected = drum.faultType == FaultType.NONE,
                                onClick = { viewModel.setSensorFault(drum.id, FaultType.NONE) }
                            )
                            FaultOptionRow(
                                title = "Disconnected Sensor (Offline)",
                                description = "Simulates RS-485 / IO-Link cable severance. Returns status OFFLINE and reading -1.0 cm.",
                                isSelected = drum.faultType == FaultType.DISCONNECTED,
                                onClick = { viewModel.setSensorFault(drum.id, FaultType.DISCONNECTED) }
                            )
                            FaultOptionRow(
                                title = "Out of Range (Beam Deflection)",
                                description = "Follower plate tilt or specular scatter causes out-of-bounds reading (>999.9 cm).",
                                isSelected = drum.faultType == FaultType.OUT_OF_RANGE,
                                onClick = { viewModel.setSensorFault(drum.id, FaultType.OUT_OF_RANGE) }
                            )
                            FaultOptionRow(
                                title = "Frozen Reading (Stale Data)",
                                description = "Sensor microcontroller locks up. Telemetry remains constant despite active material consumption.",
                                isSelected = drum.faultType == FaultType.FROZEN,
                                onClick = { viewModel.setSensorFault(drum.id, FaultType.FROZEN) }
                            )
                            FaultOptionRow(
                                title = "High Optical Noise (Scatter)",
                                description = "Dirty optical lens or airborne mist creates erratic ±5.0 cm readings.",
                                isSelected = drum.faultType == FaultType.NOISY,
                                onClick = { viewModel.setSensorFault(drum.id, FaultType.NOISY) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaultOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFFEFF6FF) else CardBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AccentBlue else CardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AccentBlue else TextPrimary
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
