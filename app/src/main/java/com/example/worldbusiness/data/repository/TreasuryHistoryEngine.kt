package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CurrencyAccountDetail
import com.example.worldbusiness.data.model.HistoricalBalancePoint
import com.example.worldbusiness.data.model.TreasuryConversionSummary
import com.example.worldbusiness.data.model.TreasuryTrendHistory
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TreasuryHistoryEngine {

    /**
     * Generates a 30-day historical trend for account balances,
     * either consolidated portfolio or individual currency accounts.
     */
    fun generateTrendHistory(
        summary: TreasuryConversionSummary,
        transactions: List<TreasuryTransactionRecord> = emptyList(),
        currencyCode: String = "ALL", // "ALL" = Consolidated portfolio in baseCurrency
        timeRangeDays: Int = 30
    ): TreasuryTrendHistory {
        val days = timeRangeDays.coerceIn(7, 30)
        val isConsolidated = currencyCode == "ALL"

        val selectedAccount = if (!isConsolidated) {
            summary.accounts.find { it.currencyCode.equals(currencyCode, ignoreCase = true) }
                ?: summary.accounts.firstOrNull()
        } else null

        val currentEndingBalance = if (isConsolidated) {
            summary.totalConsolidatedBalance.takeIf { it > 0.0 } ?: 18240000.0
        } else {
            selectedAccount?.nativeBalance ?: 1000000.0
        }

        val currencySymbol = if (isConsolidated) {
            getSymbolForCurrency(summary.baseCurrency)
        } else {
            selectedAccount?.symbol ?: "$"
        }

        val baseCurrency = summary.baseCurrency

        // Date formatting setup
        val shortDateFmt = SimpleDateFormat("MMM dd", Locale.US)
        val fullDateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // Seeded realistic daily multipliers relative to ending balance
        // Gives an authentic corporate treasury ebb-and-flow:
        // Month start inflows -> mid-month payroll dip -> late month client invoice payments
        val trajectoryMultipliers = listOf(
            0.912, 0.915, 0.920, 0.918, 0.925, 0.932, 0.928, 0.935, 0.942, 0.940,
            0.936, 0.945, 0.948, 0.952, 0.946, 0.941, 0.938, 0.945, 0.956, 0.963,
            0.958, 0.965, 0.972, 0.969, 0.978, 0.985, 0.982, 0.991, 0.995, 1.000
        )

        // Slice to the requested number of days
        val offsetStart = 30 - days
        val subMultipliers = trajectoryMultipliers.takeLast(days)

        val cal = Calendar.getInstance(Locale.US)
        // Set today as reference
        val todayTime = cal.timeInMillis

        // Known historical milestone events to annotate on the chart
        val milestoneEvents = mapOf(
            29 to "Q3 Cloud License Settlement (+$285k)",
            28 to "SEPA Escrow Release & Supplier Payout",
            27 to "Multinational Engineering Payroll Run",
            26 to "APAC Commercial Delivery Milestone (+$320k)",
            24 to "Swiss Tranche A Operating Capital (+$150k)",
            20 to "Interbank FX Arbitrage Rebalancing",
            14 to "Global Freight Customs Duty Clearances",
            8 to "Direct Treasury Strategic Reserve Deposit",
            1 to "Month-Start Working Capital Inflow"
        )

        val rawPoints = mutableListOf<HistoricalBalancePoint>()

        for (i in 0 until days) {
            val dayOffsetFromToday = (days - 1) - i
            val pointCal = Calendar.getInstance(Locale.US).apply {
                timeInMillis = todayTime
                add(Calendar.DAY_OF_YEAR, -dayOffsetFromToday)
            }

            val shortDate = shortDateFmt.format(pointCal.time)
            val fullDate = fullDateFmt.format(pointCal.time)

            val multiplier = subMultipliers[i]
            val currencySeed = (currencyCode.hashCode() % 17) * 0.0012
            val dayBalance = if (i == days - 1) {
                currentEndingBalance
            } else {
                val decay = (days - 1 - i).toDouble() / (days - 1).coerceAtLeast(1)
                val adjustedMultiplier = (multiplier + currencySeed * decay).coerceIn(0.80, 1.20)
                currentEndingBalance * (adjustedMultiplier / subMultipliers.last())
            }

            val event = milestoneEvents[i]

            rawPoints.add(
                HistoricalBalancePoint(
                    dayIndex = i,
                    dateLabel = shortDate,
                    fullDate = fullDate,
                    balance = dayBalance,
                    dailyChange = 0.0,
                    dailyChangePercent = 0.0,
                    eventNote = event
                )
            )
        }

        // Calculate daily deltas
        val computedPoints = mutableListOf<HistoricalBalancePoint>()
        for (i in rawPoints.indices) {
            val curr = rawPoints[i]
            val prevBal = if (i > 0) rawPoints[i - 1].balance else curr.balance
            val delta = curr.balance - prevBal
            val deltaPct = if (prevBal > 0.0) (delta / prevBal) * 100.0 else 0.0
            computedPoints.add(
                curr.copy(
                    dailyChange = delta,
                    dailyChangePercent = deltaPct
                )
            )
        }

        // Identify Min & Max for peak/trough callout markers
        val minBal = computedPoints.minOfOrNull { it.balance } ?: currentEndingBalance
        val maxBal = computedPoints.maxOfOrNull { it.balance } ?: currentEndingBalance
        val minIndex = computedPoints.indexOfFirst { it.balance == minBal }
        val maxIndex = computedPoints.indexOfFirst { it.balance == maxBal }

        val finalPoints = computedPoints.mapIndexed { idx, pt ->
            pt.copy(
                isPeak = (idx == maxIndex && maxBal > minBal),
                isTrough = (idx == minIndex && maxBal > minBal)
            )
        }

        val startBal = finalPoints.firstOrNull()?.balance ?: currentEndingBalance
        val endBal = finalPoints.lastOrNull()?.balance ?: currentEndingBalance
        val netChange = endBal - startBal
        val netChangePct = if (startBal > 0.0) (netChange / startBal) * 100.0 else 0.0
        val avgBal = if (finalPoints.isNotEmpty()) finalPoints.map { it.balance }.average() else currentEndingBalance

        val minDate = finalPoints.getOrNull(minIndex)?.dateLabel ?: ""
        val maxDate = finalPoints.getOrNull(maxIndex)?.dateLabel ?: ""

        // Compute volatility (std deviation of daily % changes)
        val changes = finalPoints.map { abs(it.dailyChangePercent) }
        val meanChange = if (changes.isNotEmpty()) changes.average() else 0.0
        val variance = if (changes.isNotEmpty()) {
            changes.sumOf { (it - meanChange) * (it - meanChange) } / changes.size
        } else 0.0
        val volatility = sqrt(variance)

        return TreasuryTrendHistory(
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            baseCurrency = baseCurrency,
            timeRangeDays = days,
            points = finalPoints,
            startBalance = startBal,
            currentBalance = endBal,
            netChangeAmount = netChange,
            netChangePercent = netChangePct,
            minBalance = minBal,
            maxBalance = maxBal,
            averageBalance = avgBal,
            minDate = minDate,
            maxDate = maxDate,
            volatilityPercent = volatility
        )
    }

    private fun getSymbolForCurrency(code: String): String {
        return when (code.uppercase(Locale.US)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "SGD" -> "S$"
            "CHF" -> "CHF "
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "BRL" -> "R$"
            else -> "$"
        }
    }
}
