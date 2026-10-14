package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.model.SensorReading
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.SensorHealthBadge
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*

@Composable
fun SensorHealthScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()

    val onlineCount = drums.count { it.sensorHealth == SensorHealth.ONLINE }
    val faultCount = drums.count { it.sensorHealth == SensorHealth.FAULT || it.sensorHealth == SensorHealth.OFFLINE }
    val staleCount = drums.count { it.sensorHealth == SensorHealth.STALE }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sensor_health_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Diagnostics KPI Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ONLINE SENSORS", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                        Text("$onlineCount / ${drums.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StatusNormal))
                        Text("Telemetry verified", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("FAULTS DETECTED", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                        Text("$faultCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = if (faultCount > 0) StatusFault else TextPrimary))
                        Text(if (faultCount > 0) "Requires intervention" else "Zero active faults", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                    }
                }
            }
        }

        item {
            Text(
                text = "DRUM SENSOR TELEMETRY & FAULT INJECTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
            )
        }

        // Sensor Diagnostic Cards
        items(drums, key = { it.id }) { drum ->
            val reading = sensorReadings[drum.id]
            SensorDiagnosticCard(
                drum = drum,
                reading = reading,
                onInjectFault = { fault -> viewModel.setSensorFault(drum.id, fault) }
            )
        }
    }
}

@Composable
fun SensorDiagnosticCard(
    drum: Drum,
    reading: SensorReading?,
    onInjectFault: (FaultType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = IndustrialNavy, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = drum.id,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Laser Sensor #${drum.id.takeLast(3)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                SensorHealthBadge(drum.sensorHealth, drum.faultType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Diagnostic Telemetry Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryParam("Bus Voltage", "24.1 V DC")
                TelemetryParam("Laser Distance", if (reading != null && reading.health == SensorHealth.ONLINE) "${reading.rawDistanceCm} cm" else "INVALID")
                TelemetryParam("Signal Quality", if (drum.sensorHealth == SensorHealth.ONLINE) "98% (High)" else if (drum.faultType == FaultType.NOISY) "42% (Degraded)" else "0% (Lost)")
                TelemetryParam("Latency", if (drum.sensorHealth == SensorHealth.ONLINE) "14 ms" else "TIMEOUT")
            }

            if (reading?.errorDescription != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = StatusCriticalBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reading.errorDescription,
                            style = MaterialTheme.typography.bodySmall.copy(color = StatusCritical, fontSize = 11.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Fault Trigger Buttons
            Text(
                text = "Inject Hardware Simulation Fault:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FaultQuickButton("Normal", drum.faultType == FaultType.NONE, StatusNormal) { onInjectFault(FaultType.NONE) }
                FaultQuickButton("Disconnect", drum.faultType == FaultType.DISCONNECTED, StatusFault) { onInjectFault(FaultType.DISCONNECTED) }
                FaultQuickButton("Out of Range", drum.faultType == FaultType.OUT_OF_RANGE, StatusLow) { onInjectFault(FaultType.OUT_OF_RANGE) }
                FaultQuickButton("Frozen", drum.faultType == FaultType.FROZEN, Color(0xFF64748B)) { onInjectFault(FaultType.FROZEN) }
                FaultQuickButton("Noisy", drum.faultType == FaultType.NOISY, AccentBlue) { onInjectFault(FaultType.NOISY) }
            }
        }
    }
}

@Composable
private fun RowScope.FaultQuickButton(label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isActive) activeColor else Color(0xFFF1F5F9),
            contentColor = if (isActive) Color.White else TextPrimary
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = Modifier.weight(1f).height(30.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun TelemetryParam(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextPrimary
            )
        )
    }
}
