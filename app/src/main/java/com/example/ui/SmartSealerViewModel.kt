package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InventoryRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val subtitle: String) {
    OVERVIEW("Overview", "Plant Level Summary & KPIs"),
    DRUMS("Drum Monitoring", "Real-Time Follower Plate Levels"),
    INVENTORY("Inventory Management", "Stock Movement, Receipts & Issues"),
    ANALYTICS("Consumption Analytics", "Shift & Material Consumption Trends"),
    SIMULATION("Sensor Simulation", "Laser Follower-Plate Physics & Testing"),
    SENSOR_HEALTH("Sensor Health & Faults", "IoT Telemetry Diagnostics & Fault Injection"),
    ALERTS("Alerts & Reorder", "Threshold Warnings & Replenishment"),
    SETTINGS("Settings", "Thresholds & Industrial Calibrations")
}

class SmartSealerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = InventoryRepository(application.applicationContext)

    val drums = repository.drums
    val sensorReadings = repository.sensorReadings
    val transactions = repository.transactions
    val consumptionRecords = repository.consumptionRecords
    val alerts = repository.alerts
    val settings = repository.settings

    private val _activeTab = MutableStateFlow(AppTab.OVERVIEW)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _drumSearchQuery = MutableStateFlow("")
    val drumSearchQuery: StateFlow<String> = _drumSearchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<DrumStatus?>(null)
    val statusFilter: StateFlow<DrumStatus?> = _statusFilter.asStateFlow()

    private val _selectedDrumId = MutableStateFlow<String?>("SD-001")
    val selectedDrumId: StateFlow<String?> = _selectedDrumId.asStateFlow()

    private val _simulationDrumId = MutableStateFlow<String>("SD-001")
    val simulationDrumId: StateFlow<String> = _simulationDrumId.asStateFlow()

    private val _isDemoBarExpanded = MutableStateFlow(true)
    val isDemoBarExpanded: StateFlow<Boolean> = _isDemoBarExpanded.asStateFlow()

    // Dialogs
    val showAddDrumDialog = MutableStateFlow(false)
    val showReceiveDialog = MutableStateFlow(false)
    val showIssueDialog = MutableStateFlow(false)
    val showReturnEmptyDialog = MutableStateFlow(false)
    val showDrumDetailDialog = MutableStateFlow(false)
    val showHardwareRoadmapDialog = MutableStateFlow(false)

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setDrumSearchQuery(q: String) {
        _drumSearchQuery.value = q
    }

    fun setStatusFilter(status: DrumStatus?) {
        _statusFilter.value = status
    }

    fun selectDrum(id: String) {
        _selectedDrumId.value = id
    }

    fun setSimulationDrum(id: String) {
        _simulationDrumId.value = id
    }

    fun toggleDemoBar() {
        _isDemoBarExpanded.value = !_isDemoBarExpanded.value
    }

    // Quick Actions
    fun consumeMaterial(drumId: String, amountKg: Double) {
        val success = repository.consumeMaterial(drumId, amountKg)
        if (success) {
            _toastMessage.tryEmit("Consumed $amountKg kg from drum $drumId")
        }
    }

    fun refillDrum(drumId: String, amountKg: Double) {
        val success = repository.refillDrum(drumId, amountKg)
        if (success) {
            _toastMessage.tryEmit("Refilled $amountKg kg into drum $drumId")
        }
    }

    fun resetDrum(drumId: String) {
        val success = repository.resetDrumToFull(drumId)
        if (success) {
            _toastMessage.tryEmit("Reset drum $drumId to full capacity (200 kg)")
        }
    }

    fun resetAllData() {
        repository.resetAllData()
        _toastMessage.tryEmit("All demo data and drums reset to factory baseline")
    }

    fun toggleSimulation() {
        val newState = !settings.value.isSimulationRunning
        repository.toggleSimulation(newState)
        _toastMessage.tryEmit(if (newState) "Simulation active (running)" else "Simulation paused")
    }

    fun setSensorFault(drumId: String, faultType: FaultType) {
        repository.setSensorFault(drumId, faultType)
        _toastMessage.tryEmit("Injected fault $faultType on drum $drumId")
    }

    fun acknowledgeAlert(alertId: String) {
        repository.acknowledgeAlert(alertId)
    }

    fun clearAllAlerts() {
        repository.clearAllAlerts()
        _toastMessage.tryEmit("Cleared all notifications")
    }

    fun updateSettings(newSettings: InventorySettings) {
        repository.updateSettings(newSettings)
        _toastMessage.tryEmit("Inventory settings & thresholds updated")
    }

    fun addDrum(
        id: String,
        sealerType: String,
        batchNumber: String,
        capacityKg: Double,
        initialMassKg: Double,
        location: String
    ) {
        val drum = Drum(
            id = id.trim().uppercase(),
            sealerType = sealerType.trim(),
            batchNumber = batchNumber.trim().ifBlank { "LOT-${System.currentTimeMillis() % 10000}" },
            nominalCapacityKg = capacityKg.coerceAtLeast(10.0),
            currentMassKg = initialMassKg.coerceIn(0.0, capacityKg),
            location = location.trim().ifBlank { "Staging Area" }
        )
        repository.addDrum(drum)
        _toastMessage.tryEmit("Drum ${drum.id} added to inventory")
    }

    fun receiveMaterial(drumId: String, quantityKg: Double, batchNo: String, notes: String) {
        repository.receiveMaterialStock(drumId, quantityKg, batchNo, notes)
        _toastMessage.tryEmit("Received $quantityKg kg for $drumId")
    }

    fun issueDrumToLine(drumId: String, lineLocation: String) {
        repository.issueDrumToProduction(drumId, lineLocation)
        _toastMessage.tryEmit("Issued $drumId to $lineLocation")
    }

    fun returnEmptyDrum(drumId: String) {
        repository.recordEmptyDrumReturn(drumId)
        _toastMessage.tryEmit("Drum $drumId logged as empty return")
    }

    fun exportCsv() {
        try {
            val file = repository.exportTransactionsCsv()
            val uri = FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Smart Sealer Inventory Transactions")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export Inventory CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            _toastMessage.tryEmit("Export generated: ${repository.transactions.value.size} rows")
        }
    }
}
