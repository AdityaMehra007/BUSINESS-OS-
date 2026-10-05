package com.example.worldbusiness.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.D3ShipmentRoute
import com.example.worldbusiness.data.model.GeoPoint
import com.example.worldbusiness.data.model.LogisticsChokePoint
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.SupplyChainShipmentDetail
import com.example.worldbusiness.data.model.TransportMode
import com.example.worldbusiness.data.repository.D3GeodesicMath
import com.example.worldbusiness.data.repository.SupplyChainTrackingEngine
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class D3EngineMode(val label: String) {
    D3_RADAR_CANVAS("D3 Geodesic Vector Radar"),
    D3_SVG_WEB("D3.js Interactive SVG Map")
}

/**
 * High-performance D3 Visualization Component to track the real-time movement of cross-border shipments
 * across global logistics networks.
 */
@Composable
fun D3GlobalLogisticsVisualizer(
    shipments: List<ShipmentRecord>,
    modifier: Modifier = Modifier
) {
    var pingTickCount by remember { mutableIntStateOf(0) }
    var engineMode by remember { mutableStateOf(D3EngineMode.D3_RADAR_CANVAS) }
    var selectedTrackingCode by remember { mutableStateOf<String?>(null) }
    var filterMode by remember { mutableStateOf("ALL") }
    var showChokePoints by remember { mutableStateOf(true) }

    // Convert raw records into rich D3 routes with spherical great-circle coordinates
    val routes = remember(shipments, pingTickCount) {
        shipments.map { record ->
            val detail = SupplyChainTrackingEngine.buildShipmentDetail(record, pingTickCount)
            D3GeodesicMath.buildRoute(detail)
        }
    }

    val filteredRoutes = remember(routes, filterMode) {
        when (filterMode) {
            "AIR" -> routes.filter { it.transportMode == TransportMode.AIR_FREIGHT }
            "OCEAN" -> routes.filter { it.transportMode == TransportMode.OCEAN_VESSEL }
            else -> routes
        }
    }

    val selectedRoute = remember(routes, selectedTrackingCode) {
        routes.find { it.trackingCode == selectedTrackingCode }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_global_logistics_visualizer"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar: Title, Mode Toggles, and AIS Tick Simulation Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
                listOf(CyanAccent.copy(alpha = 0.45f), BorderSubtle)
            ))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "D3 Map",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "D3 REAL-TIME GLOBAL LOGISTICS RADAR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                PulsingLiveBeacon()
                            }
                            Text(
                                text = "Great Circle Geodesic Arc Tracking • ${routes.size} Live Commercial Corridors",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Satellite Ping Stepper
                    Button(
                        onClick = { pingTickCount++ },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_tick_ais_ping")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Ping",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ADVANCE AIS (TICK $pingTickCount)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                // Mode Filters & Engine Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "ALL" to "All Corridors (${routes.size})",
                            "AIR" to "✈️ Air Express (${routes.count { it.transportMode == TransportMode.AIR_FREIGHT }})",
                            "OCEAN" to "🚢 Maritime Ocean (${routes.count { it.transportMode == TransportMode.OCEAN_VESSEL }})"
                        ).forEach { (modeKey, label) ->
                            val isSel = filterMode == modeKey
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isSel) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { filterMode = modeKey }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) SurfaceDark else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Choke Points Toggle
                    Box(
                        modifier = Modifier
                            .background(if (showChokePoints) GoldAccent.copy(alpha = 0.15f) else SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, if (showChokePoints) GoldAccent.copy(alpha = 0.4f) else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { showChokePoints = !showChokePoints }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showChokePoints) "⚓ Choke Points (6)" else "⚓ Choke Points Off",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showChokePoints) GoldAccent else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Main Visualization Surface (Canvas Vector Radar or D3 SVG WebView)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .testTag("d3_map_viewport"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
                listOf(BorderSubtle, CyanAccent.copy(alpha = 0.3f))
            ))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (engineMode == D3EngineMode.D3_RADAR_CANVAS) {
                    D3CanvasLogisticsRadar(
                        routes = filteredRoutes,
                        chokePoints = if (showChokePoints) D3GeodesicMath.GLOBAL_CHOKE_POINTS else emptyList(),
                        selectedTrackingCode = selectedTrackingCode,
                        onSelectShipment = { selectedTrackingCode = it }
                    )
                } else {
                    D3SvgWebViewLogisticsMap(
                        routes = filteredRoutes,
                        selectedTrackingCode = selectedTrackingCode
                    )
                }

                // Top-right HUD: Engine Switcher & Reset View
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceDark.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable {
                                engineMode = if (engineMode == D3EngineMode.D3_RADAR_CANVAS) {
                                    D3EngineMode.D3_SVG_WEB
                                } else {
                                    D3EngineMode.D3_RADAR_CANVAS
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (engineMode == D3EngineMode.D3_RADAR_CANVAS) "Switch to D3.js SVG" else "Switch to D3 Vector Canvas",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (selectedTrackingCode != null) {
                        Box(
                            modifier = Modifier
                                .background(SurfaceDark.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                .border(1.dp, RoseNegative.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable { selectedTrackingCode = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Reset Focus ✕",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Bottom-left Map Legend
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(SurfaceDark.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(CyanAccent, CircleShape))
                        Text(text = "Air Geodesic", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(EmeraldPositive, CircleShape))
                        Text(text = "Ocean Corridor", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(GoldAccent, CircleShape))
                        Text(text = "Choke Point", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Live Telemetry Inspection Card (When a vessel/aircraft is selected)
        selectedRoute?.let { route ->
            LiveShipmentTelemetryCard(
                route = route,
                onClose = { selectedTrackingCode = null }
            )
        }

        // Strategic Choke Point Status Strip
        if (showChokePoints) {
            ChokePointsStatusStrip(chokePoints = D3GeodesicMath.GLOBAL_CHOKE_POINTS)
        }
    }
}

/**
 * Native High-Performance Compose Canvas implementing D3 spherical projection & great circle arcs.
 */
@Composable
private fun D3CanvasLogisticsRadar(
    routes: List<D3ShipmentRoute>,
    chokePoints: List<LogisticsChokePoint>,
    selectedTrackingCode: String?,
    onSelectShipment: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "d3_radar_anim")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(routes) {
                detectTapGestures { tapOffset ->
                    // Hit test vessels to select
                    var hitFound = false
                    routes.forEach { route ->
                        val (vx, vy) = D3GeodesicMath.projectToCanvas(
                            route.currentPosition,
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                        val dist = sqrt((tapOffset.x - vx) * (tapOffset.x - vx) + (tapOffset.y - vy) * (tapOffset.y - vy))
                        if (dist <= 30f) {
                            onSelectShipment(route.trackingCode)
                            hitFound = true
                        }
                    }
                    if (!hitFound) {
                        // Check if tapped near route line
                        routes.forEach { route ->
                            route.waypoints.forEach { pt ->
                                val (px, py) = D3GeodesicMath.projectToCanvas(pt, size.width.toFloat(), size.height.toFloat())
                                val dist = sqrt((tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py))
                                if (dist <= 18f) {
                                    onSelectShipment(route.trackingCode)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw World Latitude / Longitude Graticule Grid
        drawGraticuleGrid(w, h)

        // 2. Draw Simplified Continents Vector Silhouette
        drawWorldContinentsSilhouette(w, h)

        // 3. Draw Global Maritime Choke Points
        chokePoints.forEach { cp ->
            val (cx, cy) = D3GeodesicMath.projectToCanvas(cp.point, w, h)
            // Pulse wave
            drawCircle(
                color = GoldAccent.copy(alpha = (1f - pulseProgress) * 0.4f),
                radius = 8f + pulseProgress * 12f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f)
            )
            // Choke center
            drawCircle(
                color = GoldAccent,
                radius = 3.5f,
                center = Offset(cx, cy)
            )
        }

        // 4. Draw D3 Great-Circle Geodesic Routes
        routes.forEach { route ->
            val isSelected = selectedTrackingCode == null || selectedTrackingCode == route.trackingCode
            val baseColor = if (route.transportMode == TransportMode.AIR_FREIGHT) CyanAccent else EmeraldPositive
            val routeColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.22f)
            val strokeWidth = if (selectedTrackingCode == route.trackingCode) 3.5f else 1.8f

            // Build curved path from 36 interpolated spherical points
            if (route.waypoints.size >= 2) {
                val fullPath = Path()
                val (startX, startY) = D3GeodesicMath.projectToCanvas(route.waypoints.first(), w, h)
                fullPath.moveTo(startX, startY)

                for (i in 1 until route.waypoints.size) {
                    val (px, py) = D3GeodesicMath.projectToCanvas(route.waypoints[i], w, h)
                    // Check for antimeridian wrap to prevent horizontal line streak
                    val (prevX, _) = D3GeodesicMath.projectToCanvas(route.waypoints[i - 1], w, h)
                    if (kotlin.math.abs(px - prevX) < w * 0.6f) {
                        fullPath.lineTo(px, py)
                    } else {
                        fullPath.moveTo(px, py)
                    }
                }

                // Render glowing route path
                drawPath(
                    path = fullPath,
                    color = routeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        pathEffect = if (route.transportMode == TransportMode.AIR_FREIGHT) {
                            PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                        } else null
                    )
                )

                // Render Origin & Destination Port Nodes
                val (ox, oy) = D3GeodesicMath.projectToCanvas(route.originPort.point, w, h)
                val (dx, dy) = D3GeodesicMath.projectToCanvas(route.destinationPort.point, w, h)

                drawCircle(color = TextSecondary.copy(alpha = 0.5f), radius = 4f, center = Offset(ox, oy))
                drawCircle(color = CyanAccent, radius = 2f, center = Offset(ox, oy))

                drawCircle(color = GoldAccent.copy(alpha = 0.5f), radius = 4f, center = Offset(dx, dy))
                drawCircle(color = GoldAccent, radius = 2f, center = Offset(dx, dy))

                // 5. Render Real-Time Moving Vessel / Aircraft Blip
                val (vx, vy) = D3GeodesicMath.projectToCanvas(route.currentPosition, w, h)

                // Expanding radar pulse wave
                drawCircle(
                    color = routeColor.copy(alpha = (1f - pulseProgress) * 0.7f),
                    radius = 8f + pulseProgress * 22f,
                    center = Offset(vx, vy),
                    style = Stroke(width = 1.5f)
                )

                // Glowing Halo
                drawCircle(
                    color = routeColor.copy(alpha = 0.25f),
                    radius = 9f,
                    center = Offset(vx, vy)
                )

                // Solid Core Blip with Directional Heading Arrow
                rotate(degrees = route.headingDegrees, pivot = Offset(vx, vy)) {
                    val arrowPath = Path().apply {
                        moveTo(vx, vy - 7f)
                        lineTo(vx + 5f, vy + 6f)
                        lineTo(vx, vy + 3f)
                        lineTo(vx - 5f, vy + 6f)
                        close()
                    }
                    drawPath(path = arrowPath, color = routeColor)
                }
            }
        }
    }
}

/**
 * Draws coordinate graticule grid lines (Equator, Prime Meridian, Tropics).
 */
private fun DrawScope.drawGraticuleGrid(w: Float, h: Float) {
    val gridColor = BorderSubtle.copy(alpha = 0.25f)
    val equatorColor = CyanAccent.copy(alpha = 0.25f)

    // Latitude parallels: -60, -30, 0, 30, 60
    listOf(-60.0, -30.0, 0.0, 30.0, 60.0).forEach { lat ->
        val y = (h / 2f) - (lat.toFloat() / 90f) * (h / 2f)
        drawLine(
            color = if (lat == 0.0) equatorColor else gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = if (lat == 0.0) 1.2f else 0.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }

    // Longitude meridians: -120, -60, 0, 60, 120
    listOf(-120.0, -60.0, 0.0, 60.0, 120.0).forEach { lon ->
        val x = (w / 2f) + (lon.toFloat() / 180f) * (w / 2f)
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 0.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}

/**
 * Draws simplified landmass polygons for world continents to provide geopolitical geography context.
 */
private fun DrawScope.drawWorldContinentsSilhouette(w: Float, h: Float) {
    val landColor = SurfaceElevated.copy(alpha = 0.35f)
    val coastColor = BorderSubtle.copy(alpha = 0.5f)

    // Approximate continent outlines in (lat, lon)
    val northAmerica = listOf(
        GeoPoint(65.0, -165.0), GeoPoint(70.0, -135.0), GeoPoint(60.0, -70.0),
        GeoPoint(45.0, -65.0), GeoPoint(25.0, -80.0), GeoPoint(15.0, -90.0),
        GeoPoint(20.0, -105.0), GeoPoint(35.0, -120.0), GeoPoint(55.0, -135.0)
    )

    val southAmerica = listOf(
        GeoPoint(10.0, -75.0), GeoPoint(-5.0, -35.0), GeoPoint(-25.0, -45.0),
        GeoPoint(-50.0, -70.0), GeoPoint(-40.0, -75.0), GeoPoint(-10.0, -80.0)
    )

    val eurasia = listOf(
        GeoPoint(70.0, 30.0), GeoPoint(72.0, 130.0), GeoPoint(60.0, 170.0),
        GeoPoint(40.0, 140.0), GeoPoint(22.0, 120.0), GeoPoint(10.0, 105.0),
        GeoPoint(10.0, 75.0), GeoPoint(25.0, 55.0), GeoPoint(35.0, 35.0),
        GeoPoint(40.0, -5.0), GeoPoint(55.0, 10.0)
    )

    val africa = listOf(
        GeoPoint(35.0, -5.0), GeoPoint(32.0, 32.0), GeoPoint(12.0, 50.0),
        GeoPoint(-5.0, 40.0), GeoPoint(-34.0, 20.0), GeoPoint(-15.0, 12.0),
        GeoPoint(5.0, 0.0), GeoPoint(15.0, -17.0)
    )

    val australia = listOf(
        GeoPoint(-12.0, 130.0), GeoPoint(-15.0, 145.0), GeoPoint(-35.0, 150.0),
        GeoPoint(-38.0, 140.0), GeoPoint(-32.0, 115.0), GeoPoint(-20.0, 115.0)
    )

    listOf(northAmerica, southAmerica, eurasia, africa, australia).forEach { continent ->
        val path = Path()
        val (firstX, firstY) = D3GeodesicMath.projectToCanvas(continent.first(), w, h)
        path.moveTo(firstX, firstY)
        for (i in 1 until continent.size) {
            val (px, py) = D3GeodesicMath.projectToCanvas(continent[i], w, h)
            path.lineTo(px, py)
        }
        path.close()
        drawPath(path = path, color = landColor)
        drawPath(path = path, color = coastColor, style = Stroke(width = 0.8f))
    }
}

/**
 * Embedded D3.js SVG Web Visualizer for advanced SVG path dash animations.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun D3SvgWebViewLogisticsMap(
    routes: List<D3ShipmentRoute>,
    selectedTrackingCode: String?
) {
    val htmlContent = remember(routes, selectedTrackingCode) {
        buildD3SvgHtml(routes, selectedTrackingCode)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0xFF070B12.toInt())
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://d3js.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://d3js.org", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Constructs an SVG map using D3 Great Circle geodesics.
 */
private fun buildD3SvgHtml(routes: List<D3ShipmentRoute>, selectedTrackingCode: String?): String {
    val routeLinesJs = routes.map { r ->
        val origin = r.originPort.point
        val dest = r.destinationPort.point
        val curr = r.currentPosition
        val color = if (r.transportMode == TransportMode.AIR_FREIGHT) "#00F2FE" else "#10B981"
        """
        {
          code: "${r.trackingCode}",
          carrier: "${r.carrier}",
          origin: [${origin.longitude}, ${origin.latitude}],
          dest: [${dest.longitude}, ${dest.latitude}],
          current: [${curr.longitude}, ${curr.latitude}],
          color: "$color",
          progress: ${r.progressPercent}
        }
        """
    }.joinToString(",")

    return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
      <style>
        body { margin: 0; background: #070B12; overflow: hidden; font-family: monospace; }
        svg { width: 100vw; height: 100vh; display: block; }
        .graticule { stroke: #1e293b; stroke-width: 0.5px; fill: none; }
        .equator { stroke: #00F2FE; stroke-width: 0.8px; stroke-dasharray: 4,4; opacity: 0.3; }
        .land { fill: #111827; stroke: #1f293d; stroke-width: 0.5px; }
        .route-air { stroke: #00F2FE; stroke-width: 2px; stroke-dasharray: 6,4; fill: none; }
        .route-ocean { stroke: #10B981; stroke-width: 2px; fill: none; }
        .blip-pulse { animation: pulse 2s infinite; }
        @keyframes pulse { 0% { r: 4px; opacity: 1; } 100% { r: 16px; opacity: 0; } }
      </style>
      <script src="https://d3js.org/d3.v7.min.js"></script>
    </head>
    <body>
      <svg id="d3-stage"></svg>
      <script>
        const width = window.innerWidth;
        const height = window.innerHeight;
        const svg = d3.select("#d3-stage");

        const projection = d3.geoMercator()
          .scale(width / 6.2)
          .translate([width / 2, height / 1.6]);

        const path = d3.geoPath().projection(projection);

        // Grid lines
        const graticule = d3.geoGraticule()();
        svg.append("path")
          .datum(graticule)
          .attr("class", "graticule")
          .attr("d", path);

        const routes = [$routeLinesJs];

        routes.forEach(r => {
          // Great circle arc
          const geoJsonLine = {
            type: "LineString",
            coordinates: [r.origin, r.current, r.dest]
          };

          svg.append("path")
            .datum(geoJsonLine)
            .attr("class", r.color === "#00F2FE" ? "route-air" : "route-ocean")
            .attr("d", path);

          // Animated Blip at current position
          const p = projection(r.current);
          if (p) {
            svg.append("circle")
              .attr("cx", p[0])
              .attr("cy", p[1])
              .attr("r", 4)
              .attr("fill", r.color)
              .attr("class", "blip-pulse");

            svg.append("circle")
              .attr("cx", p[0])
              .attr("cy", p[1])
              .attr("r", 4)
              .attr("fill", r.color);
          }
        });
      </script>
    </body>
    </html>
    """.trimIndent()
}

/**
 * Detailed Live Telemetry Card for Focused Shipment
 */
@Composable
private fun LiveShipmentTelemetryCard(
    route: D3ShipmentRoute,
    onClose: () -> Unit
) {
    val modeColor = if (route.transportMode == TransportMode.AIR_FREIGHT) CyanAccent else EmeraldPositive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("d3_telemetry_inspection_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
            listOf(modeColor.copy(alpha = 0.5f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(modeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, modeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = route.vesselOrFlightId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = modeColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = route.trackingCode,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Route Corridor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "ORIGIN PORT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = route.originPort.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(text = "➔", fontSize = 14.sp, color = modeColor)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "DESTINATION PORT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = route.destinationPort.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // Transit Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "GREAT CIRCLE PROGRESS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = "${route.progressPercent}% Completed (ETA: ${route.eta})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = modeColor, fontFamily = FontFamily.Monospace)
                }
                LinearProgressIndicator(
                    progress = { route.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = modeColor,
                    trackColor = SurfaceElevated
                )
            }

            // Real-Time Telemetry Metrics Grid
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "SPEED & PROPULSION", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = route.speedTelemetry, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        Text(text = "Heading: ${route.heading}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "CARGO & SENSORS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "$${Formatters.formatCompactNumber(route.cargoValueUsd)} • ${route.customsStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        Text(text = route.environmentalTelemetry.take(34) + "...", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/**
 * Strategic Choke Points Real-Time Status Strip
 */
@Composable
private fun ChokePointsStatusStrip(chokePoints: List<LogisticsChokePoint>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "GLOBAL STRATEGIC LOGISTICS CHOKE POINTS (AIS MONITORED)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chokePoints.forEach { cp ->
                val statusColor = when (cp.status) {
                    "NORMAL_TRANSIT", "CLEAR" -> EmeraldPositive
                    "HEAVY_CONVOY" -> GoldAccent
                    else -> RoseNegative
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
                        listOf(BorderSubtle, statusColor.copy(alpha = 0.35f))
                    )),
                    modifier = Modifier.width(155.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = cp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        }
                        Text(text = "Status: ${cp.status.replace("_", " ")}", fontSize = 8.sp, color = statusColor, fontFamily = FontFamily.Monospace)
                        Text(text = "Traffic: ${cp.dailyTonnageUsd}", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/**
 * Pulsing Live Beacon Indicator
 */
@Composable
private fun PulsingLiveBeacon() {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(7.dp)
            .background(EmeraldPositive.copy(alpha = alpha), CircleShape)
    )
}
