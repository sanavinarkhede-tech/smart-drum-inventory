package com.example.data

import com.example.model.Drum
import com.example.model.FaultType
import com.example.model.SensorHealth
import com.example.model.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * SensorAdapter defines the standard hardware abstraction layer for laser distance
 * sensors and load cell scale transmitters mounted on manufacturing sealer drums.
 * Real implementations could connect via ESP32 MQTT broker, RS-485 Modbus RTU, or REST.
 */
interface SensorAdapter {
    /**
     * Reads current distance measured by the top laser sensor to the follower plate (cm).
     */
    suspend fun readDistance(drum: Drum): SensorReading

    /**
     * Optional gross weight reading if floor load-cells / scale platform is installed (kg).
     */
    suspend fun readGrossWeight(drum: Drum): Double?

    /**
     * Checks sensor physical communication link, optics cleanliness, and signal quality.
     */
    suspend fun checkHealth(drum: Drum): SensorHealth

    /**
     * Stream of asynchronous sensor telemetry publications.
     */
    fun observeReadings(): Flow<SensorReading>
}

/**
 * High-fidelity MockSensorAdapter that models laser sensor physics, optical bounce,
 * follower-plate travel geometry, realistic sensor jitter, and intentional fault injection.
 */
class MockSensorAdapter : SensorAdapter {

    private val readingsFlow = MutableSharedFlow<SensorReading>(extraBufferCapacity = 64)
    private val frozenDistanceCache = mutableMapOf<String, Double>()

    override suspend fun readDistance(drum: Drum): SensorReading {
        val theoreticalDist = drum.theoreticalDistanceCm
        val reading: SensorReading

        when (drum.faultType) {
            FaultType.DISCONNECTED -> {
                reading = SensorReading(
                    drumId = drum.id,
                    rawDistanceCm = -1.0,
                    estimatedMassKg = 0.0,
                    trueMassKg = drum.currentMassKg,
                    health = SensorHealth.OFFLINE,
                    faultType = FaultType.DISCONNECTED,
                    errorDescription = "Sensor disconnected / Communication bus timeout (Modbus error 0x04)"
                )
            }
            FaultType.OUT_OF_RANGE -> {
                val outOfRangeValue = 999.9
                reading = SensorReading(
                    drumId = drum.id,
                    rawDistanceCm = outOfRangeValue,
                    estimatedMassKg = 0.0,
                    trueMassKg = drum.currentMassKg,
                    health = SensorHealth.FAULT,
                    faultType = FaultType.OUT_OF_RANGE,
                    errorDescription = "Laser beam reflection out of calibrated bounds (> 150 cm)"
                )
            }
            FaultType.FROZEN -> {
                val frozenDist = frozenDistanceCache.getOrPut(drum.id) { theoreticalDist }
                val estMass = calculateEstimatedMass(drum, frozenDist)
                reading = SensorReading(
                    drumId = drum.id,
                    rawDistanceCm = frozenDist,
                    estimatedMassKg = estMass,
                    trueMassKg = drum.currentMassKg,
                    health = SensorHealth.STALE,
                    faultType = FaultType.FROZEN,
                    errorDescription = "Frozen reading detected: Follower plate position telemetry static"
                )
            }
            FaultType.NOISY -> {
                // High amplitude optical noise (+/- 5.0 cm)
                val noise = (Random.nextDouble() - 0.5) * 10.0
                val noisyDist = (theoreticalDist + noise).coerceIn(drum.dFullCm - 2.0, drum.dEmptyCm + 5.0)
                val estMass = calculateEstimatedMass(drum, noisyDist)
                reading = SensorReading(
                    drumId = drum.id,
                    rawDistanceCm = (noisyDist * 10.0).roundToInt() / 10.0,
                    estimatedMassKg = estMass,
                    trueMassKg = drum.currentMassKg,
                    health = SensorHealth.FAULT,
                    faultType = FaultType.NOISY,
                    errorDescription = "Optical scatter / High noise detected (> 5% signal deviation)"
                )
            }
            FaultType.NONE -> {
                // Normal slight laser jitter (+/- 0.15 cm)
                val subtleJitter = (Random.nextDouble() - 0.5) * 0.3
                val distance = (theoreticalDist + subtleJitter).coerceIn(drum.dFullCm, drum.dEmptyCm)
                val roundedDistance = (distance * 10.0).roundToInt() / 10.0
                val estMass = calculateEstimatedMass(drum, roundedDistance)

                // Update frozen cache in case user switches to frozen later
                frozenDistanceCache[drum.id] = roundedDistance

                reading = SensorReading(
                    drumId = drum.id,
                    rawDistanceCm = roundedDistance,
                    estimatedMassKg = estMass,
                    trueMassKg = drum.currentMassKg,
                    health = SensorHealth.ONLINE,
                    faultType = FaultType.NONE,
                    errorDescription = null
                )
            }
        }

        readingsFlow.tryEmit(reading)
        return reading
    }

    override suspend fun readGrossWeight(drum: Drum): Double? {
        // Tare weight + current mass + scale noise
        val jitter = (Random.nextDouble() - 0.5) * 0.2
        return ((drum.grossWeightKg + jitter) * 10.0).roundToInt() / 10.0
    }

    override suspend fun checkHealth(drum: Drum): SensorHealth {
        return when (drum.faultType) {
            FaultType.DISCONNECTED -> SensorHealth.OFFLINE
            FaultType.OUT_OF_RANGE -> SensorHealth.FAULT
            FaultType.FROZEN -> SensorHealth.STALE
            FaultType.NOISY -> SensorHealth.FAULT
            FaultType.NONE -> SensorHealth.ONLINE
        }
    }

    override fun observeReadings(): Flow<SensorReading> = readingsFlow.asSharedFlow()

    /**
     * Calibrated inverse distance-to-mass mapping:
     * remainingFraction = 1.0 - ((distance - dFull) / usableTravel)
     * estimatedMass = nominalCapacity * remainingFraction
     */
    private fun calculateEstimatedMass(drum: Drum, distanceCm: Double): Double {
        if (drum.usableTravelCm <= 0.0) return 0.0
        val fractionConsumed = ((distanceCm - drum.dFullCm) / drum.usableTravelCm).coerceIn(0.0, 1.0)
        val remainingFraction = 1.0 - fractionConsumed
        val mass = drum.nominalCapacityKg * remainingFraction
        return (mass * 10.0).roundToInt() / 10.0
    }
}
