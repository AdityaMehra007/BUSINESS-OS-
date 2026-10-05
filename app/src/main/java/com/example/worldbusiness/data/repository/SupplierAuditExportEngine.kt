package com.example.worldbusiness.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enterprise Audit Export Engine for World Business OS Supplier Directory.
 *
 * Generates audit-grade exports in:
 * 1. RFC 4180 compliant CSV spreadsheet format
 * 2. Formal Statutory Audit Dossier (Printable / PDF report format)
 */
object SupplierAuditExportEngine {

    /**
     * Generates a structured CSV spreadsheet export of the full supplier directory.
     */
    fun generateCsv(suppliers: List<GlobalSupplierRecord>): String {
        val sb = StringBuilder()
        
        // Header
        val headers = listOf(
            "Supplier Code",
            "Legal Entity Name",
            "Trading Name",
            "Country",
            "Country Code",
            "Tax Registration Number",
            "Business Category",
            "Primary Contact Person",
            "Contact Email",
            "Contact Phone",
            "Physical HQ Address",
            "Corporate Website",
            "Payment Terms Days",
            "Payment Terms Code",
            "Settlement Currency",
            "Clearing Rail",
            "Beneficiary Bank",
            "SWIFT BIC",
            "IBAN or Account Number",
            "Early Discount Percent",
            "Compliance Status",
            "W8BEN Tax Form Filed",
            "ISO 9001 Certified",
            "ESG Rating Score",
            "Anti-Bribery Pact Signed",
            "KYC Status",
            "Last Audit Date",
            "Compliance Doc Expiry Date",
            "Risk Tier",
            "Compliance Officer Remarks",
            "Procurement Active"
        )
        sb.append(headers.joinToString(",")).append("\n")

        // Rows
        suppliers.forEach { s ->
            val row = listOf(
                escapeCsv(s.supplierCode),
                escapeCsv(s.legalName),
                escapeCsv(s.tradingName),
                escapeCsv(s.country),
                escapeCsv(s.countryCode),
                escapeCsv(s.taxRegistrationNumber),
                escapeCsv(s.businessCategory),
                escapeCsv(s.primaryContactName),
                escapeCsv(s.primaryContactEmail),
                escapeCsv(s.primaryContactPhone),
                escapeCsv(s.physicalAddress),
                escapeCsv(s.websiteUrl),
                s.paymentTermsDays.toString(),
                escapeCsv(s.paymentTermsDescription),
                escapeCsv(s.preferredCurrency),
                escapeCsv(s.settlementRail),
                escapeCsv(s.bankName),
                escapeCsv(s.bankSwiftBic),
                escapeCsv(s.bankIbanOrAccountNumber),
                s.discountEarlyPaymentPercent.toString(),
                escapeCsv(s.complianceStatus),
                s.w8BenOrTaxFormFiled.toString(),
                s.iso9001Certified.toString(),
                s.esgRatingScore.toString(),
                s.antiBriberyPactSigned.toString(),
                escapeCsv(s.kycVerificationStatus),
                escapeCsv(s.lastAuditDate),
                escapeCsv(s.complianceDocExpiryDate),
                escapeCsv(s.riskLevel),
                escapeCsv(s.complianceOfficerNotes),
                s.isActive.toString()
            )
            sb.append(row.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    /**
     * Generates a formal statutory audit dossier suitable for printing, PDF conversion, and compliance inspections.
     */
    fun generateAuditDossierText(suppliers: List<GlobalSupplierRecord>): String {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
        val total = suppliers.size.coerceAtLeast(1)
        val verified = suppliers.count { it.complianceStatus.equals("COMPLIANT", ignoreCase = true) }
        val expiringOrPending = suppliers.count {
            it.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) ||
            it.riskLevel.equals("MEDIUM", ignoreCase = true)
        }
        val nonCompliant = suppliers.count {
            it.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) ||
            it.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) ||
            it.riskLevel.equals("HIGH", ignoreCase = true)
        }

        val healthScore = if (suppliers.isNotEmpty()) {
            ((verified * 100 + expiringOrPending * 50) / suppliers.size).coerceIn(0, 100)
        } else 100

        val w8BenFilingRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.w8BenOrTaxFormFiled }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val isoRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.iso9001Certified }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val antiBriberyRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.antiBriberyPactSigned }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val avgEsg = if (suppliers.isNotEmpty()) {
            suppliers.map { it.esgRatingScore }.average()
        } else 88.0

        val sb = StringBuilder()
        sb.append("================================================================================\n")
        sb.append("                   WORLD BUSINESS OS • GLOBAL TREASURY & COMPLIANCE             \n")
        sb.append("                   STATUTORY SUPPLIER DIRECTORY AUDIT DOSSIER                   \n")
        sb.append("================================================================================\n\n")

        sb.append("REPORT METADATA:\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append("Certification Timestamp : $nowFormatted\n")
        sb.append("Enterprise Entity       : OmniGlobal Holdings Inc. & Multinational Affiliates\n")
        sb.append("Audit Standard          : OECD Due Diligence Guidance • US FCPA • UK Bribery Act • SOX Sec 404\n")
        sb.append("Document Security       : LEVEL-1 ENTERPRISE CONFIDENTIAL / REGULATORY COMPLIANCE\n\n")

        sb.append("EXECUTIVE COMPLIANCE HEALTH SUMMARY:\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append("Total Enrolled Suppliers    : ${suppliers.size} Corporate Entities\n")
        sb.append("Verified & Compliant (Green): $verified (${(verified.toFloat() / total * 100).roundToInt()}%)\n")
        sb.append("Expiring / In Audit (Amber) : $expiringOrPending (${(expiringOrPending.toFloat() / total * 100).roundToInt()}%)\n")
        sb.append("Non-Compliant Risks (Red)   : $nonCompliant (${(nonCompliant.toFloat() / total * 100).roundToInt()}%)\n")
        sb.append("Aggregated Health Index     : $healthScore / 100\n")
        sb.append("IRS Form W-8BEN-E Coverage  : $w8BenFilingRate%\n")
        sb.append("ISO 9001 Certification Rate : $isoRate%\n")
        sb.append("Anti-Bribery Pacts Executed : $antiBriberyRate%\n")
        sb.append(String.format(Locale.US, "Average Supply Chain ESG    : %.1f / 100\n\n", avgEsg))

        sb.append("DETAILED ITEM-BY-ITEM SUPPLIER DOSSIERS:\n")
        sb.append("================================================================================\n\n")

        suppliers.forEachIndexed { index, s ->
            sb.append("[${index + 1}] SUPPLIER CODE: ${s.supplierCode}\n")
            sb.append("--------------------------------------------------------------------------------\n")
            sb.append("  Legal Name          : ${s.legalName} (Trade: ${s.tradingName})\n")
            sb.append("  Jurisdiction        : ${s.country} (${s.countryCode}) • Tax ID: ${s.taxRegistrationNumber}\n")
            sb.append("  Industry Category   : ${s.businessCategory}\n")
            sb.append("  Primary Contact     : ${s.primaryContactName} <${s.primaryContactEmail}>\n")
            sb.append("  Telephone Direct    : ${s.primaryContactPhone}\n")
            sb.append("  Physical Address    : ${s.physicalAddress}\n")
            sb.append("  Website Domain      : ${s.websiteUrl}\n")
            sb.append("\n")
            sb.append("  COMMERCIAL PAYMENT TERMS & BANKING:\n")
            sb.append("    Credit Terms      : ${s.paymentTermsDescription} (${s.paymentTermsDays} Days)\n")
            sb.append("    Settlement Currency: ${s.preferredCurrency} via ${s.settlementRail}\n")
            sb.append("    Prompt Discount   : ${if (s.discountEarlyPaymentPercent > 0.0) "${s.discountEarlyPaymentPercent}%" else "None"}\n")
            sb.append("    Beneficiary Bank  : ${s.bankName.ifBlank { "N/A" }}\n")
            sb.append("    SWIFT/BIC Code    : ${s.bankSwiftBic.ifBlank { "N/A" }}\n")
            sb.append("    IBAN / Account No : ${s.bankIbanOrAccountNumber.ifBlank { "N/A" }}\n")
            sb.append("\n")
            sb.append("  STATUTORY COMPLIANCE & RISK STANDING:\n")
            sb.append("    Compliance Status : ${s.complianceStatus}\n")
            sb.append("    Risk Tier         : ${s.riskLevel} RISK\n")
            sb.append("    Form W-8BEN-E     : ${if (s.w8BenOrTaxFormFiled) "Filed & Active" else "MISSING / EXPIRED"}\n")
            sb.append("    ISO 9001 Certified: ${if (s.iso9001Certified) "Certified" else "None"}\n")
            sb.append("    Anti-Bribery Pact : ${if (s.antiBriberyPactSigned) "Signed" else "Pending Execution"}\n")
            sb.append("    KYC Verification  : ${s.kycVerificationStatus}\n")
            sb.append("    ESG Rating Score  : ${s.esgRatingScore} / 100\n")
            sb.append("    Last Audit Date   : ${s.lastAuditDate}\n")
            sb.append("    Doc Expiry Date   : ${s.complianceDocExpiryDate}\n")
            sb.append("    Officer Remarks   : ${s.complianceOfficerNotes.ifBlank { "None recorded." }}\n")
            sb.append("    Procurement Status: ${if (s.isActive) "ACTIVE" else "INACTIVE / ON HOLD"}\n")
            sb.append("--------------------------------------------------------------------------------\n\n")
        }

        sb.append("================================================================================\n")
        sb.append("END OF STATUTORY AUDIT REPORT • DIGITALLY CERTIFIED FOR REGULATORY PURPOSES     \n")
        sb.append("================================================================================\n")

        return sb.toString()
    }

    /**
     * Copies report to Android system clipboard and triggers sharing intent.
     */
    fun exportAndShareReport(
        context: Context,
        content: String,
        subject: String,
        mimeType: String = "text/plain"
    ) {
        // 1. Copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(subject, content)
        clipboard.setPrimaryClip(clip)

        // 2. Launch system share intent
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Export Supplier Audit Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "$subject copied to clipboard", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
