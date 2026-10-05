package com.example.worldbusiness.data.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

data class LogisticsPort(
    val code: String,
    val name: String,
    val country: String,
    val point: GeoPoint,
    val isSeaPort: Boolean
)

data class LogisticsChokePoint(
    val id: String,
    val name: String,
    val point: GeoPoint,
    val status: String, // "NORMAL_TRANSIT", "HEAVY_CONVOY", "INSPECTION_DELAY", "CLEAR"
    val dailyTonnageUsd: String
)

data class D3ShipmentRoute(
    val trackingCode: String,
    val vesselOrFlightId: String,
    val carrier: String,
    val transportMode: TransportMode,
    val originPort: LogisticsPort,
    val destinationPort: LogisticsPort,
    val currentPosition: GeoPoint,
    val progressPercent: Int,
    val speedTelemetry: String,
    val heading: String,
    val headingDegrees: Float,
    val cargoDescription: String,
    val cargoValueUsd: Double,
    val currency: String,
    val customsStatus: String,
    val environmentalTelemetry: String,
    val eta: String,
    val waypoints: List<GeoPoint>
)
