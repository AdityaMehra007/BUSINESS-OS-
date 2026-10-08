package com.example.worldbusiness.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.worldbusiness.data.model.CorporateExporterProfile
import com.example.worldbusiness.data.model.CrossBorderClientProfile
import com.example.worldbusiness.data.model.CrossBorderIncoterm
import com.example.worldbusiness.data.model.CrossBorderInvoiceDocument
import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.CrossBorderInvoiceStatus
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GeneratedPdfResult
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
import com.example.worldbusiness.data.repository.CrossBorderTaxCalculator
import com.example.worldbusiness.ui.components.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise Material 3 Screen for Cross-Border Invoice PDF Generation & Storage.
 * Allows users to inspect, customize, generate, and save statutory commercial invoice PDFs,
 * with SHA-256 cryptographic audit sealing and system sharing/viewing integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossBorderInvoicePdfScreen(
    invoices: List<InvoiceRecord>,
    entities: List<EntityRecord>,
    fxBalances: List<FxBalanceRecord>,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedInvoiceNumber by remember {
        mutableStateOf(invoices.firstOrNull()?.invoiceNumber ?: "INV-2026-CH-8921")
    }
    var invoiceDropdownExpanded by remember { mutableStateOf(false) }
    var incoterm by remember { mutableStateOf(CrossBorderIncoterm.DAP) }
    var incotermDropdownExpanded by remember { mutableStateOf(false) }

    // Selected Invoice from DB or sample
    val matchedDbInvoice = invoices.find { it.invoiceNumber == selectedInvoiceNumber }
    val matchedEntity = entities.find { it.name == matchedDbInvoice?.issuingEntityName } ?: entities.firstOrNull()

    var clientName by remember(selectedInvoiceNumber) {
        mutableStateOf(matchedDbInvoice?.clientName ?: "Stellar Logistics GmbH")
    }
    var clientCountry by remember(selectedInvoiceNumber) {
        mutableStateOf(matchedDbInvoice?.clientCountry ?: "Germany")
    }
    var currency by remember(selectedInvoiceNumber) {
        mutableStateOf(matchedDbInvoice?.currency ?: "CHF")
    }
    var paymentTerms by remember(selectedInvoiceNumber) {
        mutableStateOf("Net 30 Days (SEPA / SIC Rail)")
    }
    var isReverseCharge by remember(selectedInvoiceNumber) {
        mutableStateOf(matchedDbInvoice?.taxRatePercent == 0.0)
    }

    // Editable line items
    val lineItems = remember(selectedInvoiceNumber) {
        mutableStateListOf(
            CrossBorderInvoiceItem("Global Enterprise Architecture Consulting", 1.0, 95000.0, "998313", 8.1),
            CrossBorderInvoiceItem("ISO 20022 Cross-Border Settlement Gateway", 1.0, 50000.0, "998313", 8.1)
        )
    }

    var newItemDesc by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    // PDF Generation State
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var lastGeneratedResult by remember { mutableStateOf<GeneratedPdfResult?>(null) }
    var savedPdfList by remember { mutableStateOf<List<File>>(emptyList()) }

    fun refreshSavedPdfs() {
        savedPdfList = CrossBorderInvoicePdfEngine.listGeneratedInvoices(context)
    }

    LaunchedEffect(Unit) {
        refreshSavedPdfs()
    }

    // Recalculate Subtotal & Tax
    val subtotal = lineItems.sumOf { it.totalAmount }
    val taxCalc = remember(subtotal, currency, clientCountry, isReverseCharge) {
        CrossBorderTaxCalculator.calculate(
            subtotal = subtotal,
            currencyCode = currency,
            clientCountry = clientCountry,
            isReverseChargeApplied = isReverseCharge
        )
    }

    val currencySymbol = Formatters.getCurrencySymbol(currency)

    fun buildInvoiceDocument(): CrossBorderInvoiceDocument {
        val exporterEntity = matchedEntity ?: EntityRecord(
            id = 1,
            name = "Apex Global Treasury AG",
            jurisdiction = "Zurich, Switzerland",
            countryCode = "CH",
            entityType = "Corporation",
            taxId = "CHE-109.843.210 MWST",
            status = "GOOD_STANDING",
            baseCurrency = currency,
            operatingCapital = 5000000.0,
            annualFilingDeadline = "2026-12-31",
            localDirector = "Dr. Beatrix von Haller",
            complianceScore = 98
        )

        return CrossBorderInvoiceDocument(
            invoiceNumber = selectedInvoiceNumber,
            issueDate = matchedDbInvoice?.issueDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            dueDate = matchedDbInvoice?.dueDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 30L * 86400000L)),
            paymentTerms = paymentTerms,
            status = when (matchedDbInvoice?.status) {
                "PAID" -> CrossBorderInvoiceStatus.PAID
                "OVERDUE" -> CrossBorderInvoiceStatus.OVERDUE
                "IN_CLEARING" -> CrossBorderInvoiceStatus.IN_CLEARING
                else -> CrossBorderInvoiceStatus.ISSUED
            },
            incoterm = incoterm,
            exporter = CorporateExporterProfile(
                entityName = exporterEntity.name,
                jurisdiction = exporterEntity.jurisdiction,
                physicalAddress = "${exporterEntity.jurisdiction}, ${exporterEntity.countryCode}",
                countryCode = exporterEntity.countryCode,
                vatTaxId = exporterEntity.taxId,
                eoriCustomsNumber = "EORI-${exporterEntity.countryCode}-892341",
                registrationNumber = "REG-${exporterEntity.id}",
                legalRepresentative = exporterEntity.localDirector,
                bankInstitution = when (currency) {
                    "CHF" -> "UBS Switzerland AG (Zurich)"
                    "EUR" -> "Deutsche Bank AG (Frankfurt)"
                    "GBP" -> "Barclays Bank PLC (London)"
                    else -> "JPMorgan Chase Bank N.A. (New York)"
                },
                swiftBic = when (currency) {
                    "CHF" -> "UBSWCHZH80A"
                    "EUR" -> "DEUTDEDBFXX"
                    "GBP" -> "BARCGB22XXX"
                    else -> "CHASUS33XXX"
                },
                ibanOrAccount = when (currency) {
                    "CHF" -> "CH93 0070 0110 0002 4589 1"
                    "EUR" -> "DE89 3707 0024 0215 8920 00"
                    "GBP" -> "GB29 BARC 2000 0012 3456 78"
                    else -> "US44 0210 0002 1234 5678 90"
                },
                primaryClearingRail = when (currency) {
                    "CHF" -> "Swiss Interbank Clearing (SIC) RTGS Rail"
                    "EUR" -> "SEPA Instant / Target2 RTGS ISO 20022"
                    "GBP" -> "CHAPS / UK Faster Payments Service"
                    else -> "Fedwire / CHIPS Real-Time Gross Settlement"
                }
            ),
            client = CrossBorderClientProfile(
                clientName = clientName,
                destinationCountry = clientCountry,
                destinationCountryCode = if (clientCountry.contains("Germany")) "DE" else "US",
                billingAddress = "Taunusanlage 8, Frankfurt am Main, Germany",
                shippingAddress = "Cargo City South, Frankfurt Airport",
                clientVatGstId = "DE 289 140 821",
                contactEmail = "accounts-payable@${clientName.lowercase(Locale.US).replace(" ", "")}.com",
                contactPhone = "+49 69 9123 4500"
            ),
            currency = currency,
            currencySymbol = currencySymbol,
            exchangeRateToUsd = if (taxCalc.netReceivable > 0.0) taxCalc.equivalentUsd / taxCalc.netReceivable else 1.0,
            lineItems = lineItems.toList(),
            subtotal = subtotal,
            taxName = if (isReverseCharge) "0% Reverse Charge VAT" else "Regional VAT/GST",
            taxRatePercent = taxCalc.vatRatePercent,
            taxAmount = taxCalc.vatAmount,
            isReverseCharge = isReverseCharge,
            withholdingTaxPercent = taxCalc.whtRatePercent,
            withholdingTaxAmount = taxCalc.whtAmount,
            grossTotal = taxCalc.grossTotal,
            netReceivable = taxCalc.netReceivable,
            equivalentUsdAmount = taxCalc.equivalentUsd,
            statutoryComplianceNote = taxCalc.complianceNote,
            customsDeclarationCode = "HS-EXP-998313-GLOBAL"
        )
    }

    fun handleGeneratePdf() {
        isGeneratingPdf = true
        scope.launch {
            try {
                val doc = buildInvoiceDocument()
                val result = withContext(Dispatchers.IO) {
                    CrossBorderInvoicePdfEngine.generateAndSaveInvoicePdf(context, doc)
                }
                lastGeneratedResult = result
                refreshSavedPdfs()
                Toast.makeText(context, "Invoice PDF saved: ${result.file.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF generation failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isGeneratingPdf = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp)
            .testTag("cross_border_invoice_pdf_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_from_pdf_screen")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Studio",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CROSS-BORDER PDF STUDIO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Statutory International Commercial Invoicing",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, CyanAccent.copy(alpha = 0.3f))))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "ISO 20022",
                            tint = EmeraldPositive,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "A4 VECTOR READY",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Section 1: Invoice Selector & Bilateral Configurations Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_configuration_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. INVOICE PARAMETERS & BILATERAL ENTITIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    // Invoice Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = invoiceDropdownExpanded,
                        onExpandedChange = { invoiceDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedInvoiceNumber,
                            onValueChange = { selectedInvoiceNumber = it },
                            label = { Text("Invoice Reference Number", fontSize = 10.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .testTag("input_invoice_number_field"),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = invoiceDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = invoiceDropdownExpanded,
                            onDismissRequest = { invoiceDropdownExpanded = false }
                        ) {
                            invoices.forEach { inv ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${inv.invoiceNumber} • ${inv.clientName} (${inv.currency})", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    },
                                    onClick = {
                                        selectedInvoiceNumber = inv.invoiceNumber
                                        clientName = inv.clientName
                                        clientCountry = inv.clientCountry
                                        currency = inv.currency
                                        isReverseCharge = inv.taxRatePercent == 0.0
                                        invoiceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Client Name & Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("Client Legal Name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = clientCountry,
                            onValueChange = { clientCountry = it },
                            label = { Text("Country", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Currency & Incoterms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Currency (ISO)", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        ExposedDropdownMenuBox(
                            expanded = incotermDropdownExpanded,
                            onExpandedChange = { incotermDropdownExpanded = it },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = "${incoterm.code} - ${incoterm.name}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Incoterms 2020", fontSize = 10.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incotermDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = incotermDropdownExpanded,
                                onDismissRequest = { incotermDropdownExpanded = false }
                            ) {
                                CrossBorderIncoterm.values().forEach { term ->
                                    DropdownMenuItem(
                                        text = { Text("${term.code} - ${term.label}", fontSize = 11.sp) },
                                        onClick = {
                                            incoterm = term
                                            incotermDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Line Items & Tax Calculation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("line_items_summary_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "2. DELIVERABLES & LINE ITEMS (${lineItems.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    lineItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.description, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "SAC ${item.hsnSacCode} • Qty: ${item.quantity} @ ${Formatters.formatCurrency(item.unitPrice, currency)}", fontSize = 8.5.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Formatters.formatCurrency(item.totalAmount, currency),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (lineItems.size > 1) {
                                    IconButton(
                                        onClick = { lineItems.removeAt(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseNegative.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Add line item row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemDesc,
                            onValueChange = { newItemDesc = it },
                            placeholder = { Text("New deliverable description", fontSize = 9.sp) },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            placeholder = { Text("Price", fontSize = 9.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.7f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        IconButton(
                            onClick = {
                                val price = newItemPrice.toDoubleOrNull() ?: 0.0
                                if (newItemDesc.isNotBlank() && price > 0.0) {
                                    lineItems.add(CrossBorderInvoiceItem(newItemDesc, 1.0, price, "998313", 8.1))
                                    newItemDesc = ""
                                    newItemPrice = ""
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = CyanAccent)
                        }
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

                    // Totals Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        Text(Formatters.formatCurrency(subtotal, currency), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isReverseCharge) "Tax: 0% Reverse Charge" else "Tax (${taxCalc.vatRatePercent}%):",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "+${Formatters.formatCurrency(taxCalc.vatAmount, currency)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (taxCalc.vatAmount == 0.0) EmeraldPositive else GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL COMMERCIAL PAYABLE:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        Text(
                            text = Formatters.formatCurrency(taxCalc.netReceivable, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "≈ $${Formatters.formatCompactNumber(taxCalc.equivalentUsd)} USD Equivalent (Live Conversion)",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section 3: PRIMARY ACTION - GENERATE & SAVE INVOICE PDF
        item {
            Button(
                onClick = { handleGeneratePdf() },
                enabled = !isGeneratingPdf,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = SurfaceDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_generate_save_pdf")
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SurfaceDark, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMPILING VECTOR PDF...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Generate PDF", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE & SAVE INVOICE PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section 4: Success Result Card
        item {
            AnimatedVisibility(
                visible = lastGeneratedResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastGeneratedResult?.let { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_pdf_success_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(EmeraldPositive, BorderSubtle)))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = EmeraldPositive,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PDF DOCUMENT READY & STORED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldPositive,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = res.formattedFileSize,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "File: ${res.file.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Path: ${res.filePath}",
                                fontSize = 8.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SHA-256 Audit Seal: ${res.sha256Checksum.take(32)}...",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )

                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(CrossBorderInvoicePdfEngine.createViewPdfIntent(context, res.file))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No PDF viewer app found: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_open_pdf")
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OPEN PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(CrossBorderInvoicePdfEngine.createSharePdfIntent(context, res.file))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_pdf")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SHARE PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Saved Commercial Invoices History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVED INVOICE PDF ARCHIVE (${savedPdfList.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = { refreshSavedPdfs() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (savedPdfList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No invoice PDFs generated yet. Configure parameters above and tap 'GENERATE & SAVE INVOICE PDF'.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(savedPdfList) { file ->
                SavedPdfItemCard(
                    file = file,
                    onOpen = {
                        try {
                            context.startActivity(CrossBorderInvoicePdfEngine.createViewPdfIntent(context, file))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        try {
                            context.startActivity(CrossBorderInvoicePdfEngine.createSharePdfIntent(context, file))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        file.delete()
                        refreshSavedPdfs()
                        Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun SavedPdfItemCard(
    file: File,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeStr = when {
        file.length() >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", file.length().toDouble() / (1024 * 1024))
        file.length() >= 1024 -> String.format(Locale.US, "%.1f KB", file.length().toDouble() / 1024)
        else -> "${file.length()} B"
    }
    val modifiedStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(file.lastModified()))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_pdf_item_${file.nameWithoutExtension}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RoseNegative.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = RoseNegative,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = file.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = "$sizeStr  |  $modifiedStr",
                        fontSize = 8.5.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpen, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open", tint = CyanAccent, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseNegative.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
