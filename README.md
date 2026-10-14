# Smart Sealer Drum Inventory Monitoring System

An industrial-grade IoT digital twin and inventory monitoring platform engineered for automotive assembly plant sealer storage areas. The application continuously monitors adhesive, mastic, and underbody sealer drum levels, follower-plate sensor distances, material consumption, and replenishment forecasts.

---

## 1. System Overview & Value Proposition

In high-volume automotive manufacturing (e.g., Body-in-White framing, door hemming, windshield glazing, and underbody sealing cells), continuous automated dispensing robots consume high-viscosity sealers from 200-liter drums. 
- **The Problem:** Operators traditionally estimate remaining drum levels by manual inspection or waiting for a pump cavitation pressure alarm. An unexpected empty drum halts the production line, causing **$15,000–$40,000 per hour** in downtime losses.
- **The Solution:** A non-invasive laser distance sensor mounted on top of the drum follower-plate tracks plate descent in real time. This system calculates accurate remaining mass, consumption velocity, and automated reorder points with zero downtime risk.

---

## 2. Technology Stack & Architecture

- **Operating System / Target:** Android (API 24+ / Android 14+ compatible)
- **Programming Language:** Kotlin (100% Type-Safe)
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
- **Hardware Abstraction Layer:** `SensorAdapter` interface with decoupled `MockSensorAdapter` (ready for live ESP32 MQTT/HTTP gateway swapping)
- **Data Persistence:** Local JSON-backed transactional persistence via Android SharedPreferences
- **Analytics & Visuals:** Native Jetpack Compose Canvas charts (Real-time stock trajectory, 24-hr hourly consumption bars, and cross-section follower plate diagram)

---

## 3. Project Structure

```text
├── app/
│   ├── build.gradle.kts           # Dependencies, packaging, and signing configuration
│   └── src/main/
│       ├── AndroidManifest.xml    # App manifest, permissions, and FileProvider specs
│       ├── java/com/example/
│       │   ├── MainActivity.kt    # Root UI shell, Drawer navigation, BottomBar, and Dialog routing
│       │   ├── data/
│       │   │   ├── SensorAdapter.kt       # Hardware abstraction layer interface
│       │   │   ├── MockSensorAdapter.kt   # High-fidelity laser physics & fault simulation
│       │   │   └── InventoryRepository.kt # Central repository, transaction ledger & simulation ticker
│       │   ├── model/
│       │   │   └── Models.kt              # Drum, SensorReading, Transaction, Alert & Settings data models
│       │   └── ui/
│       │       ├── SmartSealerViewModel.kt # Central ViewModel managing state and operations
│       │       ├── components/
│       │       │   ├── CommonComponents.kt   # Drums graphics, follower-plate diagram, KPI cards, charts
│       │       │   └── PersistentDemoPanel.kt# Persistent bottom control bar with rapid action buttons
│       │       ├── dialogs/
│       │       │   ├── InventoryDialogs.kt   # Add Drum, Receive, Issue to Line, Record Empty modals
│       │       │   ├── DrumDetailDialog.kt   # Cross-section follower-plate inspector & history
│       │       │   └── HardwareRoadmapDialog.kt # ESP32 pinout, MQTT schemas, and pitch briefing
│       │       ├── screens/
│       │       │   ├── OverviewScreen.kt         # Plant KPIs, trends, and recent transaction stream
│       │       │   ├── DrumMonitoringScreen.kt   # Drum cards, level gauges, filters & quick actions
│       │       │   ├── SensorSimulationScreen.kt # Interactive laser simulation lab & fault injection
│       │       │   ├── InventoryManagementScreen.kt # Mass balance ledger & CSV export
│       │       │   ├── ConsumptionAnalyticsScreen.kt# 24H/7D/30D rate analysis & drum breakdown
│       │       │   ├── SensorHealthScreen.kt     # Sensor diagnostics, bus voltage & fault matrix
│       │       │   ├── AlertsScreen.kt           # Reorder point mathematics & alarms
│       │       │   └── SettingsScreen.kt         # Alarm thresholds, lead times & safety stock
│       │       └── theme/
│       │           ├── Color.kt     # Industrial navy, teal, emerald, amber, and red palette
│       │           ├── Theme.kt     # Material 3 dark/light dynamic theme setup
│       │           └── Type.kt      # Typography definition
│       └── res/
│           ├── drawable/            # Hero banner illustration & vector launcher assets
│           ├── mipmap-*/            # Custom adaptive launcher icon PNGs
│           ├── values/strings.xml   # App name resource ("Smart Sealer")
│           └── xml/file_paths.xml   # FileProvider cache paths for CSV export
├── metadata.json                    # Platform identification metadata
├── settings.gradle.kts              # Gradle project definitions
└── README.md                        # Documentation and user guide
```

---

## 4. Key Mathematical & Physical Formulas

### 1. Follower-Plate Laser Geometry
$$\text{Remaining Fraction} = \frac{\text{Current Mass}}{\text{Nominal Capacity}}$$
$$\text{Follower Plate Travel Distance} = d_{\text{Full}} + (1.0 - \text{Remaining Fraction}) \times H$$
- $H$: Calibrated usable travel of follower plate ($85.0\text{ cm}$ for standard 200L drums)
- $d_{\text{Full}}$: Sensor top offset distance ($10.0\text{ cm}$)
- $d_{\text{Empty}}$: Sensor reading when drum is empty ($95.0\text{ cm}$)

### 2. Inverse Mass Estimation from Laser Telemetry
$$\text{Estimated Mass (kg)} = \text{Nominal Capacity} \times \left(1.0 - \frac{d_{\text{Measured}} - d_{\text{Full}}}{H}\right)$$

### 3. Inventory Mass Conservation (Closing Stock)
$$\text{Closing Stock} = \text{Opening Stock} + \text{Receipts} - \text{Consumption} - \text{Documented Losses} + \text{Usable Returns}$$
*(Packaging tare weight of $15.0\text{ kg}$ is excluded from active sealer inventory.)*

### 4. Automated Reorder Point
$$\text{Reorder Point (kg)} = (\text{Average Daily Consumption} \times \text{Supplier Lead Time}) + \text{Safety Stock}$$

---

## 5. Local Development & Installation Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or Ladybug (2024.2+)
- Android SDK 34+ installed
- Java Development Kit (JDK 17 or JDK 21)

### Steps
1. Clone the repository:
   ```bash
   git clone <repository_url>
   cd smart-sealer-inventory
   ```
2. Build the project using Gradle:
   ```bash
   gradle assembleDebug
   ```
3. Run Robolectric and unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Install on a connected Android device or emulator:
   ```bash
   gradle installDebug
   ```

---

## 6. Cloud & Deployment Options

1. **AI Studio Streaming Emulator:**
   - The app compiles and installs natively into the streaming cloud emulator.
2. **Generating Release APK or AAB for Google Play / MDM:**
   - Run `gradle assembleRelease` or `gradle bundleRelease`.
   - The resulting APK/AAB is signed and ready for enterprise Mobile Device Management (MDM) deployment on Zebra or Honeywell rugged industrial Android scanners.

---

## 7. Short Presentation Script for Manufacturing Plant Managers

> **Speaker:** Lead IoT / Systems Engineer  
> **Audience:** Automotive Plant Operations Manager & Maintenance Supervisor  
>
> *"Good morning. When an automotive sealer drum runs dry in Body-in-White framing or door hemming, line stoppage costs exceed \$30,000 per hour. Currently, our technicians tap drums with mallets or wait for pump cavitation alarms.*
>
> *Today, we present the Smart Sealer Drum Inventory Monitoring System. By clamping an optical Time-of-Flight laser sensor to the top of the drum's ram plate, we track the pneumatic follower plate's position with millimeter precision without breaching pressurized sealer lines.*
>
> *Here is what this system delivers:*
> *1. **Predictive Replenishment:** Real-time stock coverage alerts warn logistics hours in advance before a drum reaches critical levels.*
> *2. **Accurate Mass Balance:** Our closing stock ledger prevents double-counting and provides automated CSV audits.*
> *3. **Fault-Tolerant Telemetry:** The system detects sensor disconnects, optical noise, and frozen readings immediately, preventing false stock assumptions.*
> *4. **Rapid Payback:** The entire hardware and software deployment pays for itself by preventing a single unplanned line stoppage.*
>
> *Let's review the live digital twin simulation on the dashboard now."*

---

## 8. Hardware Integration Roadmap (ESP32 & Industrial Sensors)

1. **Microcontroller:** ESP32-S3-WROOM-1 dual-core 240MHz with Wi-Fi / BLE and industrial watchdog timer.
2. **Sensors:**
   - *Phase 1 (Pilot):* ST VL53L1X Time-of-Flight distance sensor via I2C.
   - *Phase 2 (Production):* Banner Engineering Q4X or Sick OD Mini laser sensor via RS-485 Modbus RTU / IO-Link.
3. **Power:** 24V DC automotive line bus stepped down to 5V/3.3V DC via an industrial buck regulator.
4. **Enclosure:** IP67 polycarbonate with magnetic base clamp for drum lids.
5. **MQTT Protocol:**
   - Topic: `plant/sealer/{cell_id}/{drum_id}/telemetry`
   - Broker: EMQX or HiveMQ Enterprise Edge broker deployed on-premise.
6. **Integration:** Implement the clean `SensorAdapter` interface with an `MqttSensorAdapter` to feed live sensor data directly into the application with zero UI modifications.
