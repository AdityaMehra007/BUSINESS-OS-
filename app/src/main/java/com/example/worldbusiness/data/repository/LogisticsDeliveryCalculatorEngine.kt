package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CustomsClearanceCategory
import com.example.worldbusiness.data.model.DeliveryCalculationResult
import com.example.worldbusiness.data.model.LogisticsCorridorHub
import com.example.worldbusiness.data.model.LogisticsFreightMode
import com.example.worldbusiness.data.model.ShipmentRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enterprise Logistics Delivery Calculator Engine.
 * Computes high-precision international transit times, great-circle distances,
 * modal velocity, customs terminal dwell, and estimated delivery dates (ETAs).
 */
object LogisticsDeliveryCalculatorEngine {

    val GLOBAL_TRADE_HUBS: List<LogisticsCorridorHub> = listOf(
        LogisticsCorridorHub(
            code = "SHA",
            name = "Shanghai / Yantian Hub",
            city = "Shanghai",
            country = "China",
            latitude = 31.2304,
            longitude = 121.4737,
            primaryFacility = "Port of Shanghai Yangshan Terminal"
        ),
        LogisticsCorridorHub(
            code = "SIN",
            name = "Singapore Gateway Hub",
            city = "Singapore",
            country = "Singapore",
            latitude = 1.3521,
            longitude = 103.8198,
            primaryFacility = "PSA Singapore Mega Port & Changi Airport (SIN)"
        ),
        LogisticsCorridorHub(
            code = "RTM",
            name = "Rotterdam Gateway Port",
            city = "Rotterdam",
            country = "Netherlands",
            latitude = 51.9244,
            longitude = 4.4777,
            primaryFacility = "Port of Rotterdam Maasvlakte II Terminal"
        ),
        LogisticsCorridorHub(
            code = "FRA",
            name = "Frankfurt CargoCity Hub",
            city = "Frankfurt",
            country = "Germany",
            latitude = 50.0379,
            longitude = 8.5622,
            primaryFacility = "Frankfurt Airport CargoCity South (FRA)"
        ),
        LogisticsCorridorHub(
            code = "NYC",
            name = "New York / Newark Terminal",
            city = "New York",
            country = "United States",
            latitude = 40.7128,
            longitude = -74.0060,
            primaryFacility = "Port Newark Container Terminal (PNCT)"
        ),
        LogisticsCorridorHub(
            code = "LAX",
            name = "Los Angeles / Long Beach Hub",
            city = "Los Angeles",
            country = "United States",
            latitude = 33.7432,
            longitude = -118.2673,
            primaryFacility = "Port of Los Angeles Pier 400"
        ),
        LogisticsCorridorHub(
            code = "LON",
            name = "London Heathrow / Felixstowe",
            city = "London",
            country = "United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            primaryFacility = "London Heathrow Cargo Hub (LHR)"
        ),
        LogisticsCorridorHub(
            code = "TYO",
            name = "Tokyo Haneda / Yokohama",
            city = "Tokyo",
            country = "Japan",
            latitude = 35.6762,
            longitude = 139.6503,
            primaryFacility = "Port of Yokohama & Haneda Air Cargo (HND)"
        ),
        LogisticsCorridorHub(
            code = "DXB",
            name = "Dubai Jebel Ali Logistics Freezone",
            city = "Dubai",
            country = "United Arab Emirates",
            latitude = 25.2048,
            longitude = 55.2708,
            primaryFacility = "DP World Jebel Ali Port & DWC Cargo Airport"
        ),
        LogisticsCorridorHub(
            code = "SSZ",
            name = "Santos / Sao Paulo Gateway",
            city = "Santos",
            country = "Brazil",
            latitude = -23.9618,
            longitude = -46.3322,
            primaryFacility = "Port of Santos Brasil Terminal"
        )
    )

    /**
     * Computes the great circle distance between two GPS coordinates using the Haversine formula.
     */
    fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Resolves matching corridor hub by text (e.g. "Shenzhen", "Rotterdam", "US", "Singapore").
     */
    fun findHubByKeyword(query: String, defaultHub: LogisticsCorridorHub = GLOBAL_TRADE_HUBS[0]): LogisticsCorridorHub {
        val q = query.lowercase(Locale.US)
        return GLOBAL_TRADE_HUBS.find { hub ->
            hub.code.lowercase(Locale.US).contains(q) ||
                hub.city.lowercase(Locale.US).contains(q) ||
                hub.name.lowercase(Locale.US).contains(q) ||
                hub.country.lowercase(Locale.US).contains(q)
        } ?: defaultHub
    }

    /**
     * Calculates delivery estimates, transit breakdown, and target arrival date.
     */
    fun calculateDeliveryEstimate(
        originHub: LogisticsCorridorHub,
        destinationHub: LogisticsCorridorHub,
        freightMode: LogisticsFreightMode,
        clearanceCategory: CustomsClearanceCategory = CustomsClearanceCategory.STANDARD_GENERAL,
        dispatchDate: Date = Date()
    ): DeliveryCalculationResult {
        val distanceKm = calculateHaversineDistanceKm(
            originHub.latitude, originHub.longitude,
            destinationHub.latitude, destinationHub.longitude
        ).coerceAtLeast(350.0)

        // For ocean freight, route distance is typically ~1.25x great-circle due to sea-lanes (canals, straits)
        val corridorDistanceKm = if (freightMode == LogisticsFreightMode.OCEAN_CONTAINER) distanceKm * 1.28 else distanceKm * 1.08
        val nauticalMiles = corridorDistanceKm * 0.539957

        // Transit duration calculation
        val effectiveSpeedKmh = when (freightMode) {
            LogisticsFreightMode.OCEAN_CONTAINER -> freightMode.averageSpeedKnotsOrKmh * 1.852 // convert knots to km/h (~37 km/h)
            else -> freightMode.averageSpeedKnotsOrKmh
        }

        val cruisingHours = corridorDistanceKm / effectiveSpeedKmh
        val cruisingDays = (cruisingHours / 24.0).roundToInt().coerceAtLeast(1)

        val dwellDays = (freightMode.baseHandlingDwellHours / 24.0).roundToInt().coerceAtLeast(1)
        val customsDays = clearanceCategory.bufferDays

        // Contingency buffer based on distance and mode
        val contingencyDays = when {
            freightMode == LogisticsFreightMode.OCEAN_CONTAINER && corridorDistanceKm > 8000 -> 3
            freightMode == LogisticsFreightMode.OCEAN_CONTAINER -> 2
            freightMode == LogisticsFreightMode.TRANS_EURASIAN_RAIL -> 2
            else -> 1
        }

        val totalDays = cruisingDays + dwellDays + customsDays + contingencyDays

        // Compute Arrival Date
        val calendar = Calendar.getInstance().apply {
            time = dispatchDate
            add(Calendar.DAY_OF_YEAR, totalDays)
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val dispatchDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(dispatchDate)
        val arrivalDateStr = dateFormat.format(calendar.time)

        // Carbon Footprint Estimate (kg CO2)
        // Air: ~0.50 kg CO2/tonne-km, Ocean: ~0.015 kg CO2/tonne-km, Rail: ~0.03 kg CO2/tonne-km
        val emissionFactor = when (freightMode) {
            LogisticsFreightMode.AIR_CARGO -> 0.52
            LogisticsFreightMode.EXPRESS_COURIER -> 0.60
            LogisticsFreightMode.OCEAN_CONTAINER -> 0.018
            LogisticsFreightMode.TRANS_EURASIAN_RAIL -> 0.035
        }
        val estimatedCarbonKg = (corridorDistanceKm * emissionFactor * 2.5).coerceAtLeast(45.0)

        // Corridor Risk Rating
        val riskRating = when {
            freightMode == LogisticsFreightMode.OCEAN_CONTAINER && corridorDistanceKm > 10000 -> "MODERATE"
            clearanceCategory == CustomsClearanceCategory.HAZMAT_CHEMICALS -> "ELEVATED"
            else -> "LOW"
        }

        val confidence = when {
            freightMode == LogisticsFreightMode.EXPRESS_COURIER -> 98
            freightMode == LogisticsFreightMode.AIR_CARGO -> 94
            freightMode == LogisticsFreightMode.TRANS_EURASIAN_RAIL -> 89
            else -> 91
        }

        val explanation = "${originHub.city} (${originHub.code}) ➔ ${destinationHub.city} (${destinationHub.code}): " +
            "${String.format(Locale.US, "%,d", corridorDistanceKm.roundToInt())} km corridor via ${freightMode.label}. " +
            "Pure transit: ${cruisingDays}d + Origin/Destination handling: ${dwellDays}d + Customs: ${customsDays}d + Contingency: ${contingencyDays}d = Total ${totalDays} days."

        val incoterm = when (freightMode) {
            LogisticsFreightMode.OCEAN_CONTAINER -> "CIF / FOB"
            LogisticsFreightMode.AIR_CARGO -> "CPT / CIP"
            LogisticsFreightMode.EXPRESS_COURIER -> "DDP (Delivered Duty Paid)"
            LogisticsFreightMode.TRANS_EURASIAN_RAIL -> "DAP (Delivered at Place)"
        }

        return DeliveryCalculationResult(
            originHub = originHub,
            destinationHub = destinationHub,
            freightMode = freightMode,
            clearanceCategory = clearanceCategory,
            geodesicDistanceKm = corridorDistanceKm,
            nauticalMiles = nauticalMiles,
            baseTransitHours = cruisingHours,
            baseTransitDays = cruisingDays,
            terminalDwellDays = dwellDays,
            customsClearanceDays = customsDays,
            contingencyBufferDays = contingencyDays,
            totalTransitDays = totalDays,
            dispatchDateString = dispatchDateStr,
            calculatedEstimatedDeliveryDate = arrivalDateStr,
            deliveryConfidencePercent = confidence,
            carbonEstimateKgCo2 = estimatedCarbonKg,
            corridorRiskRating = riskRating,
            formulaExplanation = explanation,
            recommendedIncoterm = incoterm
        )
    }

    /**
     * Recalculates estimated arrival date when customs status is updated in real-time.
     */
    fun recalibrateShipmentEta(currentRecord: ShipmentRecord, newStatus: String): String {
        val calendar = Calendar.getInstance()

        // Buffer adjustments depending on updated status
        val daysToAdd = when (newStatus.uppercase(Locale.US)) {
            "DELIVERED" -> 0
            "CLEARED" -> 1 // expedited last-mile local delivery
            "IN_TRANSIT" -> 4
            "PORT_INSPECTION" -> 6 // includes 2-3 business days inspection dwell
            "DOCUMENTATION_REQ" -> 7 // customs hold waiting documentation
            "CUSTOMS_HOLD" -> 8
            else -> 5
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(calendar.time)
    }
}
