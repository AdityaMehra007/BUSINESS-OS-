package com.example.worldbusiness.data.model

/**
 * Schedule frequency / timing for multinational payroll execution
 */
enum class PayrollScheduleCycle(val label: String, val shortDescription: String) {
    INSTANT("Instant STP Execution", "Disburse immediately via direct local clearing rails"),
    MID_MONTH("Mid-Month Pay Run", "Scheduled for Oct 15, 2026 • 09:00 UTC cutoff"),
    MONTH_END("Month-End Cycle", "Scheduled for Oct 31, 2026 • Close of Business"),
    CUSTOM("Custom Scheduled Date", "Forward-dated corporate treasury wire release")
}

/**
 * Aggregated disbursement batch for a specific destination local currency
 */
data class PayrollCurrencyDisbursementBatch(
    val currency: String,
    val countryCode: String,
    val clearingRail: String,
    val recipientCount: Int,
    val grossLocalAmount: Double,
    val taxWithheldLocalAmount: Double,
    val netPayableLocalAmount: Double,
    val exchangeRateToFunding: Double, // Exchange rate: 1 local currency = X funding currency
    val fundingCurrencyEquivalent: Double, // Net payable in funding currency
    val isApiLive: Boolean,
    val rateProvider: String,
    val rateTimestamp: String,
    val memberIds: List<Long>
)

/**
 * Immutable digital receipt produced upon bulk multi-currency disbursement execution
 */
data class BulkPayrollDisbursementReceipt(
    val batchReferenceId: String,
    val executionTimestamp: String,
    val scheduledDate: String,
    val fundingVaultCurrency: String,
    val totalFundingOutflow: Double,
    val totalNetLocalPayouts: Map<String, Double>,
    val totalTaxesWithheldFundingEquivalent: Double,
    val processedEmployeesCount: Int,
    val clearingRailReferences: List<String>,
    val cryptographicHash: String
)
