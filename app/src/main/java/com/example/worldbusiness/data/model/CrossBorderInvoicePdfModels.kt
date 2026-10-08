package com.example.worldbusiness.data.model

import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Standard International Commercial Incoterms 2020 rules
 */
enum class CrossBorderIncoterm(val code: String, val label: String) {
    DAP("DAP", "Delivered at Place (Named Destination)"),
    DDP("DDP", "Delivered Duty Paid (Seller Pays All Duties)"),
    CIF("CIF", "Cost, Insurance and Freight"),
    FOB("FOB", "Free on Board (Port of Shipment)"),
    EXW("EXW", "Ex Works (Factory Pickup)"),
    CIP("CIP", "Carriage and Insurance Paid To")
}

/**
 * Commercial Cross-Border Invoice Status
 */
enum class CrossBorderInvoiceStatus(val label: String) {
    DRAFT("Draft Pro-Forma"),
    ISSUED("Issued / Awaiting Settlement"),
    IN_CLEARING("In Interbank Clearing"),
    PAID("Settled & Reconciled"),
    OVERDUE("Overdue / Aging Past Due")
}

/**
 * Detailed Corporate Exporter / Consignor Profile
 */
data class CorporateExporterProfile(
    val entityName: String,
    val jurisdiction: String,
    val physicalAddress: String,
    val countryCode: String,
    val vatTaxId: String,
    val eoriCustomsNumber: String,
    val registrationNumber: String,
    val legalRepresentative: String,
    val bankInstitution: String,
    val swiftBic: String,
    val ibanOrAccount: String,
    val primaryClearingRail: String
)

/**
 * Detailed International Client / Importer / Consignee Profile
 */
data class CrossBorderClientProfile(
    val clientName: String,
    val destinationCountry: String,
    val destinationCountryCode: String,
    val billingAddress: String,
    val shippingAddress: String,
    val clientVatGstId: String,
    val contactEmail: String,
    val contactPhone: String
)

/**
 * Comprehensive Cross-Border Commercial Invoice Data Model.
 * Represents all statutory fields required for cross-border customs clearance,
 * regional taxation (VAT/GST/WHT), multi-currency settlement, and audit compliance.
 */
data class CrossBorderInvoiceDocument(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val paymentTerms: String = "Net 30 Days",
    val status: CrossBorderInvoiceStatus = CrossBorderInvoiceStatus.ISSUED,
    val incoterm: CrossBorderIncoterm = CrossBorderIncoterm.DAP,
    val exporter: CorporateExporterProfile,
    val client: CrossBorderClientProfile,
    val currency: String,
    val currencySymbol: String,
    val exchangeRateToUsd: Double = 1.0,
    val lineItems: List<CrossBorderInvoiceItem>,
    val subtotal: Double,
    val taxName: String = "VAT",
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val isReverseCharge: Boolean = false,
    val withholdingTaxPercent: Double = 0.0,
    val withholdingTaxAmount: Double = 0.0,
    val grossTotal: Double = subtotal + taxAmount,
    val netReceivable: Double = subtotal + taxAmount - withholdingTaxAmount,
    val equivalentUsdAmount: Double = netReceivable * exchangeRateToUsd,
    val statutoryComplianceNote: String = "Article 196 EU VAT Directive / Zero-Rated Cross-Border B2B Supply",
    val customsDeclarationCode: String = "HS-EXP-998313-GLOBAL",
    val cryptographicHashSha256: String = "",
    val generatedPdfFilePath: String? = null,
    val generatedPdfSizeBytes: Long = 0L,
    val generatedPdfTimestamp: String? = null
) {
    val totalQuantity: Double get() = lineItems.sumOf { it.quantity }

    fun formattedSubtotal(): String = formatCurrency(subtotal, currencySymbol)
    fun formattedTax(): String = formatCurrency(taxAmount, currencySymbol)
    fun formattedNetReceivable(): String = formatCurrency(netReceivable, currencySymbol)
    fun formattedUsdEquivalent(): String = "$${NumberFormat.getNumberInstance(Locale.US).format(equivalentUsdAmount.toLong())} USD"

    companion object {
        fun formatCurrency(amount: Double, symbol: String): String {
            val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return "$symbol${formatter.format(amount)}"
        }

        fun createSampleInvoice(
            invoiceNumber: String = "INV-2026-CH-8921",
            currency: String = "CHF",
            currencySymbol: String = "CHF",
            subtotal: Double = 145000.0,
            vatRate: Double = 8.1
        ): CrossBorderInvoiceDocument {
            val vatAmt = subtotal * (vatRate / 100.0)
            val net = subtotal + vatAmt
            return CrossBorderInvoiceDocument(
                invoiceNumber = invoiceNumber,
                issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 30L * 86400000L)),
                paymentTerms = "Net 30 Days (SIC Real-Time)",
                status = CrossBorderInvoiceStatus.ISSUED,
                incoterm = CrossBorderIncoterm.DAP,
                exporter = CorporateExporterProfile(
                    entityName = "Apex Global Treasury AG",
                    jurisdiction = "Zurich, Switzerland",
                    physicalAddress = "Bahnhofstrasse 45, 8001 Zurich",
                    countryCode = "CH",
                    vatTaxId = "CHE-109.843.210 MWST",
                    eoriCustomsNumber = "CH0018923401",
                    registrationNumber = "CH-020.3.029.110-4",
                    legalRepresentative = "Dr. Beatrix von Haller",
                    bankInstitution = "UBS Switzerland AG (Zurich)",
                    swiftBic = "UBSWCHZH80A",
                    ibanOrAccount = "CH93 0070 0110 0002 4589 1",
                    primaryClearingRail = "Swiss Interbank Clearing (SIC) RTGS Rail"
                ),
                client = CrossBorderClientProfile(
                    clientName = "Stellar Logistics GmbH",
                    destinationCountry = "Germany",
                    destinationCountryCode = "DE",
                    billingAddress = "Taunusanlage 8, 60329 Frankfurt am Main",
                    shippingAddress = "Cargo City South, 60549 Frankfurt Airport",
                    clientVatGstId = "DE 289 140 821",
                    contactEmail = "billing@stellar-logistics.de",
                    contactPhone = "+49 69 9123 4500"
                ),
                currency = currency,
                currencySymbol = currencySymbol,
                exchangeRateToUsd = 1.1730,
                lineItems = listOf(
                    CrossBorderInvoiceItem("Global Cloud Architecture & Real-Time Logistics Grid", 1.0, 95000.0, "998313", vatRate),
                    CrossBorderInvoiceItem("ISO 20022 Cross-Border Payment Rail Implementation", 1.0, 50000.0, "998313", vatRate)
                ),
                subtotal = subtotal,
                taxName = "Swiss MWST / VAT",
                taxRatePercent = vatRate,
                taxAmount = vatAmt,
                isReverseCharge = false,
                withholdingTaxPercent = 0.0,
                withholdingTaxAmount = 0.0,
                grossTotal = net,
                netReceivable = net,
                equivalentUsdAmount = net * 1.1730,
                statutoryComplianceNote = "Art. 10(2) Swiss Federal VAT Act / Bilateral Swiss-EU Free Trade Agreement",
                customsDeclarationCode = "CH-EORI-EXP-998313",
                cryptographicHashSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
        }
    }
}

/**
 * Result of PDF Generation
 */
data class GeneratedPdfResult(
    val file: File,
    val invoiceNumber: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val formattedFileSize: String,
    val pageCount: Int,
    val sha256Checksum: String,
    val timestamp: String
)
