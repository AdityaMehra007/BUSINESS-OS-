package com.example.worldbusiness.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.repository.SyncOperationState
import com.example.worldbusiness.ui.components.CrossBorderInvoiceManager
import com.example.worldbusiness.ui.components.DocumentScannerDialog

/**
 * Cross-Border Commercial Ledger & Invoicing Screen.
 * Hosts the CrossBorderInvoiceManager for multi-currency invoicing, VAT/tax calculations, and status tracking.
 */
@Composable
fun CommercialScreen(
    invoices: List<InvoiceRecord>,
    entities: List<EntityRecord>,
    fxBalances: List<FxBalanceRecord> = emptyList(),
    onCreateInvoice: (
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) -> Unit,
    onMarkPaid: (invoiceId: Long, invoiceNumber: String) -> Unit,
    onUpdateInvoiceStatus: (invoiceId: Long, newStatus: String) -> Unit = { id, _ -> onMarkPaid(id, "") },
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    onRefreshRates: (() -> Unit)? = null,
    isOfflineMode: Boolean = false,
    pendingSyncCount: Int = 0,
    syncState: SyncOperationState = SyncOperationState.IDLE,
    lastSyncTimestamp: String = "Oct 04, 2026 • 12:00 UTC",
    onToggleOfflineMode: (Boolean) -> Unit = {},
    onSyncLocalInvoices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showScannerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("commercial_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CrossBorderInvoiceManager(
                invoices = invoices,
                entities = entities,
                fxBalances = fxBalances,
                onCreateInvoice = onCreateInvoice,
                onUpdateInvoiceStatus = onUpdateInvoiceStatus,
                liveCurrencyFeed = liveCurrencyFeed,
                onRefreshRates = onRefreshRates,
                onOpenScanner = { showScannerDialog = true },
                isOfflineMode = isOfflineMode,
                pendingSyncCount = pendingSyncCount,
                syncState = syncState,
                lastSyncTimestamp = lastSyncTimestamp,
                onToggleOfflineMode = onToggleOfflineMode,
                onSyncLocalInvoices = onSyncLocalInvoices
            )
        }
    }

    if (showScannerDialog) {
        DocumentScannerDialog(
            entities = entities,
            onDismiss = { showScannerDialog = false },
            onSaveInvoice = { issuing, client, country, amount, curr, tax, desc, due ->
                onCreateInvoice(issuing, client, country, amount, curr, tax, desc, due)
                showScannerDialog = false
            }
        )
    }
}
