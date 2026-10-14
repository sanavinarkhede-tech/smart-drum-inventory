package com.example

import com.example.data.MockSensorAdapter
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFollowerPlateGeometry() {
        val drum = Drum(
            id = "SD-TEST",
            sealerType = "Test Epoxy",
            batchNumber = "B-01",
            nominalCapacityKg = 200.0,
            currentMassKg = 200.0,
            usableTravelCm = 85.0,
            sensorOffsetCm = 10.0
        )

        // When full (200kg / 200kg), laser distance should equal dFull (10 cm)
        assertEquals(10.0, drum.theoreticalDistanceCm, 0.01)
        assertEquals(100.0, drum.remainingPercentage, 0.01)
        assertEquals(0.0, drum.consumedMassKg, 0.01)

        // When half full (100kg / 200kg), distance should be 10 + 0.5 * 85 = 52.5 cm
        val halfDrum = drum.copy(currentMassKg = 100.0)
        assertEquals(52.5, halfDrum.theoreticalDistanceCm, 0.01)
        assertEquals(50.0, halfDrum.remainingPercentage, 0.01)
        assertEquals(100.0, halfDrum.consumedMassKg, 0.01)

        // When empty (0kg / 200kg), distance should equal dEmpty = 10 + 85 = 95.0 cm
        val emptyDrum = drum.copy(currentMassKg = 0.0)
        assertEquals(95.0, emptyDrum.theoreticalDistanceCm, 0.01)
        assertEquals(0.0, emptyDrum.remainingPercentage, 0.01)
        assertEquals(200.0, emptyDrum.consumedMassKg, 0.01)
    }

    @Test
    fun testReorderPointCalculation() {
        val settings = InventorySettings(
            avgDailyConsumptionEstimate = 85.0,
            supplierLeadTimeDays = 5,
            safetyStockKg = 150.0
        )

        // Reorder point = (85 * 5) + 150 = 425 + 150 = 575.0 kg
        assertEquals(575.0, settings.reorderPointKg, 0.01)

        // If total stock is 500 kg, days remaining = 500 / 85 = 5.88 days
        assertEquals(5.88, settings.calculateDaysRemaining(500.0), 0.01)

        // Target stock is reorderPoint * 2 = 1150 kg; replenishment = 1150 - 500 = 650 kg
        assertEquals(650.0, settings.calculateSuggestedReplenishment(500.0), 0.01)
    }

    @Test
    fun testMockSensorAdapterFaultInjection() = runBlocking {
        val adapter = MockSensorAdapter()
        val drum = Drum(
            id = "SD-FAULT-TEST",
            sealerType = "PVC Sealer",
            batchNumber = "B-02",
            nominalCapacityKg = 200.0,
            currentMassKg = 150.0,
            faultType = FaultType.DISCONNECTED
        )

        val reading = adapter.readDistance(drum)
        assertEquals(SensorHealth.OFFLINE, reading.health)
        assertEquals(-1.0, reading.rawDistanceCm, 0.01)
        assertNotNull(reading.errorDescription)
    }
}
