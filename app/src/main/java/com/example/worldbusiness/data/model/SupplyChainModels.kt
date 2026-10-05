package com.example.worldbusiness.data.model

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val formatted: String
)

data class TransitMilestone(
    val title: String,
    val location: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean = false,
    val note: String = ""
)

enum class TransportMode(val label: String, val iconType: String) {
    OCEAN_VESSEL("Ocean Vessel", "SHIP"),
    AIR_FREIGHT("Air Cargo", "AIRPLANE"),
    OVERLAND_RAIL("Intermodal Rail", "TRAIN"),
    MULTI_MODAL("Express Freight", "TRUCK")
}

data class SupplyChainShipmentDetail(
    val record: ShipmentRecord,
    val transportMode: TransportMode,
    val vesselOrFlightId: String,
    val currentRealTimeLocation: String,
    val gpsCoordinates: GpsCoordinates,
    val transitProgressPercent: Int,
    val speedTelemetry: String,
    val heading: String,
    val environmentalTelemetry: String,
    val satelliteLastPingTime: String,
    val estimatedArrivalDate: String,
    val etaDaysRemaining: Int,
    val etaStatus: String, // "ON_SCHEDULE", "EXPEDITED", "CUSTOMS_HOLD", "ARRIVED"
    val customsClearanceStage: Int, // 1 to 4
    val milestones: List<TransitMilestone>,
    val riskLevel: String // "LOW", "MODERATE", "ELEVATED"
)

data class SupplyChainOverviewStats(
    val totalActiveShipments: Int,
    val inTransitCount: Int,
    val customsClearedCount: Int,
    val inspectionHoldCount: Int,
    val totalCargoValueUsd: Double,
    val onScheduleRatePercent: Double,
    val avgTransitDaysRemaining: Double
)
