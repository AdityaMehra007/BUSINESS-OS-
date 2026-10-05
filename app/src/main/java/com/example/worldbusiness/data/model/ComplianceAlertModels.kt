package com.example.worldbusiness.data.model

enum class ComplianceSeverity(val label: String, val level: Int) {
    CRITICAL("CRITICAL AML RISK", 4),
    HIGH("HIGH RISK", 3),
    MEDIUM("MODERATE RISK", 2),
    LOW("LOW RISK", 1),
    CLEARED("CLEARED", 0)
}

enum class ComplianceRiskType(val label: String) {
    AML_STRUCTURING("BSA/AML Smurfing & Structuring"),
    LARGE_VALUE_CTR("FinCEN Form 112 CTR Trigger (> $10k)"),
    SANCTIONS_OFAC("OFAC / EU Sanctions SDN Screening"),
    TAX_HAVEN_ARBITRAGE("FATF Non-Cooperative Jurisdiction"),
    TRAVEL_RULE("FinCEN / FATF Wire Travel Rule Missing Data"),
    BENEFICIAL_OWNERSHIP("FINMA / MAS Beneficial Owner (UBO) Gap"),
    RAPID_VELOCITY("Cross-Border Velocity Surge Spike"),
    UNREGISTERED_FOREIGN_EXCHANGE("Sisbacen FX Regulatory Filing Gap")
}

enum class ComplianceAlertStatus(val label: String) {
    FLAGGED("Active Flag"),
    UNDER_REVIEW("Under Review"),
    SAR_REPORTED("SAR Filed"),
    RESOLVED("Resolved & Cleared"),
    WHITELISTED("Whitelisted Counterparty")
}

data class ComplianceAlert(
    val id: String,
    val transactionReference: String,
    val counterparty: String,
    val amount: Double,
    val currency: String,
    val amountUsd: Double,
    val jurisdictionCode: String, // "US", "EU", "GB", "CH", "SG", "BR", "GLOBAL"
    val jurisdictionName: String,
    val regulatoryBody: String, // "FinCEN & OFAC", "FINMA / MROS", "MAS Singapore", "HMRC / FCA", "Receita / COAF"
    val statutoryRuleCode: String,
    val severity: ComplianceSeverity,
    val riskType: ComplianceRiskType,
    val title: String,
    val description: String,
    val flaggedDate: String,
    val status: ComplianceAlertStatus,
    val riskScore: Int, // 0 - 100
    val remediationRecommendation: String,
    val requiredFiling: String
)

data class ComplianceOverviewStats(
    val totalTransactionsAudited: Int,
    val activeFlagsCount: Int,
    val criticalFlagsCount: Int,
    val underReviewCount: Int,
    val resolvedCount: Int,
    val complianceHealthScore: Int, // 0 - 100
    val totalHighRiskVolumeUsd: Double,
    val monitoredJurisdictionsCount: Int
)
