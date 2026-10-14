package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrumStatus
import com.example.model.FaultType
import com.example.model.SensorHealth
import com.example.ui.theme.*

@Composable
fun SimulatedDataBanner(modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFFEF3C7),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Simulated Data Notice",
                tint = Color(0xFFB45309),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DEMO & SIMULATED IOT TELEMETRY ACTIVE — Laser follower-plate readings and dispensing simulated for validation.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF92400E),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    accentColor: Color = AccentBlue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun DrumStatusBadge(status: DrumStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        DrumStatus.NORMAL -> Triple(StatusNormalBg, StatusNormal, "NORMAL")
        DrumStatus.LOW -> Triple(StatusLowBg, StatusLow, "LOW LEVEL")
        DrumStatus.CRITICAL -> Triple(StatusCriticalBg, StatusCritical, "CRITICAL")
        DrumStatus.EMPTY -> Triple(StatusEmptyBg, StatusEmpty, "EMPTY")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun SensorHealthBadge(health: SensorHealth, faultType: FaultType = FaultType.NONE, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (health) {
        SensorHealth.ONLINE -> Triple(StatusNormalBg, StatusNormal, "ONLINE")
        SensorHealth.OFFLINE -> Triple(Color(0xFFFEF2F2), StatusFault, "OFFLINE")
        SensorHealth.STALE -> Triple(StatusLowBg, StatusLow, "STALE")
        SensorHealth.FAULT -> Triple(StatusCriticalBg, StatusCritical, "FAULT: ${faultType.name}")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (health) {
                    SensorHealth.ONLINE -> Icons.Default.Sensors
                    SensorHealth.OFFLINE -> Icons.Default.SensorsOff
                    SensorHealth.STALE -> Icons.Default.HourglassEmpty
                    SensorHealth.FAULT -> Icons.Default.Warning
                },
                contentDescription = "Sensor state",
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 9.sp
                )
            )
        }
    }
}

/**
 * High-fidelity 2D industrial steel drum graphic with realistic level filling,
 * follower plate, corrugation beads, and laser beam sensor.
 */
@Composable
fun SealerDrumVisual(
    fillPercent: Double,
    distanceCm: Double,
    status: DrumStatus,
    health: SensorHealth,
    width: Dp = 100.dp,
    height: Dp = 150.dp,
    showLaserBeam: Boolean = true,
    modifier: Modifier = Modifier
) {
    val levelFraction = (fillPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val liquidColor = when (status) {
        DrumStatus.NORMAL -> AccentTeal
        DrumStatus.LOW -> StatusLow
        DrumStatus.CRITICAL -> StatusCritical
        DrumStatus.EMPTY -> StatusEmpty
    }

    Box(
        modifier = modifier
            .size(width, height)
            .testTag("drum_visual_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val drumLeft = w * 0.18f
            val drumRight = w * 0.82f
            val drumWidth = drumRight - drumLeft
            val drumTop = h * 0.22f
            val drumBottom = h * 0.94f
            val drumHeight = drumBottom - drumTop

            // Drum Outer Steel Shell Background
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(drumLeft, drumTop),
                size = Size(drumWidth, drumHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Inner Sealer Material Fill (bottom up)
            val fillHeight = drumHeight * levelFraction
            val fillTop = drumBottom - fillHeight
            if (fillHeight > 2f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(liquidColor.copy(alpha = 0.9f), liquidColor),
                        startY = fillTop,
                        endY = drumBottom
                    ),
                    topLeft = Offset(drumLeft + 3f, fillTop),
                    size = Size(drumWidth - 6f, fillHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Industrial Drum Corrugation Ribs (typical 200L steel drum beads)
            val rib1Y = drumTop + drumHeight * 0.33f
            val rib2Y = drumTop + drumHeight * 0.66f
            val ribColor = Color(0xFF475569)
            drawLine(
                color = ribColor,
                start = Offset(drumLeft, rib1Y),
                end = Offset(drumRight, rib1Y),
                strokeWidth = 3f
            )
            drawLine(
                color = ribColor,
                start = Offset(drumLeft, rib2Y),
                end = Offset(drumRight, rib2Y),
                strokeWidth = 3f
            )

            // Follower Plate Disc (floats directly on top of the sealer)
            val followerPlateY = fillTop.coerceIn(drumTop + 4f, drumBottom - 6f)
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(drumLeft + 2f, followerPlateY - 4f),
                size = Size(drumWidth - 4f, 8f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Follower plate seal gasket
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(drumLeft + 2f, followerPlateY),
                end = Offset(drumRight - 2f, followerPlateY),
                strokeWidth = 2f
            )

            // Laser Sensor Enclosure at Top Center
            val sensorCenterX = w * 0.5f
            val sensorTop = h * 0.04f
            val sensorBottom = drumTop
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(sensorCenterX - 14f, sensorTop),
                size = Size(28f, sensorBottom - sensorTop),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Sensor Lens ring
            drawCircle(
                color = LaserCyan,
                radius = 4f,
                center = Offset(sensorCenterX, sensorBottom)
            )

            // Laser Sensor Measurement Beam (pulsing cyan beam down to follower plate)
            if (showLaserBeam && health == SensorHealth.ONLINE) {
                drawLine(
                    color = LaserCyan.copy(alpha = 0.85f),
                    start = Offset(sensorCenterX, sensorBottom),
                    end = Offset(sensorCenterX, followerPlateY - 4f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                // Laser spot hit
                drawCircle(
                    color = LaserCyan,
                    radius = 3.5f,
                    center = Offset(sensorCenterX, followerPlateY - 4f)
                )
            } else if (health == SensorHealth.FAULT || health == SensorHealth.OFFLINE) {
                // Warning indicator on beam
                drawLine(
                    color = StatusFault.copy(alpha = 0.5f),
                    start = Offset(sensorCenterX, sensorBottom),
                    end = Offset(sensorCenterX, sensorBottom + 16f),
                    strokeWidth = 2f
                )
            }

            // Drum Outer Steel Border
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(drumLeft, drumTop),
                size = Size(drumWidth, drumHeight),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

/**
 * Cross-section schematic diagram explaining the laser follower-plate calibration geometry.
 */
@Composable
fun FollowerPlateCrossSectionDiagram(
    drumCapacityKg: Double,
    currentMassKg: Double,
    usableTravelCm: Double,
    sensorOffsetCm: Double,
    measuredDistanceCm: Double,
    estimatedMassKg: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FOLLOWER-PLATE SENSOR CALIBRATION GEOMETRY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IndustrialNavy
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "A top laser distance sensor measures position of the pneumatic follower-plate as high-viscosity mastic is pumped into the manufacturing line.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visual cross-section
                SealerDrumVisual(
                    fillPercent = if (drumCapacityKg > 0) (currentMassKg / drumCapacityKg) * 100.0 else 0.0,
                    distanceCm = measuredDistanceCm,
                    status = DrumStatus.NORMAL,
                    health = SensorHealth.ONLINE,
                    width = 120.dp,
                    height = 180.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Geometric telemetry measurements readout
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    CalibrationDataRow(
                        label = "Top Mounting Offset (dFull):",
                        value = "${sensorOffsetCm} cm"
                    )
                    CalibrationDataRow(
                        label = "Usable Follower Travel (H):",
                        value = "${usableTravelCm} cm"
                    )
                    CalibrationDataRow(
                        label = "Empty State Distance (dEmpty):",
                        value = "${sensorOffsetCm + usableTravelCm} cm"
                    )
                    CalibrationDataRow(
                        label = "Current Laser Distance:",
                        value = "${measuredDistanceCm} cm",
                        highlightColor = LaserCyan
                    )
                    Divider(color = CardBorder, thickness = 1.dp)
                    CalibrationDataRow(
                        label = "True Simulated Mass:",
                        value = "${currentMassKg} kg"
                    )
                    CalibrationDataRow(
                        label = "Sensor-Estimated Mass:",
                        value = "${estimatedMassKg} kg",
                        highlightColor = AccentTeal
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationDataRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TextSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = highlightColor ?: TextPrimary
            )
        )
    }
}

/**
 * Lightweight native Jetpack Compose Line Trend Chart for stock trajectory.
 */
@Composable
fun RealtimeStockTrendChart(
    dataPoints: List<Float>, // remaining kg over time
    capacityKg: Float = 1200f,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME INVENTORY TREND (LAST 24 HRS)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
                Text(
                    text = "SIMULATED DYNAMICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 16f

                    // Grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = (h - paddingBottom) * (i / gridLines.toFloat())
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    if (dataPoints.size < 2) return@Canvas

                    val maxVal = capacityKg
                    val minVal = 0f
                    val range = (maxVal - minVal).coerceAtLeast(1f)

                    val path = Path()
                    val fillPath = Path()
                    val stepX = w / (dataPoints.size - 1).toFloat()

                    dataPoints.forEachIndexed { index, value ->
                        val normalizedY = (1f - ((value - minVal) / range)).coerceIn(0f, 1f)
                        val x = index * stepX
                        val y = normalizedY * (h - paddingBottom)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h - paddingBottom)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (index == dataPoints.size - 1) {
                            fillPath.lineTo(x, h - paddingBottom)
                            fillPath.close()
                        }
                    }

                    // Draw gradient under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = h - paddingBottom
                        )
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = AccentBlue,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Draw latest point dot
                    val latestNormalizedY = (1f - ((dataPoints.last() - minVal) / range)).coerceIn(0f, 1f)
                    val lastX = w
                    val lastY = latestNormalizedY * (h - paddingBottom)
                    drawCircle(color = AccentBlue, radius = 5f, center = Offset(lastX, lastY))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(lastX, lastY))
                }
            }
        }
    }
}

/**
 * Bar chart for hourly / daily consumption.
 */
@Composable
fun DailyConsumptionBarChart(
    bars: List<Pair<String, Double>>, // label to kg
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CONSUMPTION BY HOUR (KG)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val maxVal = (bars.maxOfOrNull { it.second } ?: 10.0).toFloat().coerceAtLeast(1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.takeLast(12).forEach { (label, value) ->
                    val fraction = (value.toFloat() / maxVal).coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${value.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(AccentTeal, AccentTeal.copy(alpha = 0.65f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        )
                    }
                }
            }
        }
    }
}
