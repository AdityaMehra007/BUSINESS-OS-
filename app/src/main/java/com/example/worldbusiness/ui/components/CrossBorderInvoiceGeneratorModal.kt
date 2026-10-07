package com.example.worldbusiness.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.CrossBorderTaxCalculator
import com.example.worldbusiness.data.repository.FormattedInvoiceEngine
import com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper
import java.util.Locale

/**
 * Interactive Modal for Generating Formatted Cross-Border Invoices
 * using corporate entities and tax rules stored in Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossBorderInvoiceGeneratorModal(
    entities: List<EntityRecord>,
    onDismiss: () -> Unit,
    onInvoiceCreated: (InvoiceRecord, List<CrossBorderInvoiceItem>) -> Unit
) {
    var selectedEntityName by remember {
        mutableStateOf(entities.firstOrNull()?.name ?: "OmniGlobal Holdings Inc.")
    }
    var entityDropdownExpanded by remember { mutableStateOf(false) }

    val sampleClients = FormattedInvoiceEngine.sampleClients
    var selectedClientIndex by remember { mutableStateOf(0) }
    val currentClient = sampleClients.getOrNull(selectedClientIndex) ?: sampleClients.first()

    var clientName by remember { mutableStateOf(currentClient.name) }
    var clientCountry by remember { mutableStateOf(currentClient.country) }
    var currency by remember { mutableStateOf(currentClient.preferredCurrency) }
    var isReverseCharge by remember { mutableStateOf(true) }

    // Dynamic line items
    val lineItems = remember {
        mutableStateListOf(
            CrossBorderInvoiceItem("Cross-Border Enterprise Architecture Consulting", 1.0, 150000.0, "998313", 0.0),
            CrossBorderInvoiceItem("ISO 20022 Multi-Rail Real-Time Integration", 1.0, 45000.0, "998313", 0.0)
        )
    }

    var newItemDesc by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemPrice by remember { mutableStateOf("") }

    val subtotal by remember {
        derivedStateOf { lineItems.sumOf { it.totalAmount } }
    }

    // Live Regional Tax Calculation based on regional rules
    val taxCalc by remember(subtotal, currency, clientCountry, isReverseCharge) {
        derivedStateOf {
            CrossBorderTaxCalculator.calculate(
                subtotal = subtotal,
                currencyCode = currency,
                clientCountry = clientCountry,
                isReverseChargeApplied = isReverseCharge
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cross_border_invoice_generator"),
        containerColor = SurfaceDark,
        confirmButton = {
            Button(
                onClick = {
                    if (subtotal > 0.0 && clientName.isNotBlank()) {
                        val invNum = "INV-2026-${(1000..9999).random()}"
                        val invoice = InvoiceRecord(
                            invoiceNumber = invNum,
                            issuingEntityName = selectedEntityName,
                            clientName = clientName,
                            clientCountry = clientCountry,
                            issueDate = "Oct 05, 2026",
                            dueDate = "Nov 05, 2026",
                            amount = subtotal,
                            currency = currency,
                            taxRatePercent = taxCalc.vatRatePercent,
                            status = "PENDING",
                            serviceDescription = lineItems.firstOrNull()?.description ?: "Cross-Border Commercial Services",
                            isSynced = true,
                            syncStatus = "SYNCED"
                        )
                        onInvoiceCreated(invoice, lineItems.toList())
                    }
                },
                enabled = subtotal > 0.0 && clientName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_generate_invoice")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Generate", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GENERATE FORMATTED INVOICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted, fontFamily = FontFamily.Monospace)
            }
        },
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Generator",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "CROSS-BORDER INVOICE GENERATOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Automatic Regional Tax Engine & Room Storage",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Issuing Entity from Room
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. ISSUING CORPORATE ENTITY (FROM ROOM)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    ExposedDropdownMenuBox(
                        expanded = entityDropdownExpanded,
                        onExpandedChange = { entityDropdownExpanded = !entityDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEntityName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("select_issuing_entity"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = entityDropdownExpanded,
                            onDismissRequest = { entityDropdownExpanded = false }
                        ) {
                            entities.forEach { ent ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(ent.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("${ent.jurisdiction} • ${ent.taxId}", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    },
                                    onClick = {
                                        selectedEntityName = ent.name
                                        entityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 2: International Client Preset Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "2. INTERNATIONAL CLIENT PRESETS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sampleClients.forEachIndexed { index, profile ->
                            val isSelected = selectedClientIndex == index
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) GoldAccent else SurfaceElevated,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(if (isSelected) GoldAccent else BorderSubtle, BorderSubtle))
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedClientIndex = index
                                        clientName = profile.name
                                        clientCountry = profile.country
                                        currency = profile.preferredCurrency
                                    }
                                    .testTag("client_preset_${profile.countryCode.lowercase(Locale.US)}")
                            ) {
                                Text(
                                    text = "${profile.name} (${profile.countryCode})",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Client Details Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name", fontSize = 9.sp) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_client_name"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = clientCountry,
                        onValueChange = { clientCountry = it },
                        label = { Text("Country", fontSize = 9.sp) },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_client_country"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase(Locale.US) },
                        label = { Text("Currency", fontSize = 9.sp) },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("input_invoice_currency"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Section 3: Line Items Editor
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "3. INVOICE LINE ITEMS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    // Existing Line Items
                    lineItems.forEachIndexed { idx, item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceElevated,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f)))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Qty: ${String.format(Locale.US, "%.0f", item.quantity)} @ ${Formatters.formatCurrency(item.unitPrice)} $currency", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${Formatters.formatCurrency(item.totalAmount)} $currency",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (lineItems.size > 1) {
                                        IconButton(
                                            onClick = { lineItems.removeAt(idx) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseNegative, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Line Item Mini-Form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemDesc,
                            onValueChange = { newItemDesc = it },
                            placeholder = { Text("Service Description", fontSize = 9.sp) },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            placeholder = { Text("Amount", fontSize = 9.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Button(
                            onClick = {
                                val p = newItemPrice.toDoubleOrNull() ?: 0.0
                                if (newItemDesc.isNotBlank() && p > 0.0) {
                                    lineItems.add(CrossBorderInvoiceItem(newItemDesc, 1.0, p, "998313", 0.0))
                                    newItemDesc = ""
                                    newItemPrice = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Section 4: Live Regional Tax Calculation Breakdown Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("regional_tax_breakdown_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GoldAccent.copy(alpha = 0.5f), BorderSubtle)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUTOMATIC REGIONAL TAX BREAKDOWN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            // Reverse charge toggle
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Reverse Charge:", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                Switch(
                                    checked = isReverseCharge,
                                    onCheckedChange = { isReverseCharge = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPositive, checkedTrackColor = EmeraldPositive.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(36.dp, 20.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("${Formatters.formatCurrency(subtotal)} $currency", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (taxCalc.isReverseCharge) "Tax: 0% Reverse Charge" else "Tax: ${String.format(Locale.US, "%.1f", taxCalc.vatRatePercent)}% VAT/GST",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "+${Formatters.formatCurrency(taxCalc.vatAmount)} $currency",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (taxCalc.vatAmount == 0.0) EmeraldPositive else GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (taxCalc.whtAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("WHT Deduction (${String.format(Locale.US, "%.1f", taxCalc.whtRatePercent)}%):", fontSize = 9.sp, color = RoseNegative, fontFamily = FontFamily.Monospace)
                                Text("-${Formatters.formatCurrency(taxCalc.whtAmount)} $currency", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseNegative, fontFamily = FontFamily.Monospace)
                            }
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Net Payable:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${Formatters.formatCurrency(taxCalc.netReceivable)} $currency (≈ $${Formatters.formatCompactNumber(taxCalc.equivalentUsd)} USD)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = taxCalc.complianceNote,
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Section 5: Real-Time Multi-Currency Conversion & Volatility Hedging Buffer
                val dualPricingQuote = remember(subtotal, currency, taxCalc.vatRatePercent) {
                    val targetSettlement = if (currency == "USD") "EUR" else "USD"
                    MultiCurrencyConversionHelper.calculateInvoiceMultiCurrencyPricing(
                        invoiceNumber = "PRO-FORMA",
                        subtotal = subtotal,
                        originalCurrency = currency,
                        settlementCurrency = targetSettlement,
                        vatRatePercent = taxCalc.vatRatePercent,
                        paymentTermsDays = 30
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_currency_conversion_quote_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REAL-TIME MULTI-CURRENCY CONVERSION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Net 30 Hedging Buffer",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Cross-Rate (1 $currency):",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${Formatters.formatRate(dualPricingQuote.effectiveFxRate)} ${dualPricingQuote.settlementCurrency}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Settlement (${dualPricingQuote.settlementCurrency}):",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${Formatters.formatCurrency(dualPricingQuote.settlementGrossTotal)} ${dualPricingQuote.settlementCurrency}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Volatility Buffer (+1.5%):",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "+${Formatters.formatCurrency(dualPricingQuote.hedgingBufferAmount)} ${dualPricingQuote.settlementCurrency}",
                                fontSize = 9.sp,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Clearing Rail: ${dualPricingQuote.clearingRail}",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    )
}
