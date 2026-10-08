package com.example.worldbusiness.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Global Logistics Corridor Hub representing prime international seaports and air hubs.
 */
data class LogisticsCorridorHub(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val primaryFacility: String,
    val isSeaport: Boolean = true,
    val isAirport: Boolean = true
)

enum class LogisticsFreightMode(
    val label: String,
    val iconEmoji: String,
    val averageSpeedKnotsOrKmh: Double,
    val speedUnit: String,
    val baseHandlingDwellHours: Int,
    val description: String
) {
    AIR_CARGO("Air Cargo", "✈️", 850.0, "km/h", 18, "High-priority direct trans-oceanic flights"),
    EXPRESS_COURIER("Express Courier", "⚡", 900.0, "km/h", 8, "Expedited door-to-door priority freight (DHL/FedEx)"),
    OCEAN_CONTAINER("Ocean Container", "🚢", 20.0, "knots", 72, "Ultra-large container vessels (20k+ TEU)"),
    TRANS_EURASIAN_RAIL("Eurasian Rail", "🚆", 70.0, "km/h", 36, "Belt & Road trans-continental freight rail")
}

enum class CustomsClearanceCategory(
    val label: String,
    val bufferDays: Int,
    val regulatoryStandard: String
) {
    STANDARD_GENERAL("General Commercial Goods", 1, "WCO Harmonized System (HS) Standard"),
    HIGH_TECH_DUAL_USE("High-Tech Electronics & Chips", 2, "Wassenaar Arrangement / Dual-Use"),
    PHARMACEUTICAL_COLD("Pharmaceuticals & Cold Chain", 2, "GDP / WHO Temperature Sensitive Protocol"),
    PHYTOSANITARY_AGRI("Agricultural & Perishables", 3, "USDA / EFSA Phytosanitary Inspection"),
    HAZMAT_CHEMICALS("Chemicals & HazMat", 4, "IMO IMDG Code Dangerous Goods Class")
}

data class DeliveryCalculationResult(
    val originHub: LogisticsCorridorHub,
    val destinationHub: LogisticsCorridorHub,
    val freightMode: LogisticsFreightMode,
    val clearanceCategory: CustomsClearanceCategory,
    val geodesicDistanceKm: Double,
    val nauticalMiles: Double,
    val baseTransitHours: Double,
    val baseTransitDays: Int,
    val terminalDwellDays: Int,
    val customsClearanceDays: Int,
    val contingencyBufferDays: Int,
    val totalTransitDays: Int,
    val dispatchDateString: String,
    val calculatedEstimatedDeliveryDate: String,
    val deliveryConfidencePercent: Int,
    val carbonEstimateKgCo2: Double,
    val corridorRiskRating: String, // "LOW", "MODERATE", "ELEVATED"
    val formulaExplanation: String,
    val recommendedIncoterm: String
)
