package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.ShipmentRecord
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EnterpriseLedgerSnapshot(
    val exportTimestamp: String,
    val enterpriseId: String,
    val totalRecordsCount: Int,
    val entitiesCount: Int,
    val invoicesCount: Int,
    val vaultsCount: Int,
    val shipmentsCount: Int,
    val auditLogsCount: Int,
    val consolidatedCashUsd: Double,
    val chainIntegrityValid: Boolean,
    val rootHash: String,
    val latestHash: String,
    val sha256ManifestChecksum: String,
    val rawJsonPayload: String
)

object DatabaseComplianceExportEngine {

    fun generateSnapshot(
        entities: List<EntityRecord>,
        invoices: List<InvoiceRecord>,
        balances: List<FxBalanceRecord>,
        shipments: List<ShipmentRecord>,
        auditLogs: List<AuditLogRecord>
    ): EnterpriseLedgerSnapshot {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val chainResult = AuditCryptographicEngine.verifyChainIntegrity(auditLogs)
        val consolidatedCash = balances.sumOf { it.balance * it.rateToUsd }

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"version\": \"1.0.0-ENTERPRISE\",\n")
        jsonBuilder.append("  \"timestamp\": \"$timestamp\",\n")
        jsonBuilder.append("  \"enterprise\": \"WorldBusiness OS Consolidated Global\",\n")
        jsonBuilder.append("  \"systemMetrics\": {\n")
        jsonBuilder.append("    \"entitiesCount\": ${entities.size},\n")
        jsonBuilder.append("    \"invoicesCount\": ${invoices.size},\n")
        jsonBuilder.append("    \"vaultsCount\": ${balances.size},\n")
        jsonBuilder.append("    \"shipmentsCount\": ${shipments.size},\n")
        jsonBuilder.append("    \"auditLogsCount\": ${auditLogs.size},\n")
        jsonBuilder.append("    \"consolidatedCashUsd\": $consolidatedCash,\n")
        jsonBuilder.append("    \"chainIntegrity\": ${chainResult.isValid},\n")
        jsonBuilder.append("    \"rootHash\": \"${chainResult.rootHash}\",\n")
        jsonBuilder.append("    \"latestHash\": \"${chainResult.latestHash}\"\n")
        jsonBuilder.append("  },\n")

        // Entities array
        jsonBuilder.append("  \"entities\": [\n")
        entities.forEachIndexed { i, e ->
            jsonBuilder.append("    {\"id\": ${e.id}, \"name\": \"${escape(e.name)}\", \"code\": \"${e.countryCode}\", \"taxId\": \"${e.taxId}\", \"status\": \"${e.status}\"}")
            if (i < entities.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("  ],\n")

        // Invoices array
        jsonBuilder.append("  \"invoices\": [\n")
        invoices.forEachIndexed { i, inv ->
            jsonBuilder.append("    {\"id\": ${inv.id}, \"number\": \"${inv.invoiceNumber}\", \"amount\": ${inv.amount}, \"currency\": \"${inv.currency}\", \"status\": \"${inv.status}\"}")
            if (i < invoices.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("  ],\n")

        // Balances array
        jsonBuilder.append("  \"vaults\": [\n")
        balances.forEachIndexed { i, b ->
            jsonBuilder.append("    {\"currency\": \"${b.currencyCode}\", \"balance\": ${b.balance}, \"rateToUsd\": ${b.rateToUsd}}")
            if (i < balances.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("  ],\n")

        // Shipments array
        jsonBuilder.append("  \"shipments\": [\n")
        shipments.forEachIndexed { i, s ->
            jsonBuilder.append("    {\"id\": ${s.id}, \"tracking\": \"${s.trackingCode}\", \"origin\": \"${escape(s.origin)}\", \"destination\": \"${escape(s.destination)}\", \"status\": \"${s.customsStatus}\", \"eta\": \"${s.estimatedArrival}\"}")
            if (i < shipments.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}\n")

        val rawJson = jsonBuilder.toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val checksumBytes = digest.digest(rawJson.toByteArray(Charsets.UTF_8))
        val checksum = checksumBytes.joinToString("") { "%02x".format(it) }

        val totalRecords = entities.size + invoices.size + balances.size + shipments.size + auditLogs.size

        return EnterpriseLedgerSnapshot(
            exportTimestamp = timestamp,
            enterpriseId = "WBOS-ENT-2026-GLOBAL",
            totalRecordsCount = totalRecords,
            entitiesCount = entities.size,
            invoicesCount = invoices.size,
            vaultsCount = balances.size,
            shipmentsCount = shipments.size,
            auditLogsCount = auditLogs.size,
            consolidatedCashUsd = consolidatedCash,
            chainIntegrityValid = chainResult.isValid,
            rootHash = chainResult.rootHash,
            latestHash = chainResult.latestHash,
            sha256ManifestChecksum = checksum,
            rawJsonPayload = rawJson
        )
    }

    private fun escape(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "")
}
