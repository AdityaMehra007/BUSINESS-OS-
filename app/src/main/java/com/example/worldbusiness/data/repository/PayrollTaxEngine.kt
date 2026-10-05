package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.EmployeePayrollDetail
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.JurisdictionSummary
import com.example.worldbusiness.data.model.TaxItemDeduction
import com.example.worldbusiness.data.model.TeamMemberRecord
import java.util.Locale
import kotlin.math.min

object PayrollTaxEngine {

    fun calculateEmployeePayroll(
        member: TeamMemberRecord,
        rateToUsd: Double
    ): EmployeePayrollDetail {
        val gross = member.monthlyCompensation.coerceAtLeast(0.0)
        val code = member.countryCode.uppercase(Locale.US)
        val isContractor = member.employmentType.contains("Contractor", ignoreCase = true)

        val deductions = mutableListOf<TaxItemDeduction>()
        var incomeTax = 0.0
        var socialSecurity = 0.0
        var otherDeductions = 0.0
        var employerContribution = 0.0
        var authority = "National Tax Bureau"
        var statutoryNotes = "Standard statutory payroll withholding schedule"
        var clearingRail = "ISO 20022 Cross-Border Rail"

        val symbol = getCurrencySymbol(member.currency)

        if (isContractor) {
            // Independent contractor / B2B consulting
            // Statutory withholding is 0%; contractor is responsible for self-remittance under W-8BEN / local law
            authority = when (code) {
                "US" -> "IRS Form 1099-NEC / Schedule C"
                "JP" -> "Japan National Tax Agency (WHT exempt B2B)"
                "GB" -> "HMRC Off-Payroll Working (IR35 Exempt)"
                else -> "Local Tax Authority / Self-Employed Direct"
            }
            statutoryNotes = "B2B Contractor Agreement: 0% statutory withholding at source. Reverse-charge / self-remitted invoicing."
            clearingRail = when (code) {
                "JP" -> "BOJ-NET / Zengin Commercial Wire"
                "US" -> "Fedwire Commercial Payment"
                else -> "SWIFT MT103 Corporate Wire"
            }
        } else {
            // W-2 / PAYE / Permanent Employee Withholdings
            when (code) {
                "US" -> {
                    authority = "IRS (Federal) & New York State Dept. of Taxation"
                    statutoryNotes = "IRS 2026 Progressive FIT Brackets + FICA OASDI (6.2%) & Medicare (1.45%) + NY State Withholding"
                    clearingRail = "US NACHA Direct Deposit (FedACH)"

                    val fitRate = 22.0
                    val fitAmount = gross * (fitRate / 100.0)
                    deductions.add(TaxItemDeduction("Federal Income Tax (FIT)", "INCOME_TAX", fitRate, fitAmount, "IRS Circular E Bracket"))

                    val ficaSsRate = 6.2
                    val ficaSsAmount = gross * (ficaSsRate / 100.0)
                    deductions.add(TaxItemDeduction("FICA Social Security (OASDI)", "SOCIAL_CONTRIBUTION", ficaSsRate, ficaSsAmount, "Social Security Trust"))

                    val ficaMedRate = 1.45
                    val ficaMedAmount = gross * (ficaMedRate / 100.0)
                    deductions.add(TaxItemDeduction("FICA Medicare (HI)", "SOCIAL_CONTRIBUTION", ficaMedRate, ficaMedAmount, "Medicare Hospital Insurance"))

                    val stateRate = 6.5
                    val stateAmount = gross * (stateRate / 100.0)
                    deductions.add(TaxItemDeduction("State Income Tax (SIT)", "INCOME_TAX", stateRate, stateAmount, "State Resident Withholding"))

                    incomeTax = fitAmount + stateAmount
                    socialSecurity = ficaSsAmount + ficaMedAmount
                    employerContribution = gross * 0.0825 // Matching FICA + FUTA/SUTA
                }
                "GB" -> {
                    authority = "HM Revenue & Customs (HMRC)"
                    statutoryNotes = "HMRC PAYE Tax Code 1257L + Class 1 National Insurance Primary Contributions (NIC)"
                    clearingRail = "UK BACS Direct Credit & Faster Payments Service"

                    val payeRate = 25.0
                    val payeAmount = gross * (payeRate / 100.0)
                    deductions.add(TaxItemDeduction("HMRC PAYE Income Tax", "INCOME_TAX", payeRate, payeAmount, "Higher/Basic Rate PAYE"))

                    val nicRate = 8.0
                    val nicAmount = gross * (nicRate / 100.0)
                    deductions.add(TaxItemDeduction("National Insurance (NIC)", "SOCIAL_CONTRIBUTION", nicRate, nicAmount, "Class 1 Primary Contribution"))

                    val pensionRate = 3.0
                    val pensionAmount = gross * (pensionRate / 100.0)
                    deductions.add(TaxItemDeduction("Workplace Pension (Auto-Enrolment)", "PENSION_HEALTH", pensionRate, pensionAmount, "Qualifying Pension Scheme"))

                    incomeTax = payeAmount
                    socialSecurity = nicAmount
                    otherDeductions = pensionAmount
                    employerContribution = gross * 0.138 // Employer NIC Secondary (13.8%)
                }
                "CH" -> {
                    authority = "Swiss Federal Tax Administration (FTA) & Zurich Cantonal Tax Office"
                    statutoryNotes = "Quellensteuer (Withholding Tax at Source Tariff A) + AHV/IV/EO 1st Pillar + BVG 2nd Pillar"
                    clearingRail = "Swiss Interbank Clearing (SIC / RTGS ISO 20022)"

                    val taxRate = 13.5
                    val taxAmount = gross * (taxRate / 100.0)
                    deductions.add(TaxItemDeduction("Quellensteuer (Direct Tax)", "INCOME_TAX", taxRate, taxAmount, "Federal & Cantonal Withholding"))

                    val ahvRate = 5.3
                    val ahvAmount = gross * (ahvRate / 100.0)
                    deductions.add(TaxItemDeduction("AHV/IV/EO (Old Age/Disability)", "SOCIAL_CONTRIBUTION", ahvRate, ahvAmount, "1st Pillar Social Security"))

                    val alvRate = 1.1
                    val alvAmount = gross * (alvRate / 100.0)
                    deductions.add(TaxItemDeduction("ALV (Unemployment Insurance)", "SOCIAL_CONTRIBUTION", alvRate, alvAmount, "Federal Unemployment Fund"))

                    val bvgRate = 3.5
                    val bvgAmount = gross * (bvgRate / 100.0)
                    deductions.add(TaxItemDeduction("BVG (Occupational Pension)", "PENSION_HEALTH", bvgRate, bvgAmount, "2nd Pillar Pension Fund"))

                    incomeTax = taxAmount
                    socialSecurity = ahvAmount + alvAmount
                    otherDeductions = bvgAmount
                    employerContribution = gross * 0.115
                }
                "SG" -> {
                    authority = "Inland Revenue Authority of Singapore (IRAS) & Central Provident Fund Board"
                    statutoryNotes = "IRAS Progressive Resident Tax Schedule + CPF Statutory Scheme (20% employee contribution, capped)"
                    clearingRail = "Singapore FAST (Fast and Secure Transfers) / MEPS"

                    val irasRate = 11.5
                    val irasAmount = gross * (irasRate / 100.0)
                    deductions.add(TaxItemDeduction("IRAS Individual Income Tax", "INCOME_TAX", irasRate, irasAmount, "Resident Progressive Withholding"))

                    // CPF Employee share: 20% capped at S$1,360
                    val cpfAmount = min(gross * 0.20, 1360.0)
                    val cpfEffectiveRate = (cpfAmount / gross) * 100.0
                    deductions.add(TaxItemDeduction("CPF Employee Contribution", "SOCIAL_CONTRIBUTION", cpfEffectiveRate, cpfAmount, "Central Provident Fund (OA/SA/MA)"))

                    incomeTax = irasAmount
                    socialSecurity = cpfAmount
                    employerContribution = min(gross * 0.17, 1156.0) // CPF Employer 17%
                }
                "BR" -> {
                    authority = "Receita Federal do Brasil & Ministério do Trabalho (CLT)"
                    statutoryNotes = "IRPF na Fonte Tabela Progressiva (27.5%) + INSS Previdência Social (11%) + FGTS Empregador"
                    clearingRail = "Banco Central do Brasil PIX Instant / TED"

                    val irpfRate = 22.5
                    val irpfAmount = gross * (irpfRate / 100.0)
                    deductions.add(TaxItemDeduction("IRPF Retido na Fonte", "INCOME_TAX", irpfRate, irpfAmount, "Receita Federal Imposto de Renda"))

                    val inssRate = 11.0
                    val inssAmount = gross * (inssRate / 100.0)
                    deductions.add(TaxItemDeduction("INSS Previdência Social", "SOCIAL_CONTRIBUTION", inssRate, inssAmount, "Seguridade Social do Trabalhador"))

                    incomeTax = irpfAmount
                    socialSecurity = inssAmount
                    employerContribution = gross * 0.28 // FGTS (8%) + INSS Patronal (20%)
                }
                "DE" -> {
                    authority = "Bundeszentralamt für Steuern (BZSt) & Deutsche Rentenversicherung"
                    statutoryNotes = "Lohnsteuer Klasse I + SolZ + GKV (Health) + GRV (Pension) + PV (Long-term Care) + AV (Unemployment)"
                    clearingRail = "Deutsche Bundesbank SEPA Instant Credit Transfer"

                    val lstRate = 26.5
                    val lstAmount = gross * (lstRate / 100.0)
                    deductions.add(TaxItemDeduction("Lohnsteuer & SolZ", "INCOME_TAX", lstRate, lstAmount, "Tarifliche Einkommensteuer"))

                    val rvRate = 9.3
                    val rvAmount = gross * (rvRate / 100.0)
                    deductions.add(TaxItemDeduction("Gesetzliche Rentenversicherung (RV)", "SOCIAL_CONTRIBUTION", rvRate, rvAmount, "Altersvorsorge Rentenfonds"))

                    val kvRate = 8.8
                    val kvAmount = gross * (kvRate / 100.0)
                    deductions.add(TaxItemDeduction("Kranken- & Pflegeversicherung (KV/PV)", "PENSION_HEALTH", kvRate, kvAmount, "Gesetzliche Krankenkasse"))

                    incomeTax = lstAmount
                    socialSecurity = rvAmount
                    otherDeductions = kvAmount
                    employerContribution = gross * 0.198
                }
                "JP" -> {
                    authority = "National Tax Agency of Japan (NTA) & Japan Pension Service"
                    statutoryNotes = "Shotokuzei (Withholding Income Tax) + Shakai Hoken (Kenko Hoken & Kosei Nenkin)"
                    clearingRail = "Bank of Japan BOJ-NET / Zengin Clearing"

                    val taxRate = 14.0
                    val taxAmount = gross * (taxRate / 100.0)
                    deductions.add(TaxItemDeduction("Shotokuzei (Income Tax)", "INCOME_TAX", taxRate, taxAmount, "NTA Statutory Table"))

                    val shakaiRate = 14.5
                    val shakaiAmount = gross * (shakaiRate / 100.0)
                    deductions.add(TaxItemDeduction("Shakai Hoken (Health & Pension)", "SOCIAL_CONTRIBUTION", shakaiRate, shakaiAmount, "Social Insurance Scheme"))

                    incomeTax = taxAmount
                    socialSecurity = shakaiAmount
                    employerContribution = gross * 0.155
                }
                else -> {
                    // Standard international baseline for any other country
                    authority = "${member.country} Revenue Agency"
                    statutoryNotes = "Standard Statutory Income Tax Withholding + National Social Security Scheme"
                    clearingRail = "SWIFT MT103 Interbank Direct Wire"

                    val taxRate = 20.0
                    val taxAmount = gross * (taxRate / 100.0)
                    deductions.add(TaxItemDeduction("National Income Tax", "INCOME_TAX", taxRate, taxAmount, "Statutory Withholding"))

                    val socRate = 8.0
                    val socAmount = gross * (socRate / 100.0)
                    deductions.add(TaxItemDeduction("Social Security Contribution", "SOCIAL_CONTRIBUTION", socRate, socAmount, "National Social Fund"))

                    incomeTax = taxAmount
                    socialSecurity = socAmount
                    employerContribution = gross * 0.10
                }
            }
        }

        val totalWithheld = incomeTax + socialSecurity + otherDeductions
        val effectiveRate = if (gross > 0.0) (totalWithheld / gross) * 100.0 else 0.0
        val netPay = (gross - totalWithheld).coerceAtLeast(0.0)

        val grossUsd = gross * rateToUsd
        val withheldUsd = totalWithheld * rateToUsd
        val netUsd = netPay * rateToUsd

        return EmployeePayrollDetail(
            memberId = member.id,
            fullName = member.fullName,
            role = member.role,
            country = member.country,
            countryCode = member.countryCode,
            employmentType = member.employmentType,
            currency = member.currency,
            currencySymbol = symbol,
            grossPayLocal = gross,
            incomeTaxLocal = incomeTax,
            socialSecurityLocal = socialSecurity,
            otherDeductionsLocal = otherDeductions,
            totalTaxWithheldLocal = totalWithheld,
            effectiveTaxRatePercent = effectiveRate,
            netPayLocal = netPay,
            rateToUsd = rateToUsd,
            grossPayUsd = grossUsd,
            totalTaxWithheldUsd = withheldUsd,
            netPayUsd = netUsd,
            taxJurisdiction = member.taxJurisdiction,
            taxAuthority = authority,
            statutoryNotes = statutoryNotes,
            deductions = deductions,
            employerContributionLocal = employerContribution,
            paymentStatus = member.status,
            clearingRail = clearingRail
        )
    }

    fun calculateGlobalPayrollSummary(
        members: List<TeamMemberRecord>,
        fxBalances: List<FxBalanceRecord>
    ): GlobalPayrollSummary {
        val details = members.map { member ->
            val rate = fxBalances.find { it.currencyCode.equals(member.currency, ignoreCase = true) }?.rateToUsd ?: 1.0
            calculateEmployeePayroll(member, rate)
        }

        val totalGrossUsd = details.sumOf { it.grossPayUsd }
        val totalWithheldUsd = details.sumOf { it.totalTaxWithheldUsd }
        val totalNetUsd = details.sumOf { it.netPayUsd }
        val overallEffectiveRate = if (totalGrossUsd > 0.0) (totalWithheldUsd / totalGrossUsd) * 100.0 else 0.0

        // Group by country / jurisdiction
        val jurisdictionGroups = details.groupBy { it.countryCode }
        val jurisdictionSummaries = jurisdictionGroups.map { (code, empList) ->
            val first = empList.first()
            val totalGrossLocal = empList.sumOf { it.grossPayLocal }
            val totalWithheldLocal = empList.sumOf { it.totalTaxWithheldLocal }
            val totalNetLocal = empList.sumOf { it.netPayLocal }
            val grossUsd = empList.sumOf { it.grossPayUsd }
            val withheldUsd = empList.sumOf { it.totalTaxWithheldUsd }
            val netUsd = empList.sumOf { it.netPayUsd }
            val avgRate = if (totalGrossLocal > 0.0) (totalWithheldLocal / totalGrossLocal) * 100.0 else 0.0

            JurisdictionSummary(
                countryCode = code,
                countryName = first.country,
                currency = first.currency,
                currencySymbol = first.currencySymbol,
                employeeCount = empList.size,
                totalGrossLocal = totalGrossLocal,
                totalTaxWithheldLocal = totalWithheldLocal,
                totalNetPayLocal = totalNetLocal,
                totalGrossUsd = grossUsd,
                totalTaxWithheldUsd = withheldUsd,
                totalNetPayUsd = netUsd,
                averageEffectiveTaxRate = avgRate,
                primaryTaxAuthority = first.taxAuthority,
                complianceStandard = "Compliant • Statutory Remittance On Schedule"
            )
        }

        return GlobalPayrollSummary(
            totalEmployees = members.size,
            totalGrossUsd = totalGrossUsd,
            totalTaxWithheldUsd = totalWithheldUsd,
            totalNetPayUsd = totalNetUsd,
            overallEffectiveTaxRate = overallEffectiveRate,
            jurisdictionCount = jurisdictionSummaries.size,
            jurisdictions = jurisdictionSummaries,
            employeeDetails = details
        )
    }

    private fun getCurrencySymbol(curr: String): String {
        return when (curr.uppercase(Locale.US)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "SGD" -> "S$"
            "CHF" -> "CHF "
            "JPY" -> "¥"
            "BRL" -> "R$"
            "AUD" -> "A$"
            "CAD" -> "C$"
            else -> "$curr "
        }
    }
}
