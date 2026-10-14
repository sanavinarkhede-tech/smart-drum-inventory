package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun HardwareRoadmapDialog(onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var selectedSection by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_hardware_roadmap"),
            shape = RoundedCornerShape(16.dp),
            color = DashboardBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Surface(
                    color = IndustrialNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "IOT HARDWARE INTEGRATION & BRIEFING",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Technical Roadmap, ESP32 Architecture & Executive Pitch",
                                style = MaterialTheme.typography.bodySmall.copy(color = LaserCyan)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndustrialNavy),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "1. Executive Pitch",
                        "2. ESP32 & Sensors",
                        "3. MQTT & Telemetry",
                        "4. Software Layer"
                    ).forEachIndexed { index, title ->
                        val isSelected = selectedSection == index
                        TextButton(
                            onClick = { selectedSection = index },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) LaserCyan else Color(0xFFCBD5E1),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedSection) {
                        0 -> ExecutivePitchSection()
                        1 -> Esp32HardwareSection()
                        2 -> MqttTelemetrySection()
                        3 -> SoftwareLayerSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutivePitchSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "PITCH SCRIPT FOR PLANT & OPERATIONS MANAGERS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IndustrialNavy)
            )

            Text(
                text = "\"Good morning, team. In our automotive body shop, line stoppages due to an unexpectedly empty sealer drum cost between \$15,000 and \$40,000 per hour. Currently, operators visually inspect drums by tapping on steel barrels or relying on pressure drop alarms after cavitation has already occurred.\n\n" +
                        "Today, we introduce the Smart Sealer Drum Inventory Monitoring System: a non-invasive, digital twin monitoring platform. By mounting an industrial-grade laser distance sensor directly onto the top ram plate of each 200-liter drum, we measure follower-plate travel in real time with millimeter accuracy.\n\n" +
                        "Key Value Drivers:\n" +
                        "1. Zero Unplanned Downtime: Predictive reorder alerts give logistics 4+ hours of lead time before a drum empties.\n" +
                        "2. Exact Material Accounting: We track consumption per shift, eliminating double-counting and material theft.\n" +
                        "3. Non-Invasive Retrofit: No plumbing cuts or pressure breaches; the sensor simply monitors the external follower plate.\n" +
                        "4. Fast ROI: Payback achieved on the first prevented line stoppage.\"",
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 19.sp)
            )
        }
    }
}

@Composable
private fun Esp32HardwareSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ESP32 & SENSOR HARDWARE SPECIFICATION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IndustrialNavy)
            )

            CodeSnippetBox(
                """
Hardware Bill of Materials (BOM):
• Microcontroller: ESP32-S3-WROOM-1 (Dual-core 240MHz, Wi-Fi 4 + BLE 5, 8MB Flash)
• Laser Distance Sensor: 
  - Prototyping: STMicroelectronics VL53L1X Time-of-Flight (up to 4m, I2C)
  - Industrial Production: Banner Engineering Q4X or Sick OD Mini (RS-485 Modbus RTU / IO-Link)
• Power Conditioning: 24V DC plant bus stepped down to 5V/3.3V DC via TI LM2596 buck converter
• Industrial Enclosure: IP67-rated polycarbonate enclosure with M12 circular connectors
• Physical Mounting: Magnetic bracket clamped directly to drum ram lid

Wiring Pinout (ESP32-S3):
• 24V DC -> DC-DC Buck (IN+)
• GND -> DC-DC Buck (IN-) -> ESP32 GND
• 5V OUT -> ESP32 5V (VBUS)
• GPIO 21 -> I2C SDA (VL53L1X)
• GPIO 22 -> I2C SCL (VL53L1X)
• GPIO 17 -> RS-485 TX (MAX485 DI for industrial Modbus)
• GPIO 16 -> RS-485 RX (MAX485 RO)
• GPIO 4  -> RS-485 DE/RE (Direction control)
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun MqttTelemetrySection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "MQTT BROKER TOPIC & TELEMETRY JSON SCHEMA",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IndustrialNavy)
            )

            CodeSnippetBox(
                """
MQTT Topic:
plant/sealer/cell_01/drum_sd001/telemetry

Payload Format (JSON):
{
  "device_id": "ESP32_SEALER_001",
  "drum_id": "SD-001",
  "timestamp_utc": 1791944762,
  "distance_cm": 25.8,
  "signal_rate_mcps": 14.2,
  "ambient_rate_mcps": 1.1,
  "optical_status": "VALID",
  "bus_voltage_v": 24.12,
  "temperature_c": 28.4,
  "raw_rssi_dbm": -58
}

Publish Frequency:
• Steady state: Every 2.0 seconds
• On rapid consumption (> 0.5 kg/min): 500 ms burst mode
• Keep-alive ping: Every 30.0 seconds
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun SoftwareLayerSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CLEAN ARCHITECTURE: SENSORADAPTER ABSTRACTION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IndustrialNavy)
            )

            Text(
                text = "The application implements the Dependency Inversion Principle. The UI and calculations depend strictly on the SensorAdapter interface. Replacing MockSensorAdapter with a real MqttSensorAdapter requires zero changes to the UI, charts, or inventory formulas.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            CodeSnippetBox(
                """
// Hardware Adapter Implementation Roadmap:
class MqttSensorAdapter(
    private val mqttClient: Mqtt5AsyncClient
) : SensorAdapter {
    override suspend fun readDistance(drum: Drum): SensorReading {
        // Reads latest decoded MQTT message from broker
        val packet = mqttClient.getLatestPacket(drum.id)
        return packet.toSensorReading()
    }
    // ...
}
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun CodeSnippetBox(code: String) {
    Surface(
        color = IndustrialNavyDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFF8FAFC),
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}
