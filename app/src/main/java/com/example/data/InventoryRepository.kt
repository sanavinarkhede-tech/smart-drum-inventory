package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class InventoryRepository(
    private val context: Context,
    val sensorAdapter: MockSensorAdapter = MockSensorAdapter(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_sealer_inventory_prefs", Context.MODE_PRIVATE)

    private val _drums = MutableStateFlow<List<Drum>>(emptyList())
    val drums: StateFlow<List<Drum>> = _drums.asStateFlow()

    private val _sensorReadings = MutableStateFlow<Map<String, SensorReading>>(emptyMap())
    val sensorReadings: StateFlow<Map<String, SensorReading>> = _sensorReadings.asStateFlow()

    private val _transactions = MutableStateFlow<List<InventoryTransaction>>(emptyList())
    val transactions: StateFlow<List<InventoryTransaction>> = _transactions.asStateFlow()

    private val _consumptionRecords = MutableStateFlow<List<ConsumptionRecord>>(emptyList())
    val consumptionRecords: StateFlow<List<ConsumptionRecord>> = _consumptionRecords.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _settings = MutableStateFlow(InventorySettings())
    val settings: StateFlow<InventorySettings> = _settings.asStateFlow()

    private var simulationJob: Job? = null

    init {
        loadOrInitializeData()
        startSimulationLoop()
    }

    private fun loadOrInitializeData() {
        val savedDrumsJson = prefs.getString("saved_drums", null)
        val initialDrums = if (!savedDrumsJson.isNullOrEmpty()) {
            parseDrumsJson(savedDrumsJson)
        } else {
            getInitialDrums()
        }
        _drums.value = initialDrums

        val savedAlertsJson = prefs.getString("saved_alerts", null)
        _alerts.value = if (!savedAlertsJson.isNullOrEmpty()) {
            parseAlertsJson(savedAlertsJson)
        } else {
            getInitialAlerts()
        }

        val savedTransactionsJson = prefs.getString("saved_transactions", null)
        _transactions.value = if (!savedTransactionsJson.isNullOrEmpty()) {
            parseTransactionsJson(savedTransactionsJson)
        } else {
            getInitialTransactions()
        }

        _consumptionRecords.value = getInitialConsumptionRecords()

        // Generate initial sensor readings
        scope.launch {
            refreshAllSensorReadings()
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive) {
                val currentSettings = _settings.value
                val intervalMs = (currentSettings.simulationIntervalSec * 1000L).coerceAtLeast(1000L)
                delay(intervalMs)

                if (currentSettings.isSimulationRunning) {
                    tickSimulation(currentSettings)
                }
            }
        }
    }

    private suspend fun tickSimulation(settings: InventorySettings) {
        val currentDrums = _drums.value
        val updatedDrums = mutableListOf<Drum>()
        val newTransactions = mutableListOf<InventoryTransaction>()
        val newAlerts = mutableListOf<Alert>()

        // Rate in kg per minute -> convert to rate per tick
        val ratePerSec = settings.simulationRateKgPerMin / 60.0
        val consumptionThisTick = ratePerSec * settings.simulationIntervalSec

        val now = System.currentTimeMillis()
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        var totalSimulatedThisTick = 0.0

        for (drum in currentDrums) {
            // Only consume from drums that are active on the production line and have sealer remaining
            if (drum.isLineActive && drum.currentMassKg > 0.0) {
                // Add slight variance to consumption rate between robotic cells
                val jitterFactor = 0.8 + (Random.nextDouble() * 0.4)
                val drumConsumption = (consumptionThisTick * jitterFactor).coerceAtMost(drum.currentMassKg)
                val roundedConsumption = (drumConsumption * 100.0).roundToInt() / 100.0

                if (roundedConsumption > 0.0) {
                    val newMass = ((drum.currentMassKg - roundedConsumption) * 10.0).roundToInt() / 10.0
                    val newStatus = calculateStatus(newMass, drum.nominalCapacityKg, settings)

                    // Check for threshold crossing alerts
                    if (drum.status != newStatus) {
                        when (newStatus) {
                            DrumStatus.CRITICAL -> {
                                newAlerts.add(
                                    Alert(
                                        id = UUID.randomUUID().toString(),
                                        timestampMillis = now,
                                        drumId = drum.id,
                                        title = "Critical Sealer Level (${drum.id})",
                                        message = "Drum ${drum.id} (${drum.sealerType}) remaining mass is ${newMass} kg (${((newMass / drum.nominalCapacityKg) * 100).toInt()}%). Prepare replenishment drum immediately.",
                                        severity = AlertSeverity.CRITICAL
                                    )
                                )
                            }
                            DrumStatus.LOW -> {
                                newAlerts.add(
                                    Alert(
                                        id = UUID.randomUUID().toString(),
                                        timestampMillis = now,
                                        drumId = drum.id,
                                        title = "Low Sealer Warning (${drum.id})",
                                        message = "Drum ${drum.id} (${drum.sealerType}) reached ${newMass} kg. Below warning threshold (${settings.lowThresholdPercent}%).",
                                        severity = AlertSeverity.WARNING
                                    )
                                )
                            }
                            DrumStatus.EMPTY -> {
                                newAlerts.add(
                                    Alert(
                                        id = UUID.randomUUID().toString(),
                                        timestampMillis = now,
                                        drumId = drum.id,
                                        title = "Drum Exhausted (${drum.id})",
                                        message = "Drum ${drum.id} is empty. Production line pump auto-shutdown / switchover required.",
                                        severity = AlertSeverity.CRITICAL
                                    )
                                )
                            }
                            else -> {}
                        }
                    }

                    val updatedDrum = drum.copy(
                        currentMassKg = newMass,
                        status = newStatus,
                        lastUpdatedMillis = now
                    )
                    updatedDrums.add(updatedDrum)
                    totalSimulatedThisTick += roundedConsumption
                } else {
                    updatedDrums.add(drum)
                }
            } else {
                updatedDrums.add(drum)
            }
        }

        if (totalSimulatedThisTick > 0.0) {
            _drums.value = updatedDrums
            // Add a consumption record for analytics
            val newRecord = ConsumptionRecord(
                id = UUID.randomUUID().toString(),
                timestampMillis = now,
                drumId = "ACTIVE_LINE",
                quantityKg = (totalSimulatedThisTick * 10.0).roundToInt() / 10.0,
                hourOfDay = hourOfDay,
                dateLabel = dateLabel
            )
            _consumptionRecords.value = (_consumptionRecords.value + newRecord).takeLast(200)

            if (newAlerts.isNotEmpty()) {
                _alerts.value = (newAlerts + _alerts.value).take(50)
                saveAlerts()
            }
            saveDrums()
        }

        refreshAllSensorReadings()
    }

    suspend fun refreshAllSensorReadings() {
        val currentDrums = _drums.value
        val map = mutableMapOf<String, SensorReading>()
        for (drum in currentDrums) {
            map[drum.id] = sensorAdapter.readDistance(drum)
        }
        _sensorReadings.value = map
    }

    fun consumeMaterial(drumId: String, amountKg: Double, operator: String = "Operator"): Boolean {
        val drum = _drums.value.find { it.id == drumId } ?: return false
        val now = System.currentTimeMillis()
        val actualConsume = amountKg.coerceIn(0.0, drum.currentMassKg)
        val newMass = ((drum.currentMassKg - actualConsume) * 10.0).roundToInt() / 10.0
        val newStatus = calculateStatus(newMass, drum.nominalCapacityKg, _settings.value)

        val updated = drum.copy(
            currentMassKg = newMass,
            status = newStatus,
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        // Log transaction
        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            type = TransactionType.CONSUMPTION,
            deltaKg = -actualConsume,
            remainingAfterKg = newMass,
            notes = "Manual test consumption of $amountKg kg for dispensing simulation",
            operator = operator
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()

        // Log consumption record for analytics
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        _consumptionRecords.value = _consumptionRecords.value + ConsumptionRecord(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            quantityKg = actualConsume,
            hourOfDay = hourOfDay,
            dateLabel = dateLabel
        )

        scope.launch {
            refreshAllSensorReadings()
        }
        return true
    }

    fun refillDrum(drumId: String, amountKg: Double, operator: String = "Operator"): Boolean {
        val drum = _drums.value.find { it.id == drumId } ?: return false
        val now = System.currentTimeMillis()
        val newMass = (drum.currentMassKg + amountKg).coerceAtMost(drum.nominalCapacityKg)
        val roundedMass = (newMass * 10.0).roundToInt() / 10.0
        val newStatus = calculateStatus(roundedMass, drum.nominalCapacityKg, _settings.value)

        val updated = drum.copy(
            currentMassKg = roundedMass,
            status = newStatus,
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            type = TransactionType.REFILL,
            deltaKg = amountKg,
            remainingAfterKg = roundedMass,
            notes = "Replenished / refilled $amountKg kg",
            operator = operator
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()

        scope.launch {
            refreshAllSensorReadings()
        }
        return true
    }

    fun resetDrumToFull(drumId: String): Boolean {
        val drum = _drums.value.find { it.id == drumId } ?: return false
        val now = System.currentTimeMillis()
        val updated = drum.copy(
            currentMassKg = drum.nominalCapacityKg,
            status = DrumStatus.NORMAL,
            sensorHealth = SensorHealth.ONLINE,
            faultType = FaultType.NONE,
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            type = TransactionType.RECEIPT,
            deltaKg = drum.nominalCapacityKg - drum.currentMassKg,
            remainingAfterKg = drum.nominalCapacityKg,
            notes = "Reset drum to nominal full capacity (100%)",
            operator = "Supervisor"
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()

        scope.launch {
            refreshAllSensorReadings()
        }
        return true
    }

    fun setSensorFault(drumId: String, faultType: FaultType) {
        val drum = _drums.value.find { it.id == drumId } ?: return
        val now = System.currentTimeMillis()
        val health = when (faultType) {
            FaultType.DISCONNECTED -> SensorHealth.OFFLINE
            FaultType.OUT_OF_RANGE -> SensorHealth.FAULT
            FaultType.FROZEN -> SensorHealth.STALE
            FaultType.NOISY -> SensorHealth.FAULT
            FaultType.NONE -> SensorHealth.ONLINE
        }

        val updated = drum.copy(
            faultType = faultType,
            sensorHealth = health,
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        if (faultType != FaultType.NONE) {
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                timestampMillis = now,
                drumId = drumId,
                title = "Sensor Hardware Fault (${drumId})",
                message = "Fault mode injected: $faultType. Telemetry reliability marked as $health. Distance reading flagged as invalid.",
                severity = AlertSeverity.CRITICAL
            )
            _alerts.value = listOf(alert) + _alerts.value
            saveAlerts()
        }

        scope.launch {
            refreshAllSensorReadings()
        }
    }

    fun addDrum(drum: Drum) {
        val now = System.currentTimeMillis()
        val initialStatus = calculateStatus(drum.currentMassKg, drum.nominalCapacityKg, _settings.value)
        val newDrum = drum.copy(status = initialStatus, lastUpdatedMillis = now)
        _drums.value = _drums.value + newDrum
        saveDrums()

        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = newDrum.id,
            type = TransactionType.RECEIPT,
            deltaKg = newDrum.currentMassKg,
            remainingAfterKg = newDrum.currentMassKg,
            notes = "New drum registered: ${newDrum.sealerType} (Lot: ${newDrum.batchNumber})",
            operator = "Warehouse Clerk"
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()

        scope.launch {
            refreshAllSensorReadings()
        }
    }

    fun issueDrumToProduction(drumId: String, targetLine: String) {
        val drum = _drums.value.find { it.id == drumId } ?: return
        val now = System.currentTimeMillis()
        val updated = drum.copy(
            isLineActive = true,
            location = targetLine,
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            type = TransactionType.ISSUE_TO_LINE,
            deltaKg = 0.0, // Physical drum issue does not consume sealer mass yet; avoids double counting
            remainingAfterKg = drum.currentMassKg,
            notes = "Issued from storage staging to active robotic cell: $targetLine",
            operator = "Line Logistics"
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()
    }

    fun recordEmptyDrumReturn(drumId: String) {
        val drum = _drums.value.find { it.id == drumId } ?: return
        val now = System.currentTimeMillis()
        val remaining = drum.currentMassKg
        val updated = drum.copy(
            currentMassKg = 0.0,
            status = DrumStatus.EMPTY,
            isLineActive = false,
            location = "Empty Return Bay (Recycling)",
            lastUpdatedMillis = now
        )
        updateDrumInternal(updated)

        val tx = InventoryTransaction(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            drumId = drumId,
            type = TransactionType.RETURN_EMPTY,
            deltaKg = -remaining,
            remainingAfterKg = 0.0,
            notes = "Empty drum returned to staging for supplier pickup. Usable remaining sealer evacuated: $remaining kg.",
            operator = "Plant Recycling"
        )
        _transactions.value = listOf(tx) + _transactions.value
        saveTransactions()

        scope.launch {
            refreshAllSensorReadings()
        }
    }

    fun receiveMaterialStock(drumId: String, additionalKg: Double, batchNo: String, notes: String) {
        val drum = _drums.value.find { it.id == drumId }
        val now = System.currentTimeMillis()
        if (drum != null) {
            val newMass = (drum.currentMassKg + additionalKg).coerceAtMost(drum.nominalCapacityKg)
            val updated = drum.copy(
                currentMassKg = newMass,
                batchNumber = if (batchNo.isNotBlank()) batchNo else drum.batchNumber,
                status = calculateStatus(newMass, drum.nominalCapacityKg, _settings.value),
                lastUpdatedMillis = now
            )
            updateDrumInternal(updated)

            val tx = InventoryTransaction(
                id = UUID.randomUUID().toString(),
                timestampMillis = now,
                drumId = drumId,
                type = TransactionType.RECEIPT,
                deltaKg = additionalKg,
                remainingAfterKg = newMass,
                notes = if (notes.isNotBlank()) notes else "Material stock received into drum $drumId",
                operator = "Receiving Dock"
            )
            _transactions.value = listOf(tx) + _transactions.value
            saveTransactions()
        }
    }

    fun updateSettings(newSettings: InventorySettings) {
        _settings.value = newSettings
        // Re-evaluate drum statuses with new thresholds
        val updatedDrums = _drums.value.map { drum ->
            drum.copy(status = calculateStatus(drum.currentMassKg, drum.nominalCapacityKg, newSettings))
        }
        _drums.value = updatedDrums
        saveDrums()
    }

    fun toggleSimulation(running: Boolean) {
        val newSettings = _settings.value.copy(isSimulationRunning = running)
        _settings.value = newSettings
    }

    fun acknowledgeAlert(alertId: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == alertId) it.copy(isAcknowledged = true) else it
        }
        saveAlerts()
    }

    fun clearAllAlerts() {
        _alerts.value = emptyList()
        saveAlerts()
    }

    fun resetAllData() {
        _drums.value = getInitialDrums()
        _alerts.value = getInitialAlerts()
        _transactions.value = getInitialTransactions()
        _consumptionRecords.value = getInitialConsumptionRecords()
        _settings.value = InventorySettings()
        saveDrums()
        saveAlerts()
        saveTransactions()
        scope.launch {
            refreshAllSensorReadings()
        }
    }

    private fun updateDrumInternal(updated: Drum) {
        _drums.value = _drums.value.map { if (it.id == updated.id) updated else it }
        saveDrums()
    }

    private fun calculateStatus(mass: Double, capacity: Double, settings: InventorySettings): DrumStatus {
        if (mass <= 0.5) return DrumStatus.EMPTY
        val pct = if (capacity > 0) (mass / capacity) * 100.0 else 0.0
        return when {
            pct <= settings.criticalThresholdPercent -> DrumStatus.CRITICAL
            pct <= settings.lowThresholdPercent -> DrumStatus.LOW
            else -> DrumStatus.NORMAL
        }
    }

    fun exportTransactionsCsv(): File {
        val file = File(context.cacheDir, "smart_sealer_inventory_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("ID,Timestamp,Date,DrumID,Type,DeltaKg,RemainingAfterKg,Operator,Notes\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (tx in _transactions.value) {
                writer.append("\"${tx.id}\",")
                writer.append("\"${tx.timestampMillis}\",")
                writer.append("\"${sdf.format(Date(tx.timestampMillis))}\",")
                writer.append("\"${tx.drumId}\",")
                writer.append("\"${tx.type.name}\",")
                writer.append("\"${tx.deltaKg}\",")
                writer.append("\"${tx.remainingAfterKg}\",")
                writer.append("\"${tx.operator}\",")
                writer.append("\"${tx.notes.replace("\"", "\"\"")}\"\n")
            }
        }
        return file
    }

    // Persistence helpers
    private fun saveDrums() {
        val array = JSONArray()
        for (d in _drums.value) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("sealerType", d.sealerType)
            obj.put("batchNumber", d.batchNumber)
            obj.put("nominalCapacityKg", d.nominalCapacityKg)
            obj.put("currentMassKg", d.currentMassKg)
            obj.put("emptyTareWeightKg", d.emptyTareWeightKg)
            obj.put("usableTravelCm", d.usableTravelCm)
            obj.put("sensorOffsetCm", d.sensorOffsetCm)
            obj.put("location", d.location)
            obj.put("isLineActive", d.isLineActive)
            obj.put("status", d.status.name)
            obj.put("sensorHealth", d.sensorHealth.name)
            obj.put("faultType", d.faultType.name)
            obj.put("lastUpdatedMillis", d.lastUpdatedMillis)
            array.put(obj)
        }
        prefs.edit().putString("saved_drums", array.toString()).apply()
    }

    private fun parseDrumsJson(json: String): List<Drum> {
        val list = mutableListOf<Drum>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Drum(
                        id = o.getString("id"),
                        sealerType = o.getString("sealerType"),
                        batchNumber = o.optString("batchNumber", "LOT-GEN"),
                        nominalCapacityKg = o.optDouble("nominalCapacityKg", 200.0),
                        currentMassKg = o.optDouble("currentMassKg", 200.0),
                        emptyTareWeightKg = o.optDouble("emptyTareWeightKg", 15.0),
                        usableTravelCm = o.optDouble("usableTravelCm", 85.0),
                        sensorOffsetCm = o.optDouble("sensorOffsetCm", 10.0),
                        location = o.optString("location", "Staging Area"),
                        isLineActive = o.optBoolean("isLineActive", true),
                        status = try { DrumStatus.valueOf(o.getString("status")) } catch (e: Exception) { DrumStatus.NORMAL },
                        sensorHealth = try { SensorHealth.valueOf(o.getString("sensorHealth")) } catch (e: Exception) { SensorHealth.ONLINE },
                        faultType = try { FaultType.valueOf(o.getString("faultType")) } catch (e: Exception) { FaultType.NONE },
                        lastUpdatedMillis = o.optLong("lastUpdatedMillis", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            return getInitialDrums()
        }
        return if (list.isNotEmpty()) list else getInitialDrums()
    }

    private fun saveAlerts() {
        val array = JSONArray()
        for (a in _alerts.value) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("timestampMillis", a.timestampMillis)
            obj.put("drumId", a.drumId ?: "")
            obj.put("title", a.title)
            obj.put("message", a.message)
            obj.put("severity", a.severity.name)
            obj.put("isAcknowledged", a.isAcknowledged)
            array.put(obj)
        }
        prefs.edit().putString("saved_alerts", array.toString()).apply()
    }

    private fun parseAlertsJson(json: String): List<Alert> {
        val list = mutableListOf<Alert>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Alert(
                        id = o.getString("id"),
                        timestampMillis = o.getLong("timestampMillis"),
                        drumId = o.optString("drumId").takeIf { it.isNotBlank() },
                        title = o.getString("title"),
                        message = o.getString("message"),
                        severity = try { AlertSeverity.valueOf(o.getString("severity")) } catch (e: Exception) { AlertSeverity.INFO },
                        isAcknowledged = o.optBoolean("isAcknowledged", false)
                    )
                )
            }
        } catch (e: Exception) {
            return getInitialAlerts()
        }
        return list
    }

    private fun saveTransactions() {
        val array = JSONArray()
        for (tx in _transactions.value.take(150)) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("timestampMillis", tx.timestampMillis)
            obj.put("drumId", tx.drumId)
            obj.put("type", tx.type.name)
            obj.put("deltaKg", tx.deltaKg)
            obj.put("remainingAfterKg", tx.remainingAfterKg)
            obj.put("notes", tx.notes)
            obj.put("operator", tx.operator)
            array.put(obj)
        }
        prefs.edit().putString("saved_transactions", array.toString()).apply()
    }

    private fun parseTransactionsJson(json: String): List<InventoryTransaction> {
        val list = mutableListOf<InventoryTransaction>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    InventoryTransaction(
                        id = o.getString("id"),
                        timestampMillis = o.getLong("timestampMillis"),
                        drumId = o.getString("drumId"),
                        type = try { TransactionType.valueOf(o.getString("type")) } catch (e: Exception) { TransactionType.CONSUMPTION },
                        deltaKg = o.getDouble("deltaKg"),
                        remainingAfterKg = o.getDouble("remainingAfterKg"),
                        notes = o.optString("notes", ""),
                        operator = o.optString("operator", "Operator")
                    )
                )
            }
        } catch (e: Exception) {
            return getInitialTransactions()
        }
        return list
    }

    // Default seed data for industrial presentation
    private fun getInitialDrums(): List<Drum> {
        val now = System.currentTimeMillis()
        return listOf(
            Drum(
                id = "SD-001",
                sealerType = "Structural Adhesive EP-420",
                batchNumber = "LOT-2026-A101",
                nominalCapacityKg = 200.0,
                currentMassKg = 168.4,
                location = "Cell 1 - BIW Framing",
                isLineActive = true,
                status = DrumStatus.NORMAL,
                lastUpdatedMillis = now
            ),
            Drum(
                id = "SD-002",
                sealerType = "Underbody PVC-300",
                batchNumber = "LOT-2026-B204",
                nominalCapacityKg = 200.0,
                currentMassKg = 44.0,
                location = "Cell 2 - Underbody Sealer",
                isLineActive = true,
                status = DrumStatus.LOW,
                lastUpdatedMillis = now
            ),
            Drum(
                id = "SD-003",
                sealerType = "Hem Flange Mastic HF-100",
                batchNumber = "LOT-2026-C099",
                nominalCapacityKg = 200.0,
                currentMassKg = 16.5,
                location = "Cell 3 - Door Hemming",
                isLineActive = true,
                status = DrumStatus.CRITICAL,
                lastUpdatedMillis = now
            ),
            Drum(
                id = "SD-004",
                sealerType = "Direct Glazing PU-850",
                batchNumber = "LOT-2026-D312",
                nominalCapacityKg = 200.0,
                currentMassKg = 194.0,
                location = "Cell 4 - Windshield Station",
                isLineActive = true,
                status = DrumStatus.NORMAL,
                lastUpdatedMillis = now
            ),
            Drum(
                id = "SD-005",
                sealerType = "Cavity Wax CW-50",
                batchNumber = "LOT-2026-E500",
                nominalCapacityKg = 200.0,
                currentMassKg = 0.0,
                location = "Empty Return Bay",
                isLineActive = false,
                status = DrumStatus.EMPTY,
                lastUpdatedMillis = now
            ),
            Drum(
                id = "SD-006",
                sealerType = "Acoustic Baffle Foam AB-12",
                batchNumber = "LOT-2026-F118",
                nominalCapacityKg = 200.0,
                currentMassKg = 122.5,
                location = "Cell 5 - Roof Sealer",
                isLineActive = true,
                status = DrumStatus.NORMAL,
                lastUpdatedMillis = now
            )
        )
    }

    private fun getInitialAlerts(): List<Alert> {
        val now = System.currentTimeMillis()
        return listOf(
            Alert(
                id = "alt-01",
                timestampMillis = now - 15 * 60 * 1000,
                drumId = "SD-003",
                title = "Critical Sealer Level (SD-003)",
                message = "SD-003 (Hem Flange Mastic HF-100) at 16.5 kg (8.2%). Less than 2 hours of production buffer remaining at current consumption.",
                severity = AlertSeverity.CRITICAL,
                isAcknowledged = false
            ),
            Alert(
                id = "alt-02",
                timestampMillis = now - 45 * 60 * 1000,
                drumId = "SD-002",
                title = "Low Sealer Warning (SD-002)",
                message = "SD-002 (Underbody PVC-300) below 25% threshold (44.0 kg remaining). Schedule pallet forklift replenishment.",
                severity = AlertSeverity.WARNING,
                isAcknowledged = false
            ),
            Alert(
                id = "alt-03",
                timestampMillis = now - 120 * 60 * 1000,
                drumId = null,
                title = "Total Sealer Stock Health",
                message = "Plant total remaining sealer is 545.4 kg against minimum recommended safety threshold of 300.0 kg.",
                severity = AlertSeverity.INFO,
                isAcknowledged = true
            )
        )
    }

    private fun getInitialTransactions(): List<InventoryTransaction> {
        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L
        return listOf(
            InventoryTransaction(
                id = "tx-101",
                timestampMillis = now - 2 * oneHour,
                drumId = "SD-004",
                type = TransactionType.ISSUE_TO_LINE,
                deltaKg = 0.0,
                remainingAfterKg = 194.0,
                notes = "Issued full drum SD-004 from staging to Cell 4 (Windshield Station)",
                operator = "M. Schmidt"
            ),
            InventoryTransaction(
                id = "tx-102",
                timestampMillis = now - 4 * oneHour,
                drumId = "SD-005",
                type = TransactionType.RETURN_EMPTY,
                deltaKg = -2.5,
                remainingAfterKg = 0.0,
                notes = "Returned exhausted drum SD-005 to recycling bay. Usable residue evacuated.",
                operator = "K. Vance"
            ),
            InventoryTransaction(
                id = "tx-103",
                timestampMillis = now - 8 * oneHour,
                drumId = "SD-001",
                type = TransactionType.RECEIPT,
                deltaKg = 200.0,
                remainingAfterKg = 200.0,
                notes = "Received new shipment drum SD-001 (Supplier: Sika Automotive)",
                operator = "R. Kowalski"
            ),
            InventoryTransaction(
                id = "tx-104",
                timestampMillis = now - 14 * oneHour,
                drumId = "SD-003",
                type = TransactionType.CONSUMPTION,
                deltaKg = -24.8,
                remainingAfterKg = 16.5,
                notes = "Shift production run: 140 automotive door hemming cycles completed",
                operator = "Shift 1 Lead"
            ),
            InventoryTransaction(
                id = "tx-105",
                timestampMillis = now - 24 * oneHour,
                drumId = "SD-002",
                type = TransactionType.CONSUMPTION,
                deltaKg = -38.2,
                remainingAfterKg = 44.0,
                notes = "Continuous underbody sealer dispensing on 82 car bodies",
                operator = "Shift 2 Lead"
            ),
            InventoryTransaction(
                id = "tx-106",
                timestampMillis = now - 48 * oneHour,
                drumId = "SYSTEM",
                type = TransactionType.OPENING_STOCK,
                deltaKg = 1200.0,
                remainingAfterKg = 1200.0,
                notes = "Monthly plant opening inventory count confirmed by Materials Management",
                operator = "Plant Manager"
            )
        )
    }

    private fun getInitialConsumptionRecords(): List<ConsumptionRecord> {
        val records = mutableListOf<ConsumptionRecord>()
        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Seed 24 hours of hourly consumption pattern
        for (h in 23 downTo 0) {
            val time = now - (h * oneHour)
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            // Industrial shift pattern: lunch dips at 12 and 18, high during shifts
            val base = if (hourOfDay in 7..11 || hourOfDay in 13..17 || hourOfDay in 19..23) 12.0 else 4.0
            val qty = ((base + Random.nextDouble() * 6.0) * 10.0).roundToInt() / 10.0

            records.add(
                ConsumptionRecord(
                    id = "c-seed-$h",
                    timestampMillis = time,
                    drumId = "PLANT_TOTAL",
                    quantityKg = qty,
                    hourOfDay = hourOfDay,
                    dateLabel = sdf.format(Date(time))
                )
            )
        }
        return records
    }
}
