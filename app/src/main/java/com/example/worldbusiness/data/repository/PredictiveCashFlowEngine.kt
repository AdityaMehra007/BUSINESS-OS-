package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CashFlowHorizon
import com.example.worldbusiness.data.model.CashFlowScenario
import com.example.worldbusiness.data.model.DailyCashFlowPoint
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiquidityHealthStatus
import com.example.worldbusiness.data.model.PredictiveCashFlowReport
import com.example.worldbusiness.data.model.ScheduledCashEvent
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PredictiveCashFlowEngine {

    val defaultRatesToUsd = mapOf(
        "USD" to 1.0,
        "EUR" to 1.0925,
        "GBP" to 1.3040,
        "SGD" to 0.7710,
        "CHF" to 1.1730,
        "BRL" to 0.1840,
        "JPY" to 0.0068
    )

    fun toUsd(amount: Double, currency: String, rates: Map<String, Double> = defaultRatesToUsd): Double {
        val rate = rates[currency.uppercase(Locale.US)] ?: 1.0
        return amount * rate
    }

    /**
     * Calculates the forward-looking cash flow forecast and runway requirements.
     */
    fun calculateForecast(
        balances: List<FxBalanceRecord>,
        invoices: List<InvoiceRecord>,
        teamMembers: List<TeamMemberRecord> = emptyList(),
        entities: List<EntityRecord> = emptyList(),
        shipments: List<ShipmentRecord> = emptyList(),
        transactions: List<TreasuryTransactionRecord> = emptyList(),
        horizon: CashFlowHorizon = CashFlowHorizon.DAYS_30,
        scenario: CashFlowScenario = CashFlowScenario.BASE_CASE,
        customRates: Map<String, Double> = defaultRatesToUsd
    ): PredictiveCashFlowReport {
        // 1. Calculate Current Consolidated Liquid Treasury in USD
        val currentLiquidCashUsd = if (balances.isNotEmpty()) {
            balances.sumOf { it.balance * it.rateToUsd }
        } else {
            10842000.0 // Default sovereign pool
        }

        // 2. Identify Pending Commercial Receivables (Global Invoices)
        val pendingInvoices = invoices.filter { it.status.uppercase(Locale.US) != "PAID" }
        val scheduledEvents = mutableListOf<ScheduledCashEvent>()

        val baseCalendar = Calendar.getInstance()

        // Map pending invoices into scheduled cash inflows
        pendingInvoices.forEachIndexed { index, inv ->
            val invoiceAmountUsd = toUsd(inv.amount, inv.currency, customRates)
            val adjustedAmount = when (scenario) {
                CashFlowScenario.CONSERVATIVE_STRESS -> invoiceAmountUsd * 0.75 // 25% default/delay haircut
                else -> invoiceAmountUsd
            }

            // Estimate collection day offset from due date
            val daysOffset = when (scenario) {
                CashFlowScenario.DELAYED_RECEIVABLES -> (index * 7) + 32 // Pushed out past 30 days
                else -> (index * 6) + 4 // Baseline: arrives on or near statutory due date
            }

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)

            scheduledEvents.add(
                ScheduledCashEvent(
                    eventId = "INFLOW-${inv.invoiceNumber}",
                    date = dateStr,
                    daysFromNow = daysOffset,
                    title = "Commercial Settlement: ${inv.clientName}",
                    counterpartyOrSource = "${inv.clientName} (${inv.clientCountry})",
                    amountUsd = adjustedAmount,
                    isInflow = true,
                    category = "CLIENT_INVOICE",
                    confidenceProbability = if (scenario == CashFlowScenario.CONSERVATIVE_STRESS) 0.75 else 0.95
                )
            )
        }

        // If no pending invoices in db, add representative enterprise receivables
        if (pendingInvoices.isEmpty()) {
            val defaultInflows = listOf(
                Triple("INV-2026-0891", "Aether Pharma Group AG (Zurich)", 285000.0 to 6),
                Triple("INV-2026-0892", "Nordic CleanTech Solutions (Helsinki)", 155000.0 to 14),
                Triple("INV-2026-0893", "Tokyo Robotics Consortium (Tokyo)", 320000.0 to 22),
                Triple("INV-2026-0894", "SingaTech Distributed Systems (SG)", 190000.0 to 28)
            )
            defaultInflows.forEach { (invNum, client, amtDays) ->
                val (amt, days) = amtDays
                val effectiveDays = if (scenario == CashFlowScenario.DELAYED_RECEIVABLES) days + 30 else days
                val effectiveAmt = if (scenario == CashFlowScenario.CONSERVATIVE_STRESS) amt * 0.75 else amt
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, effectiveDays)

                scheduledEvents.add(
                    ScheduledCashEvent(
                        eventId = "INFLOW-$invNum",
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time),
                        daysFromNow = effectiveDays,
                        title = "Commercial Settlement: $client",
                        counterpartyOrSource = client,
                        amountUsd = effectiveAmt,
                        isInflow = true,
                        category = "CLIENT_INVOICE",
                        confidenceProbability = 0.92
                    )
                )
            }
        }

        // 3. Project Committed Outflows (Workforce Payroll, Statutory Taxes, Freight Clearance, OpEx)
        // Monthly Global Payroll
        val monthlyPayrollUsd = if (teamMembers.isNotEmpty()) {
            teamMembers.sumOf { toUsd(it.monthlyCompensation, it.currency, customRates) }
        } else {
            348800.0 // Baseline global team payroll
        }

        // Schedule Payroll on Day 15 and Day 45
        listOf(15, 45, 75).forEach { day ->
            if (day <= horizon.days + 15) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, day)
                scheduledEvents.add(
                    ScheduledCashEvent(
                        eventId = "OUTFLOW-PAYROLL-$day",
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time),
                        daysFromNow = day,
                        title = "Consolidated Cross-Border Payroll Batch",
                        counterpartyOrSource = "146 Global Employees (US, UK, CH, SG, BR)",
                        amountUsd = monthlyPayrollUsd,
                        isInflow = false,
                        category = "PAYROLL_RUN",
                        confidenceProbability = 1.0
                    )
                )
            }
        }

        // Statutory Tax Deadlines (Corporate Income Tax & Federal VAT/MWST)
        val taxEvents = listOf(
            Triple("TAX-US-1120", "IRS Corporate Estimated Tax Q3", 148500.0 to 14),
            Triple("TAX-CH-MWST", "Swiss Federal Tax Administration (ESTV)", 62000.0 to 20),
            Triple("TAX-SG-IRAS", "Inland Revenue Authority of Singapore (IRAS)", 45000.0 to 25)
        )
        taxEvents.forEach { (taxId, desc, amtDays) ->
            val (amt, days) = amtDays
            if (days <= horizon.days + 15) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, days)
                scheduledEvents.add(
                    ScheduledCashEvent(
                        eventId = "OUTFLOW-$taxId",
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time),
                        daysFromNow = days,
                        title = desc,
                        counterpartyOrSource = desc,
                        amountUsd = amt,
                        isInflow = false,
                        category = "TAX_FILING",
                        confidenceProbability = 1.0
                    )
                )
            }
        }

        // Freight & Customs Duties
        val freightExpense = if (shipments.isNotEmpty()) {
            shipments.sumOf { toUsd(it.cargoValue * 0.045, it.currency, customRates) }
        } else {
            38500.0
        }
        listOf(8, 26, 52).forEach { day ->
            if (day <= horizon.days + 10) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, day)
                val stressMultiplier = if (scenario == CashFlowScenario.CONSERVATIVE_STRESS) 1.25 else 1.0
                scheduledEvents.add(
                    ScheduledCashEvent(
                        eventId = "OUTFLOW-FREIGHT-$day",
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time),
                        daysFromNow = day,
                        title = "Customs Tariff & Airfreight Clearing",
                        counterpartyOrSource = "Lufthansa Cargo / Changi Port Terminal",
                        amountUsd = freightExpense * stressMultiplier,
                        isInflow = false,
                        category = "LOGISTICS_FREIGHT",
                        confidenceProbability = 0.98
                    )
                )
            }
        }

        // Daily Baseline OpEx Burn (Cloud Infrastructure, Banking, Enterprise SLA)
        val dailyOpexBurn = when (scenario) {
            CashFlowScenario.CONSERVATIVE_STRESS -> 14500.0
            else -> 11800.0
        }

        // 4. Compute Daily Cash Flow Trajectory Points from Day 0 to horizon.days
        val dailyPoints = mutableListOf<DailyCashFlowPoint>()
        var runningBalance = currentLiquidCashUsd
        var totalInflowsInHorizon = 0.0
        var totalOutflowsInHorizon = 0.0
        var minBalanceEncountered = currentLiquidCashUsd

        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)

        for (day in 0..horizon.days) {
            val pointCal = Calendar.getInstance()
            pointCal.add(Calendar.DAY_OF_YEAR, day)
            val dateLabel = dateFormat.format(pointCal.time)

            // Events occurring on this specific day
            val dayEvents = scheduledEvents.filter { it.daysFromNow == day }
            val dayInflow = dayEvents.filter { it.isInflow }.sumOf { it.amountUsd }
            val daySpecificOutflow = dayEvents.filter { !it.isInflow }.sumOf { it.amountUsd }

            // Apply daily recurring opex (starting from day 1)
            val dayTotalOutflow = daySpecificOutflow + if (day > 0) dailyOpexBurn else 0.0

            if (day > 0) {
                totalInflowsInHorizon += dayInflow
                totalOutflowsInHorizon += dayTotalOutflow
                runningBalance = runningBalance + dayInflow - dayTotalOutflow
            }

            if (runningBalance < minBalanceEncountered) {
                minBalanceEncountered = runningBalance
            }

            val milestoneEvent = dayEvents.firstOrNull()

            dailyPoints.add(
                DailyCashFlowPoint(
                    dayIndex = day,
                    dateFormatted = dateLabel,
                    projectedBalanceUsd = runningBalance,
                    inflowUsd = dayInflow,
                    outflowUsd = dayTotalOutflow,
                    netDailyFlowUsd = dayInflow - dayTotalOutflow,
                    isMilestoneEvent = milestoneEvent != null,
                    milestoneTitle = milestoneEvent?.title
                )
            )
        }

        // 5. Working Capital Buffer (Minimum safe threshold = 15% of current liquidity)
        val minWorkingCapitalBufferUsd = currentLiquidCashUsd * 0.15
        val workingCapitalDeficitUsd = if (minBalanceEncountered < minWorkingCapitalBufferUsd) {
            minWorkingCapitalBufferUsd - minBalanceEncountered
        } else 0.0

        val netCashFlowDeltaUsd = totalInflowsInHorizon - totalOutflowsInHorizon
        val projectedNetPositionUsd = currentLiquidCashUsd + netCashFlowDeltaUsd

        // Monthly burn rate for runway calculation
        val dailyAverageNetBurn = if (totalOutflowsInHorizon > totalInflowsInHorizon) {
            (totalOutflowsInHorizon - totalInflowsInHorizon) / horizon.days
        } else {
            totalOutflowsInHorizon / (horizon.days * 2) // conservative estimate
        }
        val monthlyNetBurn = max(dailyAverageNetBurn * 30.0, 150000.0)
        val runwayMonths = if (monthlyNetBurn > 0) currentLiquidCashUsd / monthlyNetBurn else 36.0

        // 6. Liquidity Health Status & Executive Recommendation
        val (healthStatus, recommendation) = when {
            workingCapitalDeficitUsd > 0.0 -> {
                LiquidityHealthStatus.DEFICIT_RISK to
                    "Projected balance dips below $minWorkingCapitalBufferUsd USD safety threshold in ${horizon.days} days. Accelerate collection of outstanding invoices or execute USD interbank swap."
            }
            minBalanceEncountered < minWorkingCapitalBufferUsd * 1.2 -> {
                LiquidityHealthStatus.CAPITAL_WARNING to
                    "Working capital buffer is tight at lowest trough ($${minBalanceEncountered.roundToInt()} USD). Maintain credit clearing facility and monitor delayed client remittance."
            }
            netCashFlowDeltaUsd < 0 -> {
                LiquidityHealthStatus.BALANCED_RUNWAY to
                    "Planned disbursements exceed incoming collections by $${(-netCashFlowDeltaUsd).roundToInt()} USD over ${horizon.days} days, comfortably absorbed by sovereign reserve pool."
            }
            else -> {
                LiquidityHealthStatus.OPTIMAL_SURPLUS to
                    "Net positive cash expansion of +$${netCashFlowDeltaUsd.roundToInt()} USD projected. Excellent treasury liquidity with over ${String.format(Locale.US, "%.1f", runwayMonths)} months operating runway."
            }
        }

        return PredictiveCashFlowReport(
            horizon = horizon,
            scenario = scenario,
            currentLiquidCashUsd = currentLiquidCashUsd,
            totalExpectedInflowsUsd = totalInflowsInHorizon,
            totalCommittedOutflowsUsd = totalOutflowsInHorizon,
            projectedNetPositionUsd = projectedNetPositionUsd,
            netCashFlowDeltaUsd = netCashFlowDeltaUsd,
            minProjectedBalanceUsd = minBalanceEncountered,
            minWorkingCapitalBufferUsd = minWorkingCapitalBufferUsd,
            workingCapitalDeficitUsd = workingCapitalDeficitUsd,
            runwayMonths = runwayMonths,
            liquidityHealthStatus = healthStatus,
            statusRecommendation = recommendation,
            dailyPoints = dailyPoints,
            upcomingEvents = scheduledEvents.sortedBy { it.daysFromNow }
        )
    }
}
