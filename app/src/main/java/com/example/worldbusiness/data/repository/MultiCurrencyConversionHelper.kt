package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Standard ISO 4217 Currency Information
 */
data class CurrencyMetadata(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String,
    val decimalDigits: Int,
    val primaryClearingRail: String,
    val standardSettlementDays: Int,
    val defaultBankInstitution: String
)

/**
 * High-precision Real-Time Conversion Result
 */
data class DetailedConversionResult(
    val sourceAmount: Double,
    val sourceCurrency: String,
    val targetCurrency: String,
    val convertedAmount: Double,
    val midMarketRate: Double,
    val inverseRate: Double,
    val bidRate: Double,
    val askRate: Double,
    val spreadBps: Double,
    val spreadCostAmount: Double,
    val roundedFormattedText: String,
    val calculationTimestamp: String,
    val isDirectParity: Boolean
)

/**
 * Vault Valuation Breakdown for Consolidated Treasury
 */
data class VaultValuationItem(
    val currencyCode: String,
    val currencyName: String,
    val flag: String,
    val nativeBalance: Double,
    val rateToBase: Double,
    val convertedInBase: Double,
    val portfolioSharePercent: Double,
    val dailyPnLInBase: Double,
    val bankInstitution: String
)

/**
 * Consolidated Multi-Currency Treasury Valuation
 */
data class MultiCurrencyTreasuryValuation(
    val baseCurrency: String,
    val baseCurrencySymbol: String,
    val totalValuationInBase: Double,
    val vaultCount: Int,
    val vaultBreakdowns: List<VaultValuationItem>,
    val totalDailyPnLInBase: Double,
    val topCurrencyExposure: String,
    val topExposurePercent: Double,
    val timestamp: String
)

/**
 * Treasury Cross-Currency Swap Execution Quote
 */
data class TreasurySwapQuote(
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: Double,
    val toAmountReceived: Double,
    val executionRate: Double,
    val marketMidRate: Double,
    val spreadFeeAmount: Double,
    val slippagePercent: Double,
    val clearingRail: String,
    val estimatedSettlementTime: String,
    val validUntilTimestamp: String
)

/**
 * Treasury Vault Rebalancing Direction
 */
enum class RebalanceAction(val label: String) {
    BUY("ACCUMULATE"),
    SELL("DIVEST / REPATRIATE"),
    HOLD("TARGET MET")
}

/**
 * Order details to rebalance multi-currency vaults to target allocation weights
 */
data class VaultRebalanceOrder(
    val currencyCode: String,
    val currentBalance: Double,
    val currentValueInBase: Double,
    val currentSharePercent: Double,
    val targetSharePercent: Double,
    val targetValueInBase: Double,
    val varianceValueInBase: Double,
    val action: RebalanceAction,
    val recommendedTradeAmount: Double,
    val recommendedTradeInNative: Double
)

/**
 * Invoicing Multi-Currency Quotation with Real-Time Conversion & FX Volatility Buffer
 */
data class InvoiceMultiCurrencyQuote(
    val invoiceNumber: String,
    val originalAmount: Double,
    val originalCurrency: String,
    val originalCurrencySymbol: String,
    val settlementCurrency: String,
    val settlementCurrencySymbol: String,
    val settlementAmount: Double,
    val effectiveFxRate: Double,
    val vatRatePercent: Double,
    val originalVatAmount: Double,
    val settlementVatAmount: Double,
    val originalGrossTotal: Double,
    val settlementGrossTotal: Double,
    val paymentTermsDays: Int,
    val hedgingBufferPercent: Double,
    val hedgingBufferAmount: Double,
    val totalWithHedgingBuffer: Double,
    val clearingRail: String,
    val taxJurisdictionCitation: String,
    val rateTimestamp: String
)

/**
 * Dual Currency Line Item for International Invoices
 */
data class DualCurrencyLineItem(
    val description: String,
    val quantity: Double,
    val originalUnitPrice: Double,
    val originalTotal: Double,
    val settlementUnitPrice: Double,
    val settlementTotal: Double,
    val hsnSacCode: String
)

/**
 * Settlement Realized/Unrealized Foreign Exchange Impact
 */
data class CrossBorderSettlementFxImpact(
    val invoiceNumber: String,
    val originalAmount: Double,
    val invoiceCurrency: String,
    val settlementCurrency: String,
    val issuedExchangeRateToUsd: Double,
    val settledExchangeRateToUsd: Double,
    val fxVariancePercent: Double,
    val realizedGainLossAmountUsd: Double,
    val isGain: Boolean,
    val accountingEntry: String
)

/**
 * Helper class to handle real-time multi-currency conversion calculations,
 * essential for both the treasury and invoicing modules.
 */
object MultiCurrencyConversionHelper {

    private val CURRENCY_METADATA_MAP = mapOf(
        "USD" to CurrencyMetadata("USD", "US Dollar", "$", "🇺🇸", 2, "Fedwire / CHIPS Real-Time Gross Settlement", 0, "JPMorgan Chase & Co. (New York)"),
        "EUR" to CurrencyMetadata("EUR", "Euro", "€", "🇪🇺", 2, "SEPA Instant / Target2 RTGS ISO 20022", 0, "Deutsche Bank AG (Frankfurt)"),
        "GBP" to CurrencyMetadata("GBP", "British Pound", "£", "🇬🇧", 2, "CHAPS / Faster Payments UK", 0, "Barclays PLC (London)"),
        "CHF" to CurrencyMetadata("CHF", "Swiss Franc", "CHF", "🇨🇭", 2, "Swiss Interbank Clearing (SIC) RTGS", 0, "UBS Switzerland AG (Zurich)"),
        "SGD" to CurrencyMetadata("SGD", "Singapore Dollar", "S$", "🇸🇬", 2, "MEPS+ / FAST Real-Time MAS Rail", 0, "DBS Bank Ltd (Singapore)"),
        "JPY" to CurrencyMetadata("JPY", "Japanese Yen", "¥", "🇯🇵", 0, "BOJ-NET Japanese Clearing Engine", 1, "Mitsubishi UFJ Financial Group (Tokyo)"),
        "BRL" to CurrencyMetadata("BRL", "Brazilian Real", "R$", "🇧🇷", 2, "STR / PIX Central Bank of Brazil", 0, "Banco Itaú Unibanco (São Paulo)"),
        "AUD" to CurrencyMetadata("AUD", "Australian Dollar", "A$", "🇦🇺", 2, "RBA New Payments Platform (NPP)", 0, "Commonwealth Bank of Australia (Sydney)"),
        "CAD" to CurrencyMetadata("CAD", "Canadian Dollar", "C$", "🇨🇦", 2, "Lynx / ACSS High-Value Canadian Rail", 0, "Royal Bank of Canada (Toronto)"),
        "CNY" to CurrencyMetadata("CNY", "Chinese Yuan", "¥", "🇨🇳", 2, "CIPS Cross-Border Interbank Payment System", 1, "Industrial and Commercial Bank of China (Beijing)"),
        "INR" to CurrencyMetadata("INR", "Indian Rupee", "₹", "🇮🇳", 2, "RBI RTGS / NEFT Multi-Rail", 0, "State Bank of India (Mumbai)"),
        "AED" to CurrencyMetadata("AED", "UAE Dirham", "AED", "🇦🇪", 2, "UAEFTS Central Bank Real-Time Rail", 0, "First Abu Dhabi Bank (Abu Dhabi)"),
        "HKD" to CurrencyMetadata("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 2, "HKD CHATS Real-Time Payment", 0, "HSBC Holdings (Hong Kong)"),
        "MXN" to CurrencyMetadata("MXN", "Mexican Peso", "Mex$", "🇲🇽", 2, "SPEI Banco de México Immediate Rail", 0, "BBVA México (Mexico City)"),
        "SEK" to CurrencyMetadata("SEK", "Swedish Krona", "kr", "🇸🇪", 2, "RIX Sveriges Riksbank RTGS Rail", 0, "Skandinaviska Enskilda Banken (Stockholm)")
    )

    fun getMetadata(currencyCode: String): CurrencyMetadata {
        val code = currencyCode.uppercase(Locale.US).trim()
        return CURRENCY_METADATA_MAP[code] ?: CurrencyMetadata(
            code = code,
            name = "$code Sovereign Currency",
            symbol = code,
            flagEmoji = "🌐",
            decimalDigits = 2,
            primaryClearingRail = "SWIFT GPI Global Wire Network",
            standardSettlementDays = 1,
            defaultBankInstitution = "Global Sovereign Depository"
        )
    }

    /**
     * Resolves exchange rate to USD with fallback to sovereign defaults.
     */
    fun resolveRateToUsd(currencyCode: String, liveRatesToUsd: Map<String, Double> = emptyMap()): Double {
        val code = currencyCode.uppercase(Locale.US).trim()
        if (code == "USD") return 1.0

        val liveRate = liveRatesToUsd[code]
        if (liveRate != null && liveRate > 0.0) {
            return liveRate
        }

        return LiveCurrencyFeed.defaultRatesToUsd[code] ?: 1.0
    }

    /**
     * Calculates the exact cross rate between any two sovereign currencies:
     * 1 fromCurrency = X toCurrency.
     */
    fun computeCrossRate(
        fromCurrency: String,
        toCurrency: String,
        liveRatesToUsd: Map<String, Double> = emptyMap()
    ): Double {
        val from = fromCurrency.uppercase(Locale.US).trim()
        val to = toCurrency.uppercase(Locale.US).trim()

        if (from == to) return 1.0

        val fromRateToUsd = resolveRateToUsd(from, liveRatesToUsd)
        val toRateToUsd = resolveRateToUsd(to, liveRatesToUsd)

        return if (toRateToUsd > 0.0) fromRateToUsd / toRateToUsd else 1.0
    }

    /**
     * Calculates bid and ask rates given a mid-market rate and spread in basis points (bps).
     * 1 bps = 0.01% = 0.0001
     */
    fun calculateBidAsk(midRate: Double, spreadBps: Double = 5.0): Pair<Double, Double> {
        val spreadFraction = (spreadBps / 10000.0) / 2.0
        val bid = midRate * (1.0 - spreadFraction)
        val ask = midRate * (1.0 + spreadFraction)
        return Pair(bid, ask)
    }

    /**
     * High-precision currency conversion calculation.
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        liveRatesToUsd: Map<String, Double> = emptyMap(),
        spreadBps: Double = 5.0
    ): DetailedConversionResult {
        val from = fromCurrency.uppercase(Locale.US).trim()
        val to = toCurrency.uppercase(Locale.US).trim()
        val midRate = computeCrossRate(from, to, liveRatesToUsd)
        val (bid, ask) = calculateBidAsk(midRate, spreadBps)
        val inverseRate = if (midRate > 0.0) 1.0 / midRate else 0.0

        val rawConverted = amount * midRate
        val targetMeta = getMetadata(to)
        val convertedRounded = roundToDecimal(rawConverted, targetMeta.decimalDigits)

        val spreadCostFraction = spreadBps / 10000.0
        val spreadCost = amount * midRate * spreadCostFraction

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())

        return DetailedConversionResult(
            sourceAmount = amount,
            sourceCurrency = from,
            targetCurrency = to,
            convertedAmount = convertedRounded,
            midMarketRate = midRate,
            inverseRate = inverseRate,
            bidRate = bid,
            askRate = ask,
            spreadBps = spreadBps,
            spreadCostAmount = roundToDecimal(spreadCost, targetMeta.decimalDigits),
            roundedFormattedText = formatAmount(convertedRounded, to),
            calculationTimestamp = timestamp,
            isDirectParity = from == to
        )
    }

    /**
     * Round double to designated currency decimal places.
     */
    fun roundToDecimal(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(max(0, decimals), RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Clean currency amount formatter with symbol and code.
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        val meta = getMetadata(currencyCode)
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = meta.decimalDigits
            maximumFractionDigits = meta.decimalDigits
        }
        return "${meta.symbol}${formatter.format(amount)} ${meta.code}"
    }

    // =========================================================================
    // TREASURY MODULE CONVERSION CALCULATIONS
    // =========================================================================

    /**
     * Aggregates and values all treasury foreign exchange vaults in any chosen sovereign base currency.
     */
    fun calculateTreasuryValuation(
        balances: List<FxBalanceRecord>,
        baseCurrency: String,
        liveRatesToUsd: Map<String, Double> = emptyMap()
    ): MultiCurrencyTreasuryValuation {
        val base = baseCurrency.uppercase(Locale.US).trim()
        val baseMeta = getMetadata(base)

        // Compile vault breakdowns in base currency
        var totalValuation = 0.0
        var totalDailyPnL = 0.0

        val preliminaryList = balances.map { vault ->
            val rateToBase = computeCrossRate(vault.currencyCode, base, liveRatesToUsd)
            val converted = vault.balance * rateToBase
            val pnlInBase = (vault.balance * (vault.dailyChangePercent / 100.0)) * rateToBase
            totalValuation += converted
            totalDailyPnL += pnlInBase
            Triple(vault, rateToBase, Pair(converted, pnlInBase))
        }

        val breakdowns = preliminaryList.map { (vault, rateToBase, convertedAndPnl) ->
            val (convertedInBase, pnlInBase) = convertedAndPnl
            val sharePercent = if (totalValuation > 0.0) (convertedInBase / totalValuation) * 100.0 else 0.0
            val meta = getMetadata(vault.currencyCode)

            VaultValuationItem(
                currencyCode = vault.currencyCode,
                currencyName = vault.currencyName,
                flag = meta.flagEmoji,
                nativeBalance = vault.balance,
                rateToBase = rateToBase,
                convertedInBase = roundToDecimal(convertedInBase, baseMeta.decimalDigits),
                portfolioSharePercent = roundToDecimal(sharePercent, 2),
                dailyPnLInBase = roundToDecimal(pnlInBase, baseMeta.decimalDigits),
                bankInstitution = meta.defaultBankInstitution
            )
        }.sortedByDescending { it.convertedInBase }

        val topVault = breakdowns.firstOrNull()
        val topExposureCode = topVault?.currencyCode ?: base
        val topExposurePct = topVault?.portfolioSharePercent ?: 100.0

        val timestamp = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US).format(Date())

        return MultiCurrencyTreasuryValuation(
            baseCurrency = base,
            baseCurrencySymbol = baseMeta.symbol,
            totalValuationInBase = roundToDecimal(totalValuation, baseMeta.decimalDigits),
            vaultCount = balances.size,
            vaultBreakdowns = breakdowns,
            totalDailyPnLInBase = roundToDecimal(totalDailyPnL, baseMeta.decimalDigits),
            topCurrencyExposure = topExposureCode,
            topExposurePercent = topExposurePct,
            timestamp = timestamp
        )
    }

    /**
     * Computes an institutional or retail cross-currency swap execution quote for treasury rebalancing.
     */
    fun calculateSwapExecutionQuote(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        liveRatesToUsd: Map<String, Double> = emptyMap(),
        isInstitutionalWholesale: Boolean = true
    ): TreasurySwapQuote {
        val from = fromCurrency.uppercase(Locale.US).trim()
        val to = toCurrency.uppercase(Locale.US).trim()
        val midRate = computeCrossRate(from, to, liveRatesToUsd)

        // Institutional spreads: ~3 bps; Retail: ~20 bps
        val spreadBps = if (isInstitutionalWholesale) 3.0 else 20.0
        val slippagePercent = if (amount > 1_000_000.0) 0.02 else 0.005

        val effectiveSpreadRatio = (spreadBps / 10000.0) + (slippagePercent / 100.0)
        val executionRate = midRate * (1.0 - effectiveSpreadRatio)
        val toAmountReceived = amount * executionRate

        val toMeta = getMetadata(to)
        val spreadFee = amount * midRate * effectiveSpreadRatio

        val validUntil = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).format(Date(System.currentTimeMillis() + 60_000))

        return TreasurySwapQuote(
            fromCurrency = from,
            toCurrency = to,
            fromAmount = amount,
            toAmountReceived = roundToDecimal(toAmountReceived, toMeta.decimalDigits),
            executionRate = executionRate,
            marketMidRate = midRate,
            spreadFeeAmount = roundToDecimal(spreadFee, toMeta.decimalDigits),
            slippagePercent = slippagePercent,
            clearingRail = toMeta.primaryClearingRail,
            estimatedSettlementTime = if (toMeta.standardSettlementDays == 0) "Immediate (Real-Time RTGS)" else "T+${toMeta.standardSettlementDays} Standard",
            validUntilTimestamp = validUntil
        )
    }

    /**
     * Calculates the required rebalancing orders to achieve a target portfolio allocation
     * across multi-currency treasury vaults.
     */
    fun calculatePortfolioRebalance(
        balances: List<FxBalanceRecord>,
        targetAllocationsPercent: Map<String, Double>,
        baseCurrency: String = "USD",
        liveRatesToUsd: Map<String, Double> = emptyMap()
    ): List<VaultRebalanceOrder> {
        val base = baseCurrency.uppercase(Locale.US).trim()
        val valuation = calculateTreasuryValuation(balances, base, liveRatesToUsd)
        val totalValuation = valuation.totalValuationInBase

        if (totalValuation <= 0.0) return emptyList()

        return balances.map { vault ->
            val curr = vault.currencyCode.uppercase(Locale.US)
            val currentValInBase = vault.balance * computeCrossRate(curr, base, liveRatesToUsd)
            val currentSharePct = (currentValInBase / totalValuation) * 100.0
            val targetSharePct = targetAllocationsPercent[curr] ?: currentSharePct
            val targetValInBase = totalValuation * (targetSharePct / 100.0)
            val varianceInBase = targetValInBase - currentValInBase

            val action = when {
                varianceInBase > 50.0 -> RebalanceAction.BUY
                varianceInBase < -50.0 -> RebalanceAction.SELL
                else -> RebalanceAction.HOLD
            }

            val rateToBase = computeCrossRate(curr, base, liveRatesToUsd)
            val recommendedTradeInNative = if (rateToBase > 0.0) abs(varianceInBase) / rateToBase else 0.0

            VaultRebalanceOrder(
                currencyCode = curr,
                currentBalance = vault.balance,
                currentValueInBase = roundToDecimal(currentValInBase, 2),
                currentSharePercent = roundToDecimal(currentSharePct, 2),
                targetSharePercent = roundToDecimal(targetSharePct, 2),
                targetValueInBase = roundToDecimal(targetValInBase, 2),
                varianceValueInBase = roundToDecimal(varianceInBase, 2),
                action = action,
                recommendedTradeAmount = roundToDecimal(abs(varianceInBase), 2),
                recommendedTradeInNative = roundToDecimal(recommendedTradeInNative, getMetadata(curr).decimalDigits)
            )
        }
    }

    // =========================================================================
    // INVOICING MODULE CONVERSION CALCULATIONS
    // =========================================================================

    /**
     * Real-time Multi-Currency Pricing for International Invoices:
     * Converts an invoice amount from the issuing entity's base currency
     * to the international client's settlement currency, calculating:
     * - Mid-market spot conversion rate
     * - Statutory Regional VAT/GST/MWST in both source and settlement currencies
     * - Forward Volatility Buffer to protect cross-border margins over payment terms (e.g. Net 30/60)
     */
    fun calculateInvoiceMultiCurrencyPricing(
        invoiceNumber: String,
        subtotal: Double,
        originalCurrency: String,
        settlementCurrency: String,
        vatRatePercent: Double,
        paymentTermsDays: Int = 30,
        liveRatesToUsd: Map<String, Double> = emptyMap(),
        includeVolatilityBuffer: Boolean = true
    ): InvoiceMultiCurrencyQuote {
        val origCurr = originalCurrency.uppercase(Locale.US).trim()
        val settCurr = settlementCurrency.uppercase(Locale.US).trim()

        val origMeta = getMetadata(origCurr)
        val settMeta = getMetadata(settCurr)

        val fxRate = computeCrossRate(origCurr, settCurr, liveRatesToUsd)

        val origVat = subtotal * (vatRatePercent / 100.0)
        val origGross = subtotal + origVat

        val settSubtotal = subtotal * fxRate
        val settVat = settSubtotal * (vatRatePercent / 100.0)
        val settGross = settSubtotal + settVat

        // Volatility buffer calculation: ~1.5% for Net 30, 2.5% for Net 60, 3.5% for Net 90
        val bufferPercent = if (includeVolatilityBuffer && origCurr != settCurr) {
            when {
                paymentTermsDays >= 90 -> 3.5
                paymentTermsDays >= 60 -> 2.5
                paymentTermsDays >= 30 -> 1.5
                else -> 0.75
            }
        } else {
            0.0
        }

        val bufferAmount = settGross * (bufferPercent / 100.0)
        val totalWithBuffer = settGross + bufferAmount

        val citation = when (settCurr) {
            "EUR" -> "EU Directive 2006/112/EC Art. 196 (SEPA Target2 Rail)"
            "GBP" -> "HMRC Section 55A Reverse Charge / UK VAT Act 1994"
            "CHF" -> "Swiss Federal VAT Act Art. 10(2) (SIC Clearing Rail)"
            "SGD" -> "IRAS GST Act Sec. 21(3) Zero-Rated Cross-Border Export"
            "JPY" -> "Japan Consumption Tax (JCT) Foreign Business Rule"
            "BRL" -> "RFB Normative Instruction 1.455 Cross-Border WHT"
            else -> "Standard International B2B Foreign Commercial Invoice"
        }

        val timestamp = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss 'UTC'", Locale.US).format(Date())

        return InvoiceMultiCurrencyQuote(
            invoiceNumber = invoiceNumber,
            originalAmount = roundToDecimal(subtotal, origMeta.decimalDigits),
            originalCurrency = origCurr,
            originalCurrencySymbol = origMeta.symbol,
            settlementCurrency = settCurr,
            settlementCurrencySymbol = settMeta.symbol,
            settlementAmount = roundToDecimal(settSubtotal, settMeta.decimalDigits),
            effectiveFxRate = fxRate,
            vatRatePercent = vatRatePercent,
            originalVatAmount = roundToDecimal(origVat, origMeta.decimalDigits),
            settlementVatAmount = roundToDecimal(settVat, settMeta.decimalDigits),
            originalGrossTotal = roundToDecimal(origGross, origMeta.decimalDigits),
            settlementGrossTotal = roundToDecimal(settGross, settMeta.decimalDigits),
            paymentTermsDays = paymentTermsDays,
            hedgingBufferPercent = bufferPercent,
            hedgingBufferAmount = roundToDecimal(bufferAmount, settMeta.decimalDigits),
            totalWithHedgingBuffer = roundToDecimal(totalWithBuffer, settMeta.decimalDigits),
            clearingRail = settMeta.primaryClearingRail,
            taxJurisdictionCitation = citation,
            rateTimestamp = timestamp
        )
    }

    /**
     * Converts a list of line items into dual-currency representations
     * for bilingual/international invoices.
     */
    fun calculateDualCurrencyLineItems(
        items: List<CrossBorderInvoiceItem>,
        originalCurrency: String,
        settlementCurrency: String,
        liveRatesToUsd: Map<String, Double> = emptyMap()
    ): List<DualCurrencyLineItem> {
        val origCurr = originalCurrency.uppercase(Locale.US).trim()
        val settCurr = settlementCurrency.uppercase(Locale.US).trim()
        val origMeta = getMetadata(origCurr)
        val settMeta = getMetadata(settCurr)
        val fxRate = computeCrossRate(origCurr, settCurr, liveRatesToUsd)

        return items.map { item ->
            val origUnit = item.unitPrice
            val origTotal = item.quantity * item.unitPrice
            val settUnit = origUnit * fxRate
            val settTotal = origTotal * fxRate

            DualCurrencyLineItem(
                description = item.description,
                quantity = item.quantity,
                originalUnitPrice = roundToDecimal(origUnit, origMeta.decimalDigits),
                originalTotal = roundToDecimal(origTotal, origMeta.decimalDigits),
                settlementUnitPrice = roundToDecimal(settUnit, settMeta.decimalDigits),
                settlementTotal = roundToDecimal(settTotal, settMeta.decimalDigits),
                hsnSacCode = item.hsnSacCode
            )
        }
    }

    /**
     * Calculates the realized foreign exchange gain or loss when an invoice is settled:
     * Evaluates the difference between the exchange rate at invoice issuance
     * and the exchange rate at banking settlement.
     */
    fun calculateSettlementFxGainLoss(
        invoiceNumber: String,
        invoiceAmount: Double,
        invoiceCurrency: String,
        settlementCurrency: String = "USD",
        issuedRateToUsd: Double,
        settledRateToUsd: Double
    ): CrossBorderSettlementFxImpact {
        val curr = invoiceCurrency.uppercase(Locale.US).trim()
        val variancePercent = if (issuedRateToUsd > 0.0) {
            ((settledRateToUsd - issuedRateToUsd) / issuedRateToUsd) * 100.0
        } else {
            0.0
        }

        // Amount in USD at issue vs amount in USD at settlement
        val issuedUsd = invoiceAmount * issuedRateToUsd
        val settledUsd = invoiceAmount * settledRateToUsd
        val gainLossUsd = settledUsd - issuedUsd
        val isGain = gainLossUsd >= 0.0

        val formattedDiff = formatAmount(abs(gainLossUsd), "USD")
        val accountingNote = if (isGain) {
            "Realized FX Gain of $formattedDiff (+${String.format(Locale.US, "%.2f", variancePercent)}%) credited to Treasury FX Operating Reserve (ASC 830)."
        } else {
            "Realized FX Loss of $formattedDiff (${String.format(Locale.US, "%.2f", variancePercent)}%) debited to Cross-Border Hedging Buffer."
        }

        return CrossBorderSettlementFxImpact(
            invoiceNumber = invoiceNumber,
            originalAmount = invoiceAmount,
            invoiceCurrency = curr,
            settlementCurrency = settlementCurrency,
            issuedExchangeRateToUsd = issuedRateToUsd,
            settledExchangeRateToUsd = settledRateToUsd,
            fxVariancePercent = roundToDecimal(variancePercent, 2),
            realizedGainLossAmountUsd = roundToDecimal(gainLossUsd, 2),
            isGain = isGain,
            accountingEntry = accountingNote
        )
    }

    /**
     * Estimates Value-at-Risk (VaR) for multi-currency treasury holdings
     * at a 95% confidence interval over a 1-day horizon.
     */
    fun calculateTreasuryValueAtRisk(
        balances: List<FxBalanceRecord>,
        baseCurrency: String = "USD",
        liveRatesToUsd: Map<String, Double> = emptyMap(),
        confidenceFactor: Double = 1.645 // 95% one-tailed normal distribution
    ): Double {
        val valuation = calculateTreasuryValuation(balances, baseCurrency, liveRatesToUsd)
        if (valuation.totalValuationInBase <= 0.0) return 0.0

        // Aggregate variance based on portfolio asset weight and sovereign volatility
        var portfolioVariance = 0.0
        for (vault in valuation.vaultBreakdowns) {
            val weight = vault.convertedInBase / valuation.totalValuationInBase
            // Historical annual FX volatility proxy: EUR 6%, GBP 7%, JPY 9%, BRL 14%, CHF 5%
            val dailyVolatility = when (vault.currencyCode) {
                "CHF" -> 0.003
                "EUR" -> 0.004
                "GBP" -> 0.0045
                "SGD" -> 0.0035
                "JPY" -> 0.006
                "BRL" -> 0.009
                else -> 0.004
            }
            portfolioVariance += (weight * dailyVolatility) * (weight * dailyVolatility)
        }

        val portfolioDailyVol = sqrt(portfolioVariance)
        val varAmount = valuation.totalValuationInBase * portfolioDailyVol * confidenceFactor
        return roundToDecimal(varAmount, 2)
    }
}
