package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SmartSealerViewModel
import com.example.ui.theme.*

@Composable
fun PersistentDemoPanel(
    viewModel: SmartSealerViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val drums by viewModel.drums.collectAsState()
    val isExpanded by viewModel.isDemoBarExpanded.collectAsState()
    val selectedDrumId by viewModel.simulationDrumId.collectAsState()

    Surface(
        color = IndustrialNavyDark,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialNavyLight),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleDemoBar() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (settings.isSimulationRunning) StatusNormal else StatusLow,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEMO CONTROLS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LaserCyan,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (settings.isSimulationRunning) "(SIM RUNNING)" else "(SIM PAUSED)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (settings.isSimulationRunning) StatusNormal else StatusLow,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = IndustrialNavy,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Target: $selectedDrumId",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleSimulation() },
                        modifier = Modifier.size(28.dp).testTag("quick_toggle_sim")
                    ) {
                        Icon(
                            imageVector = if (settings.isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Simulation",
                            tint = if (settings.isSimulationRunning) StatusLow else StatusNormal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand/Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    // Drum quick selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        drums.forEach { d ->
                            val isSelected = d.id == selectedDrumId
                            Surface(
                                onClick = { viewModel.setSimulationDrum(d.id) },
                                color = if (isSelected) AccentBlue else IndustrialNavy,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = d.id,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.consumeMaterial(selectedDrumId, 5.0) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IndustrialNavy,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("quick_consume_5")
                        ) {
                            Text("−5 kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        FilledTonalButton(
                            onClick = { viewModel.consumeMaterial(selectedDrumId, 10.0) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IndustrialNavy,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("quick_consume_10")
                        ) {
                            Text("−10 kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        FilledTonalButton(
                            onClick = { viewModel.refillDrum(selectedDrumId, 20.0) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AccentTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("quick_refill_20")
                        ) {
                            Text("+20 kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = { viewModel.resetDrum(selectedDrumId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("quick_reset_drum")
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetAllData() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1.2f).height(32.dp).testTag("quick_reset_all")
                        ) {
                            Text("Reset All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    }
}
