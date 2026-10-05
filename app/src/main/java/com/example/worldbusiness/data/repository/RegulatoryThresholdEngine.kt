package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.ComplianceSeverity
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.RegulatoryThresholdStats
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.ThresholdAlertStatus
import com.example.worldbusiness.data.model.ThresholdCategory
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import java.util.Locale

object RegulatoryThresholdEngine {

    /**
     * Evaluates payroll expenses and cross-border treasury transactions against
     * statutory domestic & international reporting thresholds.
     */
    fun evaluateThresholds(
        teamMembers: List<TeamMemberRecord>,
        balances: List<FxBalanceRecord>,
        transactions: List<TreasuryTransactionRecord>,
        payrollSummary: GlobalPayrollSummary,
        statusOverrides: Map<String, ThresholdAlertStatus> = emptyMap()
    ): List<RegulatoryThresholdAlert> {
        val alerts = mutableListOf<RegulatoryThresholdAlert>()

        // Build FX rate lookup map
        val rates = balances.associate { it.currencyCode.uppercase(Locale.US) to it.rateToUsd }
        fun toUsd(curr: String, amount: Double): Double {
            val r = rates[curr.uppercase(Locale.US)] ?: when (curr.uppercase(Locale.US)) {
                "EUR" -> 1.09
                "GBP" -> 1.30
                "CHF" -> 1.17
                "SGD" -> 0.77
                "BRL" -> 0.184
                "JPY" -> 0.0068
                "AUD" -> 0.67
                "CAD" -> 0.74
                else -> 1.0
            }
            return amount * r
        }

        // =========================================================================
        // 1. PAYROLL EXPENSES REGULATORY THRESHOLD AUDIT
        // =========================================================================
        val membersByJurisdiction = teamMembers.groupBy { it.countryCode.uppercase(Locale.US) }

        // (A) United States: IRS Form 941 Semi-Weekly Deposit Rule (IRC § 6302)
        val usMembers = membersByJurisdiction["US"].orEmpty()
        val usGross = usMembers.sumOf { it.monthlyCompensation }
        val usThreshold = 50000.0 // $50,000 threshold for semi-weekly statutory tax deposit schedule
        if (usGross >= usThreshold * 0.75) {
            val alertId = "THRESH-PAYROLL-US-941"
            val status = statusOverrides[alertId] ?: if (usGross >= usThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
            val utilPct = (usGross / usThreshold) * 100.0
            alerts.add(
                RegulatoryThresholdAlert(
                    id = alertId,
                    category = ThresholdCategory.PAYROLL_EXPENSES,
                    title = "IRS Form 941 Semi-Weekly Deposit Threshold",
                    jurisdictionCode = "US",
                    jurisdictionName = "United States",
                    regulatoryBody = "Internal Revenue Service (IRS) & Treasury",
                    statutoryReference = "IRC § 6302 / Treas. Reg. § 31.6302-1",
                    currentAmount = usGross,
                    thresholdLimit = usThreshold,
                    currency = "USD",
                    currentAmountUsd = usGross,
                    thresholdLimitUsd = usThreshold,
                    utilizationPercent = utilPct,
                    status = status,
                    severity = if (utilPct >= 100.0) ComplianceSeverity.HIGH else ComplianceSeverity.MEDIUM,
                    requiredFilingForm = "IRS Form 941 / EFTPS Electronic Schedule",
                    reportingDeadline = "Within 3 Banking Days of Payroll Settlement",
                    remediationAction = "Ensure EFTPS direct deposit account is pre-funded; transmit electronic payroll schedule to avoid statutory penalty of 2% to 15%.",
                    description = "Monthly US workforce compensation of $${String.format(Locale.US, "%,.0f", usGross)} is at ${String.format(Locale.US, "%.1f", utilPct)}% of the $50,000 lookback threshold. Exceeding triggers mandatory semi-weekly deposit acceleration."
                )
            )
        }

        // (B) United Kingdom: HMRC Real Time Information (RTI) & Apprenticeship Levy (£30,000 / month)
        val ukMembers = membersByJurisdiction["GB"].orEmpty()
        val ukGross = ukMembers.sumOf { it.monthlyCompensation }
        val ukThreshold = 25000.0 // £25,000 monthly pay bill threshold
        if (ukGross >= ukThreshold * 0.75) {
            val alertId = "THRESH-PAYROLL-UK-HMRC"
            val status = statusOverrides[alertId] ?: if (ukGross >= ukThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
            val utilPct = (ukGross / ukThreshold) * 100.0
            alerts.add(
                RegulatoryThresholdAlert(
                    id = alertId,
                    category = ThresholdCategory.PAYROLL_EXPENSES,
                    title = "HMRC PAYE RTI & Apprenticeship Levy Threshold",
                    jurisdictionCode = "GB",
                    jurisdictionName = "United Kingdom",
                    regulatoryBody = "HM Revenue & Customs (HMRC)",
                    statutoryReference = "Income Tax (PAYE) Regulations 2003 Reg 67",
                    currentAmount = ukGross,
                    thresholdLimit = ukThreshold,
                    currency = "GBP",
                    currentAmountUsd = toUsd("GBP", ukGross),
                    thresholdLimitUsd = toUsd("GBP", ukThreshold),
                    utilizationPercent = utilPct,
                    status = status,
                    severity = if (utilPct >= 100.0) ComplianceSeverity.HIGH else ComplianceSeverity.MEDIUM,
                    requiredFilingForm = "HMRC Full Payment Submission (FPS / EPS)",
                    reportingDeadline = "On or Before Payment Date (Real-Time)",
                    remediationAction = "File Full Payment Submission (FPS) via HMRC Government Gateway before disbursing sterling BACS payments.",
                    description = "UK gross payroll liabilities of £${String.format(Locale.US, "%,.0f", ukGross)} GBP stand at ${String.format(Locale.US, "%.1f", utilPct)}% of the £25,000 statutory reporting band."
                )
            )
        }

        // (C) Singapore: Central Provident Fund (CPF) Statutory Threshold & Foreign Quota (S$25,000 / month)
        val sgMembers = membersByJurisdiction["SG"].orEmpty()
        val sgGross = sgMembers.sumOf { it.monthlyCompensation }
        val sgThreshold = 25000.0 // S$25,000 monthly statutory submission ceiling
        if (sgGross >= sgThreshold * 0.70) {
            val alertId = "THRESH-PAYROLL-SG-CPF"
            val status = statusOverrides[alertId] ?: if (sgGross >= sgThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
            val utilPct = (sgGross / sgThreshold) * 100.0
            alerts.add(
                RegulatoryThresholdAlert(
                    id = alertId,
                    category = ThresholdCategory.PAYROLL_EXPENSES,
                    title = "Singapore CPF Statutory Board Monthly Threshold",
                    jurisdictionCode = "SG",
                    jurisdictionName = "Singapore",
                    regulatoryBody = "Central Provident Fund Board (CPFB)",
                    statutoryReference = "Central Provident Fund Act 1953 (Cap. 36)",
                    currentAmount = sgGross,
                    thresholdLimit = sgThreshold,
                    currency = "SGD",
                    currentAmountUsd = toUsd("SGD", sgGross),
                    thresholdLimitUsd = toUsd("SGD", sgThreshold),
                    utilizationPercent = utilPct,
                    status = status,
                    severity = ComplianceSeverity.MEDIUM,
                    requiredFilingForm = "CPF e-Submit@web Monthly Return",
                    reportingDeadline = "14th of following calendar month",
                    remediationAction = "Verify mandatory 17% employer CPF and 20% employee contributions on CPF Direct Debit Clearing System.",
                    description = "Singapore regional headcount compensation of S$${String.format(Locale.US, "%,.0f", sgGross)} SGD is at ${String.format(Locale.US, "%.1f", utilPct)}% of the S$25,000 statutory contribution ceiling."
                )
            )
        }

        // (D) Germany / EU: Social Insurance Ceiling & DEÜV Notification (€30,000 / month)
        val deMembers = membersByJurisdiction["DE"].orEmpty()
        val deGross = deMembers.sumOf { it.monthlyCompensation }
        val deThreshold = 28000.0 // €28,000 monthly corporate gross band
        if (deGross >= deThreshold * 0.75) {
            val alertId = "THRESH-PAYROLL-DE-SOZIAL"
            val status = statusOverrides[alertId] ?: if (deGross >= deThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
            val utilPct = (deGross / deThreshold) * 100.0
            alerts.add(
                RegulatoryThresholdAlert(
                    id = alertId,
                    category = ThresholdCategory.PAYROLL_EXPENSES,
                    title = "German Beitragsbemessungsgrenze & DEÜV Notification",
                    jurisdictionCode = "DE",
                    jurisdictionName = "Germany",
                    regulatoryBody = "Deutsche Rentenversicherung & Krankenkassen",
                    statutoryReference = "SGB IV § 28a / DEÜV Meldeverordnung",
                    currentAmount = deGross,
                    thresholdLimit = deThreshold,
                    currency = "EUR",
                    currentAmountUsd = toUsd("EUR", deGross),
                    thresholdLimitUsd = toUsd("EUR", deThreshold),
                    utilizationPercent = utilPct,
                    status = status,
                    severity = ComplianceSeverity.MEDIUM,
                    requiredFilingForm = "DEÜV Beitragsnachweis Electronic Transmission",
                    reportingDeadline = "Fifth Banking Day Before Month End",
                    remediationAction = "Submit statutory Beitragsnachweis electronic declarations to designated health insurance statutory funds.",
                    description = "German subsidiary payroll commitments of €${String.format(Locale.US, "%,.0f", deGross)} EUR have reached ${String.format(Locale.US, "%.1f", utilPct)}% of the corporate statutory notification threshold."
                )
            )
        }

        // (E) Switzerland: Swissdec AHV/ALV Wage Ceiling (CHF 35,000 / month)
        val chMembers = membersByJurisdiction["CH"].orEmpty()
        val chGross = chMembers.sumOf { it.monthlyCompensation }
        val chThreshold = 35000.0
        if (chGross >= chThreshold * 0.75) {
            val alertId = "THRESH-PAYROLL-CH-AHV"
            val status = statusOverrides[alertId] ?: if (chGross >= chThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
            val utilPct = (chGross / chThreshold) * 100.0
            alerts.add(
                RegulatoryThresholdAlert(
                    id = alertId,
                    category = ThresholdCategory.PAYROLL_EXPENSES,
                    title = "Swissdec AHV/IV/EO Statutory Wage Declaration",
                    jurisdictionCode = "CH",
                    jurisdictionName = "Switzerland",
                    regulatoryBody = "Zentrale Ausgleichsstelle (ZAS) & Cantonal Ausgleichskasse",
                    statutoryReference = "Bundesgesetz über die Alters- und Hinterlassenenversicherung (AHVG)",
                    currentAmount = chGross,
                    thresholdLimit = chThreshold,
                    currency = "CHF",
                    currentAmountUsd = toUsd("CHF", chGross),
                    thresholdLimitUsd = toUsd("CHF", chThreshold),
                    utilizationPercent = utilPct,
                    status = status,
                    severity = ComplianceSeverity.MEDIUM,
                    requiredFilingForm = "ELM Swissdec XML Standard Wage Declaration",
                    reportingDeadline = "Annual Reconciliation / Monthly Advance",
                    remediationAction = "Confirm cantonal Quellensteuer and Pillar 2 BVG pension allocations via Swissdec transmission.",
                    description = "Zurich HQ workforce compensation of CHF ${String.format(Locale.US, "%,.0f", chGross)} is at ${String.format(Locale.US, "%.1f", utilPct)}% of the CHF 35,000 monthly reporting threshold."
                )
            )
        }

        // =========================================================================
        // 2. CROSS-BORDER TRANSFERS REGULATORY THRESHOLD AUDIT
        // =========================================================================
        transactions.forEach { tx ->
            val ref = tx.referenceCode
            val amountUsd = toUsd(tx.currency, tx.amount)

            // (A) FinCEN BSA Currency Transaction Report (CTR) / Wire Travel Rule ($10,000 USD limit)
            // Trigger alert when amount is between $8,000 and $10,000 (approaching) or >= $10,000 (exceeded)
            val fincenThreshold = 10000.0
            if (tx.currency.equals("USD", ignoreCase = true) && tx.amount >= fincenThreshold * 0.80) {
                val alertId = "THRESH-WIRE-US-CTR-${tx.id}"
                val status = statusOverrides[alertId] ?: if (tx.amount >= fincenThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
                val utilPct = (tx.amount / fincenThreshold) * 100.0
                alerts.add(
                    RegulatoryThresholdAlert(
                        id = alertId,
                        category = ThresholdCategory.CROSS_BORDER_TRANSFER,
                        title = "FinCEN Form 112 CTR & Travel Rule Threshold",
                        jurisdictionCode = "US",
                        jurisdictionName = "United States",
                        regulatoryBody = "Financial Crimes Enforcement Network (FinCEN)",
                        statutoryReference = "Bank Secrecy Act / 31 CFR § 1010.311 & § 1010.410",
                        currentAmount = tx.amount,
                        thresholdLimit = fincenThreshold,
                        currency = "USD",
                        currentAmountUsd = tx.amount,
                        thresholdLimitUsd = fincenThreshold,
                        utilizationPercent = utilPct,
                        status = status,
                        severity = if (utilPct >= 100.0) ComplianceSeverity.HIGH else ComplianceSeverity.MEDIUM,
                        requiredFilingForm = "FinCEN Form 112 (CTR) & BSA Travel Rule Message",
                        reportingDeadline = "15 Calendar Days After Settlement",
                        remediationAction = "Verify originator/beneficiary address, tax ID, and account metadata on Fedwire/CHIPS MT103 format.",
                        description = "Cross-border transfer of $${String.format(Locale.US, "%,.0f", tx.amount)} USD ($ref) to ${tx.senderOrCounterparty} is at ${String.format(Locale.US, "%.1f", utilPct)}% of the $10,000 FinCEN reporting threshold."
                    )
                )
            }

            // (B) MAS Singapore Notice 626 Section 13 Wire Threshold (S$20,000 SGD)
            val masThreshold = 20000.0
            if (tx.currency.equals("SGD", ignoreCase = true) && tx.amount >= masThreshold * 0.75) {
                val alertId = "THRESH-WIRE-SG-MAS-${tx.id}"
                val status = statusOverrides[alertId] ?: if (tx.amount >= masThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
                val utilPct = (tx.amount / masThreshold) * 100.0
                alerts.add(
                    RegulatoryThresholdAlert(
                        id = alertId,
                        category = ThresholdCategory.CROSS_BORDER_TRANSFER,
                        title = "MAS Notice 626 Cross-Border Wire Reporting",
                        jurisdictionCode = "SG",
                        jurisdictionName = "Singapore",
                        regulatoryBody = "Monetary Authority of Singapore (MAS)",
                        statutoryReference = "MAS Notice 626 (Prevention of Money Laundering) Sec 13",
                        currentAmount = tx.amount,
                        thresholdLimit = masThreshold,
                        currency = "SGD",
                        currentAmountUsd = toUsd("SGD", tx.amount),
                        thresholdLimitUsd = toUsd("SGD", masThreshold),
                        utilizationPercent = utilPct,
                        status = status,
                        severity = if (utilPct >= 100.0) ComplianceSeverity.HIGH else ComplianceSeverity.MEDIUM,
                        requiredFilingForm = "MAS STRO Suspicious & Large Transaction Return",
                        reportingDeadline = "Immediate / Next Business Day",
                        remediationAction = "Complete MAS Notice 626 customer due diligence (CDD) profile on counterparty prior to funds clearing.",
                        description = "Wire transfer of S$${String.format(Locale.US, "%,.0f", tx.amount)} SGD ($ref) to ${tx.senderOrCounterparty} is at ${String.format(Locale.US, "%.1f", utilPct)}% of the S$20,000 MAS regulatory threshold."
                    )
                )
            }

            // (C) FINMA Swiss AMLA Article 10 Wire Transfer Standard (CHF 50,000)
            val finmaThreshold = 50000.0
            if (tx.currency.equals("CHF", ignoreCase = true) && tx.amount >= finmaThreshold * 0.75) {
                val alertId = "THRESH-WIRE-CH-FINMA-${tx.id}"
                val status = statusOverrides[alertId] ?: if (tx.amount >= finmaThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
                val utilPct = (tx.amount / finmaThreshold) * 100.0
                alerts.add(
                    RegulatoryThresholdAlert(
                        id = alertId,
                        category = ThresholdCategory.CROSS_BORDER_TRANSFER,
                        title = "FINMA Swiss AMLA Art. 10 High-Value Wire Audit",
                        jurisdictionCode = "CH",
                        jurisdictionName = "Switzerland",
                        regulatoryBody = "Swiss Financial Market Supervisory Authority (FINMA / MROS)",
                        statutoryReference = "FINMA AMLO-FINMA Art. 10 / AMLA Art. 6",
                        currentAmount = tx.amount,
                        thresholdLimit = finmaThreshold,
                        currency = "CHF",
                        currentAmountUsd = toUsd("CHF", tx.amount),
                        thresholdLimitUsd = toUsd("CHF", finmaThreshold),
                        utilizationPercent = utilPct,
                        status = status,
                        severity = if (utilPct >= 100.0) ComplianceSeverity.CRITICAL else ComplianceSeverity.HIGH,
                        requiredFilingForm = "MROS Electronic Reporting Portal (goAML Switzerland)",
                        reportingDeadline = "Within 48 Hours of Outbound Execution",
                        remediationAction = "Execute Form A Beneficial Owner verification and retain commercial invoice justification on secure treasury ledger.",
                        description = "Outbound Swiss vaulted wire of CHF ${String.format(Locale.US, "%,.0f", tx.amount)} ($ref) is at ${String.format(Locale.US, "%.1f", utilPct)}% of the CHF 50,000 FINMA reporting ceiling."
                    )
                )
            }

            // (D) EU Regulation 2015/847 Funds Transfer Rule (€10,000)
            val euThreshold = 10000.0
            if (tx.currency.equals("EUR", ignoreCase = true) && tx.amount >= euThreshold * 0.80) {
                val alertId = "THRESH-WIRE-EU-TRANSFER-${tx.id}"
                val status = statusOverrides[alertId] ?: if (tx.amount >= euThreshold) ThresholdAlertStatus.THRESHOLD_EXCEEDED else ThresholdAlertStatus.APPROACHING_THRESHOLD
                val utilPct = (tx.amount / euThreshold) * 100.0
                alerts.add(
                    RegulatoryThresholdAlert(
                        id = alertId,
                        category = ThresholdCategory.CROSS_BORDER_TRANSFER,
                        title = "EU Regulation 2015/847 Wire Information Rule",
                        jurisdictionCode = "EU",
                        jurisdictionName = "European Union",
                        regulatoryBody = "European Banking Authority (EBA) & National Central Banks",
                        statutoryReference = "Regulation (EU) 2015/847 on Information Accompanying Transfers of Funds",
                        currentAmount = tx.amount,
                        thresholdLimit = euThreshold,
                        currency = "EUR",
                        currentAmountUsd = toUsd("EUR", tx.amount),
                        thresholdLimitUsd = toUsd("EUR", euThreshold),
                        utilizationPercent = utilPct,
                        status = status,
                        severity = if (utilPct >= 100.0) ComplianceSeverity.HIGH else ComplianceSeverity.MEDIUM,
                        requiredFilingForm = "SEPA / Target2 Regulatory Travel Schedule",
                        reportingDeadline = "Pre-Execution Verification",
                        remediationAction = "Verify that full payer LEI (Legal Entity Identifier) and IBAN are encoded in the SEPA credit message.",
                        description = "Eurozone cross-border transfer of €${String.format(Locale.US, "%,.0f", tx.amount)} EUR ($ref) has reached ${String.format(Locale.US, "%.1f", utilPct)}% of the €10,000 statutory reporting boundary."
                    )
                )
            }
        }

        // Sort: THRESHOLD_EXCEEDED first, then APPROACHING_THRESHOLD, ordered by utilization % descending
        return alerts.sortedWith(
            compareByDescending<RegulatoryThresholdAlert> { it.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED }
                .thenByDescending { it.status == ThresholdAlertStatus.APPROACHING_THRESHOLD }
                .thenByDescending { it.utilizationPercent }
        )
    }

    fun computeStats(alerts: List<RegulatoryThresholdAlert>): RegulatoryThresholdStats {
        return RegulatoryThresholdStats(
            totalAlertsCount = alerts.size,
            approachingCount = alerts.count { it.status == ThresholdAlertStatus.APPROACHING_THRESHOLD },
            exceededCount = alerts.count { it.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED },
            payrollAlertsCount = alerts.count { it.category == ThresholdCategory.PAYROLL_EXPENSES },
            transferAlertsCount = alerts.count { it.category == ThresholdCategory.CROSS_BORDER_TRANSFER },
            acknowledgedCount = alerts.count { it.status == ThresholdAlertStatus.ACKNOWLEDGED || it.status == ThresholdAlertStatus.REPORT_FILED }
        )
    }
}
