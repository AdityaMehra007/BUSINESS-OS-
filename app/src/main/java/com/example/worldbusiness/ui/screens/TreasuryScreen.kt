package com.example.worldbusiness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.worldbusiness.data.model.ComplianceAlert
import com.example.worldbusiness.data.model.ComplianceAlertStatus
import com.example.worldbusiness.data.model.ComplianceOverviewStats
import com.example.worldbusiness.data.model.CurrencyAccountDetail
import com.example.worldbusiness.data.model.CashFlowHorizon
import com.example.worldbusiness.data.model.CashFlowScenario
import com.example.worldbusiness.data.model.CurrencySyncStatus
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.model.PredictiveCashFlowReport
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.ThresholdCategory
import com.example.worldbusiness.data.model.TreasuryConversionSummary
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import com.example.worldbusiness.ui.components.D3CurrencyVolatilityHeatmap
import com.example.worldbusiness.ui.components.Formatters
import com.example.worldbusiness.ui.components.PredictiveCashFlowWidget
import com.example.worldbusiness.ui.components.RealTimeCurrencyTickerBanner
import com.example.worldbusiness.ui.components.RecentTreasuryTransactionsList
import com.example.worldbusiness.ui.components.RecordTreasuryTransactionDialog
import com.example.worldbusiness.ui.components.RegulatoryThresholdAlertCard
import com.example.worldbusiness.ui.components.TreasuryBalanceTrendVisualization
import com.example.worldbusiness.ui.components.TreasuryComplianceAlertView
import com.example.worldbusiness.ui.components.TreasuryHedgingView
import com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper
import java.text.NumberFormat
import java.util.Locale

enum class TreasuryViewMode(val label: String) {
    ACCOUNTS("Vaults & Conversions"),
    FORECAST("Cash Flow Forecast"),
    HEDGING("FX Hedging Engine"),
    VOLATILITY_HEATMAP("D3 Volatility Heatmap"),
    TRENDS("30D Balance Trends"),
    COMPLIANCE("AML Compliance Alerts"),
    TRANSACTIONS("Recent Ledger"),
    CONVERTER("Live FX Converter"),
    SWAPS("Interbank FX Swaps")
}

@Composable
fun TreasuryScreen(
    summary: TreasuryConversionSummary,
    selectedBaseCurrency: String,
    balances: List<FxBalanceRecord>,
    transactions: List<TreasuryTransactionRecord> = emptyList(),
    complianceAlerts: List<ComplianceAlert> = emptyList(),
    complianceStats: ComplianceOverviewStats = ComplianceOverviewStats(0, 0, 0, 0, 0, 100, 0.0, 5),
    invoices: List<InvoiceRecord> = emptyList(),
    onSelectBaseCurrency: (String) -> Unit,
    onExecuteSwap: (from: String, to: String, amount: Double, rate: Double) -> Unit,
    onDeposit: (currency: String, amount: Double) -> Unit,
    onAdjustBalance: (currency: String, newBal: Double) -> Unit,
    onSimulateRateTicks: () -> Unit,
    onAddVault: (code: String, name: String, symbol: String, bal: Double, rate: Double) -> Unit,
    onRecordTransaction: (
        recipient: String,
        counterparty: String,
        amount: Double,
        currency: String,
        isCredit: Boolean,
        category: String,
        note: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onOpenCalculator: () -> Unit = {},
    onRemediateAlert: (alertId: String, newStatus: ComplianceAlertStatus) -> Unit = { _, _ -> },
    onSimulateHighRisk: () -> Unit = {},
    predictiveReport: PredictiveCashFlowReport? = null,
    onSelectCashFlowHorizon: (CashFlowHorizon) -> Unit = {},
    onSelectCashFlowScenario: (CashFlowScenario) -> Unit = {},
    onExportCashForecastCsv: () -> String = { "" },
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    onRefreshRates: (() -> Unit)? = null,
    thresholdAlerts: List<RegulatoryThresholdAlert> = emptyList(),
    onAcknowledgeThresholdAlert: (String) -> Unit = {},
    onFileThresholdReport: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(TreasuryViewMode.ACCOUNTS) }
    var selectedAccountForDetail by remember { mutableStateOf<CurrencyAccountDetail?>(null) }
    var selectedAccountForDeposit by remember { mutableStateOf<CurrencyAccountDetail?>(null) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showRecordTransactionDialog by remember { mutableStateOf(false) }

    val crossBorderAlerts = remember(thresholdAlerts) {
        thresholdAlerts.filter { it.category == ThresholdCategory.CROSS_BORDER_TRANSFER }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("treasury_screen")
            .testTag("treasury_management_dashboard"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            TreasuryHeaderBar(
                onSimulateTicks = onSimulateRateTicks,
                onAddVaultClick = { showAddVaultDialog = true },
                onOpenCalculator = onOpenCalculator,
                liveCurrencyFeed = liveCurrencyFeed,
                onRefreshRates = onRefreshRates
            )
        }

        // Real-Time Financial API Live Currency Ticker Ribbon
        item {
            RealTimeCurrencyTickerBanner(
                feed = liveCurrencyFeed,
                onRefresh = onRefreshRates
            )
        }

        // Regulatory Compliance: Cross-Border Wire Transfer Threshold Alerts
        if (crossBorderAlerts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("treasury_threshold_alert_section"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    .size(24.dp)
                                    .background(GoldAccent.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Threshold Warning",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "CROSS-BORDER WIRE REPORTING THRESHOLD ALERTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "${crossBorderAlerts.size} Monitored",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    crossBorderAlerts.forEach { alert ->
                        RegulatoryThresholdAlertCard(
                            alert = alert,
                            onAcknowledge = onAcknowledgeThresholdAlert,
                            onFileReport = onFileThresholdReport
                        )
                    }
                }
            }
        }

        // Consolidated Multi-Currency Hero Card with Real-time Conversion Base Selector
        item {
            ConsolidatedTreasuryHeroCard(
                summary = summary,
                selectedBaseCurrency = selectedBaseCurrency,
                onSelectBase = onSelectBaseCurrency
            )
        }

        // Visual Multi-Currency Allocation Ring & Distribution
        item {
            TreasuryPortfolioAllocationCard(
                summary = summary,
                onCurrencyClick = { code ->
                    val acc = summary.accounts.find { it.currencyCode == code }
                    if (acc != null) selectedAccountForDetail = acc
                }
            )
        }

        // 30-Day Historical Account Balance Trend Line Visualization (D3 / Recharts Spline & Area)
        item {
            TreasuryBalanceTrendVisualization(
                summary = summary,
                transactions = transactions,
                initialCurrencyCode = "ALL",
                onSelectVault = { vaultCode ->
                    if (vaultCode != "ALL") {
                        val acc = summary.accounts.find { it.currencyCode == vaultCode }
                        if (acc != null) selectedAccountForDetail = acc
                    }
                }
            )
        }

        // Dashboard Sub-View Selector
        item {
            TreasuryViewModeSelector(
                selectedMode = viewMode,
                onSelectMode = { viewMode = it }
            )
        }

        // Content depending on selected sub-view
        when (viewMode) {
            TreasuryViewMode.ACCOUNTS -> {
                // Compliance Alert Banner if active flags exist
                if (complianceStats.activeFlagsCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoseNegative.copy(alpha = 0.12f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                                listOf(RoseNegative.copy(alpha = 0.6f), BorderSubtle)
                            )),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewMode = TreasuryViewMode.COMPLIANCE }
                                .padding(vertical = 4.dp)
                                .testTag("treasury_compliance_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "AML Alert",
                                        tint = RoseNegative,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "AML COMPLIANCE: ${complianceStats.activeFlagsCount} Active Risk Flags",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseNegative,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "FinCEN / FINMA / MAS statutory attention needed",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = "Inspect ➔",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // D3 FX Volatility Radar Quick Access Banner
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                            listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle)
                        )),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewMode = TreasuryViewMode.VOLATILITY_HEATMAP }
                            .padding(vertical = 2.dp)
                            .testTag("treasury_volatility_heatmap_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "Volatility Heatmap",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "D3 VOLATILITY HEATMAP: 10 Cross-Currency Pairs Active",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "High variance detected in USD/JPY (13.8%) & GBP/JPY (14.6%)",
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "Analyze ➔",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOVEREIGN MULTI-CURRENCY VAULTS (${summary.accounts.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "CONVERTED TO $selectedBaseCurrency",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Account cards with Real-time Conversion Summaries
                items(summary.accounts, key = { it.currencyCode }) { account ->
                    MultiCurrencyAccountCard(
                        account = account,
                        baseCurrency = selectedBaseCurrency,
                        onDepositClick = { selectedAccountForDeposit = account },
                        onDetailClick = { selectedAccountForDetail = account }
                    )
                }

                // Recent Treasury Transactions Section Card on Main Dashboard
                item {
                    RecentTreasuryTransactionsList(
                        transactions = transactions,
                        maxItems = 4,
                        title = "RECENT TREASURY TRANSACTIONS",
                        showHeader = true,
                        showFilters = false,
                        showSearch = false,
                        showSummaryStats = true,
                        onRecordTransactionClick = { showRecordTransactionDialog = true },
                        onViewAllClick = { viewMode = TreasuryViewMode.TRANSACTIONS },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Yield Arbitrage
                item {
                    TreasuryYieldSection()
                }
            }

            TreasuryViewMode.FORECAST -> {
                predictiveReport?.let { report ->
                    item {
                        PredictiveCashFlowWidget(
                            report = report,
                            onSelectHorizon = onSelectCashFlowHorizon,
                            onSelectScenario = onSelectCashFlowScenario,
                            onExportCsv = onExportCashForecastCsv
                        )
                    }
                }
            }

            TreasuryViewMode.HEDGING -> {
                item {
                    D3CurrencyVolatilityHeatmap(
                        onConfigureHedge = { pair, strategy ->
                            val parts = pair.split("/")
                            val baseCurr = parts.getOrNull(0) ?: "EUR"
                            onRecordTransaction(
                                "Hedging Desk (${strategy.shortCode})",
                                "JPMorgan Chase FX Derivatives",
                                250000.0,
                                baseCurr,
                                false,
                                "HEDGING",
                                "D3 Volatility Heatmap informed ${strategy.label} locked for $pair corridor"
                            )
                        }
                    )
                }

                item {
                    TreasuryHedgingView(
                        invoices = invoices,
                        balances = balances,
                        onExecuteHedge = { suggestion ->
                            onRecordTransaction(
                                "Hedging Desk (${suggestion.strategyType.shortCode})",
                                suggestion.counterparty,
                                suggestion.exposureAmount,
                                suggestion.currency,
                                false,
                                "HEDGING",
                                "Automated ${suggestion.strategyType.label} executed for ${suggestion.invoiceNumber} @ ${String.format(Locale.US, "%.4f", suggestion.forwardRate)}"
                            )
                        }
                    )
                }
            }

            TreasuryViewMode.VOLATILITY_HEATMAP -> {
                item {
                    D3CurrencyVolatilityHeatmap(
                        onConfigureHedge = { pair, strategy ->
                            val parts = pair.split("/")
                            val baseCurr = parts.getOrNull(0) ?: "EUR"
                            onRecordTransaction(
                                "Hedging Desk (${strategy.shortCode})",
                                "JPMorgan Chase FX Derivatives",
                                250000.0,
                                baseCurr,
                                false,
                                "HEDGING",
                                "D3 Volatility Heatmap informed ${strategy.label} locked for $pair corridor"
                            )
                        }
                    )
                }
            }

            TreasuryViewMode.TRENDS -> {
                // Dedicated Deep-Dive Analytical View for 30-Day Historical Trends
                item {
                    TreasuryBalanceTrendVisualization(
                        summary = summary,
                        transactions = transactions,
                        initialCurrencyCode = "ALL",
                        onSelectVault = { vaultCode ->
                            if (vaultCode != "ALL") {
                                val acc = summary.accounts.find { it.currencyCode == vaultCode }
                                if (acc != null) selectedAccountForDetail = acc
                            }
                        }
                    )
                }

                item {
                    TreasuryTrendDeepDiveSection(
                        summary = summary,
                        balances = balances
                    )
                }
            }

            TreasuryViewMode.TRANSACTIONS -> {
                // Dedicated Full Ledger View for Treasury Transactions
                item {
                    RecentTreasuryTransactionsList(
                        transactions = transactions,
                        maxItems = null,
                        title = "GLOBAL TREASURY TRANSACTIONS LEDGER",
                        showHeader = true,
                        showFilters = true,
                        showSearch = true,
                        showSummaryStats = true,
                        onRecordTransactionClick = { showRecordTransactionDialog = true }
                    )
                }
            }

            TreasuryViewMode.CONVERTER -> {
                // Instant Multi-Quote Real-time Converter
                item {
                    LiveCurrencyConverterCard(
                        accounts = summary.accounts,
                        selectedBaseCurrency = selectedBaseCurrency
                    )
                }

                // Real-time Interbank Cross-Rate Matrix
                item {
                    CrossRateMatrixCard(
                        balances = balances,
                        selectedBaseCurrency = selectedBaseCurrency
                    )
                }
            }

            TreasuryViewMode.SWAPS -> {
                item {
                    FxSwapExecutionSection(
                        balances = balances,
                        onExecuteSwap = onExecuteSwap
                    )
                }

                item {
                    TreasuryYieldSection()
                }
            }

            TreasuryViewMode.COMPLIANCE -> {
                item {
                    TreasuryComplianceAlertView(
                        alerts = complianceAlerts,
                        stats = complianceStats,
                        onRemediateAlert = onRemediateAlert,
                        onSimulateHighRiskTx = onSimulateHighRisk
                    )
                }
            }
        }
    }

    // Account Detail Dialog
    selectedAccountForDetail?.let { acc ->
        AccountClearingDetailDialog(
            account = acc,
            baseCurrency = selectedBaseCurrency,
            onDismiss = { selectedAccountForDetail = null },
            onQuickDeposit = {
                selectedAccountForDeposit = acc
                selectedAccountForDetail = null
            }
        )
    }

    // Deposit or Adjust Balance Dialog
    selectedAccountForDeposit?.let { acc ->
        DepositOrAdjustDialog(
            account = acc,
            onDismiss = { selectedAccountForDeposit = null },
            onDeposit = { amount ->
                onDeposit(acc.currencyCode, amount)
                selectedAccountForDeposit = null
            },
            onAdjust = { newBal ->
                onAdjustBalance(acc.currencyCode, newBal)
                selectedAccountForDeposit = null
            }
        )
    }

    // Incorporate New Foreign Exchange Vault Dialog
    if (showAddVaultDialog) {
        AddCurrencyVaultDialog(
            onDismiss = { showAddVaultDialog = false },
            onConfirm = { code, name, symbol, bal, rate ->
                onAddVault(code, name, symbol, bal, rate)
                showAddVaultDialog = false
            }
        )
    }

    // Record / Settle New Treasury Transaction Dialog
    if (showRecordTransactionDialog) {
        RecordTreasuryTransactionDialog(
            availableCurrencies = summary.accounts.map { it.currencyCode }.ifEmpty { listOf("USD", "EUR", "GBP", "SGD", "CHF", "JPY", "BRL") },
            onDismiss = { showRecordTransactionDialog = false },
            onConfirm = { recipient, counterparty, amount, currency, isCredit, category, note ->
                onRecordTransaction(recipient, counterparty, amount, currency, isCredit, category, note)
                showRecordTransactionDialog = false
            }
        )
    }
}

@Composable
private fun TreasuryHeaderBar(
    onSimulateTicks: () -> Unit,
    onAddVaultClick: () -> Unit,
    onOpenCalculator: () -> Unit = {},
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    onRefreshRates: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TREASURY & FX CONTROL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(liveCurrencyFeed.status.color.copy(alpha = alphaPulse))
                )
            }
            Text(
                text = "Multi-Currency Liquidity Engine",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Live Rates Sync from Financial API
            if (onRefreshRates != null) {
                Button(
                    onClick = onRefreshRates,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_sync_live_fx")
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Sync Live FX",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE FX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Calculator Modal Action
            Button(
                onClick = onOpenCalculator,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_open_calculator_modal")
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "FX Calculator",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "CALCULATOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Live Quotes Refresh / Tick Simulator Button
            OutlinedButton(
                onClick = onSimulateTicks,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                modifier = Modifier.testTag("btn_refresh_fx_ticks")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Ticks",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "TICKS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // New Currency Vault
            Button(
                onClick = onAddVaultClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_add_currency_vault")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Vault",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "VAULT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ConsolidatedTreasuryHeroCard(
    summary: TreasuryConversionSummary,
    selectedBaseCurrency: String,
    onSelectBase: (String) -> Unit
) {
    val supportedCurrencies = listOf("USD", "EUR", "GBP", "SGD", "CHF", "JPY", "BRL")
    val isPositiveImpact = summary.net24hImpactAmount >= 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceElevated,
                        SurfaceDark
                    )
                )
            )
            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("treasury_consolidated_hero")
            .testTag("room_multicurrency_summary_card")
            .testTag("multicurrency_balances_summary")
    ) {
        Column {
            // Base Currency Selector Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CONSOLIDATED CONVERSION BASE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .testTag("room_treasury_vaults_badge")
                            .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ROOM DB • ${summary.accountsCount} VAULTS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPositiveImpact) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositiveImpact) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = "24h Impact",
                        tint = if (isPositiveImpact) EmeraldPositive else RoseNegative,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${if (isPositiveImpact) "+" else ""}${String.format(Locale.US, "%.2f", summary.net24hImpactPercent)}% 24H",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositiveImpact) EmeraldPositive else RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Currency Selector Toggle Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                supportedCurrencies.forEach { code ->
                    val isSelected = selectedBaseCurrency == code
                    val chipColor = Formatters.getCurrencyColor(code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) chipColor else SurfaceDark)
                            .border(1.dp, if (isSelected) chipColor else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { onSelectBase(code) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("base_currency_chip_$code")
                    ) {
                        Text(
                            text = code,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) SurfaceDark else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Consolidated Value
            Text(
                text = "TOTAL CONSOLIDATED LIQUIDITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )

            Text(
                text = Formatters.formatCurrency(summary.totalConsolidatedBalance, selectedBaseCurrency),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Real-time unified valuation calculated from ${summary.accountsCount} sovereign foreign exchange accounts",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Executive Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroMetric(
                    label = "24H VALUATION GAIN",
                    value = Formatters.formatCurrency(summary.net24hImpactAmount, selectedBaseCurrency),
                    color = if (isPositiveImpact) EmeraldPositive else RoseNegative
                )
                HeroMetric(
                    label = "WEIGHTED YIELD APY",
                    value = "${String.format(Locale.US, "%.2f", summary.weightedYieldApy)}%",
                    color = GoldAccent
                )
                HeroMetric(
                    label = "HEDGED STABILITY",
                    value = "94.8% Covered",
                    color = CyanAccent
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TreasuryPortfolioAllocationCard(
    summary: TreasuryConversionSummary,
    onCurrencyClick: (String) -> Unit
) {
    val accounts = summary.accounts
    if (accounts.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("treasury_allocation_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = "Allocation",
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REAL-TIME PORTFOLIO COMPOSITION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "${accounts.size} CURRENCIES",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Segmented Allocation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(SurfaceElevated)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalWidth = size.width
                var currentX = 0f

                accounts.forEach { acc ->
                    val segmentWidth = (acc.portfolioSharePercent / 100f).toFloat() * totalWidth
                    if (segmentWidth > 0f) {
                        drawRect(
                            color = Formatters.getCurrencyColor(acc.currencyCode),
                            topLeft = Offset(currentX, 0f),
                            size = Size(segmentWidth, size.height)
                        )
                        currentX += segmentWidth
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Currency Distribution Badges / Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                val color = Formatters.getCurrencyColor(acc.currencyCode)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .clickable { onCurrencyClick(acc.currencyCode) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${acc.currencyCode}: ${String.format(Locale.US, "%.1f", acc.portfolioSharePercent)}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TreasuryViewModeSelector(
    selectedMode: TreasuryViewMode,
    onSelectMode: (TreasuryViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TreasuryViewMode.values().forEach { mode ->
            val isSelected = selectedMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyanAccent else SurfaceDark)
                    .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("treasury_tab_${mode.name.lowercase(Locale.US)}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) SurfaceDark else TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MultiCurrencyAccountCard(
    account: CurrencyAccountDetail,
    baseCurrency: String,
    onDepositClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    val currencyColor = Formatters.getCurrencyColor(account.currencyCode)
    val isPositive = account.dailyChangePercent >= 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("account_card_${account.currencyCode}")
    ) {
        Column {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currencyColor.copy(alpha = 0.15f))
                            .border(1.dp, currencyColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.currencyCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = currencyColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = account.currencyName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${account.bankInstitution} • ${account.accountMasked}",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 24h Change Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPositive) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Change",
                        tint = if (isPositive) EmeraldPositive else RoseNegative,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", account.dailyChangePercent)}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) EmeraldPositive else RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time Conversion Summary Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOCAL NATIVE BALANCE",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = Formatters.formatCurrency(account.nativeBalance, account.currencyCode),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CONVERTED TO $baseCurrency",
                            fontSize = 9.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = Formatters.formatCurrency(account.convertedBalanceInBase, baseCurrency),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1 ${account.currencyCode} = ${Formatters.formatRate(account.conversionRateToBase)} $baseCurrency",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Share: ${String.format(Locale.US, "%.1f", account.portfolioSharePercent)}% • Yield: ${account.yieldApy}% APY",
                        fontSize = 10.sp,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Deposit / Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.testTag("btn_details_${account.currencyCode}")
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Clearing Info", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("CLEARING INFO", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDepositClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_deposit_${account.currencyCode}")
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = "Deposit", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("DEPOSIT / ADJUST", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveCurrencyConverterCard(
    accounts: List<CurrencyAccountDetail>,
    selectedBaseCurrency: String
) {
    var sourceCurrency by remember { mutableStateOf("USD") }
    var sourceAmountStr by remember { mutableStateOf("100000") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    val sourceAccount = accounts.find { it.currencyCode == sourceCurrency } ?: accounts.firstOrNull()
    val sourceUsdRate = sourceAccount?.rateToUsd ?: 1.0
    val amount = sourceAmountStr.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("live_currency_converter_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "Exchange",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REAL-TIME MULTI-QUOTE CONVERTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "INTERBANK LIVE SPREAD",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Currency & Amount Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = sourceDropdownExpanded,
                onExpandedChange = { sourceDropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = sourceCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Base Currency", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = sourceDropdownExpanded,
                    onDismissRequest = { sourceDropdownExpanded = false }
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.currencyCode} - ${acc.currencyName}", fontSize = 11.sp) },
                            onClick = {
                                sourceCurrency = acc.currencyCode
                                sourceDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = sourceAmountStr,
                onValueChange = { sourceAmountStr = it },
                label = { Text("Quotation Amount", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("input_converter_amount"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "LIVE CONVERSION MATRIX ACROSS ALL CORRIDORS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Real-time Conversion Quote Grid powered by MultiCurrencyConversionHelper
        val liveRates = accounts.associate { it.currencyCode to it.rateToUsd }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            accounts.filter { it.currencyCode != sourceCurrency }.forEach { target ->
                val conversion = MultiCurrencyConversionHelper.convert(
                    amount = amount,
                    fromCurrency = sourceCurrency,
                    toCurrency = target.currencyCode,
                    liveRatesToUsd = liveRates,
                    spreadBps = 3.0
                )
                val color = Formatters.getCurrencyColor(target.currencyCode)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.currencyCode,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = target.currencyName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "1 $sourceCurrency = ${Formatters.formatRate(conversion.midMarketRate)} ${target.currencyCode} (Inv: ${Formatters.formatRate(conversion.inverseRate)})",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = conversion.roundedFormattedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Spread: 3 bps (${target.symbol}${String.format(Locale.US, "%.2f", conversion.spreadCostAmount)})",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrossRateMatrixCard(
    balances: List<FxBalanceRecord>,
    selectedBaseCurrency: String
) {
    val majorPairs = listOf(
        Pair("EUR", "USD"),
        Pair("GBP", "USD"),
        Pair("USD", "JPY"),
        Pair("USD", "CHF"),
        Pair("USD", "SGD"),
        Pair("EUR", "GBP"),
        Pair("USD", "BRL")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INTERBANK G10 CORRIDOR SPREADS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "SWIFT / EBS BENCHMARKS",
                fontSize = 9.sp,
                color = GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            majorPairs.forEach { (curr1, curr2) ->
                val r1 = balances.find { it.currencyCode == curr1 }?.rateToUsd ?: 1.0
                val r2 = balances.find { it.currencyCode == curr2 }?.rateToUsd ?: 1.0
                val rate = if (r2 > 0.0) r1 / r2 else 1.0
                val bid = rate * 0.9999
                val ask = rate * 1.0001

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$curr1 / $curr2",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Bid: ${Formatters.formatRate(bid)}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Ask: ${Formatters.formatRate(ask)}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Spread 0.02%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FxSwapExecutionSection(
    balances: List<FxBalanceRecord>,
    onExecuteSwap: (from: String, to: String, amount: Double, rate: Double) -> Unit
) {
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    var swapAmountStr by remember { mutableStateOf("50000") }
    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    val fromRecord = balances.find { it.currencyCode == fromCurrency }
    val toRecord = balances.find { it.currencyCode == toCurrency }

    val swapAmount = swapAmountStr.toDoubleOrNull() ?: 0.0
    val liveRates = balances.associate { it.currencyCode to it.rateToUsd }
    val swapQuote = MultiCurrencyConversionHelper.calculateSwapExecutionQuote(
        fromCurrency = fromCurrency,
        toCurrency = toCurrency,
        amount = swapAmount,
        liveRatesToUsd = liveRates,
        isInstitutionalWholesale = true
    )
    val convertedAmount = swapQuote.toAmountReceived
    val effectiveCrossRate = swapQuote.executionRate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("fx_swap_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "Exchange",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INSTANT INTERBANK FX SETTLEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "SPREAD: 0.02%",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Source Currency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = fromDropdownExpanded,
                onExpandedChange = { fromDropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = fromCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sell From", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = fromDropdownExpanded,
                    onDismissRequest = { fromDropdownExpanded = false }
                ) {
                    balances.forEach { b ->
                        DropdownMenuItem(
                            text = { Text("${b.currencyCode} - ${b.currencyName}", fontSize = 12.sp) },
                            onClick = {
                                fromCurrency = b.currencyCode
                                fromDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = swapAmountStr,
                onValueChange = { swapAmountStr = it },
                label = { Text("Transfer Amount", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("input_swap_amount"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        fromRecord?.let {
            Text(
                text = "Available Vault: ${Formatters.formatCurrency(it.balance, it.currencyCode)}",
                fontSize = 10.sp,
                color = if (it.balance >= swapAmount) TextSecondary else RoseNegative,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Swap Direction Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable {
                        val temp = fromCurrency
                        fromCurrency = toCurrency
                        toCurrency = temp
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap Currencies",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Target Currency & Converted Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = toDropdownExpanded,
                onExpandedChange = { toDropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = toCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Buy To", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = toDropdownExpanded,
                    onDismissRequest = { toDropdownExpanded = false }
                ) {
                    balances.forEach { b ->
                        DropdownMenuItem(
                            text = { Text("${b.currencyCode} - ${b.currencyName}", fontSize = 12.sp) },
                            onClick = {
                                toCurrency = b.currencyCode
                                toDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(text = "Estimated Proceeds", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = Formatters.formatCurrency(convertedAmount, toCurrency),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rate: 1 $fromCurrency = ${Formatters.formatRate(effectiveCrossRate)} $toCurrency (Mid: ${Formatters.formatRate(swapQuote.marketMidRate)})",
                fontSize = 9.sp,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Rail: ${swapQuote.clearingRail} (${swapQuote.estimatedSettlementTime})",
                fontSize = 9.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (swapAmount > 0) {
                    onExecuteSwap(fromCurrency, toCurrency, swapAmount, effectiveCrossRate)
                }
            },
            enabled = (fromRecord != null && fromRecord.balance >= swapAmount && swapAmount > 0),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_execute_swap"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "EXECUTE CROSS-BORDER FX SWAP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TreasuryYieldSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Yields",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SHORT-TERM BENCHMARK YIELDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "TREASURY OPTIMIZER",
                fontSize = 9.sp,
                color = GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            YieldChip(name = "US 3M T-Bill", rate = "4.38%")
            YieldChip(name = "Euribor 3M", rate = "3.12%")
            YieldChip(name = "SORA (Singapore)", rate = "3.65%")
            YieldChip(name = "SARON (Swiss)", rate = "1.20%")
        }
    }
}

@Composable
private fun YieldChip(name: String, rate: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = name, fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = rate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AccountClearingDetailDialog(
    account: CurrencyAccountDetail,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onQuickDeposit: () -> Unit
) {
    val currencyColor = Formatters.getCurrencyColor(account.currencyCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(currencyColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.currencyCode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = currencyColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${account.currencyName} Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailItem("Depository Institution", account.bankInstitution)
                DetailItem("Masked Account / IBAN", account.accountMasked)
                DetailItem("SWIFT BIC Code", account.swiftBic)
                DetailItem("Local Clearing Network", account.routingCode)
                DetailItem("Liquidity Classification", account.liquidityTier)
                DetailItem("Annualized Yield (APY)", "${account.yieldApy}%")

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "REAL-TIME CONVERSION VALUATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Native: ${Formatters.formatCurrency(account.nativeBalance, account.currencyCode)}",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Valuation in $baseCurrency: ${Formatters.formatCurrency(account.convertedBalanceInBase, baseCurrency)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Conversion Factor: 1 ${account.currencyCode} = ${Formatters.formatRate(account.conversionRateToBase)} $baseCurrency",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onQuickDeposit,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("DEPOSIT / ADJUST", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun DepositOrAdjustDialog(
    account: CurrencyAccountDetail,
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit,
    onAdjust: (Double) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0: Deposit, 1: Set Absolute Balance
    var amountStr by remember { mutableStateOf("100000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "${account.currencyCode} VAULT CAPITALIZATION",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current Reserve: ${Formatters.formatCurrency(account.nativeBalance, account.currencyCode)}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (mode == 0) CyanAccent else SurfaceElevated)
                            .clickable { mode = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ DEPOSIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == 0) SurfaceDark else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (mode == 1) CyanAccent else SurfaceElevated)
                            .clickable { mode = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SET BALANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == 1) SurfaceDark else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (mode == 0) "Deposit Amount (${account.currencyCode})" else "New Balance Amount (${account.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        if (mode == 0) onDeposit(amount) else onAdjust(amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text(if (mode == 0) "CONFIRM DEPOSIT" else "UPDATE BALANCE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddCurrencyVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, symbol: String, bal: Double, rateToUsd: Double) -> Unit
) {
    var code by remember { mutableStateOf("AUD") }
    var name by remember { mutableStateOf("Australian Dollar (Sydney Custody)") }
    var symbol by remember { mutableStateOf("A$") }
    var balanceStr by remember { mutableStateOf("1250000") }
    var rateStr by remember { mutableStateOf("0.6650") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "INCORPORATE NEW CURRENCY VAULT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("ISO Code", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("Symbol", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vault Account Descriptor", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Initial Liquidity", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Rate to USD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        val bal = balanceStr.toDoubleOrNull() ?: 500000.0
                        val rate = rateStr.toDoubleOrNull() ?: 1.0
                        onConfirm(code, name, symbol, bal, rate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("INITIALIZE VAULT", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TreasuryTrendDeepDiveSection(
    summary: TreasuryConversionSummary,
    balances: List<FxBalanceRecord>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("treasury_trend_deep_dive"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "30-DAY LIQUIDITY & YIELD DYNAMICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "DAILY CLOSING MARK-TO-MARKET",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        // Vault 30D Performance Breakdown Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "VAULT PERFORMANCE & 30D DRIFT COMPARISON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            summary.accounts.forEach { acc ->
                val isPositive = acc.dailyChangePercent >= 0.0
                val color = if (isPositive) EmeraldPositive else RoseNegative

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Formatters.getCurrencyColor(acc.currencyCode))
                        )
                        Column {
                            Text(
                                text = "${acc.currencyCode} • ${acc.bankInstitution}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "APY: ${String.format(Locale.US, "%.2f", acc.yieldApy)}% • Tier: ${acc.liquidityTier}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${acc.symbol}${Formatters.formatCurrency(acc.nativeBalance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", acc.dailyChangePercent)}% 24h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
