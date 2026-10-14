package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Drum
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.DrumStatusBadge
import com.example.ui.components.FollowerPlateCrossSectionDiagram
import com.example.ui.components.SensorHealthBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DrumDetailDialog(
    viewModel: SmartSealerViewModel,
    onDismiss: () -> Unit
) {
    val drums by viewModel.drums.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val selectedId by viewModel.selectedDrumId.collectAsState()

    val drum = drums.find { it.id == selectedId } ?: drums.firstOrNull() ?: return
    val reading = sensorReadings[drum.id]
    val drumTransactions = transactions.filter { it.drumId == drum.id }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_drum_detail"),
            shape = RoundedCornerShape(16.dp),
            color = DashboardBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Top Bar
                Surface(
                    color = IndustrialNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = drum.id,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                DrumStatusBadge(status = drum.status)
                            }
                            Text(
                                text = "${drum.sealerType} • Lot: ${drum.batchNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Follower Plate Cross-Section Diagram
                    FollowerPlateCrossSectionDiagram(
                        drumCapacityKg = drum.nominalCapacityKg,
                        currentMassKg = drum.currentMassKg,
                        usableTravelCm = drum.usableTravelCm,
                        sensorOffsetCm = drum.sensorOffsetCm,
                        measuredDistanceCm = reading?.rawDistanceCm ?: drum.theoreticalDistanceCm,
                        estimatedMassKg = reading?.estimatedMassKg ?: drum.currentMassKg
                    )

                    // Physics & Sensor Calibration Detail
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "CALIBRATION & PHYSICAL MEASUREMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailGridItem("Current Sealer Mass", "${drum.currentMassKg} kg")
                                DetailGridItem("Nominal Capacity", "${drum.nominalCapacityKg} kg")
                                DetailGridItem("Consumed Mass", "${(drum.consumedMassKg * 10).roundToInt() / 10.0} kg")
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailGridItem("Gross Weight (+Tare)", "${(drum.grossWeightKg * 10).roundToInt() / 10.0} kg")
                                DetailGridItem("Packaging Tare", "${drum.emptyTareWeightKg} kg")
                                DetailGridItem("Sensor Offset", "${drum.sensorOffsetCm} cm")
                            }
                        }
                    }

                    // Quick Action Buttons
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "DISPENSING & REFILL TEST CONTROLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.consumeMaterial(drum.id, 5.0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("−5 kg")
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.consumeMaterial(drum.id, 10.0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("−10 kg")
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.refillDrum(drum.id, 20.0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+20 kg")
                                }
                                Button(
                                    onClick = { viewModel.resetDrum(drum.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }

                    // Drum-Specific Movement History
                    Text(
                        text = "DRUM TRANSACTION HISTORY (${drumTransactions.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                    )

                    if (drumTransactions.isEmpty()) {
                        Text("No recorded stock movements for this drum yet.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    } else {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        drumTransactions.forEach { tx ->
                            Surface(
                                color = CardBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tx.type.name.replace("_", " ")}: ${tx.notes}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "${sdf.format(Date(tx.timestampMillis))} • By: ${tx.operator}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                                        )
                                    }
                                    if (tx.deltaKg != 0.0) {
                                        Text(
                                            text = "${if (tx.deltaKg > 0) "+" else ""}${tx.deltaKg} kg",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.deltaKg > 0) StatusNormal else TextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailGridItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextPrimary
            )
        )
    }
}
