package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.HedgingPortfolioSummary
import com.example.worldbusiness.data.model.HedgingStrategyType
import com.example.worldbusiness.data.model.HedgingSuggestion
import com.example.worldbusiness.data.model.HedgingUrgency
import com.example.worldbusiness.data.model.InvoiceRecord
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

object CurrencyHedgingEngine {

    // Historical 90-Day Realized Annualized Volatility by Currency
    val HISTORICAL_VOLATILITY = mapOf(
        "BRL" to 18.5,
        "JPY" to 13.8,
        "GBP" to 11.2,
        "EUR" to 8.9,
        "CHF" to 7.8,
        "SGD" to 5.2,
        "CAD" to 7.4,
        "AUD" to 10.6,
        "USD" to 0.0
    )

    // Forward Points (Annualized Basis Points relative to USD)
    val FORWARD_POINTS_BPS = mapOf(
        "BRL" to 680,   // Brazilian SELIC carry
        "JPY" to -350,  // BoJ zero-rate carry
        "CHF" to -180,  // Swiss low-rate carry
        "EUR" to -120,
        "GBP" to -60,
        "SGD" to -90,
        "USD" to 0
    )

    // Reference base date for simulated enterprise environment: Oct 01, 2026
    private const val REFERENCE_TIME_MILLIS = 1790841600000L // 2026-10-01 00:00:00 GMT

    /**
     * Evaluates open/pending invoices and produces automated rule-based hedging suggestions.
     */
    fun generateHedgingSuggestions(
        invoices: List<InvoiceRecord>,
        balances: List<FxBalanceRecord>,
        fxRatesToUsd: Map<String, Double> = defaultFxRates
    ): List<HedgingSuggestion> {
        val pendingInvoices = invoices.filter { it.status == "PENDING" || it.status == "OVERDUE" }

        val vaultBalanceMap = balances.associate { it.currencyCode.uppercase(Locale.US) to it.balance }

        return pendingInvoices.mapNotNull { invoice ->
            val currency = invoice.currency.uppercase(Locale.US)
            if (currency == "USD") return@mapNotNull null // No FX exposure for base currency invoices

            val rateToUsd = fxRatesToUsd[currency] ?: 1.0
            val exposureUsd = invoice.amount * rateToUsd
            val daysToDue = calculateDaysToDue(invoice.dueDate)
            val volatility = HISTORICAL_VOLATILITY[currency] ?: 10.0
            val vaultBalance = vaultBalanceMap[currency] ?: 0.0
            val hasNaturalVaultCoverage = vaultBalance >= invoice.amount * 0.85

            evaluateRules(
                invoice = invoice,
                currency = currency,
                rateToUsd = rateToUsd,
                exposureUsd = exposureUsd,
                daysToDue = daysToDue,
                volatility = volatility,
                hasNaturalCoverage = hasNaturalVaultCoverage
            )
        }.sortedByDescending { it.urgency.ordinal == 0 } // Critical first
    }

    private fun evaluateRules(
        invoice: InvoiceRecord,
        currency: String,
        rateToUsd: Double,
        exposureUsd: Double,
        daysToDue: Int,
        volatility: Double,
        hasNaturalCoverage: Boolean
    ): HedgingSuggestion {
        val forwardBps = FORWARD_POINTS_BPS[currency] ?: 0
        val forwardAdjustment = (rateToUsd * (forwardBps / 10000.0) * (daysToDue.coerceAtLeast(1) / 365.0))
        val forwardRate = rateToUsd + forwardAdjustment

        // Calculate 95% Confidence Value-at-Risk (VaR)
        // VaR = Exposure * 1.645 * (volatility / 100) * sqrt(days / 365)
        val timeFactor = sqrt(daysToDue.coerceAtLeast(1) / 365.0)
        val var95 = exposureUsd * 1.645 * (volatility / 100.0) * timeFactor
        val protectedDownside = var95 * 0.92 // 92% of VaR absorbed by derivative

        // Rule Evaluation Pipeline
        return when {
            // Rule 0: Natural Balance Sheet Coverage
            hasNaturalCoverage -> {
                HedgingSuggestion(
                    id = "HEDGE-NAT-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 2,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.NATURAL_MATCH,
                    urgency = HedgingUrgency.LOW,
                    ruleTriggeredName = "Rule 0: Balance Sheet Liquidity Match",
                    ruleExplanation = "Existing ${currency} treasury vault balance fully covers this obligation. Zero derivative cost: settle directly from local currency pool.",
                    actionSteps = listOf(
                        "Earmark ${formatNumber(invoice.amount)} $currency in corporate treasury vault",
                        "Suppress automated spot conversion triggers",
                        "Execute direct SEPA / FAST settlement on value date"
                    )
                )
            }

            // Rule 1: High Volatility (> 12%) and Imminent Maturity (< 30 Days)
            volatility >= 12.0 && daysToDue <= 30 -> {
                HedgingSuggestion(
                    id = "HEDGE-FWD-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 12,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.FORWARD_CONTRACT,
                    urgency = HedgingUrgency.CRITICAL,
                    ruleTriggeredName = "Rule 1: High Volatility & Imminent Horizon Forward Lock",
                    ruleExplanation = "Realized volatility (${volatility}%) with only ${daysToDue} days to due date creates immediate threat of margin erosion. Lock 100% of notional via fixed forward contract.",
                    actionSteps = listOf(
                        "Execute OTC Forward Contract at guaranteed rate ${String.format(Locale.US, "%.4f", forwardRate)}",
                        "Eliminate 100% of foreign currency tail risk ($${formatNumber(var95)} at 95% confidence)",
                        "Instruct clearing desk for value date delivery"
                    )
                )
            }

            // Rule 2: Carry Opportunity & High Differential (e.g. BRL SELIC or CHF carry)
            (currency == "BRL" || currency == "CHF") && daysToDue >= 40 -> {
                HedgingSuggestion(
                    id = "HEDGE-SWAP-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 18,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.SYNTHETIC_SWAP,
                    urgency = HedgingUrgency.HIGH,
                    ruleTriggeredName = "Rule 4: Sovereign Yield Parity Synthetic Swap",
                    ruleExplanation = "Interest rate differential (${forwardBps} bps) offers positive forward carry. Synthetic swap locks spot rate while capturing yield spread on collateralized escrow.",
                    actionSteps = listOf(
                        "Initiate synthetic FX swap with matched maturity (${daysToDue}D)",
                        "Harvest forward points yield differential",
                        "Insulate principal against currency depreciation"
                    )
                )
            }

            // Rule 3: Extended Horizon (> 60 Days) and Significant Exposure (> $200k)
            daysToDue > 60 && exposureUsd >= 200000.0 -> {
                val floor = rateToUsd * 0.97
                val cap = rateToUsd * 1.04
                HedgingSuggestion(
                    id = "HEDGE-LAY-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    collarFloorRate = floor,
                    collarCapRate = cap,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 15,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.LAYERED_TRANCHES,
                    urgency = HedgingUrgency.MODERATE,
                    ruleTriggeredName = "Rule 3: Layered Dynamic Tranches for Large Notional",
                    ruleExplanation = "Large notional ($${formatNumber(exposureUsd)}) over ${daysToDue} days requires dynamic tranching: 40% forward lock, 35% zero-cost collar, 25% spot trailing stop.",
                    actionSteps = listOf(
                        "Tranche A: Lock 40% ($${formatNumber(exposureUsd * 0.4)}) on fixed 90D forward",
                        "Tranche B: Bind 35% ($${formatNumber(exposureUsd * 0.35)}) into collar [${String.format(Locale.US, "%.3f", floor)} - ${String.format(Locale.US, "%.3f", cap)}]",
                        "Tranche C: Retain 25% open with automated -2.0% trailing stop"
                    )
                )
            }

            // Rule 4: Moderate Volatility (7% - 12%) and Medium Horizon (15 - 60 Days)
            volatility in 7.0..12.0 -> {
                val floor = rateToUsd * 0.975
                val cap = rateToUsd * 1.035
                HedgingSuggestion(
                    id = "HEDGE-COL-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    collarFloorRate = floor,
                    collarCapRate = cap,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 6,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.ZERO_COST_COLLAR,
                    urgency = if (daysToDue <= 30) HedgingUrgency.HIGH else HedgingUrgency.MODERATE,
                    ruleTriggeredName = "Rule 2: Zero-Cost Collar Range Forward",
                    ruleExplanation = "Moderate volatility (${volatility}%) and ${daysToDue}D horizon makes a Zero-Cost Collar optimal. Defines guaranteed floor (${String.format(Locale.US, "%.4f", floor)}) with zero upfront premium.",
                    actionSteps = listOf(
                        "Simultaneously purchase OTM Put option at floor ${String.format(Locale.US, "%.4f", floor)}",
                        "Sell OTM Call option at cap ${String.format(Locale.US, "%.4f", cap)} to finance put premium",
                        "Participate in upside while strictly bounding maximum currency drawdown"
                    )
                )
            }

            // Default Rule: Low Volatility Spot Optimization (e.g. SGD or stable pairs)
            else -> {
                HedgingSuggestion(
                    id = "HEDGE-SPOT-${invoice.id}",
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    counterparty = invoice.clientName,
                    currency = currency,
                    exposureAmount = invoice.amount,
                    exposureUsd = exposureUsd,
                    dueDateString = invoice.dueDate,
                    daysToDueDate = daysToDue,
                    historicalVolatilityAnnualized = volatility,
                    spotRate = rateToUsd,
                    forwardRate = forwardRate,
                    valueAtRisk95Usd = var95,
                    estimatedExecutionCostBps = 3,
                    protectedDownsideUsd = protectedDownside,
                    strategyType = HedgingStrategyType.NATURAL_MATCH,
                    urgency = HedgingUrgency.LOW,
                    ruleTriggeredName = "Rule 5: Spot Timing Optimization",
                    ruleExplanation = "Low sovereign volatility (${volatility}%) and stable currency band. Derivative hedging unneeded; rely on spot timing.",
                    actionSteps = listOf(
                        "Set automated price-alert at +/- 1.2% from spot",
                        "Execute spot conversion T+2 prior to invoice maturity",
                        "Maintain minimal treasury friction cost"
                    )
                )
            }
        }
    }

    fun computePortfolioSummary(suggestions: List<HedgingSuggestion>): HedgingPortfolioSummary {
        val totalExposure = suggestions.sumOf { it.exposureUsd }
        val totalVaR = suggestions.sumOf { it.valueAtRisk95Usd }
        val criticalCount = suggestions.count { it.urgency == HedgingUrgency.CRITICAL }
        val highCount = suggestions.count { it.urgency == HedgingUrgency.HIGH }
        val avgVol = if (suggestions.isNotEmpty()) suggestions.map { it.historicalVolatilityAnnualized }.average() else 0.0

        val forwardOrCollarExposure = suggestions
            .filter { it.strategyType == HedgingStrategyType.FORWARD_CONTRACT || it.strategyType == HedgingStrategyType.ZERO_COST_COLLAR || it.strategyType == HedgingStrategyType.LAYERED_TRANCHES }
            .sumOf { it.exposureUsd }

        val coverageRatio = if (totalExposure > 0.0) (forwardOrCollarExposure / totalExposure) * 100.0 else 0.0

        return HedgingPortfolioSummary(
            totalUnhedgedExposureUsd = totalExposure,
            totalValueAtRiskUsd = totalVaR,
            criticalUrgencyCount = criticalCount,
            highUrgencyCount = highCount,
            activeSuggestionsCount = suggestions.size,
            averageVolatilityPercent = avgVol,
            recommendedCoverageRatioPercent = coverageRatio
        )
    }

    private fun calculateDaysToDue(dueDateStr: String): Int {
        val formats = listOf(
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", Locale.US)
        )
        for (format in formats) {
            try {
                val date = format.parse(dueDateStr.trim())
                if (date != null) {
                    val diff = date.time - REFERENCE_TIME_MILLIS
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    return days.coerceAtLeast(1)
                }
            } catch (_: Exception) {
            }
        }
        return 28 // fallback realistic horizon
    }

    private fun formatNumber(num: Double): String {
        return java.text.NumberFormat.getNumberInstance(Locale.US).format(num.roundToInt())
    }

    val defaultFxRates = mapOf(
        "USD" to 1.0,
        "EUR" to 1.0925,
        "GBP" to 1.3040,
        "SGD" to 0.7710,
        "CHF" to 1.1730,
        "BRL" to 0.1840,
        "JPY" to 0.0068
    )
}
