package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.CurrencyOption
import com.example.worldbusiness.data.model.CurrencySyncStatus
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.InvoiceStatusStage
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.repository.CrossBorderTaxCalculator
import com.example.worldbusiness.data.repository.SyncOperationState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enterprise Jetpack Compose UI component for managing cross-border commercial invoices.
 *
 * Capabilities:
 * - Real-time currency conversion service integration using global financial APIs.
 * - Dynamic currency selection with live exchange rates & spread indicators.
 * - Comprehensive cross-border tax calculation engine (VAT, GST, MWST, Reverse Charge, Withholding Tax).
 * - Multi-stage lifecycle status tracking (Draft -> Issued/Pending -> In-Clearing -> Settled/Overdue).
 * - Local Room SQLite database storage (v5) with offline read/write and synchronization.
 */
@Composable
fun CrossBorderInvoiceManager(
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
    onUpdateInvoiceStatus: (invoiceId: Long, newStatus: String) -> Unit,
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    onRefreshRates: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    onOpenCreateForm: (() -> Unit)? = null,
    onOpenAnalytics: (() -> Unit)? = null,
    isOfflineMode: Boolean = false,
    pendingSyncCount: Int = 0,
    syncState: SyncOperationState = SyncOperationState.IDLE,
    lastSyncTimestamp: String = "Oct 04, 2026 • 12:00 UTC",
    onToggleOfflineMode: (Boolean) -> Unit = {},
    onSyncLocalInvoices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedCurrencyFilter by remember { mutableStateOf("ALL") }
    var showCreateModal by remember { mutableStateOf(false) }
    var selectedInvoiceForDossier by remember { mutableStateOf<InvoiceRecord?>(null) }
    var selectedInvoiceForFormattedView by remember { mutableStateOf<InvoiceRecord?>(null) }
    var customLineItemsForFormattedView by remember { mutableStateOf<List<CrossBorderInvoiceItem>?>(null) }
    var showInvoiceGeneratorModal by remember { mutableStateOf(false) }

    // Filter invoices by search text, status, and currency
    val filteredInvoices = remember(invoices, searchQuery, selectedStatusFilter, selectedCurrencyFilter) {
        invoices.filter { invoice ->
            val matchesSearch = searchQuery.isBlank() ||
                invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                invoice.clientName.contains(searchQuery, ignoreCase = true) ||
                invoice.clientCountry.contains(searchQuery, ignoreCase = true) ||
                invoice.currency.contains(searchQuery, ignoreCase = true) ||
                invoice.issuingEntityName.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "ALL" -> true
                "DRAFT" -> invoice.status.equals("DRAFT", ignoreCase = true)
                "SENT" -> invoice.status.equals("SENT", ignoreCase = true)
                "SETTLED", "PAID" -> invoice.status.equals("PAID", ignoreCase = true) || invoice.status.equals("SETTLED", ignoreCase = true)
                "IN_CLEARING" -> invoice.status.equals("IN_CLEARING", ignoreCase = true)
                "PENDING" -> invoice.status.equals("PENDING", ignoreCase = true)
                "OVERDUE" -> invoice.status.equals("OVERDUE", ignoreCase = true)
                else -> invoice.status.equals(selectedStatusFilter, ignoreCase = true)
            }

            val matchesCurrency = selectedCurrencyFilter == "ALL" ||
                invoice.currency.equals(selectedCurrencyFilter, ignoreCase = true)

            matchesSearch && matchesStatus && matchesCurrency
        }
    }

    // Consolidated KPI Calculations using Real-Time Exchange Rates
    val totalVolumeUsd = remember(invoices, liveCurrencyFeed) {
        invoices.sumOf {
            val rate = liveCurrencyFeed.getRateToUsd(it.currency)
            it.amount * rate
        }
    }

    val totalSettledUsd = remember(invoices, liveCurrencyFeed) {
        invoices.filter { it.status == "PAID" || it.status == "SETTLED" }.sumOf {
            val rate = liveCurrencyFeed.getRateToUsd(it.currency)
            it.amount * rate
        }
    }

    val totalPendingUsd = remember(invoices, liveCurrencyFeed) {
        invoices.filter { it.status == "PENDING" || it.status == "IN_CLEARING" }.sumOf {
            val rate = liveCurrencyFeed.getRateToUsd(it.currency)
            it.amount * rate
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cross_border_invoice_manager"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Management Hub Header & Actions
        CrossBorderManagerHeader(
            onOpenCreateModal = { showCreateModal = true },
            onOpenCreateForm = onOpenCreateForm,
            onOpenAnalytics = onOpenAnalytics,
            onOpenScanner = onOpenScanner,
            onOpenGeneratorModal = { showInvoiceGeneratorModal = true }
        )

        // Real-Time Currency Financial API Ticker & Synchronization Banner
        RealTimeCurrencyTickerBanner(
            feed = liveCurrencyFeed,
            onRefresh = onRefreshRates
        )

        // Local Room Database Offline Synchronization & Access Control Banner
        RoomDatabaseOfflineSyncBanner(
            isOfflineMode = isOfflineMode,
            pendingSyncCount = pendingSyncCount,
            syncState = syncState,
            lastSyncTimestamp = lastSyncTimestamp,
            onToggleOfflineMode = onToggleOfflineMode,
            onSyncLocalInvoices = onSyncLocalInvoices
        )

        // KPI Summary Ribbon
        CrossBorderKpiRibbon(
            totalVolumeUsd = totalVolumeUsd,
            totalSettledUsd = totalSettledUsd,
            totalPendingUsd = totalPendingUsd,
            invoiceCount = invoices.size
        )

        // Search Bar & Filter Controls
        CrossBorderFilterToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            selectedStatus = selectedStatusFilter,
            onSelectStatus = { selectedStatusFilter = it },
            selectedCurrency = selectedCurrencyFilter,
            onSelectCurrency = { selectedCurrencyFilter = it }
        )

        // Invoices List
        if (filteredInvoices.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredInvoices.forEach { invoice ->
                    CrossBorderInvoiceItemCard(
                        invoice = invoice,
                        liveRates = liveCurrencyFeed.ratesToUsd,
                        onSelectForDetail = { selectedInvoiceForDossier = invoice },
                        onOpenFormattedView = { selectedInvoiceForFormattedView = invoice },
                        onUpdateStatus = { nextStatus -> onUpdateInvoiceStatus(invoice.id, nextStatus) }
                    )
                }
            }
        } else {
            EmptyInvoicesCard(searchQuery = searchQuery, onResetFilter = {
                searchQuery = ""
                selectedStatusFilter = "ALL"
                selectedCurrencyFilter = "ALL"
            })
        }
    }

    // Modal: Create Cross-Border Invoice with Real-Time Currency Selection & Dynamic Tax Calculations
    if (showCreateModal) {
        CrossBorderInvoiceFormModal(
            entities = entities,
            liveRates = liveCurrencyFeed.ratesToUsd,
            isOfflineMode = isOfflineMode,
            onDismiss = { showCreateModal = false },
            onConfirm = { issuing, client, country, amount, curr, tax, desc, due ->
                onCreateInvoice(issuing, client, country, amount, curr, tax, desc, due)
                showCreateModal = false
            }
        )
    }

    // Modal: Statutory Invoice Dossier & Status Progression Detail
    selectedInvoiceForDossier?.let { invoice ->
        CrossBorderInvoiceDossierModal(
            invoice = invoice,
            liveRates = liveCurrencyFeed.ratesToUsd,
            providerName = liveCurrencyFeed.providerName,
            onDismiss = { selectedInvoiceForDossier = null },
            onUpdateStatus = { nextStatus ->
                onUpdateInvoiceStatus(invoice.id, nextStatus)
                selectedInvoiceForDossier = invoice.copy(status = nextStatus)
            }
        )
    }

    // Modal: Formatted Cross-Border Commercial Invoice Document Modal
    selectedInvoiceForFormattedView?.let { invoice ->
        FormattedCrossBorderInvoiceModal(
            invoice = invoice,
            entities = entities,
            fxBalances = fxBalances,
            customLineItems = customLineItemsForFormattedView,
            onDismiss = {
                selectedInvoiceForFormattedView = null
                customLineItemsForFormattedView = null
            },
            onUpdateStatus = onUpdateInvoiceStatus
        )
    }

    // Modal: Formatted Cross-Border Invoice Generator
    if (showInvoiceGeneratorModal) {
        CrossBorderInvoiceGeneratorModal(
            entities = entities,
            onDismiss = { showInvoiceGeneratorModal = false },
            onInvoiceCreated = { invoice, lineItems ->
                onCreateInvoice(
                    invoice.issuingEntityName,
                    invoice.clientName,
                    invoice.clientCountry,
                    invoice.amount,
                    invoice.currency,
                    invoice.taxRatePercent,
                    invoice.serviceDescription,
                    invoice.dueDate
                )
                customLineItemsForFormattedView = lineItems
                selectedInvoiceForFormattedView = invoice
                showInvoiceGeneratorModal = false
            }
        )
    }
}

/**
 * Local Room Database Offline Synchronization & Access Control Banner
 */
@Composable
fun RoomDatabaseOfflineSyncBanner(
    isOfflineMode: Boolean,
    pendingSyncCount: Int,
    syncState: SyncOperationState,
    lastSyncTimestamp: String,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSyncLocalInvoices: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isOfflineMode) RoseNegative.copy(alpha = 0.5f)
                    else if (pendingSyncCount > 0) GoldAccent.copy(alpha = 0.5f)
                    else EmeraldPositive.copy(alpha = 0.5f),
                    BorderSubtle
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_offline_sync_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Status & Storage Engine
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isOfflineMode) RoseNegative else if (pendingSyncCount > 0) GoldAccent else EmeraldPositive,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (isOfflineMode) "OFFLINE ACCESS MODE ACTIVE" else "LOCAL ROOM DATABASE (v5) • SYNCED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isOfflineMode) RoseNegative else if (pendingSyncCount > 0) GoldAccent else EmeraldPositive,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Offline Mode Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isOfflineMode) "OFFLINE" else "ONLINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineMode) RoseNegative else CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = isOfflineMode,
                        onCheckedChange = onToggleOfflineMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RoseNegative,
                            checkedTrackColor = RoseNegative.copy(alpha = 0.3f),
                            uncheckedThumbColor = CyanAccent,
                            uncheckedTrackColor = SurfaceDark
                        ),
                        modifier = Modifier.testTag("switch_offline_mode")
                    )
                }
            }

            // Sync Stats & Manual Trigger Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (pendingSyncCount > 0) GoldAccent.copy(alpha = 0.15f) else EmeraldPositive.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (pendingSyncCount > 0) "$pendingSyncCount LOCAL CHANGES QUEUED" else "LOCAL CACHE VERIFIED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingSyncCount > 0) GoldAccent else EmeraldPositive,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "SQLite v5 Local-First",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Last synced: $lastSyncTimestamp",
                        fontSize = 8.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onSyncLocalInvoices,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pendingSyncCount > 0) GoldAccent else CyanAccent,
                        contentColor = SurfaceDark
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_sync_local_invoices")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (syncState == SyncOperationState.SYNCING) "SYNCING..." else "SYNC LOCAL DATA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Real-Time Financial API Exchange Rate Ticker & Synchronization Banner
 */
@Composable
fun RealTimeCurrencyTickerBanner(
    feed: LiveCurrencyFeed,
    onRefresh: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(feed.status.color.copy(alpha = 0.6f), BorderSubtle)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realtime_currency_banner")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(feed.status.color.copy(alpha = pulseAlpha), CircleShape)
                    )
                    Text(
                        text = "REAL-TIME FX API: ${feed.status.label.uppercase(Locale.US)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = feed.status.color,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "• ${feed.providerName}",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (onRefresh != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .clickable { onRefresh() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("btn_refresh_fx_rates"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = CyanAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "SYNC FX",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Real-Time Currency Cross-Rate Marquee Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tickerPairs = listOf(
                    "EUR" to "🇪🇺",
                    "GBP" to "🇬🇧",
                    "SGD" to "🇸🇬",
                    "CHF" to "🇨🇭",
                    "JPY" to "🇯🇵",
                    "BRL" to "🇧🇷",
                    "AUD" to "🇦🇺",
                    "CAD" to "🇨🇦"
                )

                tickerPairs.forEach { (code, flag) ->
                    val rate = feed.getRateToUsd(code)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(SurfaceDark, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = flag, fontSize = 9.sp)
                        Text(
                            text = "$code/USD:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (rate >= 1.0) String.format(Locale.US, "%.4f", rate) else String.format(Locale.US, "%.4f", rate),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component Header with Title & Action Buttons
 */
@Composable
private fun CrossBorderManagerHeader(
    onOpenCreateModal: () -> Unit,
    onOpenCreateForm: (() -> Unit)? = null,
    onOpenAnalytics: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    onOpenGeneratorModal: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Cross-Border",
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "CROSS-BORDER INVOICE MANAGER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Real-Time Multi-Currency Billing & Statutory Tax Engine",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenAnalytics != null) {
                Button(
                    onClick = onOpenAnalytics,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("btn_manager_analytics")
                ) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = "Analytics", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RECHARTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            if (onOpenGeneratorModal != null) {
                Button(
                    onClick = onOpenGeneratorModal,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("btn_generate_formatted_invoice")
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "Generate", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GENERATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            if (onOpenScanner != null) {
                Button(
                    onClick = onOpenScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("btn_manager_scan")
                ) {
                    Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Scan", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Button(
                onClick = onOpenCreateForm ?: onOpenCreateModal,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.testTag("btn_manager_new_invoice")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NEW INVOICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * 4-Pillar Cross-Border Invoicing KPI Summary Ribbon
 */
@Composable
private fun CrossBorderKpiRibbon(
    totalVolumeUsd: Double,
    totalSettledUsd: Double,
    totalPendingUsd: Double,
    invoiceCount: Int
) {
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "TOTAL VOLUME (USD)", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$${numberFmt.format((totalVolumeUsd / 1000.0).roundToInt())}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "SETTLED / CLEARED", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$${numberFmt.format((totalSettledUsd / 1000.0).roundToInt())}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "IN TRANSIT / PENDING", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$${numberFmt.format((totalPendingUsd / 1000.0).roundToInt())}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "PORTFOLIO", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$invoiceCount Units",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Filter Toolbar with Search and Currency / Status Chips
 */
@Composable
private fun CrossBorderFilterToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("invoice_search_input"),
            placeholder = {
                Text(text = "Search by invoice #, client, country, or currency...", fontSize = 11.sp, color = TextMuted)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "STATUS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)

            listOf("ALL", "DRAFT", "SENT", "PAID", "OVERDUE", "PENDING").forEach { status ->
                val isSelected = selectedStatus == status
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(0.5.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectStatus(status) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("status_filter_${status.lowercase(Locale.US)}")
                ) {
                    Text(
                        text = when (status) {
                            "DRAFT" -> "DRAFT"
                            "SENT" -> "SENT"
                            "PAID" -> "PAID"
                            "OVERDUE" -> "OVERDUE"
                            "PENDING" -> "PENDING"
                            else -> status.replace("_", " ")
                        },
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "CURRENCY:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)

            listOf("ALL", "USD", "EUR", "GBP", "SGD", "CHF", "JPY", "BRL").forEach { curr ->
                val isSelected = selectedCurrency == curr
                Box(
                    modifier = Modifier
                        .background(if (isSelected) GoldAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(0.5.dp, if (isSelected) GoldAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectCurrency(curr) }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("currency_filter_$curr")
                ) {
                    Text(
                        text = curr,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Visual Status Indicator for Invoices in the Database & List View.
 * Displays distinctive iconography, color-coded badges, and optional quick-action transitions
 * for 'Draft', 'Sent', 'Paid', 'Overdue' (and intermediate banking states).
 */
@Composable
fun InvoiceStatusIndicator(
    status: String,
    modifier: Modifier = Modifier,
    invoiceId: Long? = null,
    onStatusChange: ((String) -> Unit)? = null
) {
    val stage = InvoiceStatusStage.fromString(status)
    var showMenu by remember { mutableStateOf(false) }

    val iconVector = when (stage) {
        InvoiceStatusStage.DRAFT -> Icons.Default.Drafts
        InvoiceStatusStage.SENT -> Icons.AutoMirrored.Filled.Send
        InvoiceStatusStage.PAID, InvoiceStatusStage.SETTLED -> Icons.Default.CheckCircle
        InvoiceStatusStage.OVERDUE, InvoiceStatusStage.DISPUTED -> Icons.Default.Warning
        InvoiceStatusStage.IN_CLEARING -> Icons.Default.Sync
        InvoiceStatusStage.PENDING -> Icons.Default.HourglassTop
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = stage.color.copy(alpha = 0.16f),
            border = androidx.compose.foundation.BorderStroke(1.dp, stage.color.copy(alpha = 0.7f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = onStatusChange != null) { showMenu = true }
                .testTag(if (invoiceId != null) "invoice_status_indicator_$invoiceId" else "invoice_status_indicator_${stage.key.lowercase(Locale.US)}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = stage.label,
                    tint = stage.color,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = stage.label.uppercase(Locale.US),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = stage.color,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                if (onStatusChange != null) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Change Status",
                        tint = stage.color,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        if (onStatusChange != null && showMenu) {
            androidx.compose.material3.DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                listOf(
                    Triple("DRAFT", "Draft", Icons.Default.Drafts),
                    Triple("SENT", "Sent", Icons.AutoMirrored.Filled.Send),
                    Triple("PAID", "Paid", Icons.Default.CheckCircle),
                    Triple("OVERDUE", "Overdue", Icons.Default.Warning)
                ).forEach { (statusCode, statusLabel, icon) ->
                    val isCurrent = stage.key.equals(statusCode, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val itemStage = InvoiceStatusStage.fromString(statusCode)
                                Icon(
                                    imageVector = icon,
                                    contentDescription = statusLabel,
                                    tint = itemStage.color,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = statusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) itemStage.color else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        onClick = {
                            onStatusChange(statusCode)
                            showMenu = false
                        },
                        modifier = Modifier.testTag("status_option_${statusCode.lowercase(Locale.US)}")
                    )
                }
            }
        }
    }
}

/**
 * Individual Cross-Border Invoice Card with Real-Time Conversion & Status Stepper
 */
@Composable
private fun CrossBorderInvoiceItemCard(
    invoice: InvoiceRecord,
    liveRates: Map<String, Double>,
    onSelectForDetail: () -> Unit,
    onOpenFormattedView: (() -> Unit)? = null,
    onUpdateStatus: (String) -> Unit
) {
    val stage = InvoiceStatusStage.fromString(invoice.status)
    val currencyOpt = CrossBorderTaxCalculator.getCurrencyOption(invoice.currency, liveRates)
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    val grossAmount = invoice.amount * (1.0 + (invoice.taxRatePercent / 100.0))
    val equivalentUsd = grossAmount * currencyOpt.rateToUsd

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectForDetail() }
            .testTag("invoice_card_${invoice.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(stage.color.copy(alpha = 0.5f), BorderSubtle)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Number, Flag & Country, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${countryNameToFlag(invoice.clientCountry)} ${invoice.clientCountry}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Room Database Sync Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (invoice.isSynced) EmeraldPositive.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (invoice.isSynced) EmeraldPositive.copy(alpha = 0.5f) else GoldAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(if (invoice.isSynced) EmeraldPositive else GoldAccent, CircleShape)
                            )
                            Text(
                                text = if (invoice.isSynced) "ROOM v5" else "PENDING SYNC",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                color = if (invoice.isSynced) EmeraldPositive else GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Status Indicator
                    InvoiceStatusIndicator(
                        status = invoice.status,
                        invoiceId = invoice.id,
                        onStatusChange = onUpdateStatus
                    )
                }
            }

            // Client Name & Service Description
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = invoice.clientName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = invoice.serviceDescription,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            // Financial & Tax Breakdown Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "INVOICE TOTAL", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${currencyOpt.symbol}${numberFmt.format(grossAmount.roundToInt())} ${currencyOpt.code}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (currencyOpt.code != "USD") {
                        Text(
                            text = "≈ $${numberFmt.format(equivalentUsd.roundToInt())} USD (Live Rate: ${String.format(Locale.US, "%.4f", currencyOpt.rateToUsd)})",
                            fontSize = 8.sp,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "STATUTORY TAX", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Box(
                        modifier = Modifier
                            .background(
                                if (invoice.taxRatePercent == 0.0) EmeraldPositive.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (invoice.taxRatePercent == 0.0) "0% REVERSE CHG" else "${invoice.taxRatePercent}% VAT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.taxRatePercent == 0.0) EmeraldPositive else GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "DUE DATE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = invoice.dueDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.status == "OVERDUE") RoseNegative else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Cross-Border Status Stepper
            CrossBorderStatusProgressStepper(currentStatus = invoice.status)

            // Card Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Billed by: ${invoice.issuingEntityName}",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                // Interactive Formatted View & Status Progression Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenFormattedView != null) {
                        OutlinedButton(
                            onClick = onOpenFormattedView,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            modifier = Modifier.testTag("btn_view_formatted_invoice_${invoice.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = "Formatted", modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("FORMATTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    when (invoice.status.uppercase(Locale.US)) {
                        "DRAFT" -> {
                            Button(
                                onClick = { onUpdateStatus("SENT") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_send_${invoice.id}")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SEND INVOICE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        "SENT", "PENDING" -> {
                            Button(
                                onClick = { onUpdateStatus("PAID") },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_mark_paid_${invoice.id}")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Pay", modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("MARK PAID", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        "IN_CLEARING" -> {
                            Button(
                                onClick = { onUpdateStatus("PAID") },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_settle_${invoice.id}")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Settle", modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SETTLE WIRE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        "OVERDUE" -> {
                            Button(
                                onClick = { onUpdateStatus("PAID") },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseNegative, contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_settle_overdue_${invoice.id}")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Settle", modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SETTLE NOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldPositive.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Cleared", tint = EmeraldPositive, modifier = Modifier.size(10.dp))
                                    Text("PAID & CLEARED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal Progress Stepper for Invoice Lifecycle
 */
@Composable
private fun CrossBorderStatusProgressStepper(currentStatus: String) {
    val currentStep = when (currentStatus.uppercase(Locale.US)) {
        "DRAFT" -> 0
        "SENT", "PENDING" -> 1
        "IN_CLEARING" -> 2
        "PAID", "SETTLED" -> 3
        "OVERDUE", "DISPUTED" -> 1
        else -> 1
    }

    val steps = listOf("1. Draft", "2. Sent", "3. In-Clearing", "4. Paid")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, stepName ->
            val isDone = index <= currentStep
            val isCurrent = index == currentStep

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            when {
                                isDone -> EmeraldPositive
                                else -> BorderSubtle
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = SurfaceDark, modifier = Modifier.size(9.dp))
                    } else {
                        Text(text = "${index + 1}", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }
                }

                Text(
                    text = stepName,
                    fontSize = 8.sp,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                    color = when {
                        isCurrent -> CyanAccent
                        isDone -> TextPrimary
                        else -> TextMuted
                    },
                    fontFamily = FontFamily.Monospace
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(1.dp)
                        .background(if (index < currentStep) EmeraldPositive else BorderSubtle)
                )
            }
        }
    }
}

/**
 * Modal Dialog with Input Fields for Currency Selection, Real-Time Tax Calculations & Client Information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrossBorderInvoiceFormModal(
    entities: List<EntityRecord>,
    liveRates: Map<String, Double>,
    isOfflineMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) -> Unit
) {
    var issuingEntity by remember { mutableStateOf(entities.firstOrNull()?.name ?: "OmniGlobal Holdings Inc.") }
    val activeEntity = entities.find { it.name == issuingEntity } ?: entities.firstOrNull()
    var vendorTaxId by remember(issuingEntity) { mutableStateOf(activeEntity?.taxId ?: "CHE-102.345.678 MWST") }
    var clientName by remember { mutableStateOf("") }
    var clientCountry by remember { mutableStateOf("United Kingdom") }
    var amountStr by remember { mutableStateOf("85000") }
    var selectedCurrencyCode by remember { mutableStateOf("EUR") }
    var isReverseChargeEnabled by remember { mutableStateOf(true) }
    var customTaxPercentStr by remember { mutableStateOf("0.0") }
    var isCustomTaxOverride by remember { mutableStateOf(false) }
    var serviceDescription by remember { mutableStateOf("Enterprise Cross-Border Cloud SLA & Software Licensing") }
    var dueDateStr by remember { mutableStateOf("Nov 30, 2026") }
    var entityDropdownExpanded by remember { mutableStateOf(false) }

    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
    val parsedTaxOverride = customTaxPercentStr.toDoubleOrNull()

    // Real-Time Tax Calculation using Live Financial API Rates
    val taxCalculation = remember(parsedAmount, selectedCurrencyCode, clientCountry, isReverseChargeEnabled, isCustomTaxOverride, parsedTaxOverride, liveRates) {
        CrossBorderTaxCalculator.calculate(
            subtotal = parsedAmount,
            currencyCode = selectedCurrencyCode,
            clientCountry = clientCountry,
            isReverseChargeApplied = isReverseChargeEnabled && !isCustomTaxOverride,
            customVatRate = if (isCustomTaxOverride) parsedTaxOverride else null,
            liveRatesToUsd = liveRates
        )
    }

    val selectedCurrencyOption = CrossBorderTaxCalculator.getCurrencyOption(selectedCurrencyCode, liveRates)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Invoice", tint = CyanAccent)
                Text(
                    text = "NEW CROSS-BORDER INVOICE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Offline Notice Banner if in Offline Access Mode
                if (isOfflineMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoseNegative.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, RoseNegative.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_invoice_form_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Offline Mode", tint = RoseNegative, modifier = Modifier.size(14.dp))
                            Text(
                                text = "OFFLINE MODE: Invoice will be saved locally in Room SQLite database (v5). Queued for cloud ledger sync.",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Vendor Details & Issuing Entity Dropdown
                Text(text = "1. VENDOR / ISSUING ENTITY DETAILS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                ExposedDropdownMenuBox(
                    expanded = entityDropdownExpanded,
                    onExpandedChange = { entityDropdownExpanded = !entityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = issuingEntity,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("input_issuing_entity"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = entityDropdownExpanded,
                        onDismissRequest = { entityDropdownExpanded = false }
                    ) {
                        entities.forEach { entity ->
                            DropdownMenuItem(
                                text = { Text("${entity.name} (${entity.jurisdiction})", fontSize = 11.sp) },
                                onClick = {
                                    issuingEntity = entity.name
                                    vendorTaxId = entity.taxId
                                    entityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = vendorTaxId,
                    onValueChange = { vendorTaxId = it },
                    label = { Text("Vendor VAT / Tax ID", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_modal_vendor_tax_id"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Client Name & Country
                Text(text = "2. CLIENT & BILLING JURISDICTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Legal Name", fontSize = 10.sp) },
                        placeholder = { Text("e.g. Novartis AG", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.2f).testTag("input_client_name"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientCountry,
                        onValueChange = { clientCountry = it },
                        label = { Text("Client Country", fontSize = 10.sp) },
                        placeholder = { Text("e.g. Switzerland", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.9f).testTag("input_client_country"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Currency Selection (Input Field & Quick Selector Chips with Live FX Rates)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "3. REAL-TIME CURRENCY SELECTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "1 ${selectedCurrencyOption.code} = $${String.format(Locale.US, "%.4f", selectedCurrencyOption.rateToUsd)} USD",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CrossBorderTaxCalculator.getSupportedCurrencies(liveRates).forEach { curr ->
                        val isSelected = selectedCurrencyCode == curr.code
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedCurrencyCode = curr.code }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("select_currency_${curr.code}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${curr.flag} ${curr.code}",
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.3f", curr.rateToUsd)}",
                                    fontSize = 7.sp,
                                    color = if (isSelected) SurfaceDark.copy(alpha = 0.8f) else TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Subtotal (${selectedCurrencyCode})", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f).testTag("input_invoice_amount"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dueDateStr,
                        onValueChange = { dueDateStr = it },
                        label = { Text("Due Date", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f).testTag("input_invoice_due_date"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Cross-Border Tax Calculations Box
                Text(text = "4. CROSS-BORDER TAX CALCULATIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)

                // Reverse Charge Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply B2B Reverse Charge (0% VAT)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Customer accounts for VAT in recipient jurisdiction",
                            fontSize = 8.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isReverseChargeEnabled && !isCustomTaxOverride,
                        onCheckedChange = {
                            isReverseChargeEnabled = it
                            if (it) isCustomTaxOverride = false
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                        modifier = Modifier.testTag("toggle_reverse_charge")
                    )
                }

                // Real-Time Tax Calculation Summary Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.4f), EmeraldPositive.copy(alpha = 0.4f)))
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("tax_calculation_preview")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("${taxCalculation.currency} ${String.format(Locale.US, "%,.2f", taxCalculation.subtotal)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assessed VAT/Tax (${taxCalculation.vatRatePercent}%):", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("+${taxCalculation.currency} ${String.format(Locale.US, "%,.2f", taxCalculation.vatAmount)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (taxCalculation.vatAmount == 0.0) EmeraldPositive else GoldAccent, fontFamily = FontFamily.Monospace)
                        }

                        if (taxCalculation.whtAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Less Withholding Tax (${taxCalculation.whtRatePercent}% WHT):", fontSize = 9.sp, color = RoseNegative, fontFamily = FontFamily.Monospace)
                                Text("-${taxCalculation.currency} ${String.format(Locale.US, "%,.2f", taxCalculation.whtAmount)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoseNegative, fontFamily = FontFamily.Monospace)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = BorderSubtle)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Collectible Due:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text("${taxCalculation.currency} ${String.format(Locale.US, "%,.2f", taxCalculation.netReceivable)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Live USD Equivalent:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text("≈ $${String.format(Locale.US, "%,.2f", taxCalculation.equivalentUsd)} USD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }

                        Text(
                            text = taxCalculation.complianceNote,
                            fontSize = 8.sp,
                            color = TextMuted,
                            lineHeight = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Description
                OutlinedTextField(
                    value = serviceDescription,
                    onValueChange = { serviceDescription = it },
                    label = { Text("Service Description / Incoterms", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_service_desc"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank() && parsedAmount > 0) {
                        onConfirm(
                            issuingEntity,
                            clientName,
                            clientCountry,
                            taxCalculation.grossTotal,
                            selectedCurrencyCode,
                            taxCalculation.vatRatePercent,
                            serviceDescription,
                            dueDateStr
                        )
                    }
                },
                enabled = clientName.isNotBlank() && parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_create_invoice")
            ) {
                Text("ISSUE INVOICE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Detailed Cross-Border Invoice Dossier Modal with Tax Audit & Status Stepper
 */
@Composable
private fun CrossBorderInvoiceDossierModal(
    invoice: InvoiceRecord,
    liveRates: Map<String, Double>,
    providerName: String,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val stage = InvoiceStatusStage.fromString(invoice.status)
    val currencyOpt = CrossBorderTaxCalculator.getCurrencyOption(invoice.currency, liveRates)
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)
    val taxRule = CrossBorderTaxCalculator.getTaxRuleForCountry(invoice.clientCountry)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INVOICE DOSSIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = invoice.invoiceNumber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stage.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stage.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = stage.color,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client Info Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "CLIENT & JURISDICTION", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = invoice.clientName, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(text = "${countryNameToFlag(invoice.clientCountry)} ${invoice.clientCountry} • ${taxRule.complianceReference}", fontSize = 10.sp, color = TextSecondary)
                        Text(text = "Billed by: ${invoice.issuingEntityName}", fontSize = 9.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    }
                }

                // Cross-Border Tax & Settlement Breakdown
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "CROSS-BORDER COMMERCIAL SETTLEMENT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Invoice Amount:", fontSize = 10.sp, color = TextSecondary)
                            Text("${currencyOpt.symbol}${numberFmt.format(invoice.amount.roundToInt())} ${currencyOpt.code}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Statutory VAT/GST Rate:", fontSize = 10.sp, color = TextSecondary)
                            Text(if (invoice.taxRatePercent == 0.0) "0% Reverse Charge" else "${invoice.taxRatePercent}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Real-Time USD Valuation:", fontSize = 10.sp, color = TextSecondary)
                            Text("≈ $${numberFmt.format((invoice.amount * currencyOpt.rateToUsd).roundToInt())} USD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Live Interbank FX Rate:", fontSize = 9.sp, color = TextMuted)
                            Text("1 ${currencyOpt.code} = $${String.format(Locale.US, "%.4f", currencyOpt.rateToUsd)} USD", fontSize = 9.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Financial Feed Provider:", fontSize = 8.sp, color = TextMuted)
                            Text(providerName, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Due Date:", fontSize = 10.sp, color = TextSecondary)
                            Text(invoice.dueDate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Status Progression Actions
                Text(text = "TRANSITION SETTLEMENT STATUS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("DRAFT", "SENT", "PAID", "OVERDUE").forEach { statusOption ->
                        val isCurrent = invoice.status.equals(statusOption, ignoreCase = true)
                        val optStage = InvoiceStatusStage.fromString(statusOption)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) optStage.color.copy(alpha = 0.25f) else SurfaceElevated)
                                .border(1.dp, if (isCurrent) optStage.color else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { onUpdateStatus(statusOption) }
                                .padding(vertical = 7.dp)
                                .testTag("modal_transition_${statusOption.lowercase(Locale.US)}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optStage.label.uppercase(Locale.US),
                                fontSize = 8.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                color = if (isCurrent) optStage.color else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Empty Invoices Placeholder Card
 */
@Composable
private fun EmptyInvoicesCard(
    searchQuery: String,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(32.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No cross-border invoices match \"$searchQuery\"" else "No cross-border invoices on record.",
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            if (searchQuery.isNotEmpty()) {
                TextButton(onClick = onResetFilter) {
                    Text("Clear Filter", color = CyanAccent, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Utility: Converts country name string to Unicode flag emoji
 */
private fun countryNameToFlag(countryName: String): String {
    val upper = countryName.uppercase(Locale.US)
    return when {
        upper.contains("UNITED STATES") || upper.contains("USA") -> "🇺🇸"
        upper.contains("UNITED KINGDOM") || upper.contains("UK") || upper.contains("BRITAIN") -> "🇬🇧"
        upper.contains("GERMANY") || upper.contains("DEUTSCHLAND") -> "🇩🇪"
        upper.contains("SWITZERLAND") || upper.contains("SWISS") -> "🇨🇭"
        upper.contains("SINGAPORE") -> "🇸🇬"
        upper.contains("BRAZIL") || upper.contains("BRASIL") -> "🇧🇷"
        upper.contains("JAPAN") -> "🇯🇵"
        upper.contains("FRANCE") -> "🇫🇷"
        upper.contains("NETHERLANDS") || upper.contains("HOLLAND") -> "🇳🇱"
        upper.contains("IRELAND") -> "🇮🇪"
        upper.contains("AUSTRALIA") -> "🇦🇺"
        upper.contains("CANADA") -> "🇨🇦"
        upper.contains("EUROPE") || upper.contains("EU") -> "🇪🇺"
        else -> "🌐"
    }
}
