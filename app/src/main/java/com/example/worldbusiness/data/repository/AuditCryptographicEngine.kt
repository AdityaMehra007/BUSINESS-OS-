package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.AuditChainVerificationResult
import com.example.worldbusiness.data.model.AuditLogRecord
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditCryptographicEngine {

    const val GENESIS_HASH = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"

    fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateLogHash(
        previousHash: String,
        timestamp: Long,
        logId: String,
        module: String,
        actionType: String,
        actorUsername: String,
        amount: Double?,
        currency: String?,
        sourceJurisdiction: String,
        destinationJurisdiction: String?
    ): String {
        val payload = buildString {
            append(previousHash)
            append("|").append(timestamp)
            append("|").append(logId)
            append("|").append(module)
            append("|").append(actionType)
            append("|").append(actorUsername)
            append("|").append(amount ?: 0.0)
            append("|").append(currency ?: "N/A")
            append("|").append(sourceJurisdiction)
            append("|").append(destinationJurisdiction ?: "N/A")
        }
        return computeSha256(payload)
    }

    fun verifyChainIntegrity(logs: List<AuditLogRecord>): AuditChainVerificationResult {
        if (logs.isEmpty()) {
            return AuditChainVerificationResult(
                isValid = true,
                totalRecordsVerified = 0,
                compromisedRecordId = null,
                rootHash = GENESIS_HASH,
                latestHash = GENESIS_HASH,
                verificationTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
            )
        }

        // Sort chronologically (oldest first)
        val sortedLogs = logs.sortedBy { it.timestamp }
        var expectedPrevHash = sortedLogs.first().previousHash

        for (log in sortedLogs) {
            // Verify link to previous hash
            if (log.previousHash != expectedPrevHash) {
                return AuditChainVerificationResult(
                    isValid = false,
                    totalRecordsVerified = sortedLogs.indexOf(log),
                    compromisedRecordId = log.logId,
                    rootHash = sortedLogs.first().cryptographicHash,
                    latestHash = log.cryptographicHash,
                    verificationTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
                )
            }

            // Verify payload integrity
            val expectedHash = generateLogHash(
                previousHash = log.previousHash,
                timestamp = log.timestamp,
                logId = log.logId,
                module = log.module,
                actionType = log.actionType,
                actorUsername = log.actorUsername,
                amount = log.financialAmount,
                currency = log.currency,
                sourceJurisdiction = log.sourceJurisdiction,
                destinationJurisdiction = log.destinationJurisdiction
            )

            if (log.cryptographicHash != expectedHash) {
                return AuditChainVerificationResult(
                    isValid = false,
                    totalRecordsVerified = sortedLogs.indexOf(log),
                    compromisedRecordId = log.logId,
                    rootHash = sortedLogs.first().cryptographicHash,
                    latestHash = log.cryptographicHash,
                    verificationTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
                )
            }

            expectedPrevHash = log.cryptographicHash
        }

        return AuditChainVerificationResult(
            isValid = true,
            totalRecordsVerified = sortedLogs.size,
            compromisedRecordId = null,
            rootHash = sortedLogs.first().cryptographicHash,
            latestHash = sortedLogs.last().cryptographicHash,
            verificationTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
        )
    }
}
