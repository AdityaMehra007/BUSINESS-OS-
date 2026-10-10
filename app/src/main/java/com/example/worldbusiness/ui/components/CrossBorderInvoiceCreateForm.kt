package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.worldbusiness.data.model.GeneratedPdfResult
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
import com.example.worldbusiness.data.repository.CrossBorderTaxCalculator
import com.example.worldbusiness.data.repository.FormattedInvoiceEngine
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Enterprise Form-Based Interface for Creating Cross-Border Invoices.
 *
 * Implements comprehensive fields for:
 * 1. Vendor Details (Issuing sovereign entity, VAT/Tax ID, registered address, banking SWIFT/IBAN, contact)
 * 2. Client / Buyer Details (Legal name, jurisdiction, destination country, VAT number, billing address)
 * 3. Real-Time Currency Selection (9+ global currencies, real-time FX rate indicators, USD conversion)
 * 4. Dynamic Line Items & Harmonized System (HS / SAC) codes
 * 5. Statutory Cross-Border Tax Calculations (Automatic VAT/GST engine, B2B Reverse Charge Art. 196,
 *    Custom VAT override, Double Tax Treaty Withholding Tax WHT, and compliance citations)
 * 6. Payment Terms, Incoterms, and Room SQLite persistence.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrossBorderInvoiceCreateForm(
    entities: List<EntityRecord>,
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    isOfflineMode: Boolean = false,
    onSaveInvoice: (
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // SECTION 1: VENDOR / ISSUING ENTITY DETAILS
    // ------------------------------------------------------------------------
    var selectedEntityIndex by remember { mutableStateOf(0) }
    var entityDropdownExpanded by remember { mutableStateOf(false) }

    val activeEntity = entities.getOrNull(selectedEntityIndex) ?: entities.firstOrNull()

    var vendorLegalName by remember(activeEntity) {
        mutableStateOf(activeEntity?.name ?: "Helvetia Global Asset Management AG")
    }
    var vendorJurisdiction by remember(activeEntity) {
        mutableStateOf(activeEntity?.jurisdiction ?: "Zurich, Switzerland")
    }
    var vendorTaxId by remember(activeEntity) {
        mutableStateOf(activeEntity?.taxId ?: "CHE-102.345.678 MWST")
    }
    var vendorAddress by remember(activeEntity) {
        mutableStateOf(
            when (activeEntity?.countryCode) {
                "CH" -> "Bahnhofstrasse 45, 8001 Zurich, Switzerland"
                "DE" -> "Taunusanlage 8, 60329 Frankfurt am Main, Germany"
                "SG" -> "10 Marina Boulevard #32-01, Marina Bay, Singapore 018983"
                "GB" -> "100 Bishopsgate, London EC2N 4AG, United Kingdom"
                "US" -> "1209 North Orange St, Wilmington, Delaware 19801, USA"
                else -> "Bahnhofstrasse 45, 8001 Zurich, Switzerland"
            }
        )
    }
    var vendorBankName by remember(activeEntity) {
        mutableStateOf(
            when (activeEntity?.countryCode) {
                "CH" -> "UBS Switzerland AG (Zurich)"
                "DE" -> "Deutsche Bank AG (Frankfurt)"
                "SG" -> "DBS Bank Ltd (Singapore)"
                "GB" -> "Barclays Corporate Banking (London)"
                else -> "JPMorgan Chase Bank N.A. (New York)"
            }
        )
    }
    var vendorSwiftBic by remember(activeEntity) {
        mutableStateOf(
            when (activeEntity?.countryCode) {
                "CH" -> "UBSWCHZH80A"
                "DE" -> "DEUTDEDBFXX"
                "SG" -> "DBSSSGSGXXX"
                "GB" -> "BARCGB22XXX"
                else -> "CHASUS33XXX"
            }
        )
    }
    var vendorIban by remember(activeEntity) {
        mutableStateOf(
            when (activeEntity?.countryCode) {
                "CH" -> "CH93 0024 0240 1234 5678 9"
                "DE" -> "DE89 5007 0010 0123 4567 89"
                "SG" -> "SG71 DBS0 0012 3456 7890 1"
                "GB" -> "GB29 BARC 2000 0012 3456 78"
                else -> "US33 CHAS 0210 0002 1234 5678"
            }
        )
    }
    var vendorContactEmail by remember(activeEntity) {
        mutableStateOf("treasury@omniglobal-holdings.com")
    }

    // ------------------------------------------------------------------------
    // SECTION 2: CLIENT / BUYER DETAILS
    // ------------------------------------------------------------------------
    val sampleClients = FormattedInvoiceEngine.sampleClients
    var clientName by remember { mutableStateOf("Siemens Energy AG") }
    var clientCountry by remember { mutableStateOf("Germany") }
    var clientVatId by remember { mutableStateOf("DE 302 918 201") }
    var clientAddress by remember { mutableStateOf("Otto-Hahn-Ring 6, 81739 Munich, Germany") }
    var clientContactEmail by remember { mutableStateOf("invoicing@siemens-energy.com") }

    // ------------------------------------------------------------------------
    // SECTION 3: REAL-TIME CURRENCY SELECTION
    // ------------------------------------------------------------------------
    var selectedCurrencyCode by remember { mutableStateOf("EUR") }
    val supportedCurrencies = remember(liveCurrencyFeed) {
        CrossBorderTaxCalculator.getSupportedCurrencies(liveCurrencyFeed.ratesToUsd)
    }
    val currentCurrencyOption by remember(selectedCurrencyCode, supportedCurrencies) {
        derivedStateOf {
            supportedCurrencies.find { it.code.equals(selectedCurrencyCode, ignoreCase = true) }
                ?: supportedCurrencies.first()
        }
    }

    // ------------------------------------------------------------------------
    // SECTION 4: LINE ITEMS & SERVICE CATALOG
    // ------------------------------------------------------------------------
    val lineItems = remember {
        mutableStateListOf(
            CrossBorderInvoiceItem(
                description = "Cross-Border Enterprise Architecture & Cloud SLA",
                quantity = 1.0,
                unitPrice = 85000.0,
                hsnSacCode = "998313",
                taxRatePercent = 0.0
            ),
            CrossBorderInvoiceItem(
                description = "ISO 20022 Multi-Rail Real-Time Integration & Liquidity Bridge",
                quantity = 1.0,
                unitPrice = 35000.0,
                hsnSacCode = "998313",
                taxRatePercent = 0.0
            )
        )
    }

    var newItemDescription by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemUnitPrice by remember { mutableStateOf("") }
    var newItemSacCode by remember { mutableStateOf("998313") }

    val subtotal by remember {
        derivedStateOf { lineItems.sumOf { it.totalAmount } }
    }

    // ------------------------------------------------------------------------
    // SECTION 5: STATUTORY CROSS-BORDER TAX CALCULATIONS
    // ------------------------------------------------------------------------
    var isReverseChargeEnabled by remember { mutableStateOf(true) }
    var isCustomTaxOverride by remember { mutableStateOf(false) }
    var customTaxPercentStr by remember { mutableStateOf("19.0") }
    var isWithholdingTaxEnabled by remember { mutableStateOf(false) }
    var customWhtPercentStr by remember { mutableStateOf("0.0") }

    val detectedTaxRule by remember(clientCountry) {
        derivedStateOf { CrossBorderTaxCalculator.getTaxRuleForCountry(clientCountry) }
    }

    val parsedCustomTax = customTaxPercentStr.toDoubleOrNull()
    val parsedCustomWht = if (isWithholdingTaxEnabled) customWhtPercentStr.toDoubleOrNull() else 0.0

    val taxCalculation by remember(
        subtotal,
        selectedCurrencyCode,
        clientCountry,
        isReverseChargeEnabled,
        isCustomTaxOverride,
        parsedCustomTax,
        parsedCustomWht,
        liveCurrencyFeed
    ) {
        derivedStateOf {
            CrossBorderTaxCalculator.calculate(
                subtotal = subtotal,
                currencyCode = selectedCurrencyCode,
                clientCountry = clientCountry,
                isReverseChargeApplied = isReverseChargeEnabled && !isCustomTaxOverride,
                customVatRate = if (isCustomTaxOverride) parsedCustomTax else null,
                customWhtRate = parsedCustomWht,
                liveRatesToUsd = liveCurrencyFeed.ratesToUsd
            )
        }
    }

    // ------------------------------------------------------------------------
    // SECTION 6: COMMERCIAL TERMS & LOGISTICS
    // ------------------------------------------------------------------------
    var invoiceNumber by remember {
        mutableStateOf("INV-2026-${(1000..9999).random()}")
    }
    var issueDate by remember { mutableStateOf("Oct 10, 2026") }
    var selectedPaymentTerms by remember { mutableStateOf("Net 30") }
    var dueDate by remember { mutableStateOf("Nov 10, 2026") }
    var selectedIncoterm by remember { mutableStateOf("DAP") }
    var settlementRail by remember { mutableStateOf("SWIFT GPI") }

    var formSubmittedSuccess by remember { mutableStateOf(false) }

    // PDF Generation & Download state
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfResult by remember { mutableStateOf<GeneratedPdfResult?>(null) }
    var downloadedPublicFile by remember { mutableStateOf<File?>(null) }
    var pdfStatusMessage by remember { mutableStateOf<String?>(null) }

    fun buildCurrentInvoiceDocument(): CrossBorderInvoiceDocument {
        val mappedIncoterm = when (selectedIncoterm) {
            "DDP" -> CrossBorderIncoterm.DDP
            "CIF" -> CrossBorderIncoterm.CIF
            "FOB" -> CrossBorderIncoterm.FOB
            "EXW" -> CrossBorderIncoterm.EXW
            "CIP" -> CrossBorderIncoterm.CIP
            else -> CrossBorderIncoterm.DAP
        }

        val effectiveLineItems = if (lineItems.isEmpty()) {
            listOf(
                CrossBorderInvoiceItem(
                    description = "Cross-Border Commercial Trade Settlement",
                    quantity = 1.0,
                    unitPrice = taxCalculation.grossTotal,
                    taxRatePercent = taxCalculation.vatRatePercent
                )
            )
        } else {
            lineItems.toList()
        }

        return CrossBorderInvoiceDocument(
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            paymentTerms = selectedPaymentTerms,
            status = CrossBorderInvoiceStatus.ISSUED,
            incoterm = mappedIncoterm,
            exporter = CorporateExporterProfile(
                entityName = vendorLegalName,
                jurisdiction = vendorJurisdiction,
                physicalAddress = vendorAddress,
                countryCode = activeEntity?.countryCode ?: "CH",
                vatTaxId = vendorTaxId,
                eoriCustomsNumber = "EORI-${activeEntity?.countryCode ?: "CH"}-91823",
                registrationNumber = "REG-${activeEntity?.id ?: 101}",
                legalRepresentative = activeEntity?.localDirector ?: "Corporate Managing Director",
                bankInstitution = vendorBankName,
                swiftBic = vendorSwiftBic,
                ibanOrAccount = vendorIban,
                primaryClearingRail = settlementRail
            ),
            client = CrossBorderClientProfile(
                clientName = clientName,
                destinationCountry = clientCountry,
                destinationCountryCode = detectedTaxRule.countryCode,
                billingAddress = clientAddress,
                shippingAddress = clientAddress,
                clientVatGstId = clientVatId,
                contactEmail = clientContactEmail,
                contactPhone = "+41 44 215 9000"
            ),
            currency = selectedCurrencyCode,
            currencySymbol = currentCurrencyOption.symbol,
            exchangeRateToUsd = currentCurrencyOption.rateToUsd,
            lineItems = effectiveLineItems,
            subtotal = taxCalculation.subtotal,
            taxName = if (taxCalculation.isReverseCharge) "0% Reverse Charge" else "Statutory VAT/GST",
            taxRatePercent = taxCalculation.vatRatePercent,
            taxAmount = taxCalculation.vatAmount,
            isReverseCharge = taxCalculation.isReverseCharge,
            withholdingTaxPercent = taxCalculation.whtRatePercent,
            withholdingTaxAmount = taxCalculation.whtAmount,
            grossTotal = taxCalculation.grossTotal,
            netReceivable = taxCalculation.netReceivable,
            equivalentUsdAmount = taxCalculation.equivalentUsd,
            statutoryComplianceNote = taxCalculation.complianceNote,
            customsDeclarationCode = "HS-EXP-998313-${detectedTaxRule.countryCode}"
        )
    }

    val isFormValid = subtotal > 0.0 &&
        clientName.isNotBlank() &&
        vendorLegalName.isNotBlank() &&
        clientCountry.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("cross_border_invoice_create_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation & Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .testTag("btn_back_from_invoice_form")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Invoice Form",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "CREATE CROSS-BORDER INVOICE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Real-Time Currency Selection • Statutory Tax Engine • Room SQLite Persistence",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Quick Preset / Reset Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        // Prefill preset for fast workflow
                        val presetEntity = entities.firstOrNull()
                        if (presetEntity != null) {
                            vendorLegalName = presetEntity.name
                            vendorJurisdiction = presetEntity.jurisdiction
                            vendorTaxId = presetEntity.taxId
                        }
                        val clientPreset = sampleClients.random()
                        clientName = clientPreset.name
                        clientCountry = clientPreset.country
                        clientVatId = clientPreset.vatTaxId
                        clientAddress = clientPreset.address
                        selectedCurrencyCode = clientPreset.preferredCurrency
                        isReverseChargeEnabled = true
                        isCustomTaxOverride = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_prefill_preset_invoice")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Prefill",
                        tint = GoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PREFILL PRESET",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Offline Mode Banner if active
        if (isOfflineMode) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RoseNegative.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseNegative.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_form_banner")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Offline Active",
                        tint = RoseNegative,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "OFFLINE DATABASE MODE: Invoice will be securely committed to Room SQLite (v5) local ledger and queued for background ledger reconciliation.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Success Confirmation Message with PDF Actions
        AnimatedVisibility(visible = formSubmittedSuccess) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldPositive.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPositive.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_created_success_banner")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldPositive,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INVOICE $invoiceNumber ISSUED SUCCESSFULLY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Total $selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.grossTotal)} • Stored in Room SQLite database.",
                                fontSize = 9.sp,
                                color = TextPrimary
                            )
                        }
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("VIEW IN LEDGER", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Prompt to generate / download PDF right away
                    if (generatedPdfResult == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "PDF Document",
                                        tint = RoseNegative,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Official PDF export ready to generate & download",
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        isGeneratingPdf = true
                                        try {
                                            val doc = buildCurrentInvoiceDocument()
                                            val result = CrossBorderInvoicePdfEngine.generateAndSaveInvoicePdf(context, doc)
                                            generatedPdfResult = result
                                            // Automatically export a copy to public downloads folder
                                            downloadedPublicFile = CrossBorderInvoicePdfEngine.exportToDownloadsFolder(context, result.file)
                                            pdfStatusMessage = "PDF generated & downloaded to device (${result.formattedFileSize})"
                                        } catch (e: Exception) {
                                            pdfStatusMessage = "PDF generation failed: ${e.message}"
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseNegative, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    enabled = !isGeneratingPdf,
                                    modifier = Modifier.testTag("btn_banner_generate_pdf")
                                ) {
                                    if (isGeneratingPdf) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = TextPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("GENERATING...", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    } else {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download PDF", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DOWNLOAD PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PDF Generation Status or Result Alert Banner
        AnimatedVisibility(visible = generatedPdfResult != null || pdfStatusMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_generation_result_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Result",
                                tint = RoseNegative,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "OFFICIAL INVOICE PDF GENERATED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = generatedPdfResult?.let {
                                        "${it.file.name} • ${it.formattedFileSize} • SHA-256: ${it.sha256Checksum.take(12)}..."
                                    } ?: (pdfStatusMessage ?: "Document prepared"),
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                pdfStatusMessage = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (downloadedPublicFile != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPositive.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Saved",
                                    tint = EmeraldPositive,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Saved to Downloads folder (${downloadedPublicFile?.name}). Ready for offline filing & dispatch.",
                                    fontSize = 8.sp,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Action buttons to View, Share, or re-download
                    generatedPdfResult?.let { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(CrossBorderInvoicePdfEngine.createViewPdfIntent(context, res.file))
                                    } catch (e: Exception) {
                                        pdfStatusMessage = "No PDF viewer application found on device."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_open_pdf_viewer")
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = "Open", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OPEN PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(CrossBorderInvoicePdfEngine.createSharePdfIntent(context, res.file))
                                    } catch (e: Exception) {
                                        pdfStatusMessage = "Share error: ${e.message}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_share_pdf_file")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SHARE / SEND", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val exported = CrossBorderInvoicePdfEngine.exportToDownloadsFolder(context, res.file)
                                        downloadedPublicFile = exported
                                        pdfStatusMessage = "PDF copied to Downloads folder: ${exported?.name}"
                                    } catch (e: Exception) {
                                        pdfStatusMessage = "Download error: ${e.message}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_download_to_downloads")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DOWNLOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CARD 1: VENDOR / ISSUING ENTITY DETAILS (Explicitly Requested)
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_vendor_details"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Business, contentDescription = "Vendor", tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "1. VENDOR / ISSUING ENTITY DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Select an existing corporate entity from Room SQLite or enter custom sovereign vendor details for cross-border compliance.",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                // Sovereign Entity Selector Dropdown
                if (entities.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = entityDropdownExpanded,
                        onExpandedChange = { entityDropdownExpanded = !entityDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${vendorLegalName} (${vendorJurisdiction})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Corporate Entity (From Room Database)", fontSize = 10.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("dropdown_vendor_entity"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = entityDropdownExpanded,
                            onDismissRequest = { entityDropdownExpanded = false }
                        ) {
                            entities.forEachIndexed { index, entity ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(entity.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("${entity.jurisdiction} • Tax ID: ${entity.taxId}", fontSize = 9.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedEntityIndex = index
                                        vendorLegalName = entity.name
                                        vendorJurisdiction = entity.jurisdiction
                                        vendorTaxId = entity.taxId
                                        entityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Vendor Fields Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vendorLegalName,
                        onValueChange = { vendorLegalName = it },
                        label = { Text("Vendor Legal Business Name", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_vendor_legal_name"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vendorTaxId,
                        onValueChange = { vendorTaxId = it },
                        label = { Text("Vendor VAT / Tax ID / EORI", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_vendor_tax_id"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vendorJurisdiction,
                        onValueChange = { vendorJurisdiction = it },
                        label = { Text("Vendor Jurisdiction / Country", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_vendor_jurisdiction"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vendorContactEmail,
                        onValueChange = { vendorContactEmail = it },
                        label = { Text("Vendor Billing Contact Email", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_vendor_email"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = vendorAddress,
                    onValueChange = { vendorAddress = it },
                    label = { Text("Vendor Registered Street Address", fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vendor_address"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Settlement Banking & Rail Details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Bank", tint = EmeraldPositive, modifier = Modifier.size(14.dp))
                            Text(
                                text = "VENDOR REMITTANCE & BANKING SETTLEMENT DETAILS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vendorBankName,
                                onValueChange = { vendorBankName = it },
                                label = { Text("Beneficiary Bank", fontSize = 9.sp) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("input_vendor_bank_name"),
                                shape = RoundedCornerShape(6.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = vendorSwiftBic,
                                onValueChange = { vendorSwiftBic = it },
                                label = { Text("SWIFT / BIC", fontSize = 9.sp) },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("input_vendor_swift_bic"),
                                shape = RoundedCornerShape(6.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = vendorIban,
                            onValueChange = { vendorIban = it },
                            label = { Text("Beneficiary IBAN / Account Number", fontSize = 9.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_vendor_iban"),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // ====================================================================
        // CARD 2: BUYER / CLIENT DETAILS
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_client_details"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = "Client", tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "2. BUYER / CLIENT & JURISDICTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Client Profile Chips
                Text(text = "Quick Presets:", fontSize = 9.sp, color = TextMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleClients.forEach { profile ->
                        val isSelected = clientName == profile.name
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanAccent else BorderSubtle),
                            modifier = Modifier
                                .clickable {
                                    clientName = profile.name
                                    clientCountry = profile.country
                                    clientVatId = profile.vatTaxId
                                    clientAddress = profile.address
                                    selectedCurrencyCode = profile.preferredCurrency
                                }
                                .testTag("preset_client_${profile.name.take(6)}")
                        ) {
                            Text(
                                text = "${profile.countryCode} • ${profile.name}",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyanAccent else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Legal Business Name", fontSize = 10.sp) },
                        placeholder = { Text("e.g. Novartis AG", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_form_client_name"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientCountry,
                        onValueChange = { clientCountry = it },
                        label = { Text("Destination Country", fontSize = 10.sp) },
                        placeholder = { Text("e.g. Germany", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_form_client_country"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientVatId,
                        onValueChange = { clientVatId = it },
                        label = { Text("Client VAT / Tax Registration ID", fontSize = 10.sp) },
                        placeholder = { Text("e.g. DE 123456789", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_form_client_vat"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientContactEmail,
                        onValueChange = { clientContactEmail = it },
                        label = { Text("Client Accounts Payable Email", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_form_client_email"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = clientAddress,
                    onValueChange = { clientAddress = it },
                    label = { Text("Client Registered Billing Address", fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_client_address"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        // ====================================================================
        // CARD 3: REAL-TIME CURRENCY SELECTION (Explicitly Requested)
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_currency_selection"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                        Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = "Currency", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "3. REAL-TIME CURRENCY SELECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldPositive.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(EmeraldPositive, CircleShape))
                            Text(
                                text = "LIVE INTERBANK FX",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Text(
                    text = "Select billing currency. Conversion rates are synchronized with real-time global financial feeds (${liveCurrencyFeed.providerName}).",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                // Currency Selection Grid (Chips with flag, code, and rate to USD)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    supportedCurrencies.forEach { curr ->
                        val isSelected = selectedCurrencyCode == curr.code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanAccent else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else BorderSubtle
                            ),
                            modifier = Modifier
                                .clickable { selectedCurrencyCode = curr.code }
                                .testTag("chip_currency_${curr.code}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${curr.flag} ${curr.code} (${curr.symbol})",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "1 ${curr.code} = $${String.format(Locale.US, "%.4f", curr.rateToUsd)}",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) SurfaceDark.copy(alpha = 0.85f) else EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Current Active Rate Overview Bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Currency: ${currentCurrencyOption.name} (${currentCurrencyOption.code})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mid-market benchmark rate: 1 ${currentCurrencyOption.code} = $${String.format(Locale.US, "%.4f", currentCurrencyOption.rateToUsd)} USD",
                                fontSize = 8.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyanAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${currentCurrencyOption.flag} ${currentCurrencyOption.code}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CARD 4: LINE ITEMS & SERVICE CATALOG
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_line_items"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                        Icon(imageVector = Icons.Default.Description, contentDescription = "Items", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "4. SERVICE LINE ITEMS & HS/SAC CODES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${lineItems.size} ITEMS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Render Existing Line Items
                lineItems.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("line_item_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.description,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SAC/HS: ${item.hsnSacCode}",
                                        fontSize = 8.sp,
                                        color = CyanAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Qty: ${item.quantity} × ${selectedCurrencyCode} ${String.format(Locale.US, "%,.2f", item.unitPrice)}",
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${selectedCurrencyCode} ${String.format(Locale.US, "%,.2f", item.totalAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (lineItems.size > 1) {
                                    IconButton(
                                        onClick = { lineItems.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete item",
                                            tint = RoseNegative,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add New Line Item Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ADD NEW LINE ITEM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = newItemDescription,
                        onValueChange = { newItemDescription = it },
                        label = { Text("Service / Good Description", fontSize = 9.sp) },
                        placeholder = { Text("e.g. Cross-Border Financial Telemetry Integration", fontSize = 9.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_item_desc"),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemSacCode,
                            onValueChange = { newItemSacCode = it },
                            label = { Text("SAC / HS", fontSize = 9.sp) },
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("input_new_item_sac"),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newItemQty,
                            onValueChange = { newItemQty = it },
                            label = { Text("Qty", fontSize = 9.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.6f)
                                .testTag("input_new_item_qty"),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newItemUnitPrice,
                            onValueChange = { newItemUnitPrice = it },
                            label = { Text("Unit Price ($selectedCurrencyCode)", fontSize = 9.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_new_item_price"),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val price = newItemUnitPrice.toDoubleOrNull() ?: 0.0
                                val qty = newItemQty.toDoubleOrNull() ?: 1.0
                                if (newItemDescription.isNotBlank() && price > 0.0) {
                                    lineItems.add(
                                        CrossBorderInvoiceItem(
                                            description = newItemDescription.trim(),
                                            quantity = qty,
                                            unitPrice = price,
                                            hsnSacCode = newItemSacCode.ifBlank { "998313" },
                                            taxRatePercent = 0.0
                                        )
                                    )
                                    newItemDescription = ""
                                    newItemUnitPrice = ""
                                    newItemQty = "1"
                                }
                            },
                            enabled = newItemDescription.isNotBlank() && (newItemUnitPrice.toDoubleOrNull() ?: 0.0) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_add_line_item")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CARD 5: STATUTORY TAX CALCULATIONS (Explicitly Requested)
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_tax_calculations"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = "Tax", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "5. STATUTORY CROSS-BORDER TAX CALCULATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${detectedTaxRule.countryCode}: ${detectedTaxRule.taxName}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Automated statutory regional tax rules according to bilateral treaties, destination country rules, and B2B exemptions.",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                // Reverse Charge Toggle Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Apply B2B Reverse Charge (0% Output VAT)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (detectedTaxRule.reverseChargeAllowed) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = EmeraldPositive.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "ELIGIBLE",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPositive,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Recipient customer accounts for VAT in destination jurisdiction (${detectedTaxRule.complianceReference}).",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isReverseChargeEnabled && !isCustomTaxOverride,
                            onCheckedChange = {
                                isReverseChargeEnabled = it
                                if (it) isCustomTaxOverride = false
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                            modifier = Modifier.testTag("toggle_form_reverse_charge")
                        )
                    }
                }

                // Custom Tax Override Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Manual VAT/Tax Override",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Switch(
                        checked = isCustomTaxOverride,
                        onCheckedChange = {
                            isCustomTaxOverride = it
                            if (it) isReverseChargeEnabled = false
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent),
                        modifier = Modifier.testTag("toggle_custom_tax_override")
                    )
                }

                if (isCustomTaxOverride) {
                    OutlinedTextField(
                        value = customTaxPercentStr,
                        onValueChange = { customTaxPercentStr = it },
                        label = { Text("Custom VAT / Tax Percentage (%)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_tax_percent"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Withholding Tax (WHT) Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cross-Border Withholding Tax (WHT)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Treaty rate deducted at source by recipient jurisdiction",
                            fontSize = 8.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isWithholdingTaxEnabled,
                        onCheckedChange = { isWithholdingTaxEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RoseNegative),
                        modifier = Modifier.testTag("toggle_withholding_tax")
                    )
                }

                if (isWithholdingTaxEnabled) {
                    OutlinedTextField(
                        value = customWhtPercentStr,
                        onValueChange = { customWhtPercentStr = it },
                        label = { Text("Withholding Tax Rate (%) - e.g. 5%, 10%, 15%", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_withholding_tax_percent"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = RoseNegative,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // ============================================================
                // REAL-TIME STATUTORY TAX CALCULATION AUDIT CARD
                // ============================================================
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.5f), EmeraldPositive.copy(alpha = 0.5f)))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_tax_calculation_breakdown")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TAX ENGINE AUDIT BREAKDOWN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal (Gross Services):", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.subtotal)}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Assessed VAT/Tax (${String.format(Locale.US, "%.1f", taxCalculation.vatRatePercent)}%):",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "+$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.vatAmount)}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (taxCalculation.vatAmount == 0.0) EmeraldPositive else GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (taxCalculation.whtAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Less Withholding Tax (${String.format(Locale.US, "%.1f", taxCalculation.whtRatePercent)}% WHT):",
                                    fontSize = 9.sp,
                                    color = RoseNegative,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "-$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.whtAmount)}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseNegative,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Invoice Amount:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.grossTotal)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Collectible Due (Receivable):", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.netReceivable)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Live Converted USD Valuation:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "≈ $${String.format(Locale.US, "%,.2f", taxCalculation.equivalentUsd)} USD",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceElevated,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = "STATUTORY CITATION: ${taxCalculation.complianceNote}",
                                fontSize = 7.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 11.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CARD 6: COMMERCIAL TERMS & LOGISTICS
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_commercial_terms"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "6. COMMERCIAL TERMS & SETTLEMENT LOGISTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice Reference Number", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_form_invoice_number"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_form_due_date"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Payment Terms Quick Chips
                Text(text = "Payment Terms:", fontSize = 9.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Immediate", "Net 15", "Net 30", "Net 60").forEach { term ->
                        val isSelected = selectedPaymentTerms == term
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CyanAccent else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanAccent else BorderSubtle),
                            modifier = Modifier
                                .clickable { selectedPaymentTerms = term }
                                .testTag("chip_term_$term")
                        ) {
                            Text(
                                text = term,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Incoterms Quick Chips
                Text(text = "Cross-Border Incoterms (ICC 2020):", fontSize = 9.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("DAP", "DDP", "CIF", "EXW", "FCA").forEach { inco ->
                        val isSelected = selectedIncoterm == inco
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) GoldAccent else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldAccent else BorderSubtle),
                            modifier = Modifier
                                .clickable { selectedIncoterm = inco }
                                .testTag("chip_inco_$inco")
                        ) {
                            Text(
                                text = inco,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CARD 7: PROFESSIONAL PDF GENERATION & INSTANT DOWNLOAD (Explicitly Requested)
        // ====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_pdf_generation_export"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseNegative.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Engine",
                            tint = RoseNegative,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "7. PROFESSIONAL PDF EXPORT & DOWNLOAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RoseNegative.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseNegative.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "A4 VECTOR COMPLIANT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Generate a formal, publication-quality A4 cross-border commercial invoice PDF with statutory tax citation, ISO 20022 bank routing, SHA-256 cryptographic audit seal, and instant download to device.",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                // Quick live metadata preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DOCUMENT REF:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(invoiceNumber, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EXPORTER / VENDOR:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(vendorLegalName.take(28), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CLIENT / IMPORTER:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text("$clientName ($clientCountry)", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SETTLEMENT TOTAL:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                "$selectedCurrencyCode ${String.format(Locale.US, "%,.2f", taxCalculation.grossTotal)} (USD $${String.format(Locale.US, "%,.2f", taxCalculation.equivalentUsd)})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Dual buttons: "GENERATE & DOWNLOAD PDF" and "SHARE"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isFormValid) {
                                isGeneratingPdf = true
                                try {
                                    val doc = buildCurrentInvoiceDocument()
                                    val result = CrossBorderInvoicePdfEngine.generateAndSaveInvoicePdf(context, doc)
                                    generatedPdfResult = result
                                    downloadedPublicFile = CrossBorderInvoicePdfEngine.exportToDownloadsFolder(context, result.file)
                                    pdfStatusMessage = "Invoice PDF generated & downloaded (${result.formattedFileSize})"
                                } catch (e: Exception) {
                                    pdfStatusMessage = "PDF generation failed: ${e.message}"
                                } finally {
                                    isGeneratingPdf = false
                                }
                            }
                        },
                        enabled = isFormValid && !isGeneratingPdf,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseNegative,
                            contentColor = TextPrimary,
                            disabledContainerColor = SurfaceDark,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("btn_generate_and_download_pdf")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GENERATING PDF...", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GENERATE & DOWNLOAD PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isFormValid) {
                                isGeneratingPdf = true
                                try {
                                    val doc = buildCurrentInvoiceDocument()
                                    val result = CrossBorderInvoicePdfEngine.generateAndSaveInvoicePdf(context, doc)
                                    generatedPdfResult = result
                                    context.startActivity(CrossBorderInvoicePdfEngine.createSharePdfIntent(context, result.file))
                                    pdfStatusMessage = "Opening system share dispatcher..."
                                } catch (e: Exception) {
                                    pdfStatusMessage = "Share failed: ${e.message}"
                                } finally {
                                    isGeneratingPdf = false
                                }
                            }
                        },
                        enabled = isFormValid && !isGeneratingPdf,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_share_generated_pdf")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }
                }

                // If already generated, show direct Open & Download shortcuts
                generatedPdfResult?.let { result ->
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FILE: ${result.file.name} (${result.formattedFileSize})",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SHA-256: ${result.sha256Checksum.take(16)}...",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(CrossBorderInvoicePdfEngine.createViewPdfIntent(context, result.file))
                                    } catch (e: Exception) {
                                        pdfStatusMessage = "No PDF reader installed: ${e.message}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_quick_open_pdf")
                            ) {
                                Text("VIEW PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    val exported = CrossBorderInvoicePdfEngine.exportToDownloadsFolder(context, result.file)
                                    downloadedPublicFile = exported
                                    pdfStatusMessage = "Downloaded to device: ${exported?.name}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_quick_save_downloads")
                            ) {
                                Text("SAVE COPY", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // PRIMARY ACTION BUTTONS
        // ====================================================================
        Button(
            onClick = {
                if (isFormValid) {
                    val primaryDesc = lineItems.firstOrNull()?.description
                        ?: "Cross-Border Commercial Services"
                    onSaveInvoice(
                        vendorLegalName,
                        clientName,
                        clientCountry,
                        taxCalculation.grossTotal,
                        selectedCurrencyCode,
                        taxCalculation.vatRatePercent,
                        "$primaryDesc ($selectedIncoterm / $selectedPaymentTerms)",
                        dueDate
                    )
                    formSubmittedSuccess = true
                }
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = SurfaceDark,
                disabledContainerColor = SurfaceElevated,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_submit_create_invoice")
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Submit",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ISSUE & SAVE CROSS-BORDER INVOICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_cancel_invoice_form")
        ) {
            Text(
                text = "RETURN TO COMMERCIAL LEDGER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
