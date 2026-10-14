package com.example.ui.screens

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
import com.example.model.InventorySettings
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    var lowThreshold by remember(settings) { mutableStateOf(settings.lowThresholdPercent) }
    var critThreshold by remember(settings) { mutableStateOf(settings.criticalThresholdPercent) }
    var minInventoryText by remember(settings) { mutableStateOf("${settings.minInventoryKg.toInt()}") }
    var dailyConsumptionText by remember(settings) { mutableStateOf("${settings.avgDailyConsumptionEstimate.toInt()}") }
    var leadTimeText by remember(settings) { mutableStateOf("${settings.supplierLeadTimeDays}") }
    var safetyStockText by remember(settings) { mutableStateOf("${settings.safetyStockKg.toInt()}") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Industrial Thresholds
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
                        text = "DRUM LEVEL ALARM THRESHOLDS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Low Level Warning: ${lowThreshold.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = lowThreshold,
                        onValueChange = { lowThreshold = it },
                        valueRange = 15f..40f,
                        steps = 24,
                        modifier = Modifier.fillMaxWidth().testTag("slider_low_threshold")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Critical Emergency Level: ${critThreshold.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = StatusCritical)
                    )
                    Slider(
                        value = critThreshold,
                        onValueChange = { critThreshold = it },
                        valueRange = 5f..20f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth().testTag("slider_crit_threshold")
                    )
                }
            }
        }

        // Reorder Parameters
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
                        text = "MATERIAL REORDER PARAMETERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyConsumptionText,
                            onValueChange = { dailyConsumptionText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Daily Usage (kg/day)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = leadTimeText,
                            onValueChange = { leadTimeText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Lead Time (days)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = safetyStockText,
                            onValueChange = { safetyStockText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Safety Stock (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minInventoryText,
                            onValueChange = { minInventoryText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Min Inventory (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val newSettings = settings.copy(
                                lowThresholdPercent = lowThreshold,
                                criticalThresholdPercent = critThreshold,
                                minInventoryKg = minInventoryText.toDoubleOrNull() ?: settings.minInventoryKg,
                                avgDailyConsumptionEstimate = dailyConsumptionText.toDoubleOrNull() ?: settings.avgDailyConsumptionEstimate,
                                supplierLeadTimeDays = leadTimeText.toIntOrNull() ?: settings.supplierLeadTimeDays,
                                safetyStockKg = safetyStockText.toDoubleOrNull() ?: settings.safetyStockKg
                            )
                            viewModel.updateSettings(newSettings)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_settings")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Inventory Configuration")
                    }
                }
            }
        }

        // Hardware Architecture & Presentation Roadmap Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FUTURE IOT HARDWARE INTEGRATION & SCADA ROADMAP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LaserCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "View ESP32 schematic, RS-485 Modbus / IO-Link wiring, MQTT payload specs, and plant manager briefing script.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.showHardwareRoadmapDialog.value = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_open_hardware_roadmap")
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Hardware Specs & Presentation Script")
                    }
                }
            }
        }

        // Factory Reset Demo Data
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
                        text = "DEMO & PRESENTATION RESET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Resets all 6 sealer drums, simulated sensor telemetry, alarm logs, and transactions back to the initial deterministic presentation baseline.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetAllData() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_reset_all_data")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All Demo Data to Baseline")
                    }
                }
            }
        }
    }
}
