package com.example.worldbusiness.data.model

enum class ThresholdCategory(val label: String) {
    PAYROLL_EXPENSES("Payroll Statutory Expenses"),
    CROSS_BORDER_TRANSFER("Cross-Border Wire Transfers")
}

enum class ThresholdAlertStatus(val label: String) {
    APPROACHING_THRESHOLD("Approaching Threshold"),
    THRESHOLD_EXCEEDED("Threshold Exceeded"),
    ACKNOWLEDGED("Acknowledged by Compliance"),
    REPORT_FILED("Regulatory Filing Submitted")
}

data class RegulatoryThresholdAlert(
    val id: String,
    val category: ThresholdCategory,
    val title: String,
    val jurisdictionCode: String, // "US", "GB", "DE", "SG", "CH", "JP", "BR"
    val jurisdictionName: String,
    val regulatoryBody: String, // "IRS & FinCEN", "HMRC (UK)", "MAS (Singapore)", "FINMA (Switzerland)", "BAFIN / Deutsche Rentenversicherung"
    val statutoryReference: String, // e.g. "IRC § 6302 / Form 941", "HMRC RTI Reg 67", "31 CFR § 1010.311", "MAS Notice 626"
    val currentAmount: Double,
    val thresholdLimit: Double,
    val currency: String,
    val currentAmountUsd: Double,
    val thresholdLimitUsd: Double,
    val utilizationPercent: Double, // e.g. 88.5
    val status: ThresholdAlertStatus,
    val severity: ComplianceSeverity,
    val requiredFilingForm: String, // e.g. "IRS Form 941 / EFTPS", "FinCEN Form 112 CTR", "HMRC FPS Return", "MAS Form 626"
    val reportingDeadline: String,
    val remediationAction: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RegulatoryThresholdStats(
    val totalAlertsCount: Int,
    val approachingCount: Int,
    val exceededCount: Int,
    val payrollAlertsCount: Int,
    val transferAlertsCount: Int,
    val acknowledgedCount: Int
)
