package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.GpsCoordinates
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.SupplyChainOverviewStats
import com.example.worldbusiness.data.model.SupplyChainShipmentDetail
import com.example.worldbusiness.data.model.TransitMilestone
import com.example.worldbusiness.data.model.TransportMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object SupplyChainTrackingEngine {

    fun buildShipmentDetail(
        record: ShipmentRecord,
        pingTickCount: Int = 0
    ): SupplyChainShipmentDetail {
        val carrierUpper = record.carrier.uppercase(Locale.US)
        val isAir = carrierUpper.contains("AIR") ||
            carrierUpper.contains("LUFTHANSA") ||
            carrierUpper.contains("ANA") ||
            carrierUpper.contains("BOEING") ||
            carrierUpper.contains("EXPRESS")

        val transportMode = if (isAir) TransportMode.AIR_FREIGHT else TransportMode.OCEAN_VESSEL

        // Specialized deterministic telemetry mapping per seed tracking code,
        // with fallback dynamic telemetry for user-dispatched freight
        return when (record.trackingCode) {
            "WBOS-FRT-9921" -> {
                val pingShift = (pingTickCount % 5) * 0.15
                val lat = 12.58 + pingShift
                val lon = 43.33 + pingShift * 1.2
                val progress = (68 + (pingTickCount % 5) * 2).coerceIn(10, 98)

                SupplyChainShipmentDetail(
                    record = record,
                    transportMode = TransportMode.OCEAN_VESSEL,
                    vesselOrFlightId = "Vessel: Triple-E Madrid (IMO 9632064)",
                    currentRealTimeLocation = "Bab el-Mandeb Strait • Red Sea Maritime Corridor",
                    gpsCoordinates = GpsCoordinates(lat, lon, String.format(Locale.US, "%.2f° N, %.2f° E", lat, lon)),
                    transitProgressPercent = progress,
                    speedTelemetry = "19.8 knots • Ocean Cruising",
                    heading = "320° NW (Heading to Suez Canal)",
                    environmentalTelemetry = "IoT Sealed • Internal Temp: 21.2°C • Humidity: 38% • Shock: 0.01G Normal",
                    satelliteLastPingTime = "Live AIS Satellite • Just Now",
                    estimatedArrivalDate = record.estimatedArrival,
                    etaDaysRemaining = 4,
                    etaStatus = "ON_SCHEDULE",
                    customsClearanceStage = 2, // International Transit Active
                    milestones = listOf(
                        TransitMilestone("Origin Yantian Terminal Gate-In", "Shenzhen Port (SZX)", "Sep 22, 2026 • 08:30 UTC", true),
                        TransitMilestone("China Customs Export Clearance", "Shenzhen Customs (SZX)", "Sep 23, 2026 • 14:15 UTC", true),
                        TransitMilestone("Malacca Strait Maritime Transit", "Singapore Strait Rail", "Sep 26, 2026 • 02:40 UTC", true),
                        TransitMilestone("Bab el-Mandeb Red Sea Waypoint", "Red Sea Corridor Bravo", "Live GPS Position", true, isCurrent = true, "En route to Suez Canal convoy slot"),
                        TransitMilestone("Port of Rotterdam Pier Berth", "Rotterdam Terminal (RTM)", "Scheduled Oct 04, 2026", false),
                        TransitMilestone("EU Single Market Import Release", "Rotterdam Customs", "Scheduled Oct 05, 2026", false)
                    ),
                    riskLevel = "LOW"
                )
            }

            "WBOS-AIR-8012" -> {
                val pingShift = (pingTickCount % 5) * 0.4
                val lat = 11.85 + pingShift
                val lon = 92.74 + pingShift * 0.8
                val progress = (82 + (pingTickCount % 5) * 3).coerceIn(15, 99)

                SupplyChainShipmentDetail(
                    record = record,
                    transportMode = TransportMode.AIR_FREIGHT,
                    vesselOrFlightId = "Flight: LH8410 (Boeing 777-200F Heavy)",
                    currentRealTimeLocation = "Bay of Bengal High-Altitude Corridor (Waypoint VAMPI FL380)",
                    gpsCoordinates = GpsCoordinates(lat, lon, String.format(Locale.US, "%.2f° N, %.2f° E", lat, lon)),
                    transitProgressPercent = progress,
                    speedTelemetry = "885 km/h (478 knots) • Altitude 38,000 ft",
                    heading = "128° SE (Direct to Changi Approach)",
                    environmentalTelemetry = "Clean-Air Spec Class 1000 • Pressurized Hold: 19.4°C • Stable",
                    satelliteLastPingTime = "ADS-B Avionics Telemetry • Active",
                    estimatedArrivalDate = record.estimatedArrival,
                    etaDaysRemaining = 1,
                    etaStatus = "EXPEDITED",
                    customsClearanceStage = 3, // Cleared & Final Approach
                    milestones = listOf(
                        TransitMilestone("CargoCity South ULD Palletizing", "Frankfurt CargoCity (FRA)", "Sep 29, 2026 • 18:00 UTC", true),
                        TransitMilestone("German Zoll Export Electronic Clearance", "Frankfurt Zollamt", "Sep 29, 2026 • 21:30 UTC", true),
                        TransitMilestone("Direct Trans-Continental Flight Departure", "FRA Runway 18", "Sep 30, 2026 • 01:15 UTC", true),
                        TransitMilestone("Bay of Bengal Oceanic Transit", "Waypoint VAMPI FL380", "Live Telemetry", true, isCurrent = true, "Pre-arrival electronic clearance active"),
                        TransitMilestone("Singapore Changi Airfreight Center", "SIN Airport Terminal", "Scheduled Oct 01, 2026", false)
                    ),
                    riskLevel = "LOW"
                )
            }

            "WBOS-SEA-3410" -> {
                val lat = 40.48
                val lon = -74.02

                SupplyChainShipmentDetail(
                    record = record,
                    transportMode = TransportMode.OCEAN_VESSEL,
                    vesselOrFlightId = "Vessel: CMA CGM Pegasus (IMO 9399210)",
                    currentRealTimeLocation = "Port of New York / Newark Outer Harbor Anchoring Bay",
                    gpsCoordinates = GpsCoordinates(lat, lon, String.format(Locale.US, "%.2f° N, %.2f° W", lat, abs(lon))),
                    transitProgressPercent = 95,
                    speedTelemetry = "2.1 knots • Harbor Anchorage / Pilot Onboard",
                    heading = "010° N (Anchored)",
                    environmentalTelemetry = "Bulk Grain / Biomass Hold • Humidity 11.8% • Pest-Free Certified",
                    satelliteLastPingTime = "Port VTS Radar • Live",
                    estimatedArrivalDate = record.estimatedArrival,
                    etaDaysRemaining = 2,
                    etaStatus = "CUSTOMS_HOLD",
                    customsClearanceStage = 3, // Destination Port Inspection
                    milestones = listOf(
                        TransitMilestone("Santos Berth Quay Loading", "Port of Santos (SSZ), Brazil", "Sep 15, 2026 • 12:00 UTC", true),
                        TransitMilestone("Receita Federal Export Clearance", "Santos Customs", "Sep 16, 2026 • 16:30 UTC", true),
                        TransitMilestone("North Atlantic Sea-Lane Transit", "Atlantic Basin Corridor", "Sep 24, 2026 • 09:00 UTC", true),
                        TransitMilestone("New York Harbor Outer Roads Anchorage", "Lower New York Bay", "Live Status", true, isCurrent = true, "USDA-APHIS Phytosanitary Port Inspection in progress"),
                        TransitMilestone("Maher Terminal Berth Discharge", "Port Newark (EWR)", "Scheduled Oct 03, 2026", false),
                        TransitMilestone("US CBP Form 7501 Final Release", "New York Port District", "Scheduled Oct 12, 2026", false)
                    ),
                    riskLevel = "MODERATE"
                )
            }

            "WBOS-AIR-4109" -> {
                val pingShift = (pingTickCount % 5) * 0.1
                val lat = 53.40 + pingShift
                val lon = 1.25 + pingShift

                SupplyChainShipmentDetail(
                    record = record,
                    transportMode = TransportMode.AIR_FREIGHT,
                    vesselOrFlightId = "Flight: NH8485 (Boeing 777F)",
                    currentRealTimeLocation = "North Sea Airspace • UK Radar Vectoring Corridor (FL180)",
                    gpsCoordinates = GpsCoordinates(lat, lon, String.format(Locale.US, "%.2f° N, %.2f° E", lat, lon)),
                    transitProgressPercent = 92,
                    speedTelemetry = "640 km/h • Descending through FL180",
                    heading = "255° WSW (Direct to Heathrow LHR)",
                    environmentalTelemetry = "Precision Sealed • Anti-Static Anti-Vibration Cradles Normal",
                    satelliteLastPingTime = "NATS Radar Tracking • Live",
                    estimatedArrivalDate = record.estimatedArrival,
                    etaDaysRemaining = 2,
                    etaStatus = "ON_SCHEDULE",
                    customsClearanceStage = 3, // Cleared & Approach
                    milestones = listOf(
                        TransitMilestone("Haneda Freight Terminal Induction", "Tokyo Haneda (HND)", "Sep 29, 2026 • 10:00 UTC", true),
                        TransitMilestone("Japan Customs Export Authorization", "Tokyo Customs Air Cargo", "Sep 29, 2026 • 15:40 UTC", true),
                        TransitMilestone("Polar Flight Route Direct Transit", "Arctic Great Circle Corridor", "Sep 30, 2026 • 04:20 UTC", true),
                        TransitMilestone("North Sea Descent Sector", "Waypoint EKRUL FL180", "Live Telemetry", true, isCurrent = true, "UK HMRC electronic pre-clearance green"),
                        TransitMilestone("London Heathrow Cargo Hub", "LHR WorldCargo Terminal", "Scheduled Oct 02, 2026", false)
                    ),
                    riskLevel = "LOW"
                )
            }

            else -> {
                // Dynamic generator for newly dispatched shipments
                val progress = 35 + (pingTickCount % 10) * 5
                val lat = 25.0 + (record.id % 20)
                val lon = 55.0 + (record.id % 40)

                val daysRemaining = when (record.customsStatus) {
                    "CLEARED" -> 1
                    "IN_TRANSIT" -> 5
                    "PORT_INSPECTION" -> 3
                    else -> 6
                }

                SupplyChainShipmentDetail(
                    record = record,
                    transportMode = transportMode,
                    vesselOrFlightId = if (isAir) "Air Freight Express (Direct AWB)" else "Commercial Cargo Vessel",
                    currentRealTimeLocation = "${record.origin.substringBefore(",")} ➔ ${record.destination.substringBefore(",")} Transit Sector",
                    gpsCoordinates = GpsCoordinates(lat, lon, String.format(Locale.US, "%.2f° N, %.2f° E", lat, lon)),
                    transitProgressPercent = progress.coerceIn(10, 95),
                    speedTelemetry = if (isAir) "780 km/h • Air Transit" else "18.5 knots • Marine Transit",
                    heading = "270° W",
                    environmentalTelemetry = "Dry Storage • Ambient Conditions Normal",
                    satelliteLastPingTime = "Live AIS / ADS-B • Verified",
                    estimatedArrivalDate = record.estimatedArrival,
                    etaDaysRemaining = daysRemaining,
                    etaStatus = if (record.customsStatus == "PORT_INSPECTION") "CUSTOMS_HOLD" else "ON_SCHEDULE",
                    customsClearanceStage = if (record.customsStatus == "CLEARED") 4 else 2,
                    milestones = listOf(
                        TransitMilestone("Origin Hub Gate-In", record.origin, "Sep 28, 2026 • 10:00 UTC", true),
                        TransitMilestone("Export Customs Clearance", "Origin Terminal Customs", "Sep 29, 2026 • 14:00 UTC", true),
                        TransitMilestone("International Commercial Transit", "Active Corridor", "Live Telemetry", true, isCurrent = true),
                        TransitMilestone("Destination Terminal Clearance", record.destination, "Scheduled: ${record.estimatedArrival}", false)
                    ),
                    riskLevel = if (record.customsStatus == "PORT_INSPECTION") "MODERATE" else "LOW"
                )
            }
        }
    }

    fun buildOverviewStats(details: List<SupplyChainShipmentDetail>): SupplyChainOverviewStats {
        val totalActive = details.size
        val inTransit = details.count { it.record.customsStatus == "IN_TRANSIT" }
        val cleared = details.count { it.record.customsStatus == "CLEARED" }
        val inspection = details.count { it.record.customsStatus == "PORT_INSPECTION" || it.record.customsStatus == "DOCUMENTATION_REQ" }
        val totalValue = details.sumOf { it.record.cargoValue }
        val onSchedule = if (totalActive > 0) {
            (details.count { it.etaStatus == "ON_SCHEDULE" || it.etaStatus == "EXPEDITED" }.toDouble() / totalActive) * 100.0
        } else 100.0
        val avgDays = if (details.isNotEmpty()) details.map { it.etaDaysRemaining }.average() else 0.0

        return SupplyChainOverviewStats(
            totalActiveShipments = totalActive,
            inTransitCount = inTransit,
            customsClearedCount = cleared,
            inspectionHoldCount = inspection,
            totalCargoValueUsd = totalValue,
            onScheduleRatePercent = onSchedule,
            avgTransitDaysRemaining = avgDays
        )
    }
}
