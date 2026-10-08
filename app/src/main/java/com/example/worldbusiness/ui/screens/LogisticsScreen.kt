package com.example.worldbusiness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.ui.components.D3GlobalLogisticsVisualizer
import com.example.worldbusiness.ui.components.Formatters
import com.example.worldbusiness.ui.components.InternationalLogisticsDashboard
import com.example.worldbusiness.ui.components.SupplierComplianceColorStatusBarCard
import com.example.worldbusiness.ui.components.SupplierComplianceDashboard
import com.example.worldbusiness.ui.components.SupplierComplianceMetrics
import com.example.worldbusiness.ui.components.SupplyChainTrackingView
import com.example.worldbusiness.data.model.LogisticsFreightMode
import com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine
import java.util.Locale
import kotlin.math.roundToInt

enum class LogisticsViewMode(val label: String) {
    DASHBOARD("Dashboard"),
    LIVE_TRACKING("Telemetry"),
    SUPPLIERS("Suppliers & Risk"),
    D3_RADAR("Global Radar"),
    MANIFEST("Manifest")
}

@Composable
fun LogisticsScreen(
    shipments: List<ShipmentRecord>,
    suppliers: List<GlobalSupplierRecord> = emptyList(),
    invoices: List<InvoiceRecord> = emptyList(),
    onDispatchShipment: (
        origin: String,
        destination: String,
        carrier: String,
        incoterm: String,
        description: String,
        value: Double,
        currency: String,
        eta: String
    ) -> Unit,
    onAddSupplier: (GlobalSupplierRecord) -> Unit = {},
    onUpdateSupplierCompliance: (id: Long, newStatus: String, notes: String) -> Unit = { _, _, _ -> },
    onUpdateSupplierPaymentTerms: (id: Long, termsDays: Int, description: String, currency: String) -> Unit = { _, _, _, _ -> },
    onCreateInvoiceForSupplier: ((GlobalSupplierRecord) -> Unit)? = null,
    onUpdateShipmentStatus: ((id: Long, newStatus: String, newEta: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(LogisticsViewMode.DASHBOARD) }
    var showDispatchDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredShipments = when (selectedFilter) {
        "TRANSIT" -> shipments.filter { it.customsStatus == "IN_TRANSIT" }
        "CLEARED" -> shipments.filter { it.customsStatus == "CLEARED" }
        "INSPECTION" -> shipments.filter { it.customsStatus == "PORT_INSPECTION" || it.customsStatus == "DOCUMENTATION_REQ" }
        else -> shipments
    }

    val totalCargoValueUsd = shipments.sumOf { it.cargoValue }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("logistics_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header & Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CROSS-BORDER SUPPLY CHAIN & FREIGHT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${shipments.size} Active Commercial Shipments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { showDispatchDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_dispatch_freight")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dispatch",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DISPATCH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Mode Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogisticsViewMode.values().forEach { mode ->
                val isSelected = viewMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent else SurfaceDark)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { viewMode = mode }
                        .padding(vertical = 9.dp)
                        .testTag("logistics_tab_${mode.name.lowercase(Locale.US)}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (viewMode) {
            LogisticsViewMode.DASHBOARD -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        InternationalLogisticsDashboard(
                            shipments = shipments,
                            onUpdateShipmentStatus = onUpdateShipmentStatus ?: { _, _, _ -> },
                            onDispatchShipmentClick = { showDispatchDialog = true }
                        )
                    }
                }
            }

            LogisticsViewMode.LIVE_TRACKING -> {
                val supplierMetrics = remember(suppliers) {
                    val total = suppliers.size.coerceAtLeast(1)
                    val verified = suppliers.count { it.complianceStatus.equals("COMPLIANT", ignoreCase = true) }
                    val expiringOrPending = suppliers.count {
                        it.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) ||
                        it.riskLevel.equals("MEDIUM", ignoreCase = true)
                    }
                    val nonCompliant = suppliers.count {
                        it.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) ||
                        it.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) ||
                        it.riskLevel.equals("HIGH", ignoreCase = true)
                    }
                    SupplierComplianceMetrics(
                        totalCount = suppliers.size,
                        verifiedCount = verified,
                        expiringOrPendingCount = expiringOrPending,
                        nonCompliantCount = nonCompliant,
                        verifiedRatio = verified.toFloat() / total.toFloat(),
                        expiringRatio = expiringOrPending.toFloat() / total.toFloat(),
                        nonCompliantRatio = nonCompliant.toFloat() / total.toFloat(),
                        overallHealthScore = if (suppliers.isNotEmpty()) ((verified * 100 + expiringOrPending * 50) / suppliers.size).coerceIn(0, 100) else 100,
                        w8BenFilingRate = if (suppliers.isNotEmpty()) ((suppliers.count { it.w8BenOrTaxFormFiled }.toDouble() / suppliers.size) * 100).roundToInt() else 100,
                        isoCertRate = if (suppliers.isNotEmpty()) ((suppliers.count { it.iso9001Certified }.toDouble() / suppliers.size) * 100).roundToInt() else 100,
                        antiBriberyRate = if (suppliers.isNotEmpty()) ((suppliers.count { it.antiBriberyPactSigned }.toDouble() / suppliers.size) * 100).roundToInt() else 100,
                        averageEsgScore = if (suppliers.isNotEmpty()) suppliers.map { it.esgRatingScore }.average() else 88.0
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SupplierComplianceColorStatusBarCard(
                            metrics = supplierMetrics,
                            onSelectFilter = { viewMode = LogisticsViewMode.SUPPLIERS }
                        )
                    }

                    item {
                        SupplyChainTrackingView(
                            shipments = shipments,
                            onDispatchClick = { showDispatchDialog = true }
                        )
                    }
                }
            }

            LogisticsViewMode.SUPPLIERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SupplierComplianceDashboard(
                            suppliers = suppliers,
                            invoices = invoices,
                            onAddSupplier = onAddSupplier,
                            onUpdateSupplierCompliance = onUpdateSupplierCompliance,
                            onUpdateSupplierPaymentTerms = onUpdateSupplierPaymentTerms,
                            onCreateInvoiceForSupplier = onCreateInvoiceForSupplier
                        )
                    }
                }
            }

            LogisticsViewMode.D3_RADAR -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        D3GlobalLogisticsVisualizer(
                            shipments = shipments
                        )
                    }
                }
            }

            LogisticsViewMode.MANIFEST -> {
                // Cargo Value Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TOTAL FREIGHT CARGO VALUE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = Formatters.formatUsd(totalCargoValueUsd),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "CUSTOMS CLEARANCE RATE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "98.2% Fast-Track",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "TRANSIT", "CLEARED", "INSPECTION").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanAccent else SurfaceElevated)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("filter_logistics_$filter")
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredShipments, key = { it.id }) { shipment ->
                        ShipmentCard(shipment = shipment)
                    }
                }
            }
        }
    }

    if (showDispatchDialog) {
        DispatchFreightDialog(
            onDismiss = { showDispatchDialog = false },
            onConfirm = { origin, dest, carrier, incoterm, desc, value, curr, eta ->
                onDispatchShipment(origin, dest, carrier, incoterm, desc, value, curr, eta)
                showDispatchDialog = false
            }
        )
    }
}

@Composable
private fun ShipmentCard(shipment: ShipmentRecord) {
    val statusColor = Formatters.getStatusColor(shipment.customsStatus)
    val isAir = shipment.carrier.contains("Cargo", ignoreCase = true) || shipment.carrier.contains("Lufthansa", ignoreCase = true) || shipment.carrier.contains("ANA", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
            .testTag("shipment_card_${shipment.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAir) Icons.Default.FlightTakeoff else Icons.Default.Anchor,
                        contentDescription = "Transport Type",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shipment.trackingCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
                        text = shipment.customsStatus.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${shipment.origin} ➔ ${shipment.destination}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "${shipment.carrier} • Incoterm: ${shipment.incoterm}",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "CARGO VALUATION", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = Formatters.formatCurrency(shipment.cargoValue, shipment.currency),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ESTIMATED ARRIVAL", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = shipment.estimatedArrival,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Manifest: ${shipment.cargoDescription}",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun DispatchFreightDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        origin: String,
        destination: String,
        carrier: String,
        incoterm: String,
        description: String,
        value: Double,
        currency: String,
        eta: String
    ) -> Unit
) {
    var origin by remember { mutableStateOf("Port of Shanghai (SHA), China") }
    var destination by remember { mutableStateOf("Port of Los Angeles (LAX), USA") }
    var carrier by remember { mutableStateOf("Maersk Line") }
    var incoterm by remember { mutableStateOf("DDP") }
    var description by remember { mutableStateOf("High-Density Compute Blades & Cryo Coolers") }
    var valueStr by remember { mutableStateOf("780000") }
    var currency by remember { mutableStateOf("USD") }
    var eta by remember { mutableStateOf("Oct 18, 2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "DISPATCH COMMERCIAL FREIGHT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origin Hub / Port", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination Hub / Port", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carrier,
                        onValueChange = { carrier = it },
                        label = { Text("Carrier", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = incoterm,
                        onValueChange = { incoterm = it },
                        label = { Text("Incoterm", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        label = { Text("Cargo Value", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Cargo Manifest Description", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTIMATED ARRIVAL (ETA)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = {
                            val oHub = LogisticsDeliveryCalculatorEngine.findHubByKeyword(origin)
                            val dHub = LogisticsDeliveryCalculatorEngine.findHubByKeyword(destination)
                            val mode = if (carrier.lowercase(Locale.US).contains("air") || carrier.lowercase(Locale.US).contains("flight")) {
                                LogisticsFreightMode.AIR_CARGO
                            } else {
                                LogisticsFreightMode.OCEAN_CONTAINER
                            }
                            val calc = LogisticsDeliveryCalculatorEngine.calculateDeliveryEstimate(oHub, dHub, mode)
                            eta = calc.calculatedEstimatedDeliveryDate
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "⚡ AUTO-CALCULATE ETA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedTextField(
                    value = eta,
                    onValueChange = { eta = it },
                    label = { Text("Estimated Arrival", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (origin.isNotBlank() && destination.isNotBlank()) {
                        val v = valueStr.toDoubleOrNull() ?: 500000.0
                        onConfirm(origin, destination, carrier, incoterm, description, v, currency, eta)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("DISPATCH BILL OF LADING", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
