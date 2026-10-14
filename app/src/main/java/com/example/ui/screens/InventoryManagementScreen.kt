package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.InventoryTransaction
import com.example.model.TransactionType
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.SimulatedDataBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementScreen(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val drums by viewModel.drums.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var txSearchQuery by remember { mutableStateOf("") }
    var selectedTxType by remember { mutableStateOf<TransactionType?>(null) }

    // Inventory Ledger Calculations
    val openingStockKg = 1200.0 // Baseline plant opening stock
    val receiptsKg = transactions.filter { it.type == TransactionType.RECEIPT || it.type == TransactionType.REFILL }
        .sumOf { if (it.deltaKg > 0) it.deltaKg else 0.0 }
    val consumptionKg = transactions.filter { it.type == TransactionType.CONSUMPTION }
        .sumOf { kotlin.math.abs(it.deltaKg) }
    val lossesKg = 0.0
    val usableReturnsKg = 0.0
    val totalRemainingDrumsStock = drums.sumOf { it.currentMassKg }
    val calculatedClosingStock = openingStockKg + receiptsKg - consumptionKg - lossesKg + usableReturnsKg

    val filteredTransactions = remember(transactions, txSearchQuery, selectedTxType) {
        transactions.filter { tx ->
            val matchesSearch = tx.drumId.contains(txSearchQuery, ignoreCase = true) ||
                    tx.notes.contains(txSearchQuery, ignoreCase = true) ||
                    tx.operator.contains(txSearchQuery, ignoreCase = true)
            val matchesType = selectedTxType == null || tx.type == selectedTxType
            matchesSearch && matchesType
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimulatedDataBanner()
        }

        // Inventory Formula & Closing Stock Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavyDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MATERIAL INVENTORY EQUATION (MASS CONSERVATION)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LaserCyan,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Closing Stock = Opening + Receipts − Consumption − Losses + Usable Returns",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Measured Stock",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "${(totalRemainingDrumsStock * 10).roundToInt() / 10.0} kg",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Packaging Exclusion",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "Tare excluded",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusNormal
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = IndustrialNavyLight, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EquationBreakdownItem("Opening", "${openingStockKg.toInt()} kg")
                        EquationBreakdownItem("Receipts", "+${(receiptsKg * 10).roundToInt() / 10.0} kg", StatusNormal)
                        EquationBreakdownItem("Consumed", "−${(consumptionKg * 10).roundToInt() / 10.0} kg", AccentBlueLight)
                        EquationBreakdownItem("Losses", "0.0 kg")
                    }
                }
            }
        }

        // Action Buttons Grid (Forms Launchers)
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
                        text = "MATERIAL MOVEMENT & LOGISTICS FORMS",
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
                        Button(
                            onClick = { viewModel.showAddDrumDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_form_add_drum")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Drum", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.showReceiveDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_form_receive_material")
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receive", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showIssueDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_form_issue_line")
                        ) {
                            Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Issue Line", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.showReturnEmptyDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_form_return_empty")
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return Empty", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.exportCsv() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_csv")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Transaction Ledger to CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search & Filter for Ledger
        item {
            OutlinedTextField(
                value = txSearchQuery,
                onValueChange = { txSearchQuery = it },
                label = { Text("Filter transactions by Drum, operator, or notes...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Transaction Type Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTxType == null,
                    onClick = { selectedTxType = null },
                    label = { Text("All (${transactions.size})") }
                )
                TransactionType.values().forEach { t ->
                    FilterChip(
                        selected = selectedTxType == t,
                        onClick = { selectedTxType = if (selectedTxType == t) null else t },
                        label = { Text(t.name.replace("_", " ")) }
                    )
                }
            }
        }

        // Transaction History Ledger
        items(filteredTransactions, key = { it.id }) { tx ->
            TransactionRowItem(tx = tx)
        }
    }
}

@Composable
private fun EquationBreakdownItem(label: String, value: String, valueColor: Color = Color.White) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun TransactionRowItem(tx: InventoryTransaction, modifier: Modifier = Modifier) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val dateStr = sdf.format(Date(tx.timestampMillis))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when (tx.type) {
                            TransactionType.RECEIPT, TransactionType.REFILL -> StatusNormalBg
                            TransactionType.CONSUMPTION -> AccentBlue.copy(alpha = 0.12f)
                            TransactionType.ISSUE_TO_LINE -> Color(0xFFEFF6FF)
                            TransactionType.RETURN_EMPTY -> StatusEmptyBg
                            else -> Color(0xFFF1F5F9)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (tx.type) {
                                TransactionType.RECEIPT, TransactionType.REFILL -> StatusNormal.copy(alpha = 0.3f)
                                TransactionType.CONSUMPTION -> AccentBlue.copy(alpha = 0.3f)
                                TransactionType.ISSUE_TO_LINE -> AccentTeal.copy(alpha = 0.3f)
                                else -> CardBorder
                            }
                        )
                    ) {
                        Text(
                            text = tx.type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (tx.type) {
                                    TransactionType.RECEIPT, TransactionType.REFILL -> StatusNormal
                                    TransactionType.CONSUMPTION -> AccentBlue
                                    TransactionType.ISSUE_TO_LINE -> AccentTeal
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drum: ${tx.drumId}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (tx.deltaKg != 0.0) {
                    Text(
                        text = "${if (tx.deltaKg > 0) "+" else ""}${tx.deltaKg} kg",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (tx.deltaKg > 0) StatusNormal else TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tx.notes,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Op: ${tx.operator}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                )
                Text(
                    text = "Remaining after: ${tx.remainingAfterKg} kg • $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                )
            }
        }
    }
}
