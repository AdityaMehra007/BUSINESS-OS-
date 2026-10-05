package com.example.worldbusiness.data.model

enum class HedgingStrategyType(val label: String, val shortCode: String) {
    FORWARD_CONTRACT("Fixed FX Forward Contract", "FWD"),
    ZERO_COST_COLLAR("Zero-Cost Collar Corridor", "COLLAR"),
    NATURAL_MATCH("Natural Balance Sheet Match", "NATURAL"),
    LAYERED_TRANCHES("Layered Dynamic Tranches", "LAYERED"),
    SYNTHETIC_SWAP("Synthetic Currency Swap", "SWAP")
}

enum class HedgingUrgency(val label: String) {
    CRITICAL("Critical Action Required"),
    HIGH("High Risk Exposure"),
    MODERATE("Moderate Exposure"),
    LOW("Low Volatility / Optional")
}

data class HedgingSuggestion(
    val id: String,
    val invoiceId: Long,
    val invoiceNumber: String,
    val counterparty: String,
    val currency: String,
    val exposureAmount: Double,
    val exposureUsd: Double,
    val dueDateString: String,
    val daysToDueDate: Int,
    val historicalVolatilityAnnualized: Double, // e.g. 14.2%
    val spotRate: Double,
    val forwardRate: Double,
    val collarFloorRate: Double? = null,
    val collarCapRate: Double? = null,
    val valueAtRisk95Usd: Double,
    val estimatedExecutionCostBps: Int,
    val protectedDownsideUsd: Double,
    val strategyType: HedgingStrategyType,
    val urgency: HedgingUrgency,
    val ruleTriggeredName: String,
    val ruleExplanation: String,
    val actionSteps: List<String>,
    val isExecuted: Boolean = false
)

data class HedgingPortfolioSummary(
    val totalUnhedgedExposureUsd: Double,
    val totalValueAtRiskUsd: Double,
    val criticalUrgencyCount: Int,
    val highUrgencyCount: Int,
    val activeSuggestionsCount: Int,
    val averageVolatilityPercent: Double,
    val recommendedCoverageRatioPercent: Double
)
