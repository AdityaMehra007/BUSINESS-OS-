package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.worldbusiness.data.model.CustomsClearanceCategory
import com.example.worldbusiness.data.model.DeliveryCalculationResult
import com.example.worldbusiness.data.model.LogisticsCorridorHub
import com.example.worldbusiness.data.model.LogisticsFreightMode
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enterprise International Logistics Dashboard.
 * Backed by Room database with real-time status updates and dynamic delivery date (ETA) calculations.
 */
@Composable
fun InternationalLogisticsDashboard(
    shipments: List<ShipmentRecord>,
    onUpdateShipmentStatus: (id: Long, newStatus: String, newEta: String?) -> Unit,
    onDispatchShipmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedShipmentForStatusUpdate by remember { mutableStateOf<ShipmentRecord?>(null) }
    var showEtaCalculatorModal by remember { mutableStateOf(false) }
    var prefilledOriginForEta by remember { mutableStateOf<String?>(null) }
    var prefilledDestForEta by remember { mutableStateOf<String?>(null) }
    var prefilledShipmentIdForEta by remember { mutableStateOf<Long?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(shipments, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "TRANSIT" -> shipments.filter { it.customsStatus == "IN_TRANSIT" }
            "INSPECTION" -> shipments.filter { it.customsStatus == "PORT_INSPECTION" || it.customsStatus == "DOCUMENTATION_REQ" || it.customsStatus == "CUSTOMS_HOLD" }
            "CLEARED" -> shipments.filter { it.customsStatus == "CLEARED" }
            "DELIVERED" -> shipments.filter { it.customsStatus == "DELIVERED" }
            else -> shipments
        }
    }

    val totalCargoValueUsd = remember(shipments) {
        shipments.sumOf { it.cargoValue }
    }
    val inTransitCount = remember(shipments) {
        shipments.count { it.customsStatus == "IN_TRANSIT" }
    }
    val customsHoldCount = remember(shipments) {
        shipments.count { it.customsStatus == "PORT_INSPECTION" || it.customsStatus == "DOCUMENTATION_REQ" || it.customsStatus == "CUSTOMS_HOLD" }
    }
    val clearedCount = remember(shipments) {
        shipments.count { it.customsStatus == "CLEARED" || it.customsStatus == "DELIVERED" }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("international_logistics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dashboard Executive Metric Strip
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logistics_kpi_summary_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Logistics",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "INTERNATIONAL FREIGHT & SHIPMENT TRACKER",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Room SQLite Database Persistence • Real-Time Customs Status",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Open ETA Calculator Tool
                    Button(
                        onClick = {
                            prefilledOriginForEta = null
                            prefilledDestForEta = null
                            prefilledShipmentIdForEta = null
                            showEtaCalculatorModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(CyanAccent, BorderSubtle))
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_eta_calculator")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "ETA Calculator",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CALCULATE ETA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 4-Pill Metric Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniPill(
                        modifier = Modifier.weight(1f),
                        title = "TOTAL SHIPMENTS",
                        value = "${shipments.size}",
                        subtext = "Tracked in Room",
                        color = TextPrimary
                    )
                    MetricMiniPill(
                        modifier = Modifier.weight(1f),
                        title = "IN TRANSIT",
                        value = "$inTransitCount",
                        subtext = "Active High-Seas/Air",
                        color = CyanAccent
                    )
                    MetricMiniPill(
                        modifier = Modifier.weight(1f),
                        title = "CUSTOMS HOLD",
                        value = "$customsHoldCount",
                        subtext = "Inspection / Dwell",
                        color = if (customsHoldCount > 0) RoseNegative else EmeraldPositive
                    )
                    MetricMiniPill(
                        modifier = Modifier.weight(1f),
                        title = "CLEARED / DELIVERED",
                        value = "$clearedCount",
                        subtext = "Customs Passed",
                        color = EmeraldPositive
                    )
                }
            }
        }

        // 2. Filter & Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All (${shipments.size})",
                    "TRANSIT" to "In Transit ($inTransitCount)",
                    "INSPECTION" to "Customs Dwell ($customsHoldCount)",
                    "CLEARED" to "Cleared ($clearedCount)"
                ).forEach { (key, label) ->
                    val isSelected = selectedStatusFilter == key
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { selectedStatusFilter = key }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("filter_status_$key")
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

            // Quick Dispatch Button
            Button(
                onClick = onDispatchShipmentClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_quick_dispatch_freight")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dispatch",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "DISPATCH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. International Shipments List with Status Update & ETA Recalculation
        if (filteredList.isEmpty()) {
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
                    text = "No international shipments in this status category.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredList.forEach { shipment ->
                    InteractiveShipmentCard(
                        shipment = shipment,
                        onUpdateStatusClick = { selectedShipmentForStatusUpdate = shipment },
                        onCalculateEtaClick = {
                            prefilledOriginForEta = shipment.origin
                            prefilledDestForEta = shipment.destination
                            prefilledShipmentIdForEta = shipment.id
                            showEtaCalculatorModal = true
                        }
                    )
                }
            }
        }
    }

    // Modal: Real-Time Shipment Status Updater
    selectedShipmentForStatusUpdate?.let { shipment ->
        ShipmentStatusUpdateModal(
            shipment = shipment,
            onDismiss = { selectedShipmentForStatusUpdate = null },
            onConfirmUpdate = { newStatus, newEta ->
                onUpdateShipmentStatus(shipment.id, newStatus, newEta)
                selectedShipmentForStatusUpdate = null
            }
        )
    }

    // Modal: Estimated Delivery Date (ETA) Calculator
    if (showEtaCalculatorModal) {
        DeliveryEtaCalculatorModal(
            initialOrigin = prefilledOriginForEta,
            initialDestination = prefilledDestForEta,
            targetShipmentId = prefilledShipmentIdForEta,
            onDismiss = { showEtaCalculatorModal = false },
            onApplyEtaToShipment = { shipmentId, calculatedEta ->
                onUpdateShipmentStatus(shipmentId, "IN_TRANSIT", calculatedEta)
                showEtaCalculatorModal = false
            }
        )
    }
}

/**
 * Individual International Shipment Card with direct Room update triggers
 */
@Composable
private fun InteractiveShipmentCard(
    shipment: ShipmentRecord,
    onUpdateStatusClick: () -> Unit,
    onCalculateEtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (shipment.customsStatus) {
        "CLEARED" -> EmeraldPositive
        "DELIVERED" -> EmeraldPositive
        "PORT_INSPECTION" -> RoseNegative
        "DOCUMENTATION_REQ" -> GoldAccent
        "CUSTOMS_HOLD" -> RoseNegative
        else -> CyanAccent
    }

    val isAir = shipment.carrier.uppercase(Locale.US).contains("AIR") ||
        shipment.carrier.uppercase(Locale.US).contains("LUFTHANSA") ||
        shipment.carrier.uppercase(Locale.US).contains("EXPRESS")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shipment_card_${shipment.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(statusColor.copy(alpha = 0.45f), BorderSubtle))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Tracking code, Mode, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAir) "✈️" else "🚢",
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = shipment.trackingCode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${shipment.carrier} • Incoterm: ${shipment.incoterm}",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Customs Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(statusColor, statusColor.copy(alpha = 0.5f)))
                    )
                ) {
                    Text(
                        text = shipment.customsStatus.replace('_', ' '),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Route Corridor Display: Origin -> Destination
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "ORIGIN HUB", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = shipment.origin, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Corridor",
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "DESTINATION PORT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = shipment.destination, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // Cargo Description, Valuation & Estimated Delivery Date (ETA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "CARGO MANIFEST", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = shipment.cargoDescription, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(
                        text = "Valuation: ${Formatters.formatCurrency(shipment.cargoValue, shipment.currency)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // ETA Box
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceVariantDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.5f), BorderSubtle))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "ETA",
                                tint = GoldAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "CALCULATED ETA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = shipment.estimatedArrival,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))

            // Real-Time Action Buttons (Update Status, Recalculate ETA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recalculate ETA Button
                TextButton(
                    onClick = onCalculateEtaClick,
                    modifier = Modifier.testTag("btn_recalculate_eta_${shipment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "ETA Engine",
                        tint = GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECALCULATE ETA",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Update Status Button
                Button(
                    onClick = onUpdateStatusClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier.testTag("btn_update_status_${shipment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "UPDATE STATUS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Real-Time Status Update Modal: Persists state to Room database
 */
@Composable
fun ShipmentStatusUpdateModal(
    shipment: ShipmentRecord,
    onDismiss: () -> Unit,
    onConfirmUpdate: (newStatus: String, newEta: String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(shipment.customsStatus) }
    var autoRecalculateEta by remember { mutableStateOf(true) }

    val calculatedRevisedEta = remember(shipment, selectedStatus) {
        LogisticsDeliveryCalculatorEngine.recalibrateShipmentEta(shipment, selectedStatus)
    }

    var customEtaInput by remember { mutableStateOf(shipment.estimatedArrival) }

    val effectiveEta = if (autoRecalculateEta) calculatedRevisedEta else customEtaInput

    val availableStatuses = listOf(
        "IN_TRANSIT" to "International Transit Active (Corridor Cruising)",
        "PORT_INSPECTION" to "Destination Port Physical Inspection (+2d buffer)",
        "DOCUMENTATION_REQ" to "Customs Document Request Hold (+3d buffer)",
        "CUSTOMS_HOLD" to "Regulatory / Phytosanitary Examination Hold (+4d buffer)",
        "CLEARED" to "Customs Cleared • Out for Last-Mile Delivery (1d)",
        "DELIVERED" to "Delivered to Consignee Facility"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UPDATE SHIPMENT STATUS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${shipment.trackingCode} • Room Database Sync",
                        fontSize = 10.sp,
                        color = TextSecondary
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
                Text(
                    text = "SELECT NEW CUSTOMS & TRANSIT STATUS:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                // Status List Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableStatuses.forEach { (statusCode, statusDesc) ->
                        val isSelected = selectedStatus == statusCode
                        val badgeColor = when (statusCode) {
                            "CLEARED", "DELIVERED" -> EmeraldPositive
                            "PORT_INSPECTION", "CUSTOMS_HOLD" -> RoseNegative
                            "DOCUMENTATION_REQ" -> GoldAccent
                            else -> CyanAccent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) badgeColor.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(1.dp, if (isSelected) badgeColor else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedStatus = statusCode }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("select_status_$statusCode"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusCode.replace('_', ' '),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) badgeColor else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = statusDesc,
                                    fontSize = 8.5.sp,
                                    color = TextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = badgeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // ETA Recalibration Preview Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.5f), BorderSubtle))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUTOMATIC ETA RECALIBRATION",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (autoRecalculateEta) "Active Engine" else "Manual",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current ETA: ${shipment.estimatedArrival}",
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Revised ETA: $effectiveEta",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { autoRecalculateEta = !autoRecalculateEta },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (autoRecalculateEta) GoldAccent.copy(alpha = 0.2f) else SurfaceElevated,
                                    contentColor = GoldAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (autoRecalculateEta) "Auto-Recalc" else "Use Manual",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmUpdate(selectedStatus, effectiveEta) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_status_room")
            ) {
                Text(
                    text = "SAVE TO ROOM DATABASE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        containerColor = SurfaceDark
    )
}

/**
 * Interactive Estimated Delivery Date (ETA) Calculator Modal
 */
@Composable
fun DeliveryEtaCalculatorModal(
    initialOrigin: String? = null,
    initialDestination: String? = null,
    targetShipmentId: Long? = null,
    onDismiss: () -> Unit,
    onApplyEtaToShipment: ((shipmentId: Long, calculatedEta: String) -> Unit)? = null
) {
    val hubs = LogisticsDeliveryCalculatorEngine.GLOBAL_TRADE_HUBS

    var selectedOriginHub by remember {
        mutableStateOf(
            if (!initialOrigin.isNullOrBlank()) LogisticsDeliveryCalculatorEngine.findHubByKeyword(initialOrigin, hubs[0])
            else hubs[0] // Shanghai
        )
    }

    var selectedDestHub by remember {
        mutableStateOf(
            if (!initialDestination.isNullOrBlank()) LogisticsDeliveryCalculatorEngine.findHubByKeyword(initialDestination, hubs[2])
            else hubs[2] // Rotterdam
        )
    }

    var selectedFreightMode by remember { mutableStateOf(LogisticsFreightMode.OCEAN_CONTAINER) }
    var selectedClearanceCategory by remember { mutableStateOf(CustomsClearanceCategory.STANDARD_GENERAL) }

    val calculationResult = remember(selectedOriginHub, selectedDestHub, selectedFreightMode, selectedClearanceCategory) {
        LogisticsDeliveryCalculatorEngine.calculateDeliveryEstimate(
            originHub = selectedOriginHub,
            destinationHub = selectedDestHub,
            freightMode = selectedFreightMode,
            clearanceCategory = selectedClearanceCategory,
            dispatchDate = Date()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ESTIMATED DELIVERY DATE ENGINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Geodesic Great-Circle & Corridor Transit Mathematics",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
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
                // 1. Origin Hub Selection
                Text(
                    text = "ORIGIN TRADE HUB:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hubs.forEach { hub ->
                        val isSelected = hub.code == selectedOriginHub.code
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedOriginHub = hub }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("calc_origin_${hub.code.lowercase(Locale.US)}")
                        ) {
                            Text(
                                text = "${hub.city} (${hub.code})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 2. Destination Hub Selection
                Text(
                    text = "DESTINATION TRADE HUB:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hubs.forEach { hub ->
                        val isSelected = hub.code == selectedDestHub.code
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) GoldAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) GoldAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedDestHub = hub }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("calc_dest_${hub.code.lowercase(Locale.US)}")
                        ) {
                            Text(
                                text = "${hub.city} (${hub.code})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 3. Freight Transport Mode
                Text(
                    text = "FREIGHT VELOCITY MODE:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LogisticsFreightMode.values().forEach { mode ->
                        val isSelected = mode == selectedFreightMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedFreightMode = mode }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = mode.iconEmoji, fontSize = 12.sp)
                                Text(
                                    text = mode.label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyanAccent else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // 4. Customs Clearance Category
                Text(
                    text = "CUSTOMS CLEARANCE CATEGORY:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomsClearanceCategory.values().forEach { cat ->
                        val isSelected = cat == selectedClearanceCategory
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) EmeraldPositive.copy(alpha = 0.2f) else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) EmeraldPositive else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedClearanceCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${cat.label} (+${cat.bufferDays}d)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) EmeraldPositive else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 5. Resulting Delivery Calculation Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(CyanAccent, GoldAccent))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CALCULATED ESTIMATED DELIVERY DATE (ETA)",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = calculationResult.calculatedEstimatedDeliveryDate,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Confidence pill
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldPositive.copy(alpha = 0.15f),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.5f)))
                                )
                            ) {
                                Text(
                                    text = "${calculationResult.deliveryConfidencePercent}% ON-TIME",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Breakdown metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Corridor: ${String.format(Locale.US, "%,d", calculationResult.geodesicDistanceKm.roundToInt())} km (${String.format(Locale.US, "%,d", calculationResult.nauticalMiles.roundToInt())} NM)",
                                fontSize = 9.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Total Transit: ${calculationResult.totalTransitDays} Days",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Formula Step Pill
                        Text(
                            text = "Transit (${calculationResult.baseTransitDays}d) + Dwell (${calculationResult.terminalDwellDays}d) + Customs (${calculationResult.customsClearanceDays}d) + Buffer (${calculationResult.contingencyBufferDays}d)",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CO₂ Estimate: ~${String.format(Locale.US, "%,d", calculationResult.carbonEstimateKgCo2.roundToInt())} kg",
                                fontSize = 8.5.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Recommended: ${calculationResult.recommendedIncoterm}",
                                fontSize = 8.5.sp,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (targetShipmentId != null && onApplyEtaToShipment != null) {
                Button(
                    onClick = {
                        onApplyEtaToShipment(targetShipmentId, calculationResult.calculatedEstimatedDeliveryDate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_apply_calculated_eta")
                ) {
                    Text(
                        text = "APPLY ETA TO SHIPMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DONE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CLOSE",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun MetricMiniPill(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.2f))))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(text = title, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, fontFamily = FontFamily.Monospace)
            Text(text = subtext, fontSize = 7.5.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        }
    }
}
