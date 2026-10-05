package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.SupplyChainOverviewStats
import com.example.worldbusiness.data.model.SupplyChainShipmentDetail
import com.example.worldbusiness.data.model.TransportMode
import com.example.worldbusiness.data.repository.SupplyChainTrackingEngine
import java.util.Locale

/**
 * Supply Chain Tracking View
 * Displays shipment status cards with real-time GPS locations, route telemetry,
 * customs clearance stages, and estimated arrival dates (ETAs) for international logistics.
 */
@Composable
fun SupplyChainTrackingView(
    shipments: List<ShipmentRecord>,
    onDispatchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var telemetryPingCount by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedShipmentForDetail by remember { mutableStateOf<SupplyChainShipmentDetail?>(null) }

    // Map records to rich real-time telemetry details
    val shipmentDetails = remember(shipments, telemetryPingCount) {
        shipments.map { record ->
            SupplyChainTrackingEngine.buildShipmentDetail(record, telemetryPingCount)
        }
    }

    val overviewStats = remember(shipmentDetails) {
        SupplyChainTrackingEngine.buildOverviewStats(shipmentDetails)
    }

    val filteredDetails = remember(shipmentDetails, selectedFilter, searchQuery) {
        shipmentDetails.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "OCEAN" -> item.transportMode == TransportMode.OCEAN_VESSEL
                "AIR" -> item.transportMode == TransportMode.AIR_FREIGHT
                "TRANSIT" -> item.record.customsStatus == "IN_TRANSIT"
                "CLEARED" -> item.record.customsStatus == "CLEARED"
                "INSPECTION" -> item.record.customsStatus == "PORT_INSPECTION" || item.record.customsStatus == "DOCUMENTATION_REQ"
                else -> true
            }

            val matchesQuery = searchQuery.isBlank() ||
                item.record.trackingCode.contains(searchQuery, ignoreCase = true) ||
                item.record.carrier.contains(searchQuery, ignoreCase = true) ||
                item.record.cargoDescription.contains(searchQuery, ignoreCase = true) ||
                item.record.origin.contains(searchQuery, ignoreCase = true) ||
                item.record.destination.contains(searchQuery, ignoreCase = true) ||
                item.currentRealTimeLocation.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("supply_chain_tracking_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Global Telemetry & Logistics KPI Header Card
        SupplyChainKpiHeader(
            stats = overviewStats,
            onPingTelemetry = { telemetryPingCount++ },
            onDispatchClick = onDispatchClick
        )

        // Real-Time D3 Global Movement Radar Component
        D3GlobalLogisticsVisualizer(
            shipments = shipments
        )

        // Search & Filter Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tracking code, carrier, cargo, or port...", fontSize = 11.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("supply_chain_search_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true
            )
        }

        // Mode & Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All Active (${overviewStats.totalActiveShipments})",
                "TRANSIT" to "In Transit (${overviewStats.inTransitCount})",
                "OCEAN" to "Ocean Vessels 🚢",
                "AIR" to "Air Cargo ✈️",
                "CLEARED" to "Cleared (${overviewStats.customsClearedCount})",
                "INSPECTION" to "Customs Hold (${overviewStats.inspectionHoldCount})"
            ).forEach { (filterKey, label) ->
                val isSelected = selectedFilter == filterKey
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { selectedFilter = filterKey }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("filter_shipment_$filterKey")
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section Title with Live Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Live Telemetry",
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "LIVE SHIPMENT TRACKING & ETAS (${filteredDetails.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "AIS / ADS-B TELEMETRY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        // Shipment Status Cards List
        if (filteredDetails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shipments match the current criteria.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredDetails.forEach { detail ->
                    ShipmentStatusCard(
                        detail = detail,
                        onClick = { selectedShipmentForDetail = detail }
                    )
                }
            }
        }
    }

    // Detailed Telemetry & Route Waypoints Dialog
    selectedShipmentForDetail?.let { detail ->
        ShipmentTelemetryDetailDialog(
            detail = detail,
            onDismiss = { selectedShipmentForDetail = null }
        )
    }
}

/**
 * Top Global Telemetry & Logistics KPI Header Card
 */
@Composable
private fun SupplyChainKpiHeader(
    stats: SupplyChainOverviewStats,
    onPingTelemetry: () -> Unit,
    onDispatchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supply_chain_kpi_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.45f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar with Real-Time Ping Action
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
                            .size(34.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar Tracking",
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
                                text = "GLOBAL SUPPLY CHAIN TELEMETRY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(EmeraldPositive.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, EmeraldPositive.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SATELLITE AIS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "Real-time vessel & flight tracking • Customs status • Live ETAs",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Ping Telemetry Button
                    Button(
                        onClick = onPingTelemetry,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyanAccent, BorderSubtle))),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_ping_telemetry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Ping",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Dispatch Freight Button
                    Button(
                        onClick = onDispatchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_dispatch_shipment")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dispatch",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DISPATCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // 4-Column Performance KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total In-Transit Cargo Valuation
                SupplyChainKpiTile(
                    title = "IN-TRANSIT VALUE",
                    value = "$${Formatters.formatCompactNumber(stats.totalCargoValueUsd)}",
                    subtitle = "Insured Marine/Air",
                    tint = EmeraldPositive,
                    modifier = Modifier.weight(1f)
                )

                // Active Vessel / Flight Movements
                SupplyChainKpiTile(
                    title = "MOVEMENTS",
                    value = "${stats.totalActiveShipments} Active",
                    subtitle = "${stats.inTransitCount} In Corridor",
                    tint = CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                // On-Schedule Performance Rate
                SupplyChainKpiTile(
                    title = "ON-SCHEDULE ETA",
                    value = "${String.format(Locale.US, "%.1f", stats.onScheduleRatePercent)}%",
                    subtitle = "Target Reliability",
                    tint = if (stats.onScheduleRatePercent >= 90) EmeraldPositive else GoldAccent,
                    modifier = Modifier.weight(1f)
                )

                // Customs Clearance Hold
                SupplyChainKpiTile(
                    title = "CUSTOMS QUEUE",
                    value = "${stats.inspectionHoldCount} In Hold",
                    subtitle = "${stats.customsClearedCount} Cleared",
                    tint = if (stats.inspectionHoldCount > 0) GoldAccent else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * KPI Tile for Supply Chain Overview
 */
@Composable
private fun SupplyChainKpiTile(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))
        )),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Shipment Status Card with Real-Time Location and Estimated Arrival Date
 */
@Composable
private fun ShipmentStatusCard(
    detail: SupplyChainShipmentDetail,
    onClick: () -> Unit
) {
    val record = detail.record
    val statusColor = Formatters.getStatusColor(record.customsStatus)
    val isAir = detail.transportMode == TransportMode.AIR_FREIGHT

    // Pulsing radar beacon animation for the real-time location indicator
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("shipment_status_card_${record.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.35f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Tracking Code, Carrier, Transport Mode & Customs Status Badge
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
                            .size(30.dp)
                            .background(CyanAccent.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAir) Icons.Default.FlightTakeoff else Icons.Default.Anchor,
                            contentDescription = detail.transportMode.label,
                            tint = CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = record.trackingCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .background(SurfaceElevated, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = record.incoterm,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "${record.carrier} • ${detail.vesselOrFlightId}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Customs Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.customsStatus.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Route Header: Origin -> Destination
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = record.origin,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "➔",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = record.destination,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Real-Time Location Banner with GPS Coordinates & Telemetry
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated.copy(alpha = 0.7f),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                    listOf(CyanAccent.copy(alpha = 0.4f), BorderSubtle)
                )),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(CyanAccent.copy(alpha = beaconAlpha), CircleShape)
                            )
                            Text(
                                text = "REAL-TIME LOCATION:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = detail.gpsCoordinates.formatted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = detail.currentRealTimeLocation,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    // Speed, Heading & Signal Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = EmeraldPositive,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = detail.speedTelemetry,
                                fontSize = 9.sp,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = detail.satelliteLastPingTime,
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Transit Progress Bar with Animated Stepper
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOYAGE PROGRESS: ${detail.transitProgressPercent}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = detail.heading,
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                LinearProgressIndicator(
                    progress = { detail.transitProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyanAccent,
                    trackColor = SurfaceElevated
                )
            }

            // Estimated Arrival Date & Cargo Valuation Footer Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariantDark.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estimated Arrival Date (ETA)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "ETA",
                                tint = GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ESTIMATED ARRIVAL (ETA)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = detail.estimatedArrivalDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${detail.etaDaysRemaining} days remaining • ${detail.etaStatus.replace("_", " ")}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (detail.etaStatus == "CUSTOMS_HOLD") RoseNegative else EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Cargo Valuation
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CARGO VALUE",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = Formatters.formatCurrency(record.cargoValue, record.currency),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Incoterm: ${record.incoterm}",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Manifest Summary & Details Affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cargo: ${record.cargoDescription}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Telemetry & Milestones",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Details",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Telemetry & Route Waypoints Dialog
 */
@Composable
private fun ShipmentTelemetryDetailDialog(
    detail: SupplyChainShipmentDetail,
    onDismiss: () -> Unit
) {
    val record = detail.record
    val isAir = detail.transportMode == TransportMode.AIR_FREIGHT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAir) Icons.Default.FlightTakeoff else Icons.Default.Anchor,
                        contentDescription = "Mode",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = record.trackingCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${record.origin} ➔ ${record.destination}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = detail.transportMode.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Real-Time Location & Telemetry Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                        listOf(CyanAccent.copy(alpha = 0.4f), BorderSubtle)
                    )),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LIVE SATELLITE POSITION & TELEMETRY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = detail.currentRealTimeLocation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "GPS: ${detail.gpsCoordinates.formatted}", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(text = detail.speedTelemetry, fontSize = 10.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Estimated Arrival & Cargo Valuation
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "ESTIMATED ARRIVAL (ETA)", fontSize = 9.sp, color = GoldAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = detail.estimatedArrivalDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            Text(text = "${detail.etaDaysRemaining} Days Remaining (${detail.etaStatus})", fontSize = 9.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "CARGO VALUATION", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(text = Formatters.formatCurrency(record.cargoValue, record.currency), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                            Text(text = "Incoterm: ${record.incoterm}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Environmental Cargo IoT Telemetry
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Thermostat, contentDescription = "IoT", tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Text(text = "CARGO IOT ENVIRONMENTAL SENSORS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = detail.environmentalTelemetry, fontSize = 10.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Transit Route Milestones List
                Text(
                    text = "CORRIDOR WAYPOINTS & TRANSIT MILESTONES:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.milestones.forEach { milestone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (milestone.isCurrent) CyanAccent.copy(alpha = 0.12f) else SurfaceElevated.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (milestone.isCompleted) EmeraldPositive else if (milestone.isCurrent) CyanAccent else TextMuted,
                                            CircleShape
                                        )
                                )
                                Column {
                                    Text(
                                        text = milestone.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (milestone.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (milestone.isCurrent) CyanAccent else TextPrimary
                                    )
                                    Text(text = milestone.location, fontSize = 9.sp, color = TextSecondary)
                                }
                            }

                            Text(
                                text = milestone.timestamp,
                                fontSize = 9.sp,
                                color = if (milestone.isCurrent) CyanAccent else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark
    )
}
