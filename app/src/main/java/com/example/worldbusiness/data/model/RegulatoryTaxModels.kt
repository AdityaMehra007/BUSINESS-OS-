package com.example.worldbusiness.data.model

enum class TaxRequirementType(val label: String, val shortCode: String) {
    CORPORATE_INCOME_TAX("Corporate Income Tax", "CIT"),
    VAT_GST_SALES("VAT / GST / Indirect Tax", "VAT"),
    ANNUAL_CORPORATE_RETURN("Annual Registry & Corporate Return", "REG"),
    TRANSFER_PRICING_BEPS("Transfer Pricing & BEPS Local File", "TP"),
    BENEFICIAL_OWNERSHIP_AML("Beneficial Ownership & AML (BOI)", "BOI"),
    WITHHOLDING_TAX("Cross-Border Withholding Tax (WHT)", "WHT"),
    PILLAR_TWO_GLOBE("OECD Pillar Two Minimum Tax (15%)", "P2")
}

enum class TaxDeadlineUrgency(val label: String) {
    CRITICAL_OVERDUE("Overdue / Immediate"),
    DUE_IMMINENT_15D("Due in < 15 Days"),
    UPCOMING_30D("Upcoming (15-30 Days)"),
    SCHEDULED_60D_PLUS("Scheduled (> 30 Days)"),
    COMPLETED_FILED("Filed & Verified")
}

data class RegulatoryTaxDeadlineItem(
    val id: String,
    val entityId: Long,
    val entityName: String,
    val countryCode: String,
    val countryName: String,
    val countryFlag: String,
    val jurisdictionName: String,
    val filingTitle: String,
    val statutoryAuthority: String,
    val dueDateString: String,
    val daysRemaining: Int,
    val statutoryCategory: TaxRequirementType,
    val estimatedTaxLiabilityUsd: Double?,
    val potentialPenaltyRiskUsd: Double,
    val statutoryFormCode: String,
    val submissionMethod: String,
    val localResidentSignatory: String,
    val complianceStatus: String, // "PENDING_PREPARATION", "READY_FOR_FILING", "FILED_CONFIRMED", "EXTENSION_GRANTED"
    val statutoryNotes: String,
    val requiredSchedules: List<String> = emptyList(),
    val penalCodeReference: String = "",
    val auditTrailVerified: Boolean = true,
    val electronicFilingReceipt: String? = null
)

data class JurisdictionRegulatoryOverview(
    val totalFilingDeadlines: Int,
    val upcomingIn30DaysCount: Int,
    val urgentActionCount: Int,
    val totalEstimatedTaxDueUsd: Double,
    val totalStatutoryPenaltyAtRiskUsd: Double,
    val averageJurisdictionComplianceScore: Int,
    val jurisdictionsCoveredCount: Int
)
