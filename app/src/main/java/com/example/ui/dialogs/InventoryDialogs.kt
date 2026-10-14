package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Drum
import com.example.ui.SmartSealerViewModel
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.IndustrialNavy

@Composable
fun AddDrumDialog(
    viewModel: SmartSealerViewModel,
    onDismiss: () -> Unit
) {
    var drumId by remember { mutableStateOf("SD-007") }
    var sealerType by remember { mutableStateOf("Structural Epoxy EP-500") }
    var batchNo by remember { mutableStateOf("LOT-2026-N901") }
    var capacityText by remember { mutableStateOf("200") }
    var massText by remember { mutableStateOf("200") }
    var location by remember { mutableStateOf("Cell 6 - Rear Floor Sealer") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Sealer Drum", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = drumId,
                    onValueChange = { drumId = it },
                    label = { Text("Drum ID (e.g. SD-007)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_add_drum_id")
                )
                OutlinedTextField(
                    value = sealerType,
                    onValueChange = { sealerType = it },
                    label = { Text("Sealer Chemical Material / Grade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = batchNo,
                    onValueChange = { batchNo = it },
                    label = { Text("Manufacturer Batch / Lot No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Capacity (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = massText,
                        onValueChange = { massText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Initial Mass (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Line / Cell Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacityText.toDoubleOrNull() ?: 200.0
                    val mass = massText.toDoubleOrNull() ?: cap
                    viewModel.addDrum(drumId, sealerType, batchNo, cap, mass, location)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                modifier = Modifier.testTag("btn_confirm_add_drum")
            ) {
                Text("Register Drum")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMaterialDialog(
    viewModel: SmartSealerViewModel,
    onDismiss: () -> Unit
) {
    val drums by viewModel.drums.collectAsState()
    var selectedDrumId by remember { mutableStateOf(drums.firstOrNull()?.id ?: "") }
    var qtyText by remember { mutableStateOf("200") }
    var batchNo by remember { mutableStateOf("LOT-NEW-RCV") }
    var notes by remember { mutableStateOf("Delivered via Sika Automotive logistics") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Material Shipment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select target drum / lot entry to receive incoming sealer:", style = MaterialTheme.typography.bodySmall)

                // Select Drum Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    drums.take(4).forEach { d ->
                        FilterChip(
                            selected = selectedDrumId == d.id,
                            onClick = { selectedDrumId = d.id },
                            label = { Text(d.id) }
                        )
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Received Quantity (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_receive_qty")
                )

                OutlinedTextField(
                    value = batchNo,
                    onValueChange = { batchNo = it },
                    label = { Text("Supplier Lot / Certificate No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Receiving Notes / Delivery Docket") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toDoubleOrNull() ?: 0.0
                    if (qty > 0 && selectedDrumId.isNotBlank()) {
                        viewModel.receiveMaterial(selectedDrumId, qty, batchNo, notes)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.testTag("btn_confirm_receive_material")
            ) {
                Text("Post Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IssueToLineDialog(
    viewModel: SmartSealerViewModel,
    onDismiss: () -> Unit
) {
    val drums by viewModel.drums.collectAsState()
    var selectedDrumId by remember { mutableStateOf(drums.firstOrNull()?.id ?: "") }
    var targetLocation by remember { mutableStateOf("Cell 1 - BIW Framing") }

    val locations = listOf(
        "Cell 1 - BIW Framing",
        "Cell 2 - Underbody Sealer",
        "Cell 3 - Door Hemming",
        "Cell 4 - Windshield Station",
        "Cell 5 - Roof Sealer"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Issue Drum to Production Line", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Transfer a verified full drum from storage staging into an active robotic dispensing cell:",
                    style = MaterialTheme.typography.bodySmall
                )

                // Select Drum
                Text("Select Drum:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    drums.take(4).forEach { d ->
                        FilterChip(
                            selected = selectedDrumId == d.id,
                            onClick = { selectedDrumId = d.id },
                            label = { Text(d.id) }
                        )
                    }
                }

                Text("Target Robotic Cell:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    locations.forEach { loc ->
                        Surface(
                            onClick = { targetLocation = loc },
                            color = if (targetLocation == loc) Color(0xFFEFF6FF) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = targetLocation == loc,
                                    onClick = { targetLocation = loc }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(loc, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDrumId.isNotBlank()) {
                        viewModel.issueDrumToLine(selectedDrumId, targetLocation)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy),
                modifier = Modifier.testTag("btn_confirm_issue_line")
            ) {
                Text("Issue to Line")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordEmptyDialog(
    viewModel: SmartSealerViewModel,
    onDismiss: () -> Unit
) {
    val drums by viewModel.drums.collectAsState()
    var selectedDrumId by remember { mutableStateOf(drums.find { it.currentMassKg < 20.0 }?.id ?: drums.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Empty Drum Return", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Log an exhausted sealer drum removed from the dispensing ram and returned to staging/recycling bay for supplier credit:",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("Select Exhausted Drum:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    drums.take(4).forEach { d ->
                        FilterChip(
                            selected = selectedDrumId == d.id,
                            onClick = { selectedDrumId = d.id },
                            label = { Text("${d.id} (${d.currentMassKg}kg)") }
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Packaging Tare Weight (15 kg) is automatically excluded from active chemical sealer inventory calculations.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDrumId.isNotBlank()) {
                        viewModel.returnEmptyDrum(selectedDrumId)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                modifier = Modifier.testTag("btn_confirm_record_empty")
            ) {
                Text("Confirm Empty Return")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
