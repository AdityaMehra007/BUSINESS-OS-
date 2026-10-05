package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FormattedCrossBorderInvoice
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InternationalClientProfile
import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.InvoiceRecord
import java.util.Locale

object FormattedInvoiceEngine {

    val sampleClients = listOf(
        InternationalClientProfile(
            name = "Siemens AG",
            country = "Germany",
            countryCode = "DE",
            address = "Werner-von-Siemens-Straße 1, 80333 Munich",
            vatTaxId = "DE 129274202",
            preferredCurrency = "EUR"
        ),
        InternationalClientProfile(
            name = "Barclays PLC",
            country = "United Kingdom",
            countryCode = "GB",
            address = "1 Churchill Place, Canary Wharf, London E14 5HP",
            vatTaxId = "GB 243 8522 62",
            preferredCurrency = "GBP"
        ),
        InternationalClientProfile(
            name = "Aether Pharma Group AG",
            country = "Switzerland",
            countryCode = "CH",
            address = "Grenzacherstrasse 124, 4058 Basel",
            vatTaxId = "CHE-116.281.934 MWST",
            preferredCurrency = "CHF"
        ),
        InternationalClientProfile(
            name = "DBS Bank Ltd",
            country = "Singapore",
            countryCode = "SG",
            address = "12 Marina Boulevard, Marina Bay Financial Centre, Singapore 018982",
            vatTaxId = "M2-0004523-X GST",
            preferredCurrency = "SGD"
        ),
        InternationalClientProfile(
            name = "Tokyo Robotics Consortium",
            country = "Japan",
            countryCode = "JP",
            address = "6-10-1 Roppongi, Minato-ku, Tokyo 106-6108",
            vatTaxId = "JP 9010001000000 JCT",
            preferredCurrency = "JPY"
        ),
        InternationalClientProfile(
            name = "Nordic CleanTech Solutions Oy",
            country = "Finland",
            countryCode = "FI",
            address = "Keilaranta 1, 02150 Espoo",
            vatTaxId = "FI 24901928 ALV",
            preferredCurrency = "EUR"
        ),
        InternationalClientProfile(
            name = "Petrobras SA",
            country = "Brazil",
            countryCode = "BR",
            address = "Av. República do Chile 65, Centro, Rio de Janeiro",
            vatTaxId = "CNPJ 33.000.167/0001-01",
            preferredCurrency = "BRL"
        ),
        InternationalClientProfile(
            name = "Stratos Defense Aerospace Inc.",
            country = "United States",
            countryCode = "US",
            address = "100 Aerospace Parkway, Arlington, VA 22202",
            vatTaxId = "US-EIN 54-1928401",
            preferredCurrency = "USD"
        )
    )

    fun buildFormattedInvoice(
        invoice: InvoiceRecord,
        entities: List<EntityRecord>,
        fxBalances: List<FxBalanceRecord>,
        customLineItems: List<CrossBorderInvoiceItem>? = null
    ): FormattedCrossBorderInvoice {
        // Resolve issuing entity from Room entities table
        val issuingEntity = entities.find { it.name.equals(invoice.issuingEntityName, ignoreCase = true) }
            ?: entities.find { it.countryCode.equals("US", ignoreCase = true) }
            ?: entities.firstOrNull()
            ?: EntityRecord(
                name = invoice.issuingEntityName,
                jurisdiction = "United States (Delaware)",
                countryCode = "US",
                entityType = "Delaware C-Corp",
                taxId = "US-EIN 84-2910481",
                status = "GOOD_STANDING",
                baseCurrency = "USD",
                operatingCapital = 5000000.0,
                annualFilingDeadline = "Nov 15, 2026",
                localDirector = "Corporate Treasury Officer",
                complianceScore = 96
            )

        // Resolve client profile
        val matchedClient = sampleClients.find { it.name.equals(invoice.clientName, ignoreCase = true) }
            ?: sampleClients.find { it.country.equals(invoice.clientCountry, ignoreCase = true) }

        val clientAddress = matchedClient?.address
            ?: "Commercial Financial District, ${invoice.clientCountry}"
        val clientVat = matchedClient?.vatTaxId
            ?: "VAT-ID-${invoice.clientCountry.take(2).uppercase(Locale.US)}-${(100000..999999).random()}"

        // Build live rate map
        val liveRates = fxBalances.associate { it.currencyCode.uppercase(Locale.US) to it.rateToUsd }

        // Line items
        val lineItems = if (!customLineItems.isNullOrEmpty()) {
            customLineItems
        } else {
            // Generate clean line items based on invoice serviceDescription and amount
            val mainDesc = invoice.serviceDescription.takeIf { it.isNotBlank() }
                ?: "Cross-Border Enterprise Architecture & SaaS Services"
            listOf(
                CrossBorderInvoiceItem(
                    description = mainDesc,
                    quantity = 1.0,
                    unitPrice = invoice.amount,
                    hsnSacCode = "998313",
                    taxRatePercent = invoice.taxRatePercent
                )
            )
        }

        val subtotal = lineItems.sumOf { it.totalAmount }

        // Automatic Regional Tax Calculation
        val isReverseCharge = invoice.taxRatePercent == 0.0 && invoice.clientCountry != "United States"
        val taxCalc = CrossBorderTaxCalculator.calculate(
            subtotal = subtotal,
            currencyCode = invoice.currency,
            clientCountry = invoice.clientCountry,
            isReverseChargeApplied = isReverseCharge,
            customVatRate = if (invoice.taxRatePercent > 0.0) invoice.taxRatePercent else null,
            liveRatesToUsd = liveRates
        )

        // Banking Rails resolution from Room fxBalances or Entity country
        val (bank, swift, iban, rail) = when (invoice.currency.uppercase(Locale.US)) {
            "CHF" -> Quadruple(
                "UBS Switzerland AG (Zurich)",
                "UBSWCHZH80A",
                "CH93 0070 0110 0002 4589 1",
                "Swiss Interbank Clearing (SIC) High-Value Rail"
            )
            "EUR" -> Quadruple(
                "Deutsche Bank AG (Frankfurt)",
                "DEUTDEDBFXX",
                "DE89 3707 0024 0215 8920 00",
                "SEPA Instant / Target2 RTGS ISO 20022"
            )
            "GBP" -> Quadruple(
                "Barclays Bank PLC (London)",
                "BARCGB22XXX",
                "GB29 BARC 2000 0012 3456 78",
                "CHAPS / UK Faster Payments Service"
            )
            "SGD" -> Quadruple(
                "DBS Bank Ltd (Singapore)",
                "DBSSSGSGXXX",
                "SG12 0010 0012 9012 3456",
                "MEPS+ / FAST Real-Time Corporate Rail"
            )
            "JPY" -> Quadruple(
                "Mitsubishi UFJ Financial Group (Tokyo)",
                "BOTKJPJTXXX",
                "JP44 0005 0001 2345 6789",
                "Bank of Japan Financial Network (BOJ-NET)"
            )
            "BRL" -> Quadruple(
                "Banco Itaú Unibanco (São Paulo)",
                "ITAUBRSPXXX",
                "BR15 0341 0000 1234 5678 9012",
                "STR / PIX International Commercial Gateway"
            )
            else -> Quadruple(
                "JPMorgan Chase Bank N.A. (New York)",
                "CHASUS33XXX",
                "US02 CHAS 0210 0002 1234 5678",
                "Fedwire Commercial / CHIPS ISO 20022 Rail"
            )
        }

        val symbol = when (invoice.currency.uppercase(Locale.US)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "SGD" -> "S$"
            "CHF" -> "CHF "
            "JPY" -> "¥"
            "BRL" -> "R$"
            "AUD" -> "A$"
            "CAD" -> "C$"
            else -> "$"
        }

        return FormattedCrossBorderInvoice(
            invoiceNumber = invoice.invoiceNumber,
            issueDate = invoice.issueDate,
            dueDate = invoice.dueDate,
            paymentTerms = "Net 30 Days • ISO 20022 Cross-Border Settlement",
            status = invoice.status,
            issuingEntity = issuingEntity,
            clientName = invoice.clientName,
            clientCountry = invoice.clientCountry,
            clientAddress = clientAddress,
            clientVatId = clientVat,
            currency = invoice.currency,
            currencySymbol = symbol,
            lineItems = lineItems,
            subtotal = subtotal,
            taxCalculation = taxCalc,
            bankInstitution = bank,
            swiftBic = swift,
            ibanOrAccount = iban,
            clearingRail = rail,
            customsCertification = "We hereby certify that this commercial invoice is authentic and genuine, and represents export of high-technology services under bilateral Double Taxation Avoidance Agreements (DTAA)."
        )
    }

    fun generatePlainTextDocument(doc: FormattedCrossBorderInvoice): String {
        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                        INTERNATIONAL COMMERCIAL INVOICE                         ")
        sb.appendLine("                      ISO 20022 CROSS-BORDER SETTLEMENT                         ")
        sb.appendLine("================================================================================")
        sb.appendLine("INVOICE NO: ${doc.invoiceNumber}          STATUS: ${doc.status}")
        sb.appendLine("ISSUE DATE: ${doc.issueDate}              DUE DATE: ${doc.dueDate}")
        sb.appendLine("TERMS:      ${doc.paymentTerms}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("ISSUED BY (EXPORTER / SELLER):")
        sb.appendLine("  ${doc.issuingEntity.name}")
        sb.appendLine("  Jurisdiction: ${doc.issuingEntity.jurisdiction}")
        sb.appendLine("  Tax ID / UID: ${doc.issuingEntity.taxId}")
        sb.appendLine("  Authorized:   ${doc.issuingEntity.localDirector}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("BILLED TO (IMPORTER / CLIENT):")
        sb.appendLine("  ${doc.clientName}")
        sb.appendLine("  Address:      ${doc.clientAddress}")
        sb.appendLine("  Country:      ${doc.clientCountry}")
        sb.appendLine("  VAT/Tax ID:   ${doc.clientVatId}")
        sb.appendLine("================================================================================")
        sb.appendLine(String.format(Locale.US, "%-40s | %-6s | %-12s | %-14s", "SERVICE / DESCRIPTION", "QTY", "UNIT RATE", "TOTAL"))
        sb.appendLine("--------------------------------------------------------------------------------")
        doc.lineItems.forEach { item ->
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%-40s | %-6.1f | %-12s | %-14s",
                    item.description.take(40),
                    item.quantity,
                    "${doc.currencySymbol}${String.format(Locale.US, "%,.2f", item.unitPrice)}",
                    "${doc.currencySymbol}${String.format(Locale.US, "%,.2f", item.totalAmount)}"
                )
            )
        }
        sb.appendLine("================================================================================")
        sb.appendLine(String.format(Locale.US, "%-50s: %s %,.2f %s", "SUBTOTAL (NET OF TAX)", doc.currencySymbol, doc.subtotal, doc.currency))
        sb.appendLine(String.format(Locale.US, "%-50s: %s %,.2f (%s%%)", "REGIONAL TAX ASSESSED (${doc.taxCalculation.complianceNote.take(25)}...)", doc.currencySymbol, doc.taxCalculation.vatAmount, String.format(Locale.US, "%.1f", doc.taxCalculation.vatRatePercent)))
        if (doc.taxCalculation.whtAmount > 0.0) {
            sb.appendLine(String.format(Locale.US, "%-50s: -%s %,.2f (%s%%)", "WITHHOLDING TAX DEDUCTION", doc.currencySymbol, doc.taxCalculation.whtAmount, String.format(Locale.US, "%.1f", doc.taxCalculation.whtRatePercent)))
        }
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine(String.format(Locale.US, "%-50s: %s %,.2f %s", "TOTAL COMMERCIAL INVOICE PAYABLE", doc.currencySymbol, doc.taxCalculation.netReceivable, doc.currency))
        sb.appendLine(String.format(Locale.US, "%-50s: $ %,.2f USD", "EQUIVALENT USD CONSOLIDATED VALUATION", doc.taxCalculation.equivalentUsd))
        sb.appendLine("================================================================================")
        sb.appendLine("BANKING & CROSS-BORDER SETTLEMENT INSTRUCTIONS:")
        sb.appendLine("  Bank:         ${doc.bankInstitution}")
        sb.appendLine("  SWIFT/BIC:    ${doc.swiftBic}")
        sb.appendLine("  IBAN/Account: ${doc.ibanOrAccount}")
        sb.appendLine("  Clearing:     ${doc.clearingRail}")
        sb.appendLine("  Reference:    ${doc.invoiceNumber}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("COMPLIANCE CERTIFICATION:")
        sb.appendLine("  ${doc.customsCertification}")
        sb.appendLine("================================================================================")
        return sb.toString()
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
