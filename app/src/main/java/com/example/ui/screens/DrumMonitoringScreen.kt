package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.Drum
import com.example.model.DrumStatus
import com.example.model.SensorHealth
import com.example.model.SensorReading
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.DrumStatusBadge
import com.example.ui.components.SealerDrumVisual
import com.example.ui.components.SensorHealthBadge
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumMonitoringScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val searchQuery by viewModel.drumSearchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

    // Distinct sealer types for filter chips
    val allTypes = remember(drums) { drums.map { it.sealerType }.distinct() }

    // Filtered drums
    val filteredDrums = remember(drums, searchQuery, statusFilter, selectedTypeFilter) {
        drums.filter { drum ->
            val matchesSearch = drum.id.contains(searchQuery, ignoreCase = true) ||
                    drum.sealerType.contains(searchQuery, ignoreCase = true) ||
                    drum.location.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == null || drum.status == statusFilter
            val matchesType = selectedTypeFilter == null || drum.sealerType == selectedTypeFilter
            matchesSearch && matchesStatus && matchesType
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("drum_monitoring_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Search Bar & Filter Controls
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setDrumSearchQuery(it) },
                label = { Text("Search drums by ID, type, or cell location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setDrumSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drum_search_input")
            )
        }

        // Status Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All (${drums.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialNavy,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )
                DrumStatus.values().forEach { st ->
                    val count = drums.count { it.status == st }
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { viewModel.setStatusFilter(if (statusFilter == st) null else st) },
                        label = { Text("${st.name} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (st) {
                                DrumStatus.NORMAL -> StatusNormal
                                DrumStatus.LOW -> StatusLow
                                DrumStatus.CRITICAL -> StatusCritical
                                DrumStatus.EMPTY -> StatusEmpty
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Material Type Filter Chips Row
        if (allTypes.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTypes.forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                            label = { Text(type, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.15f),
                                selectedLabelColor = AccentBlue
                            )
                        )
                    }
                }
            }
        }

        // Results count summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONITORED SEALER DRUMS (${filteredDrums.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
                TextButton(onClick = { viewModel.showAddDrumDialog.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Drum", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Drum Cards
        items(filteredDrums, key = { it.id }) { drum ->
            val reading = sensorReadings[drum.id]
            DrumCard(
                drum = drum,
                reading = reading,
                onInspect = {
                    viewModel.selectDrum(drum.id)
                    viewModel.showDrumDetailDialog.value = true
                },
                onConsume = { amount -> viewModel.consumeMaterial(drum.id, amount) },
                onRefill = { amount -> viewModel.refillDrum(drum.id, amount) },
                onReset = { viewModel.resetDrum(drum.id) }
            )
        }
    }
}

@Composable
fun DrumCard(
    drum: Drum,
    reading: SensorReading?,
    onInspect: () -> Unit,
    onConsume: (Double) -> Unit,
    onRefill: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val lastUpdateStr = sdf.format(Date(drum.lastUpdatedMillis))

    val distanceDisplay = reading?.let {
        if (it.health == SensorHealth.ONLINE) "${it.rawDistanceCm} cm"
        else "FAULT / ${it.rawDistanceCm} cm"
    } ?: "${(drum.theoreticalDistanceCm * 10).roundToInt() / 10.0} cm"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .testTag("drum_card_${drum.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: ID + Status + Sensor Health
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = IndustrialNavy,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = drum.id,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = drum.sealerType,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Batch: ${drum.batchNumber} • ${drum.location}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }
                DrumStatusBadge(status = drum.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Visual Drum Graphic + Telemetry Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-fidelity drum illustration
                SealerDrumVisual(
                    fillPercent = drum.remainingPercentage,
                    distanceCm = reading?.rawDistanceCm ?: drum.theoreticalDistanceCm,
                    status = drum.status,
                    health = drum.sensorHealth,
                    width = 80.dp,
                    height = 120.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Telemetry Metrics Grid
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Level progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "SEALER LEVEL",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                        )
                        Text(
                            text = "${drum.remainingPercentage.toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (drum.status) {
                                    DrumStatus.NORMAL -> TextPrimary
                                    DrumStatus.LOW -> StatusLow
                                    DrumStatus.CRITICAL -> StatusCritical
                                    DrumStatus.EMPTY -> StatusEmpty
                                }
                            )
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (drum.remainingPercentage / 100.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = when (drum.status) {
                            DrumStatus.NORMAL -> AccentTeal
                            DrumStatus.LOW -> StatusLow
                            DrumStatus.CRITICAL -> StatusCritical
                            DrumStatus.EMPTY -> StatusEmpty
                        },
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    MetricMiniRow(
                        label = "Remaining Sealer:",
                        value = "${drum.currentMassKg} kg / ${drum.nominalCapacityKg} kg"
                    )
                    MetricMiniRow(
                        label = "Consumed Material:",
                        value = "${(drum.consumedMassKg * 10).roundToInt() / 10.0} kg"
                    )
                    MetricMiniRow(
                        label = "Laser Sensor Distance:",
                        value = distanceDisplay,
                        highlight = LaserCyan
                    )
                    MetricMiniRow(
                        label = "Sensor Telemetry:",
                        value = drum.sensorHealth.name,
                        highlight = if (drum.sensorHealth == SensorHealth.ONLINE) StatusNormal else StatusFault
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Last updated + Quick test actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updated: $lastUpdateStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onConsume(5.0) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_consume_5_${drum.id}")
                    ) {
                        Text("−5 kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    FilledTonalButton(
                        onClick = { onRefill(20.0) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_refill_20_${drum.id}")
                    ) {
                        Text("+20 kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_inspect_${drum.id}")
                    ) {
                        Text("Details", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniRow(label: String, value: String, highlight: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = highlight ?: TextPrimary
            )
        )
    }
}
