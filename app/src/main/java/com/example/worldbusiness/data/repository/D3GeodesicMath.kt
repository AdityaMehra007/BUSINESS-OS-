package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.D3ShipmentRoute
import com.example.worldbusiness.data.model.GeoPoint
import com.example.worldbusiness.data.model.LogisticsChokePoint
import com.example.worldbusiness.data.model.LogisticsPort
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.SupplyChainShipmentDetail
import com.example.worldbusiness.data.model.TransportMode
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object D3GeodesicMath {

    val GLOBAL_CHOKE_POINTS = listOf(
        LogisticsChokePoint("SUEZ", "Suez Canal", GeoPoint(30.58, 32.56), "NORMAL_TRANSIT", "$4.2B/day"),
        LogisticsChokePoint("MALACCA", "Strait of Malacca", GeoPoint(4.15, 100.50), "HEAVY_CONVOY", "$3.8B/day"),
        LogisticsChokePoint("PANAMA", "Panama Canal", GeoPoint(9.12, -79.72), "NORMAL_TRANSIT", "$1.9B/day"),
        LogisticsChokePoint("MANDAB", "Bab el-Mandeb", GeoPoint(12.58, 43.33), "INSPECTION_DELAY", "$2.4B/day"),
        LogisticsChokePoint("GIBRALTAR", "Strait of Gibraltar", GeoPoint(35.98, -5.60), "CLEAR", "$1.7B/day"),
        LogisticsChokePoint("DOVER", "English Channel", GeoPoint(51.12, 1.35), "NORMAL_TRANSIT", "$2.1B/day")
    )

    private val KNOWN_PORTS = mapOf(
        "SHENZHEN" to LogisticsPort("SZX", "Port of Shenzhen (Yantian)", "China", GeoPoint(22.58, 114.28), true),
        "ROTTERDAM" to LogisticsPort("RTM", "Port of Rotterdam (Maasvlakte)", "Netherlands", GeoPoint(51.92, 4.48), true),
        "FRANKFURT" to LogisticsPort("FRA", "Frankfurt CargoCity (FRA)", "Germany", GeoPoint(50.03, 8.57), false),
        "SINGAPORE" to LogisticsPort("SIN", "Singapore Changi Airfreight / PSA Hub", "Singapore", GeoPoint(1.36, 103.99), true),
        "SANTOS" to LogisticsPort("SSZ", "Santos Container Terminal", "Brazil", GeoPoint(-23.96, -46.33), true),
        "NEW_YORK" to LogisticsPort("EWR", "Port of New York / Newark Container Terminal", "United States", GeoPoint(40.68, -74.17), true),
        "TOKYO" to LogisticsPort("HND", "Tokyo Haneda Air Cargo Hub", "Japan", GeoPoint(35.54, 139.77), false),
        "LONDON" to LogisticsPort("LHR", "London Heathrow World Cargo Center", "United Kingdom", GeoPoint(51.47, -0.45), false),
        "DUBAI" to LogisticsPort("DXB", "Dubai Logistics City / Jebel Ali", "UAE", GeoPoint(25.02, 55.10), true),
        "LOS_ANGELES" to LogisticsPort("LAX", "Port of Los Angeles / Long Beach", "United States", GeoPoint(33.74, -118.26), true)
    )

    fun resolvePort(text: String, isDestination: Boolean): LogisticsPort {
        val upper = text.uppercase(Locale.US)
        return when {
            upper.contains("SHENZHEN") || upper.contains("YANTIAN") || upper.contains("SZX") -> KNOWN_PORTS["SHENZHEN"]!!
            upper.contains("ROTTERDAM") || upper.contains("RTM") -> KNOWN_PORTS["ROTTERDAM"]!!
            upper.contains("FRANKFURT") || upper.contains("FRA") -> KNOWN_PORTS["FRANKFURT"]!!
            upper.contains("SINGAPORE") || upper.contains("CHANGI") || upper.contains("SIN") -> KNOWN_PORTS["SINGAPORE"]!!
            upper.contains("SANTOS") || upper.contains("BRAZIL") || upper.contains("SSZ") -> KNOWN_PORTS["SANTOS"]!!
            upper.contains("NEW YORK") || upper.contains("NEWARK") || upper.contains("EWR") -> KNOWN_PORTS["NEW_YORK"]!!
            upper.contains("TOKYO") || upper.contains("HANEDA") || upper.contains("HND") -> KNOWN_PORTS["TOKYO"]!!
            upper.contains("LONDON") || upper.contains("HEATHROW") || upper.contains("LHR") -> KNOWN_PORTS["LONDON"]!!
            upper.contains("DUBAI") || upper.contains("DXB") -> KNOWN_PORTS["DUBAI"]!!
            upper.contains("LOS ANGELES") || upper.contains("LAX") -> KNOWN_PORTS["LOS_ANGELES"]!!
            else -> {
                // Fallback default coordinates
                if (isDestination) {
                    LogisticsPort("DEST", text.take(24), "Global Hub", GeoPoint(38.0, 15.0), false)
                } else {
                    LogisticsPort("ORIG", text.take(24), "Global Hub", GeoPoint(18.0, 95.0), false)
                }
            }
        }
    }

    /**
     * D3 Spherical Great Circle Geodesic Interpolation (matching d3.geoInterpolate)
     * Calculates intermediate points along the shortest spherical path between two points on Earth.
     */
    fun interpolateGreatCircle(
        start: GeoPoint,
        end: GeoPoint,
        steps: Int = 30
    ): List<GeoPoint> {
        val phi1 = start.latitude * (PI / 180.0)
        val lam1 = start.longitude * (PI / 180.0)
        val phi2 = end.latitude * (PI / 180.0)
        val lam2 = end.longitude * (PI / 180.0)

        val dPhi = phi2 - phi1
        val dLam = lam2 - lam1

        val a = sin(dPhi / 2.0) * sin(dPhi / 2.0) +
            cos(phi1) * cos(phi2) * sin(dLam / 2.0) * sin(dLam / 2.0)
        val d = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        if (d < 0.0001) {
            return listOf(start, end)
        }

        val points = mutableListOf<GeoPoint>()
        for (i in 0..steps) {
            val f = i.toDouble() / steps.toDouble()
            val A = sin((1.0 - f) * d) / sin(d)
            val B = sin(f * d) / sin(d)

            val x = A * cos(phi1) * cos(lam1) + B * cos(phi2) * cos(lam2)
            val y = A * cos(phi1) * sin(lam1) + B * cos(phi2) * sin(lam2)
            val z = A * sin(phi1) + B * sin(phi2)

            val phiF = atan2(z, sqrt(x * x + y * y))
            val lamF = atan2(y, x)

            val latF = phiF * (180.0 / PI)
            val lonF = lamF * (180.0 / PI)

            points.add(GeoPoint(latF, lonF))
        }
        return points
    }

    /**
     * Calculate initial bearing / forward azimuth from start to end point in degrees (0..360).
     */
    fun calculateAzimuth(start: GeoPoint, end: GeoPoint): Float {
        val phi1 = start.latitude * (PI / 180.0)
        val phi2 = end.latitude * (PI / 180.0)
        val dLam = (end.longitude - start.longitude) * (PI / 180.0)

        val y = sin(dLam) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLam)
        val bearingRad = atan2(y, x)
        val bearingDeg = (bearingRad * (180.0 / PI) + 360.0) % 360.0
        return bearingDeg.toFloat()
    }

    /**
     * Build rich D3ShipmentRoute from ShipmentRecord & detailed telemetry
     */
    fun buildRoute(detail: SupplyChainShipmentDetail): D3ShipmentRoute {
        val originPort = resolvePort(detail.record.origin, isDestination = false)
        val destPort = resolvePort(detail.record.destination, isDestination = true)

        val currentPoint = GeoPoint(detail.gpsCoordinates.latitude, detail.gpsCoordinates.longitude)

        // Interpolate full great circle arc
        val waypoints = interpolateGreatCircle(originPort.point, destPort.point, steps = 36)

        // Heading in degrees
        val headingDegrees = calculateAzimuth(currentPoint, destPort.point)

        return D3ShipmentRoute(
            trackingCode = detail.record.trackingCode,
            vesselOrFlightId = detail.vesselOrFlightId,
            carrier = detail.record.carrier,
            transportMode = detail.transportMode,
            originPort = originPort,
            destinationPort = destPort,
            currentPosition = currentPoint,
            progressPercent = detail.transitProgressPercent,
            speedTelemetry = detail.speedTelemetry,
            heading = detail.heading,
            headingDegrees = headingDegrees,
            cargoDescription = detail.record.cargoDescription,
            cargoValueUsd = detail.record.cargoValue,
            currency = detail.record.currency,
            customsStatus = detail.record.customsStatus,
            environmentalTelemetry = detail.environmentalTelemetry,
            eta = detail.record.estimatedArrival,
            waypoints = waypoints
        )
    }

    /**
     * Projected coordinate on a 2D Map Canvas (Equirectangular / Mercator style)
     */
    fun projectToCanvas(
        point: GeoPoint,
        canvasWidth: Float,
        canvasHeight: Float,
        zoom: Float = 1.0f,
        panOffsetX: Float = 0.0f,
        panOffsetY: Float = 0.0f
    ): Pair<Float, Float> {
        val centerX = canvasWidth / 2.0f + panOffsetX
        val centerY = canvasHeight / 2.0f + panOffsetY

        // Longitude: -180..180 -> -w/2..+w/2
        val x = centerX + (point.longitude.toFloat() / 180.0f) * (canvasWidth / 2.0f) * zoom
        // Latitude: -90..90 -> +h/2..-h/2 (inverted Y axis)
        val y = centerY - (point.latitude.toFloat() / 90.0f) * (canvasHeight / 2.0f) * zoom

        return Pair(x, y)
    }
}
