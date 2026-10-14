package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.DailyConsumptionBarChart
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.RealtimeStockTrendChart
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class AnalyticsTimeframe {
    HOURLY_24H,
    DAILY_7D,
    WEEKLY_30D
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionAnalyticsScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val consumptionRecords by viewModel.consumptionRecords.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var timeframe by remember { mutableStateOf(AnalyticsTimeframe.HOURLY_24H) }
    var filterDrumId by remember { mutableStateOf<String?>(null) }

    val filteredRecords = remember(consumptionRecords, filterDrumId) {
        if (filterDrumId == null) consumptionRecords
        else consumptionRecords.filter { it.drumId == filterDrumId || it.drumId == "ACTIVE_LINE" }
    }

    val totalConsumed = filteredRecords.sumOf { it.quantityKg }
    val avgPerHour = if (filteredRecords.isNotEmpty()) totalConsumed / filteredRecords.size else 0.0
    val avgPerShift = avgPerHour * 8.0
    val totalRemainingDrumsStock = drums.sumOf { it.currentMassKg }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("consumption_analytics_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Timeframe & Filter Chips
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
                        text = "ANALYTICS TIMEFRAME & DRUM FILTER",
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
                        listOf(
                            AnalyticsTimeframe.HOURLY_24H to "24 Hours (Hourly)",
                            AnalyticsTimeframe.DAILY_7D to "7 Days (Daily)",
                            AnalyticsTimeframe.WEEKLY_30D to "30 Days (Weekly)"
                        ).forEach { (tf, label) ->
                            FilterChip(
                                selected = timeframe == tf,
                                onClick = { timeframe = tf },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndustrialNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterDrumId == null,
                            onClick = { filterDrumId = null },
                            label = { Text("All Plant Drums") }
                        )
                        drums.forEach { d ->
                            FilterChip(
                                selected = filterDrumId == d.id,
                                onClick = { filterDrumId = if (filterDrumId == d.id) null else d.id },
                                label = { Text(d.id) }
                            )
                        }
                    }
                }
            }
        }

        // Analytical Rate KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = "Avg Hourly Rate",
                    value = "${(avgPerHour * 10).roundToInt() / 10.0}",
                    unit = "kg/h",
                    subtitle = "Automotive line active",
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp)) },
                    accentColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "Avg Shift Rate",
                    value = "${(avgPerShift * 10).roundToInt() / 10.0}",
                    unit = "kg/8h",
                    subtitle = "1 shift production",
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp)) },
                    accentColor = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hourly / Period Consumption Bar Chart
        item {
            val bars = when (timeframe) {
                AnalyticsTimeframe.HOURLY_24H -> {
                    filteredRecords.takeLast(12).map { "${it.hourOfDay}:00" to it.quantityKg }
                }
                AnalyticsTimeframe.DAILY_7D -> {
                    listOf(
                        "Mon" to 82.4,
                        "Tue" to 91.0,
                        "Wed" to 88.5,
                        "Thu" to 79.2,
                        "Fri" to 94.6,
                        "Sat" to 42.1,
                        "Sun" to 12.0
                    )
                }
                AnalyticsTimeframe.WEEKLY_30D -> {
                    listOf(
                        "Wk 38" to 580.0,
                        "Wk 39" to 610.0,
                        "Wk 40" to 595.0,
                        "Wk 41" to 630.0
                    )
                }
            }
            DailyConsumptionBarChart(bars = bars)
        }

        // Inventory Depletion Curve
        item {
            val simulatedTrend = listOf(
                totalRemainingDrumsStock.toFloat() + 50f,
                totalRemainingDrumsStock.toFloat() + 42f,
                totalRemainingDrumsStock.toFloat() + 31f,
                totalRemainingDrumsStock.toFloat() + 24f,
                totalRemainingDrumsStock.toFloat() + 18f,
                totalRemainingDrumsStock.toFloat() + 7f,
                totalRemainingDrumsStock.toFloat()
            )
            RealtimeStockTrendChart(
                dataPoints = simulatedTrend,
                capacityKg = (drums.sumOf { it.nominalCapacityKg }).toFloat()
            )
        }

        // Consumption Breakdown by Sealer Type
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
                        text = "CONSUMPTION BY MATERIAL / DRUM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    drums.forEach { drum ->
                        val consumed = drum.consumedMassKg
                        val fraction = if (drum.nominalCapacityKg > 0) (consumed / drum.nominalCapacityKg).toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${drum.id} - ${drum.sealerType}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(consumed * 10).roundToInt() / 10.0} kg (${(fraction * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = AccentBlue,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}
