package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WorldBusinessOSTheme
import com.example.worldbusiness.data.local.WorldBusinessDatabase
import com.example.worldbusiness.data.repository.WorldBusinessRepository
import com.example.worldbusiness.ui.OSNavigationTab
import com.example.worldbusiness.ui.WorldBusinessViewModel
import com.example.worldbusiness.ui.WorldBusinessViewModelFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.worldbusiness.ui.components.ExecutiveHeader
import com.example.worldbusiness.ui.components.CurrencyConversionCalculatorModal
import com.example.worldbusiness.ui.components.RegulatoryThresholdAlertBanner
import com.example.worldbusiness.ui.components.RegulatoryThresholdModal
import com.example.worldbusiness.ui.screens.AuditLogScreen
import com.example.worldbusiness.ui.screens.CockpitScreen
import com.example.worldbusiness.ui.screens.CommercialScreen
import com.example.worldbusiness.ui.screens.EntitiesScreen
import com.example.worldbusiness.ui.screens.LogisticsScreen
import com.example.worldbusiness.ui.screens.TreasuryScreen
import com.example.worldbusiness.ui.screens.WorkforceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldBusinessOSTheme {
                val context = LocalContext.current
                val database = remember { WorldBusinessDatabase.getDatabase(context) }
                val repository = remember { WorldBusinessRepository(database) }
                val viewModel: WorldBusinessViewModel = viewModel(
                    factory = WorldBusinessViewModelFactory(repository)
                )

                WorldBusinessApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorldBusinessApp(
    viewModel: WorldBusinessViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val kpis by viewModel.executiveKpis.collectAsStateWithLifecycle()
    val entities by viewModel.entities.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val globalPayrollSummary by viewModel.globalPayrollSummary.collectAsStateWithLifecycle()
    val shipments by viewModel.shipments.collectAsStateWithLifecycle()
    val fxBalances by viewModel.fxBalances.collectAsStateWithLifecycle()
    val treasuryTransactions by viewModel.treasuryTransactions.collectAsStateWithLifecycle()
    val treasurySummary by viewModel.treasuryConversionSummary.collectAsStateWithLifecycle()
    val selectedBaseCurrency by viewModel.selectedBaseCurrency.collectAsStateWithLifecycle()
    val selectedHub by viewModel.selectedHub.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val calculatorModalVisible by viewModel.calculatorModalVisible.collectAsStateWithLifecycle()
    val calculatorAmount by viewModel.calculatorAmount.collectAsStateWithLifecycle()
    val calculatorFromCurrency by viewModel.calculatorFromCurrency.collectAsStateWithLifecycle()
    val calculatorToCurrency by viewModel.calculatorToCurrency.collectAsStateWithLifecycle()
    val conversionQuoteState by viewModel.conversionQuoteState.collectAsStateWithLifecycle()

    val complianceAlerts by viewModel.complianceAlerts.collectAsStateWithLifecycle()
    val complianceStats by viewModel.complianceOverviewStats.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val auditSummaryStats by viewModel.auditLogSummaryStats.collectAsStateWithLifecycle()
    val predictiveReport by viewModel.predictiveCashFlowReport.collectAsStateWithLifecycle()
    val liveCurrencyFeed by viewModel.liveCurrencyFeed.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val pendingInvoiceSyncCount by viewModel.pendingInvoiceSyncCount.collectAsStateWithLifecycle()
    val invoiceSyncState by viewModel.invoiceSyncState.collectAsStateWithLifecycle()
    val lastInvoiceSyncTimestamp by viewModel.lastInvoiceSyncTimestamp.collectAsStateWithLifecycle()
    val roomPayrollCurrencies by viewModel.roomPayrollCurrencies.collectAsStateWithLifecycle()
    val roomPayrollJurisdictions by viewModel.roomPayrollJurisdictions.collectAsStateWithLifecycle()
    val roomPayrollEmploymentTypes by viewModel.roomPayrollEmploymentTypes.collectAsStateWithLifecycle()
    val regulatoryThresholdAlerts by viewModel.regulatoryThresholdAlerts.collectAsStateWithLifecycle()
    val regulatoryThresholdStats by viewModel.regulatoryThresholdStats.collectAsStateWithLifecycle()
    var showRegulatoryThresholdModal by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // BackHandler: If user is on a secondary tab, pressing Back returns to the COCKPIT
    if (currentTab != OSNavigationTab.COCKPIT) {
        BackHandler {
            viewModel.selectTab(OSNavigationTab.COCKPIT)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("world_business_scaffold"),
        topBar = {
            Column {
                ExecutiveHeader(
                    consolidatedCashUsd = kpis.consolidatedCashUsd,
                    activeEntitiesCount = kpis.activeEntitiesCount
                )
                RegulatoryThresholdAlertBanner(
                    alerts = regulatoryThresholdAlerts,
                    onOpenModal = { showRegulatoryThresholdModal = true }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar"),
                containerColor = SurfaceDark
            ) {
                OSNavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val icon = when (tab) {
                        OSNavigationTab.COCKPIT -> Icons.Default.Public
                        OSNavigationTab.ENTITIES -> Icons.Default.AccountBalance
                        OSNavigationTab.TREASURY -> Icons.Default.AccountBalanceWallet
                        OSNavigationTab.COMMERCIAL -> Icons.Default.Receipt
                        OSNavigationTab.WORKFORCE -> Icons.Default.Groups
                        OSNavigationTab.LOGISTICS -> Icons.Default.LocalShipping
                        OSNavigationTab.AUDIT -> Icons.Default.Security
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SurfaceDark,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("global_snackbar_host")
            )
        },
        containerColor = SurfaceDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceDark)
        ) {
            when (currentTab) {
                OSNavigationTab.COCKPIT -> {
                    CockpitScreen(
                        kpis = kpis,
                        regionalHubs = viewModel.regionalHubs,
                        selectedHub = selectedHub,
                        macroIndicators = viewModel.macroIndicators,
                        entities = entities,
                        auditLogs = auditLogs,
                        auditSummaryStats = auditSummaryStats,
                        predictiveReport = predictiveReport,
                        onSelectCashFlowHorizon = { viewModel.setCashFlowHorizon(it) },
                        onSelectCashFlowScenario = { viewModel.setCashFlowScenario(it) },
                        onExportCashForecastCsv = { viewModel.exportCashForecastCsv() },
                        onSelectHub = { viewModel.selectHub(it) },
                        onNavigateTab = { viewModel.selectTab(it) },
                        onOpenCalculator = { viewModel.openConversionCalculator() }
                    )
                }
                OSNavigationTab.ENTITIES -> {
                    EntitiesScreen(
                        entities = entities,
                        onAddEntity = { name, jurisdiction, code, type, taxId, curr, cap, deadline, dir ->
                            viewModel.createEntity(name, jurisdiction, code, type, taxId, curr, cap, deadline, dir)
                        },
                        onUpdateEntityStatus = { entity, status ->
                            viewModel.updateEntityStatus(entity, status)
                        }
                    )
                }
                OSNavigationTab.TREASURY -> {
                    TreasuryScreen(
                        summary = treasurySummary,
                        selectedBaseCurrency = selectedBaseCurrency,
                        balances = fxBalances,
                        transactions = treasuryTransactions,
                        invoices = invoices,
                        predictiveReport = predictiveReport,
                        onSelectCashFlowHorizon = { viewModel.setCashFlowHorizon(it) },
                        onSelectCashFlowScenario = { viewModel.setCashFlowScenario(it) },
                        onExportCashForecastCsv = { viewModel.exportCashForecastCsv() },
                        onSelectBaseCurrency = { viewModel.selectBaseCurrency(it) },
                        onExecuteSwap = { from, to, amount, rate ->
                            viewModel.executeFxSwap(from, to, amount, rate)
                        },
                        onDeposit = { curr, amount ->
                            viewModel.depositFunds(curr, amount)
                        },
                        onAdjustBalance = { curr, newBal ->
                            viewModel.adjustBalance(curr, newBal)
                        },
                        onSimulateRateTicks = {
                            viewModel.simulateLiveMarketTicks()
                        },
                        onAddVault = { code, name, symbol, bal, rate ->
                            viewModel.addCurrencyVault(code, name, symbol, bal, rate)
                        },
                        onRecordTransaction = { recipient, counterparty, amount, currency, isCredit, category, note ->
                            viewModel.recordTreasuryTransaction(recipient, counterparty, amount, currency, isCredit, category, note)
                        },
                        onOpenCalculator = {
                            viewModel.openConversionCalculator()
                        },
                        complianceAlerts = complianceAlerts,
                        complianceStats = complianceStats,
                        onRemediateAlert = { alertId, newStatus ->
                            viewModel.remediateComplianceAlert(alertId, newStatus)
                        },
                        onSimulateHighRisk = {
                            viewModel.simulateComplianceRiskTransaction()
                        },
                        liveCurrencyFeed = liveCurrencyFeed,
                        onRefreshRates = {
                            viewModel.refreshLiveExchangeRates()
                        },
                        thresholdAlerts = regulatoryThresholdAlerts,
                        onAcknowledgeThresholdAlert = { viewModel.acknowledgeThresholdAlert(it) },
                        onFileThresholdReport = { viewModel.fileRegulatoryReport(it) }
                    )
                }
                OSNavigationTab.COMMERCIAL -> {
                    CommercialScreen(
                        invoices = invoices,
                        entities = entities,
                        fxBalances = fxBalances,
                        onCreateInvoice = { issuing, client, country, amount, curr, tax, desc, due ->
                            viewModel.createInvoice(issuing, client, country, amount, curr, tax, desc, due)
                        },
                        onMarkPaid = { id, num ->
                            viewModel.markInvoiceAsPaid(id, num)
                        },
                        onUpdateInvoiceStatus = { id, status ->
                            viewModel.updateInvoiceStatus(id, status)
                        },
                        liveCurrencyFeed = liveCurrencyFeed,
                        onRefreshRates = {
                            viewModel.refreshLiveExchangeRates()
                        },
                        isOfflineMode = isOfflineMode,
                        pendingSyncCount = pendingInvoiceSyncCount,
                        syncState = invoiceSyncState,
                        lastSyncTimestamp = lastInvoiceSyncTimestamp,
                        onToggleOfflineMode = {
                            viewModel.toggleOfflineMode(it)
                        },
                        onSyncLocalInvoices = {
                            viewModel.syncLocalInvoices()
                        }
                    )
                }
                OSNavigationTab.WORKFORCE -> {
                    WorkforceScreen(
                        teamMembers = teamMembers,
                        monthlyPayrollUsd = kpis.monthlyPayrollRunUsd,
                        payrollSummary = globalPayrollSummary,
                        balances = fxBalances,
                        roomCurrencies = roomPayrollCurrencies,
                        roomJurisdictions = roomPayrollJurisdictions,
                        roomEmploymentTypes = roomPayrollEmploymentTypes,
                        onRunPayroll = {
                            viewModel.runGlobalPayroll()
                        },
                        onExecuteBulkDisbursement = { schedDate, fundCurr, batches, memberIds, onSuccess ->
                            viewModel.executeBulkPayrollDisbursement(schedDate, fundCurr, batches, memberIds, onSuccess)
                        },
                        onFetchLiveRate = { from, to ->
                            viewModel.fetchLiveRate(from, to)
                        },
                        onAddMember = { name, role, country, code, type, sal, curr, tax ->
                            viewModel.addTeamMember(name, role, country, code, type, sal, curr, tax)
                        },
                        thresholdAlerts = regulatoryThresholdAlerts,
                        onAcknowledgeThresholdAlert = { viewModel.acknowledgeThresholdAlert(it) },
                        onFileThresholdReport = { viewModel.fileRegulatoryReport(it) }
                    )
                }
                OSNavigationTab.LOGISTICS -> {
                    LogisticsScreen(
                        shipments = shipments,
                        suppliers = suppliers,
                        invoices = invoices,
                        onDispatchShipment = { origin, dest, carrier, incoterm, desc, value, curr, eta ->
                            viewModel.dispatchShipment(origin, dest, carrier, incoterm, desc, value, curr, eta)
                        },
                        onAddSupplier = { supplier ->
                            viewModel.addSupplier(supplier)
                        },
                        onUpdateSupplierCompliance = { id, status, notes ->
                            viewModel.updateSupplierCompliance(id, status, notes)
                        },
                        onUpdateSupplierPaymentTerms = { id, days, desc, curr ->
                            viewModel.updateSupplierPaymentTerms(id, days, desc, curr)
                        },
                        onCreateInvoiceForSupplier = { supplier ->
                            viewModel.createInvoice(
                                issuingEntity = entities.firstOrNull()?.name ?: "OmniGlobal Holdings Inc.",
                                clientName = supplier.legalName,
                                clientCountry = supplier.country,
                                amount = when (supplier.preferredCurrency) {
                                    "EUR" -> 145000.0
                                    "JPY" -> 20000000.0
                                    "CHF" -> 160000.0
                                    "SGD" -> 210000.0
                                    else -> 175000.0
                                },
                                currency = supplier.preferredCurrency,
                                taxPercent = if (supplier.countryCode == "US" || supplier.countryCode == "CH") 0.0 else 19.0,
                                description = "Payable Settlement: ${supplier.businessCategory.replace('_', ' ')} (${supplier.supplierCode})",
                                dueDate = "Nov 30, 2026"
                            )
                        }
                    )
                }
                OSNavigationTab.AUDIT -> {
                    AuditLogScreen(
                        auditLogs = auditLogs,
                        summaryStats = auditSummaryStats,
                        onRecordAdminAction = { actionType, module, desc, amount, curr, src, dest ->
                            viewModel.recordAdminAction(actionType, module, desc, amount, curr, src, dest)
                        },
                        onVerifyChain = { viewModel.verifyAuditChainIntegrity() },
                        onExportCsv = { viewModel.exportAuditTrailCsv() }
                    )
                }
            }
        }
    }

    if (calculatorModalVisible) {
        CurrencyConversionCalculatorModal(
            amount = calculatorAmount,
            fromCurrency = calculatorFromCurrency,
            toCurrency = calculatorToCurrency,
            conversionState = conversionQuoteState,
            onAmountChange = { viewModel.setCalculatorAmount(it) },
            onFromCurrencyChange = { viewModel.setCalculatorFromCurrency(it) },
            onToCurrencyChange = { viewModel.setCalculatorToCurrency(it) },
            onSwapCurrencies = { viewModel.swapCalculatorCurrencies() },
            onRefreshRates = { viewModel.refreshCalculatorRates() },
            onDismiss = { viewModel.closeConversionCalculator() },
            onExecuteSwap = { from, to, amount, rate ->
                viewModel.executeFxSwap(from, to, amount, rate)
                viewModel.closeConversionCalculator()
            }
        )
    }

    if (showRegulatoryThresholdModal) {
        RegulatoryThresholdModal(
            alerts = regulatoryThresholdAlerts,
            stats = regulatoryThresholdStats,
            onDismiss = { showRegulatoryThresholdModal = false },
            onAcknowledge = { viewModel.acknowledgeThresholdAlert(it) },
            onFileReport = { viewModel.fileRegulatoryReport(it) }
        )
    }
}
