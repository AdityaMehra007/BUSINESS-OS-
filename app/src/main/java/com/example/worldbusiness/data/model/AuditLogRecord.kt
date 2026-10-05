package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuditLogModule(val label: String, val code: String) {
    TREASURY("Treasury & FX", "TRSY"),
    COMMERCIAL("Commercial & Invoicing", "COMM"),
    ENTITIES("Corporate Entities", "ENTS"),
    WORKFORCE("Global Workforce & Payroll", "WRKF"),
    LOGISTICS("Cross-Border Logistics", "LOGI"),
    TAX_COMPLIANCE("Tax & Regulatory Filing", "TAX"),
    SYSTEM_ADMIN("System & Security Administration", "ADMN")
}

enum class AuditActionType(val label: String) {
    FX_SWAP_EXECUTED("FX Interbank Swap Executed"),
    TREASURY_TRANSFER("Cross-Border Treasury Transfer"),
    VAULT_BALANCE_ADJUSTED("Vault Reserve Balance Adjusted"),
    HEDGE_CONTRACT_LOCKED("Derivative FX Hedge Locked"),
    INVOICE_CREATED("Commercial Invoice Created"),
    INVOICE_OCR_SCANNED("Invoice OCR Scanned & Ingested"),
    INVOICE_STATUS_UPDATED("Cross-Border Invoice Status Transitioned"),
    INVOICE_PAID("Cross-Border Invoice Settled"),
    TAX_RETURN_FILED("Statutory Tax Return Transmitted"),
    TAX_EXTENSION_REQUESTED("Statutory Filing Extension Granted"),
    ENTITY_CREATED("Corporate Subsidiary Incorporated"),
    ENTITY_STATUS_UPDATED("Entity Standing Status Updated"),
    SHIPMENT_DISPATCHED("International Freight Dispatched"),
    PAYROLL_EXECUTED("Cross-Border Global Payroll Executed"),
    AML_FLAG_REMEDIATED("Compliance AML Alert Remediated"),
    SECURITY_KEY_ROTATED("Cryptographic Key Rotated"),
    MANUAL_COMPLIANCE_OVERRIDE("Manual Compliance Override")
}

enum class AuditLogStatus(val label: String) {
    VERIFIED_IMMUTABLE("Verified & Immutable"),
    FLAGGED_AML_ALERT("Flagged AML Alert"),
    PENDING_REVIEW("Pending Compliance Review"),
    REMEDIATED("Audited & Remediated")
}

@Entity(tableName = "audit_logs")
data class AuditLogRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logId: String,                           // e.g. "AUD-2026-X8921"
    val timestamp: Long,                         // Epoch millis
    val timestampFormatted: String,              // "2026-10-01 14:18:22 UTC"
    val module: String,                          // AuditLogModule name
    val actionType: String,                      // AuditActionType name
    val actorUsername: String,                   // e.g. "alex.chen@omniglobal.ch"
    val actorRole: String,                       // e.g. "VP Global Treasury"
    val actorIpAddress: String,                  // e.g. "192.168.1.104 (VPN-ZRH)"
    val sourceJurisdiction: String,              // e.g. "Switzerland (Zurich)"
    val destinationJurisdiction: String? = null, // e.g. "Germany (Bavaria)"
    val currency: String? = null,                // e.g. "EUR"
    val financialAmount: Double? = null,         // e.g. 232050.0
    val status: String,                          // AuditLogStatus name
    val complianceStandard: String,              // e.g. "SOX Sec 404 • EU MiFID II"
    val previousHash: String,                    // SHA-256 link to prior record
    val cryptographicHash: String,               // Tamper-evident SHA-256 checksum
    val description: String,                     // Full audit trail description
    val metadataJson: String = "{}"              // Structured key-value details
)

data class AuditChainVerificationResult(
    val isValid: Boolean,
    val totalRecordsVerified: Int,
    val compromisedRecordId: String? = null,
    val rootHash: String,
    val latestHash: String,
    val verificationTimestamp: String
)

data class AuditLogSummaryStats(
    val totalLogsCount: Int,
    val crossBorderTxCount: Int,
    val adminActionsCount: Int,
    val totalVolumeAuditedUsd: Double,
    val verifiedImmutablePercent: Double,
    val flaggedAmlCount: Int,
    val chainIntegrityValid: Boolean
)
