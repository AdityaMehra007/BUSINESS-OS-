package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a multinational corporate supplier in the global supply directory.
 *
 * Captures comprehensive details including:
 * - Identification & legal jurisdiction
 * - Multi-channel contact information
 * - International payment & banking terms
 * - Statutory compliance documentation status (W-8BEN, ISO 9001, Anti-Bribery, KYC)
 */
@Entity(
    tableName = "global_suppliers",
    indices = [
        Index(value = ["supplierCode"], unique = true),
        Index(value = ["countryCode"]),
        Index(value = ["complianceStatus"]),
        Index(value = ["businessCategory"])
    ]
)
data class GlobalSupplierRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Supplier Identity & Jurisdiction
    val supplierCode: String,                       // e.g. "SUP-DE-0042"
    val legalName: String,                          // e.g. "Siemens AG Industrial Systems"
    val tradingName: String,                        // e.g. "Siemens Industrial"
    val country: String,                            // e.g. "Germany"
    val countryCode: String,                        // e.g. "DE"
    val taxRegistrationNumber: String,              // e.g. "DE129274202"
    val businessCategory: String,                   // e.g. "MANUFACTURING", "RAW_MATERIALS", "LOGISTICS_FREIGHT", "IT_SERVICES"

    // Contact Information
    val primaryContactName: String,                 // e.g. "Dr. Klaus Weber"
    val primaryContactEmail: String,                // e.g. "klaus.weber@siemens-supply.de"
    val primaryContactPhone: String,                // e.g. "+49 89 636 00"
    val physicalAddress: String,                    // e.g. "Werner-von-Siemens-Straße 1, 80333 Munich, Germany"
    val websiteUrl: String = "",                    // e.g. "https://www.siemens.com/suppliers"

    // Payment Terms & Banking Details
    val paymentTermsDays: Int = 30,                 // e.g. 15, 30, 45, 60, 90 days
    val paymentTermsDescription: String = "NET_30", // e.g. "NET_30", "NET_60", "2_10_NET_30"
    val preferredCurrency: String = "EUR",          // e.g. "EUR", "USD", "SGD", "CHF", "JPY"
    val settlementRail: String = "SEPA",            // e.g. "SWIFT", "SEPA", "FEDWIRE", "ACH"
    val bankName: String = "",                      // e.g. "Deutsche Bank AG"
    val bankSwiftBic: String = "",                  // e.g. "DEUTDEDD"
    val bankIbanOrAccountNumber: String = "",       // e.g. "DE89370400440532013000"
    val discountEarlyPaymentPercent: Double = 0.0,  // e.g. 2.0% for prompt settlement

    // Compliance Documentation & Verification Status
    val complianceStatus: String = "COMPLIANT",     // "COMPLIANT", "AUDIT_PENDING", "DOCUMENTATION_EXPIRED", "NON_COMPLIANT"
    val w8BenOrTaxFormFiled: Boolean = true,        // W-8BEN-E or foreign tax residency certificate on file
    val iso9001Certified: Boolean = true,           // ISO 9001 / ISO 14001 Quality certification verified
    val esgRatingScore: Int = 88,                   // 0-100 ESG compliance score
    val antiBriberyPactSigned: Boolean = true,      // OECD / FCPA Anti-corruption declaration executed
    val kycVerificationStatus: String = "VERIFIED", // "VERIFIED", "IN_REVIEW", "UNVERIFIED"
    val lastAuditDate: String = "2026-08-15",        // Last on-site or digital compliance audit
    val complianceDocExpiryDate: String = "2027-08-15", // Expiry date of primary compliance certification
    val complianceOfficerNotes: String = "",        // Statutory compliance remarks
    val riskLevel: String = "LOW",                  // "LOW", "MEDIUM", "HIGH"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SupplierComplianceStatus(val label: String, val code: String) {
    COMPLIANT("Fully Compliant", "COMPLIANT"),
    AUDIT_PENDING("Audit Pending Review", "AUDIT_PENDING"),
    DOCUMENTATION_EXPIRED("Documentation Expired", "DOCUMENTATION_EXPIRED"),
    NON_COMPLIANT("Non-Compliant Alert", "NON_COMPLIANT"),
    BLACKLISTED("Restricted / Blacklisted", "BLACKLISTED")
}
