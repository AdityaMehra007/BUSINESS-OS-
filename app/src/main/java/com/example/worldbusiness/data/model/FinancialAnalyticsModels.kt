package com.example.worldbusiness.data.model

import java.text.NumberFormat
import java.util.Locale

enum class FinancialChartType(val label: String, val description: String) {
    REVENUE("Monthly Revenue", "Gross vs Net Revenue Spline & Area"),
    TAX("Tax Liabilities", "VAT/GST, WHT & Corporate Accruals"),
    INVOICES("Invoice Status", "Settlement Velocity, DSO & Aging Trend"),
    COMPOSITE("Composite", "Dual-Axis Revenue vs Tax Matrix")
}

enum class FinancialTimeRange(val label: String, val months: Int) {
    RANGE_6M("6M", 6),
    RANGE_12M("12M", 12),
    RANGE_YTD("YTD", 10)
}

data class MonthlyRevenueData(
    val month: String,
    val monthShort: String,
    val grossRevenue: Double,
    val netRevenue: Double,
    val operatingExpenses: Double,
    val growthRatePercent: Double
) {
    val profitMarginPercent: Double
        get() = if (grossRevenue > 0) ((grossRevenue - operatingExpenses) / grossRevenue) * 100.0 else 0.0
}

data class MonthlyTaxLiabilityData(
    val month: String,
    val monthShort: String,
    val vatGstAmount: Double,
    val withholdingTaxAmount: Double,
    val corporateTaxAmount: Double,
    val totalLiability: Double = vatGstAmount + withholdingTaxAmount + corporateTaxAmount,
    val complianceStatus: String = "PROVISIONED" // FILED, PROVISIONED, DUE_SOON
)

data class InvoiceStatusTrendData(
    val month: String,
    val monthShort: String,
    val paidVolume: Double,
    val paidCount: Int,
    val inClearingVolume: Double,
    val inClearingCount: Int,
    val pendingVolume: Double,
    val pendingCount: Int,
    val overdueVolume: Double,
    val overdueCount: Int,
    val collectionEfficiencyPercent: Double,
    val dsoDays: Int
) {
    val totalVolume: Double get() = paidVolume + inClearingVolume + pendingVolume + overdueVolume
    val totalCount: Int get() = paidCount + inClearingCount + pendingCount + overdueCount
}

data class FinancialAnalyticsOverview(
    val timeRange: FinancialTimeRange,
    val revenueSeries: List<MonthlyRevenueData>,
    val taxSeries: List<MonthlyTaxLiabilityData>,
    val invoiceSeries: List<InvoiceStatusTrendData>,
    val totalGrossRevenue: Double,
    val totalTaxLiabilities: Double,
    val totalInClearing: Double,
    val averageDsoDays: Int,
    val currentRunRateAnnualized: Double,
    val yoyRevenueGrowthPercent: Double
) {
    fun formatRevenue(): String = "$${NumberFormat.getNumberInstance(Locale.US).format(totalGrossRevenue.toLong())}"
    fun formatTax(): String = "$${NumberFormat.getNumberInstance(Locale.US).format(totalTaxLiabilities.toLong())}"
    fun formatInClearing(): String = "$${NumberFormat.getNumberInstance(Locale.US).format(totalInClearing.toLong())}"
}
