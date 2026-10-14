package com.example.model

enum class DrumStatus {
    NORMAL,
    LOW,
    CRITICAL,
    EMPTY
}

enum class SensorHealth {
    ONLINE,
    OFFLINE,
    STALE,
    FAULT
}

enum class FaultType {
    NONE,
    DISCONNECTED,
    OUT_OF_RANGE,
    FROZEN,
    NOISY
}

enum class TransactionType {
    OPENING_STOCK,
    RECEIPT,
    ISSUE_TO_LINE,
    RETURN_EMPTY,
    CONSUMPTION,
    LOSS_ADJUSTMENT,
    REFILL
}

enum class AlertSeverity {
    CRITICAL,
    WARNING,
    INFO
}

data class Drum(
    val id: String,
    val sealerType: String,
    val batchNumber: String,
    val nominalCapacityKg: Double = 200.0,
    val currentMassKg: Double = 200.0,
    val emptyTareWeightKg: Double = 15.0,
    val usableTravelCm: Double = 85.0,
    val sensorOffsetCm: Double = 10.0,
    val location: String = "Staging Area",
    val isLineActive: Boolean = true,
    val status: DrumStatus = DrumStatus.NORMAL,
    val sensorHealth: SensorHealth = SensorHealth.ONLINE,
    val faultType: FaultType = FaultType.NONE,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val remainingPercentage: Double
        get() = if (nominalCapacityKg > 0) {
            ((currentMassKg / nominalCapacityKg) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

    val consumedMassKg: Double
        get() = (nominalCapacityKg - currentMassKg).coerceIn(0.0, nominalCapacityKg)

    val grossWeightKg: Double
        get() = currentMassKg + emptyTareWeightKg

    val dFullCm: Double
        get() = sensorOffsetCm

    val dEmptyCm: Double
        get() = sensorOffsetCm + usableTravelCm

    /**
     * Theoretical geometric distance from top sensor to follower plate based on mass:
     * When full (mass = capacity): distance = dFull
     * When empty (mass = 0): distance = dEmpty
     */
    val theoreticalDistanceCm: Double
        get() {
            val fractionConsumed = if (nominalCapacityKg > 0) {
                (1.0 - (currentMassKg / nominalCapacityKg)).coerceIn(0.0, 1.0)
            } else 1.0
            return dFullCm + (fractionConsumed * usableTravelCm)
        }
}

data class SensorReading(
    val drumId: String,
    val rawDistanceCm: Double,
    val estimatedMassKg: Double,
    val trueMassKg: Double,
    val timestampMillis: Long = System.currentTimeMillis(),
    val health: SensorHealth = SensorHealth.ONLINE,
    val faultType: FaultType = FaultType.NONE,
    val errorDescription: String? = null
)

data class InventoryTransaction(
    val id: String,
    val timestampMillis: Long,
    val drumId: String,
    val type: TransactionType,
    val deltaKg: Double,
    val remainingAfterKg: Double,
    val notes: String,
    val operator: String
)

data class ConsumptionRecord(
    val id: String,
    val timestampMillis: Long,
    val drumId: String,
    val quantityKg: Double,
    val hourOfDay: Int,
    val dateLabel: String
)

data class Alert(
    val id: String,
    val timestampMillis: Long,
    val drumId: String?,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val isAcknowledged: Boolean = false
)

data class InventorySettings(
    val lowThresholdPercent: Float = 25f,
    val criticalThresholdPercent: Float = 10f,
    val minInventoryKg: Double = 300.0,
    val avgDailyConsumptionEstimate: Double = 85.0,
    val supplierLeadTimeDays: Int = 5,
    val safetyStockKg: Double = 150.0,
    val simulationRateKgPerMin: Double = 2.5,
    val simulationIntervalSec: Int = 2,
    val isSimulationRunning: Boolean = true
) {
    /**
     * Industrial reorder formula:
     * Reorder Point = (Average Daily Consumption * Lead Time) + Safety Stock
     */
    val reorderPointKg: Double
        get() = (avgDailyConsumptionEstimate * supplierLeadTimeDays) + safetyStockKg

    fun calculateDaysRemaining(totalStockKg: Double): Double {
        return if (avgDailyConsumptionEstimate > 0) {
            totalStockKg / avgDailyConsumptionEstimate
        } else 0.0
    }

    fun calculateSuggestedReplenishment(totalStockKg: Double): Double {
        val targetStock = reorderPointKg * 2.0
        return (targetStock - totalStockKg).coerceAtLeast(0.0)
    }
}
