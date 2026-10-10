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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.repository.SyncOperationState
import com.example.worldbusiness.ui.components.CrossBorderInvoiceCreateForm
import com.example.worldbusiness.ui.components.CrossBorderInvoiceManager
import com.example.worldbusiness.ui.components.DocumentScannerDialog
import com.example.worldbusiness.ui.components.RechartsInvoiceAnalyticsDashboard
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart

enum class CommercialSubView(val label: String) {
    LEDGER("Commercial Ledger"),
    RECHARTS_ANALYTICS("Recharts Analytics"),
    INVOICE_FORM("Create Invoice"),
    PDF_STUDIO("PDF Export Studio")
}

/**
 * Cross-Border Commercial Ledger & Invoicing Screen.
 * Hosts the CrossBorderInvoiceManager for multi-currency invoicing, VAT/tax calculations, and status tracking,
 * as well as the CrossBorderInvoicePdfScreen for generating and saving publication-ready invoice PDFs.
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
    var activeSubView by remember { mutableStateOf(CommercialSubView.LEDGER) }
    var showScannerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("commercial_screen")
    ) {
        // Top Sub-view Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(4.dp)
                .testTag("commercial_subview_selector"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CommercialSubView.values().forEach { subView ->
                val isSelected = activeSubView == subView
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else SurfaceDark)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { activeSubView = subView }
                        .padding(vertical = 8.dp)
                        .testTag("subview_${subView.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val iconVector = when (subView) {
                            CommercialSubView.LEDGER -> Icons.Default.Receipt
                            CommercialSubView.RECHARTS_ANALYTICS -> Icons.Default.BarChart
                            CommercialSubView.INVOICE_FORM -> Icons.Default.AddCircle
                            CommercialSubView.PDF_STUDIO -> Icons.Default.PictureAsPdf
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = subView.label,
                            tint = if (isSelected) CyanAccent else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subView.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CyanAccent else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        when (activeSubView) {
            CommercialSubView.RECHARTS_ANALYTICS -> {
                RechartsInvoiceAnalyticsDashboard(
                    invoices = invoices,
                    entities = entities,
                    liveCurrencyFeed = liveCurrencyFeed,
                    onNavigateBack = { activeSubView = CommercialSubView.LEDGER },
                    onOpenCreateInvoice = { activeSubView = CommercialSubView.INVOICE_FORM }
                )
            }
            CommercialSubView.INVOICE_FORM -> {
                CrossBorderInvoiceCreateForm(
                    entities = entities,
                    liveCurrencyFeed = liveCurrencyFeed,
                    isOfflineMode = isOfflineMode,
                    onSaveInvoice = { issuing, client, country, amount, curr, tax, desc, due ->
                        onCreateInvoice(issuing, client, country, amount, curr, tax, desc, due)
                        activeSubView = CommercialSubView.LEDGER
                    },
                    onCancel = { activeSubView = CommercialSubView.LEDGER }
                )
            }
            CommercialSubView.PDF_STUDIO -> {
                CrossBorderInvoicePdfScreen(
                    invoices = invoices,
                    entities = entities,
                    fxBalances = fxBalances,
                    onBack = { activeSubView = CommercialSubView.LEDGER }
                )
            }
            CommercialSubView.LEDGER -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
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
                            onOpenCreateForm = { activeSubView = CommercialSubView.INVOICE_FORM },
                            onOpenAnalytics = { activeSubView = CommercialSubView.RECHARTS_ANALYTICS },
                            isOfflineMode = isOfflineMode,
                            pendingSyncCount = pendingSyncCount,
                            syncState = syncState,
                            lastSyncTimestamp = lastSyncTimestamp,
                            onToggleOfflineMode = onToggleOfflineMode,
                            onSyncLocalInvoices = onSyncLocalInvoices
                        )
                    }
                }
            }
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
