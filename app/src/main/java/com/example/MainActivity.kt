package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrumStatus
import com.example.ui.AppTab
import com.example.ui.SmartSealerViewModel
import com.example.ui.components.PersistentDemoPanel
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SmartSealerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSealerTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: SmartSealerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeTab by viewModel.activeTab.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val drums by viewModel.drums.collectAsState()

    val showAddDrum by viewModel.showAddDrumDialog.collectAsState()
    val showReceive by viewModel.showReceiveDialog.collectAsState()
    val showIssue by viewModel.showIssueDialog.collectAsState()
    val showReturnEmpty by viewModel.showReturnEmptyDialog.collectAsState()
    val showDrumDetail by viewModel.showDrumDetailDialog.collectAsState()
    val showHardwareRoadmap by viewModel.showHardwareRoadmapDialog.collectAsState()

    // Handle back button: return to Overview if in subscreen
    BackHandler(enabled = activeTab != AppTab.OVERVIEW) {
        viewModel.selectTab(AppTab.OVERVIEW)
    }

    // Toast listener
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Dialogs
    if (showAddDrum) {
        AddDrumDialog(viewModel = viewModel, onDismiss = { viewModel.showAddDrumDialog.value = false })
    }
    if (showReceive) {
        ReceiveMaterialDialog(viewModel = viewModel, onDismiss = { viewModel.showReceiveDialog.value = false })
    }
    if (showIssue) {
        IssueToLineDialog(viewModel = viewModel, onDismiss = { viewModel.showIssueDialog.value = false })
    }
    if (showReturnEmpty) {
        RecordEmptyDialog(viewModel = viewModel, onDismiss = { viewModel.showReturnEmptyDialog.value = false })
    }
    if (showDrumDetail) {
        DrumDetailDialog(viewModel = viewModel, onDismiss = { viewModel.showDrumDetailDialog.value = false })
    }
    if (showHardwareRoadmap) {
        HardwareRoadmapDialog(onDismiss = { viewModel.showHardwareRoadmapDialog.value = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = IndustrialNavyDark,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndustrialNavy)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = LaserCyan,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = IndustrialNavyDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Sealer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Automotive Body Shop IoT",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = LaserCyan,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "PLANT STATUS: NORMAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StatusNormal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                AppTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val badgeCount = when (tab) {
                        AppTab.ALERTS -> alerts.count { !it.isAcknowledged }
                        AppTab.DRUMS -> drums.count { it.status == DrumStatus.LOW || it.status == DrumStatus.CRITICAL }
                        else -> 0
                    }

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.OVERVIEW -> Icons.Default.Dashboard
                                    AppTab.DRUMS -> Icons.Default.ViewKanban
                                    AppTab.INVENTORY -> Icons.Default.Warehouse
                                    AppTab.ANALYTICS -> Icons.Default.BarChart
                                    AppTab.SIMULATION -> Icons.Default.Tune
                                    AppTab.SENSOR_HEALTH -> Icons.Default.Sensors
                                    AppTab.ALERTS -> Icons.Default.Notifications
                                    AppTab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = if (isSelected) LaserCyan else Color(0xFF94A3B8)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                            )
                        },
                        badge = {
                            if (badgeCount > 0) {
                                Surface(
                                    color = if (tab == AppTab.DRUMS) StatusLow else StatusCritical,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            viewModel.selectTab(tab)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = IndustrialNavy,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("nav_item_${tab.name.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = IndustrialNavyLight, thickness = 1.dp)

                // Bottom Drawer Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.showHardwareRoadmapDialog.value = true
                            scope.launch { drawerState.close() }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ESP32 Roadmap & Pitch",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Hardware wiring & SCADA specs",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = activeTab.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = activeTab.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = LaserCyan,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectTab(AppTab.ALERTS) }) {
                            val unackCount = alerts.count { !it.isAcknowledged }
                            BadgedBox(
                                badge = {
                                    if (unackCount > 0) {
                                        Badge(containerColor = StatusCritical) { Text("$unackCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = Color.White)
                            }
                        }

                        IconButton(onClick = { viewModel.showHardwareRoadmapDialog.value = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Roadmap & Pitch", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = IndustrialNavyDark
                    )
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Persistent Demo Control Panel
                    PersistentDemoPanel(viewModel = viewModel)

                    // Compact Mobile Bottom Navigation
                    NavigationBar(
                        containerColor = IndustrialNavy,
                        contentColor = Color.White,
                        tonalElevation = 4.dp,
                        modifier = Modifier.height(60.dp)
                    ) {
                        listOf(
                            AppTab.OVERVIEW to Icons.Default.Dashboard,
                            AppTab.DRUMS to Icons.Default.ViewKanban,
                            AppTab.SIMULATION to Icons.Default.Tune,
                            AppTab.INVENTORY to Icons.Default.Warehouse,
                            AppTab.ANALYTICS to Icons.Default.BarChart
                        ).forEach { (tab, icon) ->
                            val isSelected = activeTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) LaserCyan else Color(0xFF94A3B8)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) LaserCyan else Color(0xFF94A3B8)
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LaserCyan,
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    indicatorColor = IndustrialNavyDark
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            containerColor = DashboardBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    AppTab.OVERVIEW -> OverviewScreen(viewModel = viewModel)
                    AppTab.DRUMS -> DrumMonitoringScreen(viewModel = viewModel)
                    AppTab.INVENTORY -> InventoryManagementScreen(viewModel = viewModel)
                    AppTab.ANALYTICS -> ConsumptionAnalyticsScreen(viewModel = viewModel)
                    AppTab.SIMULATION -> SensorSimulationScreen(viewModel = viewModel)
                    AppTab.SENSOR_HEALTH -> SensorHealthScreen(viewModel = viewModel)
                    AppTab.ALERTS -> AlertsScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
