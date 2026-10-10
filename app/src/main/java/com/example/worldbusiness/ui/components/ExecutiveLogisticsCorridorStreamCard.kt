package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine
import java.util.Locale

/**
 * Executive Global Logistics & Transit Telemetry Stream Widget for the Cockpit Screen.
 * Provides real-time visibility into cross-border freight corridors, active transit values,
 * dynamic ETA recalculations, and one-tap status updates backed by the Room database.
 */
@Composable
fun ExecutiveLogisticsCorridorStreamCard(
    shipments: List<ShipmentRecord>,
    onNavigateToLogistics: () -> Unit,
    onUpdateShipmentStatus: (id: Long, newStatus: String, newEta: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedShipmentForUpdate by remember { mutableStateOf<ShipmentRecord?>(null) }

    val activeCount = shipments.count { it.customsStatus != "DELIVERED" }
    val inTransitValue = shipments.filter { it.customsStatus != "DELIVERED" }.sumOf { it.cargoValue }
    val inspectionHolds = shipments.count { it.customsStatus == "PORT_INSPECTION" || it.customsStatus == "CUSTOMS_HOLD" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceElevated,
                        SurfaceDark
                    )
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("executive_logistics_corridor_stream_card")
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Logistics Corridors",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GLOBAL FREIGHT CORRIDORS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldPositive.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ROOM LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "Real-Time Telemetry • Geodesic ETA Engine • Room DB",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToLogistics,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_cockpit_view_all_logistics")
                ) {
                    Text(
                        text = "VIEW RADAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Logistics",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Summary Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogisticsMiniMetric(
                    label = "ACTIVE CARGO",
                    value = Formatters.formatUsd(inTransitValue),
                    subtext = "$activeCount Dispatches",
                    color = CyanAccent,
                    modifier = Modifier.weight(1.2f)
                )
                LogisticsMiniMetric(
                    label = "CLEARANCE VELOCITY",
                    value = if (activeCount > 0) "${((activeCount - inspectionHolds) * 100 / activeCount)}%" else "100%",
                    subtext = "WCO SAFE Standard",
                    color = EmeraldPositive,
                    modifier = Modifier.weight(1f)
                )
                LogisticsMiniMetric(
                    label = "INSPECTIONS",
                    value = "$inspectionHolds Holds",
                    subtext = if (inspectionHolds > 0) "Customs Dwell" else "0 Disputed",
                    color = if (inspectionHolds > 0) GoldAccent else TextMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Scrollable Corridor Stream Cards
            if (shipments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, BorderSubtle))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active international shipments in Room database. Tap to dispatch commercial freight.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(shipments.take(6)) { shipment ->
                        CorridorShipmentStreamItem(
                            shipment = shipment,
                            onQuickUpdateClick = { selectedShipmentForUpdate = shipment }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog for Quick Status Update & ETA Recalibration directly from Cockpit
    selectedShipmentForUpdate?.let { shipment ->
        QuickStatusUpdateDialog(
            shipment = shipment,
            onDismiss = { selectedShipmentForUpdate = null },
            onConfirm = { newStatus, newEta ->
                onUpdateShipmentStatus(shipment.id, newStatus, newEta)
                selectedShipmentForUpdate = null
            }
        )
    }
}

@Composable
private fun LogisticsMiniMetric(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, BorderSubtle))),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtext,
                fontSize = 8.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CorridorShipmentStreamItem(
    shipment: ShipmentRecord,
    onQuickUpdateClick: () -> Unit
) {
    val isAir = shipment.carrier.contains("air", ignoreCase = true) || shipment.carrier.contains("flight", ignoreCase = true)
    val modeIcon: ImageVector = if (isAir) Icons.Default.FlightTakeoff else Icons.Default.Anchor

    val statusColor = when (shipment.customsStatus) {
        "CLEARED" -> EmeraldPositive
        "IN_TRANSIT" -> CyanAccent
        "PORT_INSPECTION" -> GoldAccent
        "CUSTOMS_HOLD", "DOCUMENTATION_REQ" -> RoseNegative
        else -> TextMuted
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    BorderSubtle,
                    statusColor.copy(alpha = 0.3f)
                )
            )
        ),
        modifier = Modifier
            .width(260.dp)
            .clickable { onQuickUpdateClick() }
            .testTag("cockpit_shipment_stream_item_${shipment.trackingCode}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Carrier, Incoterm, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = "Freight Mode",
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = shipment.carrier,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = shipment.customsStatus,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Origin -> Destination Corridor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = extractCityCode(shipment.origin),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "To",
                    tint = TextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = extractCityCode(shipment.destination),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.weight(1f))

                // Incoterm Pill
                Text(
                    text = shipment.incoterm,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = shipment.cargoDescription,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Value & ETA Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CARGO VALUE",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = Formatters.formatUsd(shipment.cargoValue),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ESTIMATED ARRIVAL",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = shipment.estimatedArrival,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Status Update Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to update status ⚡",
                    fontSize = 8.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun extractCityCode(hubLocation: String): String {
    val parenIndex = hubLocation.indexOf('(')
    val closeParenIndex = hubLocation.indexOf(')')
    if (parenIndex != -1 && closeParenIndex != -1 && closeParenIndex > parenIndex) {
        return hubLocation.substring(parenIndex + 1, closeParenIndex).trim()
    }
    val parts = hubLocation.split(",")
    return parts.firstOrNull()?.trim()?.take(14) ?: hubLocation.take(14)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickStatusUpdateDialog(
    shipment: ShipmentRecord,
    onDismiss: () -> Unit,
    onConfirm: (newStatus: String, newEta: String?) -> Unit
) {
    val statuses = listOf("IN_TRANSIT", "PORT_INSPECTION", "DOCUMENTATION_REQ", "CUSTOMS_HOLD", "CLEARED", "DELIVERED")
    var selectedStatus by remember { mutableStateOf(shipment.customsStatus) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Auto-calculate recalibrated ETA preview
    val autoEta = remember(selectedStatus) {
        LogisticsDeliveryCalculatorEngine.recalibrateShipmentEta(shipment, selectedStatus)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Update Status",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECALIBRATE SHIPMENT STATUS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tracking: ${shipment.trackingCode} (${shipment.carrier})",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Route: ${shipment.origin} ➔ ${shipment.destination}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customs & Transit Status", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = status,
                                        fontSize = 11.sp,
                                        fontWeight = if (status == selectedStatus) FontWeight.Bold else FontWeight.Normal,
                                        color = if (status == selectedStatus) CyanAccent else TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                onClick = {
                                    selectedStatus = status
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // ETA Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, BorderSubtle))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CURRENT ETA",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "RECALIBRATED ETA",
                                fontSize = 9.sp,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = shipment.estimatedArrival,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = autoEta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus, autoEta) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text(
                    text = "COMMIT STATUS TO ROOM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
