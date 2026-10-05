package com.example.worldbusiness.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative

enum class CashFlowHorizon(val days: Int, val label: String) {
    DAYS_14(14, "14D Flash"),
    DAYS_30(30, "30D Horizon"),
    DAYS_60(60, "60D Horizon"),
    DAYS_90(90, "90D Horizon")
}

enum class CashFlowScenario(val label: String, val shortName: String, val description: String) {
    BASE_CASE(
        "Baseline (100% On-Time)",
        "Baseline",
        "Assumes clients pay exactly by statutory due date with historical operating burn."
    ),
    DELAYED_RECEIVABLES(
        "Delayed Invoices (+30D)",
        "Delayed Collections",
        "Simulates 30-day cross-border collection delays from enterprise pharmaceutical and commercial buyers."
    ),
    CONSERVATIVE_STRESS(
        "Stress Test (-25% Inflow)",
        "Stress Test",
        "Applies 25% receivable haircut and 15% surge in logistics freight & emergency operating costs."
    )
}

enum class LiquidityHealthStatus(
    val title: String,
    val badgeText: String,
    val color: Color
) {
    OPTIMAL_SURPLUS("Optimal Cash Surplus", "HEALTHY SURPLUS", EmeraldPositive),
    BALANCED_RUNWAY("Balanced Liquidity", "BALANCED", CyanAccent),
    CAPITAL_WARNING("Working Capital Buffer Alert", "BUFFER WARNING", GoldAccent),
    DEFICIT_RISK("Critical Liquidity Shortfall", "DEFICIT RISK", RoseNegative)
}

data class DailyCashFlowPoint(
    val dayIndex: Int,
    val dateFormatted: String,
    val projectedBalanceUsd: Double,
    val inflowUsd: Double,
    val outflowUsd: Double,
    val netDailyFlowUsd: Double,
    val isMilestoneEvent: Boolean = false,
    val milestoneTitle: String? = null
)

data class ScheduledCashEvent(
    val eventId: String,
    val date: String,
    val daysFromNow: Int,
    val title: String,
    val counterpartyOrSource: String,
    val amountUsd: Double,
    val isInflow: Boolean,
    val category: String,
    val confidenceProbability: Double = 0.95
)

data class PredictiveCashFlowReport(
    val horizon: CashFlowHorizon,
    val scenario: CashFlowScenario,
    val currentLiquidCashUsd: Double,
    val totalExpectedInflowsUsd: Double,
    val totalCommittedOutflowsUsd: Double,
    val projectedNetPositionUsd: Double,
    val netCashFlowDeltaUsd: Double,
    val minProjectedBalanceUsd: Double,
    val minWorkingCapitalBufferUsd: Double,
    val workingCapitalDeficitUsd: Double,
    val runwayMonths: Double,
    val liquidityHealthStatus: LiquidityHealthStatus,
    val statusRecommendation: String,
    val dailyPoints: List<DailyCashFlowPoint>,
    val upcomingEvents: List<ScheduledCashEvent>
)
