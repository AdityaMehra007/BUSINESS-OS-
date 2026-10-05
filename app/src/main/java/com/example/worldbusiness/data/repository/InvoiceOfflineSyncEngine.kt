package com.example.worldbusiness.data.repository

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.TextMuted
import com.example.worldbusiness.data.local.InvoiceDao
import com.example.worldbusiness.data.model.AuditActionType
import com.example.worldbusiness.data.model.AuditLogModule
import com.example.worldbusiness.data.model.InvoiceRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncOperationState(
    val label: String,
    val color: Color,
    val isAnimated: Boolean
) {
    IDLE("All Invoices Synchronized", EmeraldPositive, false),
    SYNCING("Syncing with Sovereign Ledger...", CyanAccent, true),
    PENDING_CHANGES("Offline Invoices Pending Sync", GoldAccent, false),
    OFFLINE_ACTIVE("Offline Access Mode Active", RoseNegative, false),
    ERROR("Sync Reconciliation Alert", RoseNegative, false)
}

data class InvoiceSyncStatusSummary(
    val totalInvoicesCount: Int = 0,
    val syncedInvoicesCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val lastSyncTimestamp: String = "Oct 04, 2026 • 12:00 UTC",
    val isOfflineMode: Boolean = false,
    val databaseEngine: String = "Local Room SQLite v5",
    val syncState: SyncOperationState = SyncOperationState.IDLE
)

data class InvoiceSyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val timestamp: String,
    val details: String,
    val syncedInvoiceNumbers: List<String>
)

/**
 * Enterprise Room Database Offline Sync Engine for Cross-Border Commercial Invoices.
 *
 * Responsibilities:
 * - Local-first offline persistence in Room Database.
 * - Queueing and tracking offline-created or modified commercial invoices.
 * - Two-way simulated enterprise ledger synchronization.
 * - Conflict detection and audit logging.
 */
class InvoiceOfflineSyncEngine(
    private val invoiceDao: InvoiceDao,
    private val repository: WorldBusinessRepository
) {
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow("Oct 04, 2026 • 12:00 UTC")
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    private val _syncState = MutableStateFlow(SyncOperationState.IDLE)
    val syncState: StateFlow<SyncOperationState> = _syncState.asStateFlow()

    fun getPendingSyncCountFlow(): Flow<Int> = invoiceDao.getPendingSyncCount()

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        _syncState.value = if (enabled) {
            SyncOperationState.OFFLINE_ACTIVE
        } else {
            SyncOperationState.IDLE
        }
    }

    /**
     * Inserts an invoice into Room with offline sync status determined by the active connection state.
     */
    suspend fun saveInvoice(
        invoice: InvoiceRecord,
        forceOffline: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
        val nowStr = timeFormat.format(Date())
        val offline = forceOffline || _isOfflineMode.value

        val recordToSave = invoice.copy(
            isSynced = !offline,
            syncStatus = if (offline) "PENDING_SYNC" else "SYNCED",
            lastSyncedAt = if (offline) "Pending Cloud Sync" else nowStr,
            offlineCreated = offline,
            localVersion = 1L,
            syncError = null
        )

        val id = invoiceDao.insertInvoice(recordToSave)

        if (offline) {
            _syncState.value = SyncOperationState.PENDING_CHANGES
            repository.recordAuditLog(
                module = AuditLogModule.COMMERCIAL,
                actionType = AuditActionType.INVOICE_CREATED,
                sourceJurisdiction = invoice.issuingEntityName,
                destinationJurisdiction = invoice.clientCountry,
                financialAmount = invoice.amount,
                currency = invoice.currency,
                description = "Offline invoice ${invoice.invoiceNumber} stored in local Room database (v5)",
                complianceStandard = "Local Room Persistence • Offline Cache"
            )
        } else {
            repository.recordAuditLog(
                module = AuditLogModule.COMMERCIAL,
                actionType = AuditActionType.INVOICE_CREATED,
                sourceJurisdiction = invoice.issuingEntityName,
                destinationJurisdiction = invoice.clientCountry,
                financialAmount = invoice.amount,
                currency = invoice.currency,
                description = "Commercial invoice ${invoice.invoiceNumber} created and cloud synchronized",
                complianceStandard = "EU VAT Directive 2006/112/EC • Real-Time Ledger"
            )
        }

        id
    }

    /**
     * Synchronizes all local pending Room invoices with the central ledger.
     */
    suspend fun syncPendingInvoices(): InvoiceSyncResult = withContext(Dispatchers.IO) {
        _syncState.value = SyncOperationState.SYNCING
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss 'UTC'", Locale.US)
        val nowStr = timeFormat.format(Date())

        val pendingInvoices = invoiceDao.getPendingSyncInvoices()

        if (pendingInvoices.isEmpty()) {
            _syncState.value = if (_isOfflineMode.value) SyncOperationState.OFFLINE_ACTIVE else SyncOperationState.IDLE
            return@withContext InvoiceSyncResult(
                syncedCount = 0,
                failedCount = 0,
                timestamp = nowStr,
                details = "Local Room database already in sync with enterprise ledger.",
                syncedInvoiceNumbers = emptyList()
            )
        }

        val syncedIds = mutableListOf<Long>()
        val syncedNumbers = mutableListOf<String>()

        pendingInvoices.forEach { inv ->
            try {
                // In production, sends payload to cloud endpoint. Here we certify and update local Room state.
                invoiceDao.markAsSynced(inv.id, nowStr)
                syncedIds.add(inv.id)
                syncedNumbers.add(inv.invoiceNumber)

                repository.recordAuditLog(
                    module = AuditLogModule.COMMERCIAL,
                    actionType = AuditActionType.TAX_RETURN_FILED,
                    sourceJurisdiction = inv.issuingEntityName,
                    destinationJurisdiction = inv.clientCountry,
                    financialAmount = inv.amount,
                    currency = inv.currency,
                    description = "Offline invoice ${inv.invoiceNumber} synchronized from Room database to cloud ledger",
                    complianceStandard = "ISO 20022 Ledger Reconciliation"
                )
            } catch (e: Exception) {
                invoiceDao.updateSyncStatus(inv.id, "SYNC_CONFLICT", nowStr, e.message)
            }
        }

        _lastSyncTimestamp.value = nowStr
        _syncState.value = if (_isOfflineMode.value) SyncOperationState.OFFLINE_ACTIVE else SyncOperationState.IDLE

        InvoiceSyncResult(
            syncedCount = syncedIds.size,
            failedCount = pendingInvoices.size - syncedIds.size,
            timestamp = nowStr,
            details = "Successfully synchronized ${syncedIds.size} local invoices from Room database.",
            syncedInvoiceNumbers = syncedNumbers
        )
    }
}
