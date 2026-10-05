package com.example.worldbusiness.data.model

data class TaxItemDeduction(
    val name: String,
    val category: String, // "INCOME_TAX", "SOCIAL_CONTRIBUTION", "PENSION_HEALTH", "OTHER"
    val ratePercent: Double,
    val amountLocal: Double,
    val description: String = ""
)

data class EmployeePayrollDetail(
    val memberId: Long,
    val fullName: String,
    val role: String,
    val country: String,
    val countryCode: String,
    val employmentType: String,
    val currency: String,
    val currencySymbol: String,
    val grossPayLocal: Double,
    val incomeTaxLocal: Double,
    val socialSecurityLocal: Double,
    val otherDeductionsLocal: Double,
    val totalTaxWithheldLocal: Double,
    val effectiveTaxRatePercent: Double,
    val netPayLocal: Double,
    val rateToUsd: Double,
    val grossPayUsd: Double,
    val totalTaxWithheldUsd: Double,
    val netPayUsd: Double,
    val taxJurisdiction: String,
    val taxAuthority: String,
    val statutoryNotes: String,
    val deductions: List<TaxItemDeduction>,
    val employerContributionLocal: Double,
    val paymentStatus: String,
    val clearingRail: String
)

data class JurisdictionSummary(
    val countryCode: String,
    val countryName: String,
    val currency: String,
    val currencySymbol: String,
    val employeeCount: Int,
    val totalGrossLocal: Double,
    val totalTaxWithheldLocal: Double,
    val totalNetPayLocal: Double,
    val totalGrossUsd: Double,
    val totalTaxWithheldUsd: Double,
    val totalNetPayUsd: Double,
    val averageEffectiveTaxRate: Double,
    val primaryTaxAuthority: String,
    val complianceStandard: String
)

data class GlobalPayrollSummary(
    val totalEmployees: Int,
    val totalGrossUsd: Double,
    val totalTaxWithheldUsd: Double,
    val totalNetPayUsd: Double,
    val overallEffectiveTaxRate: Double,
    val jurisdictionCount: Int,
    val jurisdictions: List<JurisdictionSummary>,
    val employeeDetails: List<EmployeePayrollDetail>
)
