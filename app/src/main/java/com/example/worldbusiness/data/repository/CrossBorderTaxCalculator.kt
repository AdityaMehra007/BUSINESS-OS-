package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CrossBorderTaxCalculation
import com.example.worldbusiness.data.model.CrossBorderTaxRule
import com.example.worldbusiness.data.model.CurrencyOption
import java.util.Locale

object CrossBorderTaxCalculator {

    val supportedCurrencies = listOf(
        CurrencyOption("USD", "$", "US Dollar", "🇺🇸", 1.0),
        CurrencyOption("EUR", "€", "Euro", "🇪🇺", 1.0925),
        CurrencyOption("GBP", "£", "British Pound", "🇬🇧", 1.3040),
        CurrencyOption("SGD", "S$", "Singapore Dollar", "🇸🇬", 0.7710),
        CurrencyOption("CHF", "CHF", "Swiss Franc", "🇨🇭", 1.1730),
        CurrencyOption("JPY", "¥", "Japanese Yen", "🇯🇵", 0.0068),
        CurrencyOption("BRL", "R$", "Brazilian Real", "🇧🇷", 0.1840),
        CurrencyOption("AUD", "A$", "Australian Dollar", "🇦🇺", 0.6580),
        CurrencyOption("CAD", "C$", "Canadian Dollar", "🇨🇦", 0.7320)
    )

    private val taxRules = mapOf(
        "US" to CrossBorderTaxRule("US", "United States", "State Sales Tax", 0.0, false, 0.0, "US-IRC Sec. 861 Export Zero-Rate"),
        "GB" to CrossBorderTaxRule("GB", "United Kingdom", "UK VAT", 20.0, true, 0.0, "HMRC Reverse Charge Scheme / Sec. 55A"),
        "DE" to CrossBorderTaxRule("DE", "Germany", "USt (MwSt)", 19.0, true, 0.0, "EU Directive 2006/112/EC Art. 196"),
        "CH" to CrossBorderTaxRule("CH", "Switzerland", "MWST / TVA", 8.1, true, 0.0, "Swiss Federal VAT Act Art. 10(2)"),
        "SG" to CrossBorderTaxRule("SG", "Singapore", "GST", 9.0, true, 0.0, "IRAS Zero-Rated International Services Sec. 21(3)"),
        "BR" to CrossBorderTaxRule("BR", "Brazil", "ISS / PIS-COFINS", 5.0, false, 15.0, "RFB Normative Instruction 1.455 WHT"),
        "JP" to CrossBorderTaxRule("JP", "Japan", "JCT", 10.0, true, 0.0, "NTA Cross-Border Digital Services Rules"),
        "FR" to CrossBorderTaxRule("FR", "France", "TVA", 20.0, true, 0.0, "CGI Art. 283-2 Autoliquidation"),
        "NL" to CrossBorderTaxRule("NL", "Netherlands", "BTW", 21.0, true, 0.0, "Wet op de omzetbelasting 1968"),
        "IE" to CrossBorderTaxRule("IE", "Ireland", "VAT", 23.0, true, 0.0, "Irish VAT Consolidation Act 2010"),
        "AU" to CrossBorderTaxRule("AU", "Australia", "GST", 10.0, true, 0.0, "ATO Cross-Border B2B Exemption"),
        "CA" to CrossBorderTaxRule("CA", "Canada", "GST / HST", 5.0, true, 0.0, "CRA Zero-Rated Export Rules")
    )

    fun getSupportedCurrencies(liveRatesToUsd: Map<String, Double> = emptyMap()): List<CurrencyOption> {
        if (liveRatesToUsd.isEmpty()) return supportedCurrencies
        return supportedCurrencies.map { curr ->
            val liveRate = liveRatesToUsd[curr.code.uppercase(Locale.US)]
            if (liveRate != null && liveRate > 0.0) curr.copy(rateToUsd = liveRate) else curr
        }
    }

    fun getCurrencyOption(code: String, liveRatesToUsd: Map<String, Double> = emptyMap()): CurrencyOption {
        val list = getSupportedCurrencies(liveRatesToUsd)
        return list.find { it.code.equals(code, ignoreCase = true) }
            ?: list.first()
    }

    fun getTaxRuleForCountry(countryNameOrCode: String): CrossBorderTaxRule {
        val trimmed = countryNameOrCode.trim().uppercase(Locale.US)

        // Match by code first
        taxRules[trimmed]?.let { return it }

        // Match by country name keywords
        return when {
            trimmed.contains("UNITED STATES") || trimmed.contains("USA") || trimmed.contains("DELAWARE") -> taxRules["US"]!!
            trimmed.contains("UNITED KINGDOM") || trimmed.contains("UK") || trimmed.contains("BRITAIN") || trimmed.contains("LONDON") -> taxRules["GB"]!!
            trimmed.contains("GERMANY") || trimmed.contains("DEUTSCHLAND") || trimmed.contains("FRANKFURT") -> taxRules["DE"]!!
            trimmed.contains("SWITZERLAND") || trimmed.contains("SWISS") || trimmed.contains("ZURICH") -> taxRules["CH"]!!
            trimmed.contains("SINGAPORE") -> taxRules["SG"]!!
            trimmed.contains("BRAZIL") || trimmed.contains("BRASIL") || trimmed.contains("SAO PAULO") -> taxRules["BR"]!!
            trimmed.contains("JAPAN") || trimmed.contains("TOKYO") -> taxRules["JP"]!!
            trimmed.contains("FRANCE") || trimmed.contains("PARIS") -> taxRules["FR"]!!
            trimmed.contains("NETHERLANDS") || trimmed.contains("HOLLAND") || trimmed.contains("AMSTERDAM") -> taxRules["NL"]!!
            trimmed.contains("IRELAND") || trimmed.contains("DUBLIN") -> taxRules["IE"]!!
            trimmed.contains("AUSTRALIA") || trimmed.contains("SYDNEY") -> taxRules["AU"]!!
            trimmed.contains("CANADA") || trimmed.contains("TORONTO") -> taxRules["CA"]!!
            else -> CrossBorderTaxRule("INTL", countryNameOrCode, "Statutory VAT/GST", 0.0, true, 0.0, "Standard Cross-Border B2B Treaty")
        }
    }

    fun calculate(
        subtotal: Double,
        currencyCode: String,
        clientCountry: String,
        isReverseChargeApplied: Boolean,
        customVatRate: Double? = null,
        customWhtRate: Double? = null,
        liveRatesToUsd: Map<String, Double> = emptyMap()
    ): CrossBorderTaxCalculation {
        val rule = getTaxRuleForCountry(clientCountry)
        val currency = getCurrencyOption(currencyCode, liveRatesToUsd)

        val vatRate = if (isReverseChargeApplied) {
            0.0
        } else {
            customVatRate ?: rule.standardRatePercent
        }

        val whtRate = customWhtRate ?: rule.defaultWhtPercent

        val vatAmount = subtotal * (vatRate / 100.0)
        val whtAmount = subtotal * (whtRate / 100.0)
        val grossTotal = subtotal + vatAmount
        val netReceivable = subtotal + vatAmount - whtAmount
        val equivalentUsd = netReceivable * currency.rateToUsd

        val note = when {
            isReverseChargeApplied -> "0% Reverse Charge: Customer accounts for ${rule.taxName} under ${rule.complianceReference}."
            vatRate == 0.0 -> "0% Zero-Rated: International commercial export under ${rule.complianceReference}."
            whtRate > 0.0 -> "Standard ${rule.taxName} (${vatRate}%) applied. Less ${whtRate}% Withholding Tax ($${String.format(Locale.US, "%.2f", whtAmount)}) deducted under ${rule.complianceReference}."
            else -> "Standard cross-border ${rule.taxName} (${vatRate}%) assessed under ${rule.complianceReference}."
        }

        return CrossBorderTaxCalculation(
            subtotal = subtotal,
            currency = currency.code,
            vatRatePercent = vatRate,
            vatAmount = vatAmount,
            isReverseCharge = isReverseChargeApplied,
            whtRatePercent = whtRate,
            whtAmount = whtAmount,
            grossTotal = grossTotal,
            netReceivable = netReceivable,
            equivalentUsd = equivalentUsd,
            complianceNote = note
        )
    }
}
