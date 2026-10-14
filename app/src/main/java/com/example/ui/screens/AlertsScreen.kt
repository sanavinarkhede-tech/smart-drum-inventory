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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Alert
import com.example.model.AlertSeverity
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun AlertsScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val totalStockKg = drums.sumOf { it.currentMassKg }
    val reorderPointKg = settings.reorderPointKg
    val isBelowReorder = totalStockKg < reorderPointKg
    val suggestedReplenishmentKg = settings.calculateSuggestedReplenishment(totalStockKg)
    val daysRemaining = settings.calculateDaysRemaining(totalStockKg)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("alerts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Reorder Point & Safety Stock Analytics Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isBelowReorder) StatusCritical else CardBorder,
                        RoundedCornerShape(14.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBelowReorder) Color(0xFFFEF2F2) else CardBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTOMATED REORDER POINT CALCULATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isBelowReorder) StatusCritical else IndustrialNavy,
                                letterSpacing = 0.8.sp
                            )
                        )
                        if (isBelowReorder) {
                            Surface(
                                color = StatusCritical,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "REORDER REQUIRED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reorder Point = (Avg Daily Consumption × Supplier Lead Time) + Safety Stock",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Stock", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            Text(
                                "${(totalStockKg * 10).roundToInt() / 10.0} kg",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBelowReorder) StatusCritical else TextPrimary
                                )
                            )
                        }
                        Column {
                            Text("Reorder Threshold", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            Text(
                                "${reorderPointKg.toInt()} kg",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialNavy
                                )
                            )
                        }
                        Column {
                            Text("Suggested Order", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            Text(
                                "${(suggestedReplenishmentKg * 10).roundToInt() / 10.0} kg",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = CardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Run Rate: ${settings.avgDailyConsumptionEstimate} kg/day • Lead Time: ${settings.supplierLeadTimeDays} days • Safety: ${settings.safetyStockKg} kg",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        // Notification Controls Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IN-APP EVENT LOG & ALERTS (${alerts.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )

                if (alerts.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllAlerts() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Notice about in-app alerting
        item {
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notice: Events and notifications are processed locally in-app. External SMS/Email dispatch requires industrial SCADA gateway configuration.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Alerts List
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Active Alerts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("All monitored sealer drums and sensor nodes are operational.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertCardItem(
                    alert = alert,
                    onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                )
            }
        }
    }
}

@Composable
fun AlertCardItem(
    alert: Alert,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    val timeStr = sdf.format(Date(alert.timestampMillis))

    val (cardBg, borderColor, iconTint) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(StatusCriticalBg, StatusCritical, StatusCritical)
        AlertSeverity.WARNING -> Triple(StatusLowBg, StatusLow, StatusLow)
        AlertSeverity.INFO -> Triple(CardBackground, CardBorder, AccentBlue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (alert.severity) {
                            AlertSeverity.CRITICAL -> Icons.Default.ReportProblem
                            AlertSeverity.WARNING -> Icons.Default.Warning
                            AlertSeverity.INFO -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (alert.severity == AlertSeverity.CRITICAL) StatusCritical else TextPrimary
                        )
                    )
                }

                if (!alert.isAcknowledged) {
                    FilledTonalButton(
                        onClick = onAcknowledge,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Ack", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                } else {
                    Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "ACKNOWLEDGED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (alert.drumId != null) {
                    Text(
                        text = "Target Drum: ${alert.drumId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                } else {
                    Text(text = "System-Wide Notice", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                )
            }
        }
    }
}
