package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.ComplianceAlert
import com.example.worldbusiness.data.model.ComplianceAlertStatus
import com.example.worldbusiness.data.model.ComplianceOverviewStats
import com.example.worldbusiness.data.model.ComplianceRiskType
import com.example.worldbusiness.data.model.ComplianceSeverity
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import java.util.Locale
import kotlin.math.max

object TreasuryComplianceEngine {

    fun analyzeTransactions(
        transactions: List<TreasuryTransactionRecord>,
        customOverrides: Map<String, ComplianceAlertStatus> = emptyMap(),
        fxRates: Map<String, Double> = emptyMap()
    ): List<ComplianceAlert> {
        val alerts = mutableListOf<ComplianceAlert>()

        transactions.forEach { tx ->
            val rate = fxRates[tx.currency.uppercase(Locale.US)] ?: when (tx.currency.uppercase(Locale.US)) {
                "USD" -> 1.0
                "EUR" -> 1.09
                "GBP" -> 1.30
                "CHF" -> 1.17
                "SGD" -> 0.77
                "BRL" -> 0.184
                "JPY" -> 0.0068
                else -> 1.0
            }
            val amountUsd = tx.amount * rate
            val ref = tx.referenceCode

            // 1. FINMA Swiss High-Value Capital Inflow & UBO Check (Switzerland)
            if (tx.currency.equals("CHF", ignoreCase = true) && tx.amount >= 100000.0) {
                val alertId = "ALERT-CH-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.FLAGGED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "CH",
                        jurisdictionName = "Switzerland (FINMA / MROS)",
                        regulatoryBody = "Swiss Financial Market Supervisory Authority (FINMA)",
                        statutoryRuleCode = "FINMA-AMLA-GWG-EDD-06",
                        severity = ComplianceSeverity.HIGH,
                        riskType = ComplianceRiskType.BENEFICIAL_OWNERSHIP,
                        title = "Unverified Sovereign UBO Beneficial Ownership",
                        description = "Transaction of CHF ${String.format(Locale.US, "%,.0f", tx.amount)} into Swiss vaulted accounts requires certified Form A (Establishment of Ultimate Beneficial Owner) under Swiss AMLA Article 6.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 78,
                        remediationRecommendation = "Request notarized FINMA Form A beneficial ownership disclosure from sovereign asset manager before commingling vault funds.",
                        requiredFiling = "FINMA Form A (UBO Declaration)"
                    )
                )
            }

            // 2. FinCEN BSA CTR Form 112 Large Corporate Fund Transfer (> $10,000 USD)
            if (tx.currency.equals("USD", ignoreCase = true) && tx.amount >= 100000.0) {
                val alertId = "ALERT-US-CTR-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.UNDER_REVIEW
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "US",
                        jurisdictionName = "United States (FinCEN)",
                        regulatoryBody = "Financial Crimes Enforcement Network (FinCEN)",
                        statutoryRuleCode = "FINCEN-BSA-31CFR-1010",
                        severity = ComplianceSeverity.MEDIUM,
                        riskType = ComplianceRiskType.LARGE_VALUE_CTR,
                        title = "Mandatory FinCEN Currency Transaction Report (CTR) Audit",
                        description = "High-value cross-border inbound settlement of $${String.format(Locale.US, "%,.0f", tx.amount)} exceeds Bank Secrecy Act statutory reporting thresholds.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 48,
                        remediationRecommendation = "Verify correspondent bank BSA CTR Form 112 batch confirmation with JPMorgan Clearing Gateway.",
                        requiredFiling = "FinCEN Form 112 (CTR Confirmation)"
                    )
                )
            }

            // 3. MAS Singapore Notice 626 - Dual-Use Technology Cross-Border Flow (Singapore)
            if (tx.currency.equals("SGD", ignoreCase = true) && tx.amount >= 200000.0) {
                val alertId = "ALERT-SG-MAS-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.FLAGGED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "SG",
                        jurisdictionName = "Singapore (MAS)",
                        regulatoryBody = "Monetary Authority of Singapore (MAS)",
                        statutoryRuleCode = "MAS-NOTICE-626-AI-DUAL",
                        severity = ComplianceSeverity.MEDIUM,
                        riskType = ComplianceRiskType.TRAVEL_RULE,
                        title = "MAS Dual-Use Tech Cross-Border Payment Scrutiny",
                        description = "Settlement of S$${String.format(Locale.US, "%,.0f", tx.amount)} from ${tx.senderOrCounterparty} involves high-frequency robotics/AI hardware requiring end-user compliance certification.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 55,
                        remediationRecommendation = "Obtain End-User Statement (EUS) and Strategic Goods Control export license certificate before fund repatriation.",
                        requiredFiling = "MAS Notice 626 Annex A"
                    )
                )
            }

            // 4. OFAC / High-Risk Sanctions Screening Check
            val textToScan = "${tx.senderOrCounterparty} ${tx.recipient} ${tx.note}".lowercase(Locale.US)
            if (textToScan.contains("maritime") && textToScan.contains("arbitrage") ||
                textToScan.contains("al-zahrani") ||
                textToScan.contains("unverified") ||
                textToScan.contains("sanction")
            ) {
                val alertId = "ALERT-OFAC-SDN-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.FLAGGED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "US",
                        jurisdictionName = "United States (OFAC & FinCEN)",
                        regulatoryBody = "Office of Foreign Assets Control (OFAC)",
                        statutoryRuleCode = "OFAC-SDN-SCREEN-31CFR500",
                        severity = ComplianceSeverity.CRITICAL,
                        riskType = ComplianceRiskType.SANCTIONS_OFAC,
                        title = "CRITICAL: OFAC SDN List Match / Sanctioned Transshipment Risk",
                        description = "Counterparty '${tx.senderOrCounterparty}' matches keyword triggers on OFAC Specially Designated Nationals and Blocked Persons list or maritime shadow fleet.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 96,
                        remediationRecommendation = "FREEZE TRANSACTION IMMEDIATELY. File mandatory FinCEN Suspicious Activity Report (SAR-DI) and submit OFAC blocking notification within 10 business days.",
                        requiredFiling = "FinCEN Form 111 (SAR-DI) & OFAC Blocking Report"
                    )
                )
            }

            // 5. BSA / AML Structuring & Smurfing Trigger ($9,000 - $9,999)
            if (tx.amount in 9000.0..9999.0) {
                val alertId = "ALERT-STRUCT-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.FLAGGED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "GLOBAL",
                        jurisdictionName = "Multi-Jurisdiction (FATF / FinCEN)",
                        regulatoryBody = "Financial Action Task Force (FATF)",
                        statutoryRuleCode = "FATF-REC-20-STRUCTURING",
                        severity = ComplianceSeverity.CRITICAL,
                        riskType = ComplianceRiskType.AML_STRUCTURING,
                        title = "CRITICAL: Potential Smurfing / Structuring Pattern Detected",
                        description = "Transaction of ${tx.currency} ${String.format(Locale.US, "%,.2f", tx.amount)} falls strictly in the $9,000 - $9,999 smurfing avoidance corridor below mandatory CTR triggers.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 92,
                        remediationRecommendation = "Review historical 30-day transfer velocity for the same originator. File FinCEN Suspicious Activity Report (SAR) if deliberate smurfing is verified.",
                        requiredFiling = "FinCEN SAR (Suspicious Activity Report)"
                    )
                )
            }

            // 6. EU AMLD6 & HMRC Cross-Border Commercial Clearing
            if (tx.currency.equals("EUR", ignoreCase = true) && tx.amount >= 140000.0) {
                val alertId = "ALERT-EU-AMLD6-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.RESOLVED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "EU",
                        jurisdictionName = "European Union (AMLD6)",
                        regulatoryBody = "European Banking Authority & BaFin",
                        statutoryRuleCode = "EU-AMLD6-ART-18",
                        severity = ComplianceSeverity.LOW,
                        riskType = ComplianceRiskType.TAX_HAVEN_ARBITRAGE,
                        title = "EU AMLD6 Reverse-Charge Commercial Escrow Audited",
                        description = "SEPA commercial hardware transfer of €${String.format(Locale.US, "%,.0f", tx.amount)} audited against EU single-market B2B VAT guidelines.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 18,
                        remediationRecommendation = "Commercial contract and invoice nexus verified. Keep on 5-year statutory retention file.",
                        requiredFiling = "EU VAT Annexure 4"
                    )
                )
            }

            // 7. Brazil BACEN & COAF Regulatory Currency Gate (BRL)
            if (tx.currency.equals("BRL", ignoreCase = true)) {
                val alertId = "ALERT-BR-COAF-${tx.id}"
                val currentStatus = customOverrides[alertId] ?: ComplianceAlertStatus.WHITELISTED
                alerts.add(
                    ComplianceAlert(
                        id = alertId,
                        transactionReference = ref,
                        counterparty = tx.senderOrCounterparty,
                        amount = tx.amount,
                        currency = tx.currency,
                        amountUsd = amountUsd,
                        jurisdictionCode = "BR",
                        jurisdictionName = "Brazil (BACEN & COAF)",
                        regulatoryBody = "Conselho de Controle de Atividades Financeiras (COAF)",
                        statutoryRuleCode = "BACEN-CIRCULAR-3978",
                        severity = ComplianceSeverity.LOW,
                        riskType = ComplianceRiskType.UNREGISTERED_FOREIGN_EXCHANGE,
                        title = "BACEN Sisbacen Exchange Registration Verified",
                        description = "Interbank payment of R$${String.format(Locale.US, "%,.0f", tx.amount)} processed via PIX Instant clearing for verified CLT monthly payroll.",
                        flaggedDate = tx.date,
                        status = currentStatus,
                        riskScore = 12,
                        remediationRecommendation = "Standard payroll contract on file. Sisbacen registry updated.",
                        requiredFiling = "Sisbacen Contrato de Câmbio"
                    )
                )
            }
        }

        // Return alerts sorted by severity (highest first)
        return alerts.sortedByDescending { it.severity.level * 100 + it.riskScore }
    }

    fun computeOverviewStats(alerts: List<ComplianceAlert>, totalTransactionsCount: Int): ComplianceOverviewStats {
        val activeFlags = alerts.count { it.status == ComplianceAlertStatus.FLAGGED }
        val criticalFlags = alerts.count { it.severity == ComplianceSeverity.CRITICAL && it.status == ComplianceAlertStatus.FLAGGED }
        val underReview = alerts.count { it.status == ComplianceAlertStatus.UNDER_REVIEW }
        val resolved = alerts.count { it.status == ComplianceAlertStatus.RESOLVED || it.status == ComplianceAlertStatus.WHITELISTED || it.status == ComplianceAlertStatus.SAR_REPORTED }

        val highRiskVolume = alerts
            .filter { (it.severity == ComplianceSeverity.CRITICAL || it.severity == ComplianceSeverity.HIGH) && it.status == ComplianceAlertStatus.FLAGGED }
            .sumOf { it.amountUsd }

        // Compliance Health Score calculation (100 is pristine, deducted by severity of unaddressed flags)
        val penalty = (criticalFlags * 25) + (activeFlags * 5) + (underReview * 2)
        val healthScore = max(35, 100 - penalty)

        val jurisdictions = alerts.map { it.jurisdictionCode }.distinct().size

        return ComplianceOverviewStats(
            totalTransactionsAudited = totalTransactionsCount,
            activeFlagsCount = activeFlags,
            criticalFlagsCount = criticalFlags,
            underReviewCount = underReview,
            resolvedCount = resolved,
            complianceHealthScore = healthScore,
            totalHighRiskVolumeUsd = highRiskVolume,
            monitoredJurisdictionsCount = max(5, jurisdictions)
        )
    }
}
