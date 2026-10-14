package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AlertSeverity
import com.example.model.DrumStatus
import com.example.ui.AppTab
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val consumptionRecords by viewModel.consumptionRecords.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Calculations
    val totalDrums = drums.size
    val totalRemainingKg = drums.sumOf { it.currentMassKg }
    val totalCapacityKg = drums.sumOf { it.nominalCapacityKg }
    val avgPercentage = if (totalCapacityKg > 0) (totalRemainingKg / totalCapacityKg) * 100.0 else 0.0

    val todayRecords = consumptionRecords.takeLast(24)
    val todayConsumptionKg = todayRecords.sumOf { it.quantityKg }

    val lowCount = drums.count { it.status == DrumStatus.LOW }
    val criticalCount = drums.count { it.status == DrumStatus.CRITICAL }
    val lowOrCriticalCount = lowCount + criticalCount
    val estimatedCoverageDays = settings.calculateDaysRemaining(totalRemainingKg)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("overview_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simulated Data Banner
        item {
            SimulatedDataBanner()
        }

        // Hero Card with industrial background illustration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavyDark)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    // Try to display hero banner illustration
                    Image(
                        painter = painterResource(id = R.drawable.img_sealer_hero),
                        contentDescription = "Automotive Sealer Dispensing Line",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        IndustrialNavyDark.copy(alpha = 0.92f),
                                        IndustrialNavyDark.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = LaserCyan.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LaserCyan)
                        ) {
                            Text(
                                text = "AUTOMOTIVE BODY SHOP SEALER MONITOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LaserCyan,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Smart Sealer Inventory",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real-time laser follower-plate tracking & consumption forecasting",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }
        }

        // KPI Summary Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Total Drums",
                        value = "$totalDrums",
                        unit = "units",
                        subtitle = "${drums.count { it.isLineActive }} active on line",
                        icon = {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        },
                        accentColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Remaining Stock",
                        value = "${(totalRemainingKg * 10).roundToInt() / 10.0}",
                        unit = "kg",
                        subtitle = "Avg ${avgPercentage.toInt()}% full",
                        icon = {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                        },
                        accentColor = AccentTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Today Consumed",
                        value = "${(todayConsumptionKg * 10).roundToInt() / 10.0}",
                        unit = "kg",
                        subtitle = "Simulated run rate",
                        icon = {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(18.dp))
                        },
                        accentColor = LaserCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Stock Coverage",
                        value = "${(estimatedCoverageDays * 10).roundToInt() / 10.0}",
                        unit = "days",
                        subtitle = if (totalRemainingKg < settings.reorderPointKg) "BELOW REORDER POINT!" else "Normal supply buffer",
                        icon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (totalRemainingKg < settings.reorderPointKg) StatusCritical else AccentBlue, modifier = Modifier.size(18.dp))
                        },
                        accentColor = if (totalRemainingKg < settings.reorderPointKg) StatusCritical else AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (lowOrCriticalCount > 0) {
                    // Urgent Low-Stock Alert Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectTab(AppTab.DRUMS) },
                        colors = CardDefaults.cardColors(containerColor = if (criticalCount > 0) StatusCriticalBg else StatusLowBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (criticalCount > 0) StatusCritical else StatusLow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (criticalCount > 0) StatusCritical else StatusLow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (criticalCount > 0) "$criticalCount DRUM(S) CRITICAL & $lowCount LOW!" else "$lowCount DRUM(S) RUNNING LOW",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (criticalCount > 0) StatusCritical else Color(0xFFB45309)
                                    )
                                )
                                Text(
                                    text = "Tap to view drum levels or trigger replenishment simulation.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Charts Section
        item {
            // Trend curve
            val trendPoints = listOf(
                totalRemainingKg.toFloat() + 45f,
                totalRemainingKg.toFloat() + 38f,
                totalRemainingKg.toFloat() + 29f,
                totalRemainingKg.toFloat() + 22f,
                totalRemainingKg.toFloat() + 15f,
                totalRemainingKg.toFloat() + 8f,
                totalRemainingKg.toFloat()
            )
            RealtimeStockTrendChart(
                dataPoints = trendPoints,
                capacityKg = (totalCapacityKg.toFloat()).coerceAtLeast(100f)
            )
        }

        item {
            // Hourly consumption bars
            val hourlyBars = consumptionRecords.takeLast(10).map {
                "${it.hourOfDay}:00" to it.quantityKg
            }
            DailyConsumptionBarChart(bars = hourlyBars)
        }

        // Drum Status Distribution
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
                        text = "DRUM STATUS BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        val normalCount = drums.count { it.status == DrumStatus.NORMAL }
                        val emptyCount = drums.count { it.status == DrumStatus.EMPTY }
                        val safeTotal = totalDrums.coerceAtLeast(1).toFloat()

                        if (normalCount > 0) {
                            Box(modifier = Modifier.weight(normalCount / safeTotal).fillMaxHeight().background(StatusNormal))
                        }
                        if (lowCount > 0) {
                            Box(modifier = Modifier.weight(lowCount / safeTotal).fillMaxHeight().background(StatusLow))
                        }
                        if (criticalCount > 0) {
                            Box(modifier = Modifier.weight(criticalCount / safeTotal).fillMaxHeight().background(StatusCritical))
                        }
                        if (emptyCount > 0) {
                            Box(modifier = Modifier.weight(emptyCount / safeTotal).fillMaxHeight().background(StatusEmpty))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusLegendItem("Normal", drums.count { it.status == DrumStatus.NORMAL }, StatusNormal)
                        StatusLegendItem("Low", lowCount, StatusLow)
                        StatusLegendItem("Critical", criticalCount, StatusCritical)
                        StatusLegendItem("Empty", drums.count { it.status == DrumStatus.EMPTY }, StatusEmpty)
                    }
                }
            }
        }

        // Recent Activity Feed
        item {
            Text(
                text = "RECENT INVENTORY TRANSACTIONS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IndustrialNavy
                )
            )
        }

        items(transactions.take(4)) { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.US)
                    val timeStr = sdf.format(Date(tx.timestampMillis))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                when (tx.type) {
                                    com.example.model.TransactionType.RECEIPT, com.example.model.TransactionType.REFILL -> StatusNormalBg
                                    com.example.model.TransactionType.CONSUMPTION -> AccentBlue.copy(alpha = 0.12f)
                                    com.example.model.TransactionType.ISSUE_TO_LINE -> Color(0xFFEFF6FF)
                                    com.example.model.TransactionType.RETURN_EMPTY -> StatusEmptyBg
                                    else -> Color(0xFFF1F5F9)
                                },
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tx.type) {
                                com.example.model.TransactionType.RECEIPT, com.example.model.TransactionType.REFILL -> Icons.Default.Add
                                com.example.model.TransactionType.CONSUMPTION -> Icons.Default.Remove
                                com.example.model.TransactionType.ISSUE_TO_LINE -> Icons.Default.PrecisionManufacturing
                                com.example.model.TransactionType.RETURN_EMPTY -> Icons.Default.AssignmentReturn
                                else -> Icons.Default.Sync
                            },
                            contentDescription = null,
                            tint = when (tx.type) {
                                com.example.model.TransactionType.RECEIPT, com.example.model.TransactionType.REFILL -> StatusNormal
                                com.example.model.TransactionType.CONSUMPTION -> AccentBlue
                                com.example.model.TransactionType.ISSUE_TO_LINE -> AccentTeal
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tx.type.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = tx.drumId,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = tx.notes,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (tx.deltaKg != 0.0) {
                            Text(
                                text = "${if (tx.deltaKg > 0) "+" else ""}${tx.deltaKg} kg",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.deltaKg > 0) StatusNormal else TextPrimary
                                )
                            )
                        }
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
        )
    }
}
