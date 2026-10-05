package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.JurisdictionRegulatoryOverview
import com.example.worldbusiness.data.model.RegulatoryTaxDeadlineItem
import com.example.worldbusiness.data.model.TaxDeadlineUrgency
import com.example.worldbusiness.data.model.TaxRequirementType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

object CorporateTaxComplianceEngine {

    // Reference enterprise timestamp: Oct 01, 2026
    private const val REFERENCE_TIME_MILLIS = 1790841600000L // 2026-10-01

    /**
     * Pulls upcoming tax filing deadlines and regulatory reporting requirements
     * for all jurisdictions where the corporation operates based on registered corporate entities.
     */
    fun generateDeadlines(entities: List<EntityRecord>): List<RegulatoryTaxDeadlineItem> {
        val deadlines = mutableListOf<RegulatoryTaxDeadlineItem>()

        // Fallback default list if entities list is empty
        val effectiveEntities = if (entities.isNotEmpty()) entities else defaultEntities

        effectiveEntities.forEach { entity ->
            val countryCode = entity.countryCode.uppercase(Locale.US)
            val itemsForEntity = getStatutoryRequirementsForEntity(entity, countryCode)
            deadlines.addAll(itemsForEntity)
        }

        // Sort by urgency: shortest days remaining first
        return deadlines.sortedBy { it.daysRemaining }
    }

    fun computeOverview(
        deadlines: List<RegulatoryTaxDeadlineItem>,
        entities: List<EntityRecord>
    ): JurisdictionRegulatoryOverview {
        val upcoming30Days = deadlines.count { it.daysRemaining in 0..30 && it.complianceStatus != "FILED_CONFIRMED" }
        val urgentAction = deadlines.count { it.daysRemaining <= 15 && it.complianceStatus != "FILED_CONFIRMED" }
        val totalTaxDue = deadlines.filter { it.complianceStatus != "FILED_CONFIRMED" }.sumOf { it.estimatedTaxLiabilityUsd ?: 0.0 }
        val totalPenalties = deadlines.filter { it.complianceStatus != "FILED_CONFIRMED" }.sumOf { it.potentialPenaltyRiskUsd }
        val avgCompliance = if (entities.isNotEmpty()) entities.map { it.complianceScore }.average().roundToInt() else 96
        val jurisdictionSet = deadlines.map { it.countryCode }.toSet()

        return JurisdictionRegulatoryOverview(
            totalFilingDeadlines = deadlines.size,
            upcomingIn30DaysCount = upcoming30Days,
            urgentActionCount = urgentAction,
            totalEstimatedTaxDueUsd = totalTaxDue,
            totalStatutoryPenaltyAtRiskUsd = totalPenalties,
            averageJurisdictionComplianceScore = avgCompliance,
            jurisdictionsCoveredCount = jurisdictionSet.size
        )
    }

    private fun getStatutoryRequirementsForEntity(
        entity: EntityRecord,
        countryCode: String
    ): List<RegulatoryTaxDeadlineItem> {
        val list = mutableListOf<RegulatoryTaxDeadlineItem>()

        when (countryCode) {
            "US" -> {
                // 1. IRS Form 1120 Extended Corporate Return
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-US-1120-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "US",
                        countryName = "United States",
                        countryFlag = "🇺🇸",
                        jurisdictionName = "United States (Delaware Federal)",
                        filingTitle = "IRS Form 1120 Extended Corporate Income Tax",
                        statutoryAuthority = "Internal Revenue Service (IRS)",
                        dueDateString = "Oct 15, 2026",
                        daysRemaining = 14,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 148500.0,
                        potentialPenaltyRiskUsd = 25000.0,
                        statutoryFormCode = "IRS-1120",
                        submissionMethod = "IRS Modernized e-File (MeF) Gateway",
                        localResidentSignatory = entity.localDirector.ifEmpty { "Wilmington Trust SPV" },
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "Final extended deadline for FY2025/2026 worldwide corporate income tax consolidation.",
                        requiredSchedules = listOf("Schedule M-3 (Book-to-Tax)", "Form 5472 (Foreign Ownership)", "Form 8990 (Sec 163j)"),
                        penalCodeReference = "IRC Sec. 6651(a)(1) • 5% per month late filing penalty up to 25%"
                    )
                )

                // 2. FinCEN BOI Annual Audit
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-US-BOI-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "US",
                        countryName = "United States",
                        countryFlag = "🇺🇸",
                        jurisdictionName = "United States (Federal AML)",
                        filingTitle = "FinCEN Beneficial Ownership Information (BOI)",
                        statutoryAuthority = "Financial Crimes Enforcement Network (FinCEN)",
                        dueDateString = "Dec 31, 2026",
                        daysRemaining = 91,
                        statutoryCategory = TaxRequirementType.BENEFICIAL_OWNERSHIP_AML,
                        estimatedTaxLiabilityUsd = 0.0,
                        potentialPenaltyRiskUsd = 10000.0,
                        statutoryFormCode = "FinCEN-BOIR",
                        submissionMethod = "FinCEN Secure Direct Portal",
                        localResidentSignatory = entity.localDirector,
                        complianceStatus = "PENDING_PREPARATION",
                        statutoryNotes = "Corporate Transparency Act statutory beneficial ownership annual verification.",
                        requiredSchedules = listOf("Beneficial Owners Identity Verification", "Company Applicant Proof", "Passport Escrow"),
                        penalCodeReference = "31 U.S.C. 5336 • $500 per day civil penalty"
                    )
                )
            }

            "CH" -> {
                // 1. Swiss Cantonal & Federal Tax
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-CH-CIT-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "CH",
                        countryName = "Switzerland",
                        countryFlag = "🇨🇭",
                        jurisdictionName = "Switzerland (Zurich / Federal)",
                        filingTitle = "Swiss Cantonal & Federal Corporate Tax Declaration",
                        statutoryAuthority = "Eidgenössische Steuerverwaltung (ESTV)",
                        dueDateString = "Nov 30, 2026",
                        daysRemaining = 60,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 82400.0,
                        potentialPenaltyRiskUsd = 15000.0,
                        statutoryFormCode = "ESTV-ZH-2026",
                        submissionMethod = "eTax.zh.ch Cantonal Portal",
                        localResidentSignatory = entity.localDirector.ifEmpty { "KPMG AG Zurich" },
                        complianceStatus = "PENDING_PREPARATION",
                        statutoryNotes = "Consolidated income & capital tax declaration including participation exemption relief.",
                        requiredSchedules = listOf("Auditor's Report (Revisionsbericht)", "Erfolgsrechnung", "Participation Relief Matrix"),
                        penalCodeReference = "Swiss DBG Art. 175 • Fines up to CHF 10,000 plus interest"
                    )
                )

                // 2. Swiss MWST Q3 Return
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-CH-VAT-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "CH",
                        countryName = "Switzerland",
                        countryFlag = "🇨🇭",
                        jurisdictionName = "Switzerland (Federal MWST)",
                        filingTitle = "Swiss MWST / VAT Electronic Q3 Declaration",
                        statutoryAuthority = "ESTV Hauptabteilung MWST",
                        dueDateString = "Nov 30, 2026",
                        daysRemaining = 60,
                        statutoryCategory = TaxRequirementType.VAT_GST_SALES,
                        estimatedTaxLiabilityUsd = 12800.0,
                        potentialPenaltyRiskUsd = 3500.0,
                        statutoryFormCode = "MWST-Q3",
                        submissionMethod = "ESTV TaxMe Online Gateway",
                        localResidentSignatory = entity.localDirector,
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "Standard rate 8.1% domestic supply and zero-rated export reconciliation.",
                        requiredSchedules = listOf("Export Customs Declarations", "Input Tax Proof", "B2B Reverse Charge Ledger"),
                        penalCodeReference = "MWSTG Art. 98 • Administrative fines & default interest"
                    )
                )
            }

            "SG" -> {
                // 1. Singapore IRAS Form C-S
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-SG-CIT-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "SG",
                        countryName = "Singapore",
                        countryFlag = "🇸🇬",
                        jurisdictionName = "Singapore (IRAS)",
                        filingTitle = "IRAS Form C-S Corporate Income Tax (YA 2026)",
                        statutoryAuthority = "Inland Revenue Authority of Singapore (IRAS)",
                        dueDateString = "Nov 30, 2026",
                        daysRemaining = 60,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 65200.0,
                        potentialPenaltyRiskUsd = 10000.0,
                        statutoryFormCode = "IRAS-Form-CS",
                        submissionMethod = "IRAS myTax Portal (CorpPass)",
                        localResidentSignatory = entity.localDirector.ifEmpty { "Rajah & Tann Corporate Services" },
                        complianceStatus = "PENDING_PREPARATION",
                        statutoryNotes = "17% headline CIT with partial tax exemption and regional tech IP incentive claim.",
                        requiredSchedules = listOf("Tax Computation Schedules", "Capital Allowance Additions", "Foreign Tax Credit Relief"),
                        penalCodeReference = "Singapore Income Tax Act 1947 Sec. 94 • Strict late filing penalties"
                    )
                )

                // 2. Singapore GST F5 Q3
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-SG-GST-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "SG",
                        countryName = "Singapore",
                        countryFlag = "🇸🇬",
                        jurisdictionName = "Singapore (Customs & GST)",
                        filingTitle = "Singapore GST F5 Quarterly Return (9.0%)",
                        statutoryAuthority = "IRAS Goods and Services Tax Division",
                        dueDateString = "Oct 31, 2026",
                        daysRemaining = 30,
                        statutoryCategory = TaxRequirementType.VAT_GST_SALES,
                        estimatedTaxLiabilityUsd = 31400.0,
                        potentialPenaltyRiskUsd = 5000.0,
                        statutoryFormCode = "GST-F5-Q3",
                        submissionMethod = "myTax Portal e-Filing",
                        localResidentSignatory = entity.localDirector,
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "Statutory 9% GST on taxable supplies and zero-rated international freight services.",
                        requiredSchedules = listOf("Box 1 Total Taxable Supplies", "Box 5 Net GST Due", "Major Exporter Scheme MES Audit"),
                        penalCodeReference = "GST Act Sec. 59 • 5% penalty + $200 per month"
                    )
                )

                // 3. ACRA Annual Return & XBRL
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-SG-ACRA-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "SG",
                        countryName = "Singapore",
                        countryFlag = "🇸🇬",
                        jurisdictionName = "Singapore (ACRA Registry)",
                        filingTitle = "ACRA Annual Return & XBRL Financials",
                        statutoryAuthority = "Accounting & Corporate Regulatory Authority (ACRA)",
                        dueDateString = "Oct 20, 2026",
                        daysRemaining = 19,
                        statutoryCategory = TaxRequirementType.ANNUAL_CORPORATE_RETURN,
                        estimatedTaxLiabilityUsd = 0.0,
                        potentialPenaltyRiskUsd = 2500.0,
                        statutoryFormCode = "ACRA-AR-XBRL",
                        submissionMethod = "ACRA BizFile+ Direct Filing",
                        localResidentSignatory = entity.localDirector,
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "Annual statutory filing of audited accounts in full XBRL format within 7 months of AGM.",
                        requiredSchedules = listOf("XBRL Simplified Taxonomy", "Directors' Statement", "Audited Financial Statements"),
                        penalCodeReference = "Companies Act Sec. 197 • Up to $5,000 default penalty on officers"
                    )
                )
            }

            "GB" -> {
                // UK HMRC CT600
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-UK-CT600-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "GB",
                        countryName = "United Kingdom",
                        countryFlag = "🇬🇧",
                        jurisdictionName = "United Kingdom (HMRC)",
                        filingTitle = "HMRC CT600 Corporation Tax & iXBRL Accounts",
                        statutoryAuthority = "HM Revenue & Customs (HMRC)",
                        dueDateString = "Dec 31, 2026",
                        daysRemaining = 91,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 94000.0,
                        potentialPenaltyRiskUsd = 12000.0,
                        statutoryFormCode = "HMRC-CT600",
                        submissionMethod = "HMRC Corporation Tax Online Gateway",
                        localResidentSignatory = entity.localDirector.ifEmpty { "Slaughter and May Nominees" },
                        complianceStatus = "PENDING_PREPARATION",
                        statutoryNotes = "Main corporate tax rate 25% with full iXBRL tagged tagged financial reporting.",
                        requiredSchedules = listOf("CT600 Computations", "R&D Expenditure Credit (RDEC)", "Capital Allowances Super-Deduction"),
                        penalCodeReference = "Finance Act 1998 Sched. 18 • Incremental late payment interest"
                    )
                )
            }

            "DE" -> {
                // Germany Corporate Tax (KSt) & Trade Tax (GewSt)
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-DE-KST-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "DE",
                        countryName = "Germany",
                        countryFlag = "🇩🇪",
                        jurisdictionName = "Germany (Finanzamt Bavaria)",
                        filingTitle = "Körperschaftsteuer & Gewerbesteuer (KSt / GewSt)",
                        statutoryAuthority = "Finanzamt München / Bundeszentralamt für Steuern",
                        dueDateString = "Oct 31, 2026",
                        daysRemaining = 30,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 118000.0,
                        potentialPenaltyRiskUsd = 18000.0,
                        statutoryFormCode = "KSt-GewSt-2026",
                        submissionMethod = "ELSTER Online Secure API",
                        localResidentSignatory = entity.localDirector.ifEmpty { "PwC Deutschland GmbH" },
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "15% KSt + 5.5% Solidaritätszuschlag + municipal trade tax GewSt multiplier.",
                        requiredSchedules = listOf("E-Bilanz (Electronic Balance Sheet)", "AStG CFC Income Inclusion", "Tax Reconciliation"),
                        penalCodeReference = "Abgabenordnung § 152 • Verspätungszuschlag (Late filing surcharge)"
                    )
                )
            }

            "BR" -> {
                // Brazil ECF / SPED Fiscal
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-BR-ECF-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = "BR",
                        countryName = "Brazil",
                        countryFlag = "🇧🇷",
                        jurisdictionName = "Brazil (Receita Federal)",
                        filingTitle = "Receita Federal ECF (Escrituração Contábil Fiscal)",
                        statutoryAuthority = "Secretaria da Receita Federal do Brasil (RFB)",
                        dueDateString = "Nov 15, 2026",
                        daysRemaining = 45,
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 42000.0,
                        potentialPenaltyRiskUsd = 8000.0,
                        statutoryFormCode = "SPED-ECF-2026",
                        submissionMethod = "ReceitaNet SPED Java Client",
                        localResidentSignatory = entity.localDirector.ifEmpty { "Mattos Filho Corporate Legal" },
                        complianceStatus = "PENDING_PREPARATION",
                        statutoryNotes = "IRPJ (Corporate Income Tax) and CSLL (Social Contribution on Net Profit) e-Lalur reconciliation.",
                        requiredSchedules = listOf("Bloco M (e-Lalur e-Lacs)", "Bloco X (Foreign Transactions)", "Preço de Transferência"),
                        penalCodeReference = "Lei 12.973/2014 • Penalty up to 3% of gross revenue"
                    )
                )
            }

            else -> {
                // Generic Global Corporate Return
                list.add(
                    RegulatoryTaxDeadlineItem(
                        id = "TAX-GEN-${entity.id}",
                        entityId = entity.id,
                        entityName = entity.name,
                        countryCode = countryCode,
                        countryName = entity.jurisdiction,
                        countryFlag = "🌐",
                        jurisdictionName = entity.jurisdiction,
                        filingTitle = "Annual Corporate Statutory Tax Return",
                        statutoryAuthority = "Ministry of Finance / Sovereign Tax Authority",
                        dueDateString = entity.annualFilingDeadline.ifEmpty { "Nov 30, 2026" },
                        daysRemaining = calculateDaysToDue(entity.annualFilingDeadline),
                        statutoryCategory = TaxRequirementType.CORPORATE_INCOME_TAX,
                        estimatedTaxLiabilityUsd = 25000.0,
                        potentialPenaltyRiskUsd = 5000.0,
                        statutoryFormCode = "STAT-CIT-2026",
                        submissionMethod = "Statutory e-Tax Portal",
                        localResidentSignatory = entity.localDirector,
                        complianceStatus = "READY_FOR_FILING",
                        statutoryNotes = "Annual statutory filing of audited accounts and corporate tax assessment.",
                        requiredSchedules = listOf("Financial Statements", "Auditors' Opinion", "Tax Computation"),
                        penalCodeReference = "Statutory Corporate Tax Act Penal Clauses"
                    )
                )
            }
        }

        return list
    }

    private fun calculateDaysToDue(dueDateStr: String): Int {
        val formats = listOf(
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", Locale.US)
        )
        for (format in formats) {
            try {
                val date = format.parse(dueDateStr.trim())
                if (date != null) {
                    val diff = date.time - REFERENCE_TIME_MILLIS
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    return days.coerceAtLeast(1)
                }
            } catch (_: Exception) {
            }
        }
        return 45
    }

    private val defaultEntities = listOf(
        EntityRecord(
            id = 1,
            name = "OmniGlobal Holdings Inc.",
            jurisdiction = "United States (Delaware)",
            countryCode = "US",
            entityType = "C-Corp",
            taxId = "US-EIN-98-4421903",
            status = "GOOD_STANDING",
            baseCurrency = "USD",
            operatingCapital = 45000000.0,
            annualFilingDeadline = "2026-10-15",
            localDirector = "Sarah Jenkins (Esq.)",
            complianceScore = 98
        ),
        EntityRecord(
            id = 2,
            name = "OmniGlobal DACH GmbH",
            jurisdiction = "Switzerland (Zurich)",
            countryCode = "CH",
            entityType = "GmbH",
            taxId = "CHE-105.890.112 MWST",
            status = "GOOD_STANDING",
            baseCurrency = "CHF",
            operatingCapital = 18500000.0,
            annualFilingDeadline = "2026-11-30",
            localDirector = "Dr. Beat Meier",
            complianceScore = 95
        ),
        EntityRecord(
            id = 3,
            name = "OmniGlobal Singapore Pte. Ltd.",
            jurisdiction = "Singapore",
            countryCode = "SG",
            entityType = "Pte. Ltd.",
            taxId = "UEN: 202109842K",
            status = "GOOD_STANDING",
            baseCurrency = "SGD",
            operatingCapital = 22000000.0,
            annualFilingDeadline = "2026-10-20",
            localDirector = "Wei Ming Tan",
            complianceScore = 97
        ),
        EntityRecord(
            id = 4,
            name = "OmniGlobal UK Limited",
            jurisdiction = "United Kingdom (London)",
            countryCode = "GB",
            entityType = "Private Ltd.",
            taxId = "GB-VAT-902-1481-99",
            status = "GOOD_STANDING",
            baseCurrency = "GBP",
            operatingCapital = 12000000.0,
            annualFilingDeadline = "2026-12-31",
            localDirector = "Alistair Vance",
            complianceScore = 94
        )
    )
}
