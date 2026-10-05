package com.example.worldbusiness.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.worldbusiness.data.local.RoomTreasuryAggregation
import com.example.worldbusiness.data.local.RoomPayrollCurrencyAggregation
import com.example.worldbusiness.data.local.RoomPayrollJurisdictionAggregation
import com.example.worldbusiness.data.local.RoomPayrollEmploymentTypeAggregation
import com.example.worldbusiness.data.local.RoomPayrollTotalsAggregation
import com.example.worldbusiness.data.model.AuditActionType
import com.example.worldbusiness.data.model.AuditChainVerificationResult
import com.example.worldbusiness.data.model.AuditLogModule
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.AuditLogStatus
import com.example.worldbusiness.data.model.AuditLogSummaryStats
import com.example.worldbusiness.data.model.CashFlowHorizon
import com.example.worldbusiness.data.model.CashFlowScenario
import com.example.worldbusiness.data.model.CurrencyAccountDetail
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.MacroIndicator
import com.example.worldbusiness.data.model.PredictiveCashFlowReport
import com.example.worldbusiness.data.model.RegionalHub
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.TreasuryConversionSummary
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import com.example.worldbusiness.data.model.ComplianceAlert
import com.example.worldbusiness.data.model.ComplianceAlertStatus
import com.example.worldbusiness.data.model.ComplianceOverviewStats
import com.example.worldbusiness.data.model.ConversionQuote
import com.example.worldbusiness.data.model.ConversionUiState
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch
import com.example.worldbusiness.data.model.BulkPayrollDisbursementReceipt
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.RegulatoryThresholdStats
import com.example.worldbusiness.data.model.ThresholdAlertStatus
import com.example.worldbusiness.data.repository.RegulatoryThresholdEngine
import com.example.worldbusiness.data.repository.AuditCryptographicEngine
import com.example.worldbusiness.data.repository.PayrollTaxEngine
import com.example.worldbusiness.data.repository.PredictiveCashFlowEngine
import com.example.worldbusiness.data.repository.RealTimeCurrencyService
import com.example.worldbusiness.data.repository.SupplierAuditExportEngine
import com.example.worldbusiness.data.repository.SyncOperationState
import com.example.worldbusiness.data.repository.TreasuryComplianceEngine
import com.example.worldbusiness.data.repository.WorldBusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OSNavigationTab(val label: String) {
    COCKPIT("Cockpit"),
    ENTITIES("Entities"),
    TREASURY("Treasury & FX"),
    COMMERCIAL("Invoicing"),
    WORKFORCE("Global Team"),
    LOGISTICS("Logistics"),
    AUDIT("Audit Log")
}

data class ExecutiveKpis(
    val consolidatedCashUsd: Double = 0.0,
    val pendingInvoicesUsd: Double = 0.0,
    val activeEntitiesCount: Int = 0,
    val globalTeamCount: Int = 0,
    val monthlyPayrollRunUsd: Double = 0.0,
    val inTransitCargoUsd: Double = 0.0,
    val complianceHealthScore: Int = 96
)

data class BankMeta(
    val institution: String,
    val maskedAccount: String,
    val swiftBic: String,
    val routing: String,
    val tier: String,
    val yieldApy: Double
)

class WorldBusinessViewModel(
    private val repository: WorldBusinessRepository
) : ViewModel() {

    val entities: StateFlow<List<EntityRecord>> = repository.allEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceRecord>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamMembers: StateFlow<List<TeamMemberRecord>> = repository.allTeamMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shipments: StateFlow<List<ShipmentRecord>> = repository.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fxBalances: StateFlow<List<FxBalanceRecord>> = repository.allBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomTreasuryTotals: StateFlow<RoomTreasuryAggregation> = repository.roomTreasuryTotals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomTreasuryAggregation(0, 0.0, 0.0, 0.0))

    val roomPayrollCurrencies: StateFlow<List<RoomPayrollCurrencyAggregation>> = repository.roomPayrollCurrencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomPayrollJurisdictions: StateFlow<List<RoomPayrollJurisdictionAggregation>> = repository.roomPayrollJurisdictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomPayrollEmploymentTypes: StateFlow<List<RoomPayrollEmploymentTypeAggregation>> = repository.roomPayrollEmploymentTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomPayrollTotals: StateFlow<RoomPayrollTotalsAggregation> = repository.roomPayrollTotals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomPayrollTotalsAggregation(0, 0))

    val treasuryTransactions: StateFlow<List<TreasuryTransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogRecord>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<GlobalSupplierRecord>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOfflineMode: StateFlow<Boolean> = repository.isOfflineMode
    val pendingInvoiceSyncCount: StateFlow<Int> = repository.pendingInvoiceSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val invoiceSyncState: StateFlow<SyncOperationState> = repository.invoiceSyncState
    val lastInvoiceSyncTimestamp: StateFlow<String> = repository.lastInvoiceSyncTimestamp

    fun toggleOfflineMode(enabled: Boolean) {
        repository.setOfflineMode(enabled)
        _userMessage.value = if (enabled) {
            "Offline Access Mode active: Invoices will be saved locally in Room SQLite."
        } else {
            "Online Mode restored: Ready to synchronize local Room database with enterprise ledger."
        }
    }

    fun syncLocalInvoices() {
        viewModelScope.launch {
            val result = repository.syncLocalInvoices()
            _userMessage.value = if (result.syncedCount > 0) {
                "Synchronized ${result.syncedCount} offline invoices from Room database to cloud ledger."
            } else {
                result.details
            }
        }
    }

    fun addSupplier(supplier: GlobalSupplierRecord) {
        viewModelScope.launch {
            repository.insertSupplier(supplier)
            _userMessage.value = "Supplier enrolled: ${supplier.legalName} (${supplier.country})"
        }
    }

    fun updateSupplierCompliance(supplierId: Long, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.updateSupplierCompliance(supplierId, newStatus, notes)
            _userMessage.value = "Supplier #$supplierId compliance updated to ${newStatus.replace('_', ' ')}"
        }
    }

    fun updateSupplierPaymentTerms(supplierId: Long, termsDays: Int, description: String, currency: String) {
        viewModelScope.launch {
            repository.updateSupplierPaymentTerms(supplierId, termsDays, description, currency)
            _userMessage.value = "Supplier #$supplierId payment terms updated: $description ($termsDays days in $currency)"
        }
    }

    val auditLogSummaryStats: StateFlow<AuditLogSummaryStats> = auditLogs.combine(repository.allAuditLogs) { logs, _ ->
        val totalVolume = logs.sumOf { it.financialAmount ?: 0.0 }
        val crossBorderCount = logs.count { it.destinationJurisdiction != null && it.destinationJurisdiction != "N/A" }
        val adminCount = logs.count { it.module == AuditLogModule.SYSTEM_ADMIN.name || it.module == AuditLogModule.ENTITIES.name }
        val verifiedPercent = if (logs.isNotEmpty()) {
            (logs.count { it.status == AuditLogStatus.VERIFIED_IMMUTABLE.name }.toDouble() / logs.size) * 100.0
        } else 100.0
        val flaggedAml = logs.count { it.status == AuditLogStatus.FLAGGED_AML_ALERT.name }
        val chainResult = AuditCryptographicEngine.verifyChainIntegrity(logs)

        AuditLogSummaryStats(
            totalLogsCount = logs.size,
            crossBorderTxCount = crossBorderCount,
            adminActionsCount = adminCount,
            totalVolumeAuditedUsd = totalVolume,
            verifiedImmutablePercent = verifiedPercent,
            flaggedAmlCount = flaggedAml,
            chainIntegrityValid = chainResult.isValid
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuditLogSummaryStats(0, 0, 0, 0.0, 100.0, 0, true))

    val globalPayrollSummary: StateFlow<GlobalPayrollSummary> = combine(
        teamMembers,
        fxBalances
    ) { members, balances ->
        PayrollTaxEngine.calculateGlobalPayrollSummary(members, balances)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GlobalPayrollSummary(0, 0.0, 0.0, 0.0, 0.0, 0, emptyList(), emptyList())
    )

    private val _complianceOverrides = MutableStateFlow<Map<String, ComplianceAlertStatus>>(emptyMap())

    val complianceAlerts: StateFlow<List<ComplianceAlert>> = combine(
        treasuryTransactions,
        _complianceOverrides,
        fxBalances
    ) { txs, overrides, balances ->
        val rateMap = balances.associate { it.currencyCode.uppercase(Locale.US) to it.rateToUsd }
        TreasuryComplianceEngine.analyzeTransactions(txs, overrides, rateMap)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val complianceOverviewStats: StateFlow<ComplianceOverviewStats> = combine(
        complianceAlerts,
        treasuryTransactions
    ) { alerts, txs ->
        TreasuryComplianceEngine.computeOverviewStats(alerts, txs.size)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ComplianceOverviewStats(0, 0, 0, 0, 0, 100, 0.0, 5)
    )

    private val _thresholdStatusOverrides = MutableStateFlow<Map<String, ThresholdAlertStatus>>(emptyMap())

    val regulatoryThresholdAlerts: StateFlow<List<RegulatoryThresholdAlert>> = combine(
        teamMembers,
        fxBalances,
        treasuryTransactions,
        globalPayrollSummary,
        _thresholdStatusOverrides
    ) { members, balances, txs, payrollSum, overrides ->
        RegulatoryThresholdEngine.evaluateThresholds(
            teamMembers = members,
            balances = balances,
            transactions = txs,
            payrollSummary = payrollSum,
            statusOverrides = overrides
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val regulatoryThresholdStats: StateFlow<RegulatoryThresholdStats> = regulatoryThresholdAlerts.combine(_thresholdStatusOverrides) { alerts, _ ->
        RegulatoryThresholdEngine.computeStats(alerts)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RegulatoryThresholdStats(0, 0, 0, 0, 0, 0)
    )

    fun acknowledgeThresholdAlert(alertId: String) {
        _thresholdStatusOverrides.value = _thresholdStatusOverrides.value + (alertId to ThresholdAlertStatus.ACKNOWLEDGED)
        _userMessage.value = "Regulatory compliance alert $alertId acknowledged by Chief Compliance Officer."
    }

    fun fileRegulatoryReport(alertId: String) {
        _thresholdStatusOverrides.value = _thresholdStatusOverrides.value + (alertId to ThresholdAlertStatus.REPORT_FILED)
        val alert = regulatoryThresholdAlerts.value.find { it.id == alertId }
        val formName = alert?.requiredFilingForm ?: "Statutory Reporting Declaration"
        val ref = "REG-FILING-${(10000..99999).random()}"
        viewModelScope.launch {
            repository.recordAuditLog(
                module = AuditLogModule.TAX_COMPLIANCE,
                actionType = AuditActionType.TAX_RETURN_FILED,
                sourceJurisdiction = alert?.jurisdictionCode ?: "GLOBAL",
                financialAmount = alert?.currentAmount,
                currency = alert?.currency,
                description = "Electronic regulatory submission $formName ($ref) transmitted to ${alert?.regulatoryBody ?: "Regulatory Agency"} for ${alert?.title ?: alertId}",
                complianceStandard = alert?.statutoryReference ?: "Statutory Compliance"
            )
        }
        _userMessage.value = "Regulatory Filing $formName successfully transmitted ($ref)."
    }

    val currencyService = RealTimeCurrencyService(viewModelScope)
    val liveCurrencyFeed: StateFlow<LiveCurrencyFeed> = currencyService.currencyFeed

    fun refreshLiveExchangeRates() {
        currencyService.refresh()
        _userMessage.value = "Synchronizing live exchange rates from financial API..."
    }

    private val _cashFlowHorizon = MutableStateFlow(CashFlowHorizon.DAYS_30)
    val cashFlowHorizon: StateFlow<CashFlowHorizon> = _cashFlowHorizon.asStateFlow()

    private val _cashFlowScenario = MutableStateFlow(CashFlowScenario.BASE_CASE)
    val cashFlowScenario: StateFlow<CashFlowScenario> = _cashFlowScenario.asStateFlow()

    val predictiveCashFlowReport: StateFlow<PredictiveCashFlowReport> = combine(
        combine(fxBalances, invoices, teamMembers) { b, i, tm -> Triple(b, i, tm) },
        combine(entities, shipments, treasuryTransactions) { e, s, tx -> Triple(e, s, tx) },
        combine(_cashFlowHorizon, _cashFlowScenario, liveCurrencyFeed) { h, sc, feed -> Triple(h, sc, feed) }
    ) { (balances, invs, members), (ents, shps, txs), (horizon, scenario, feed) ->
        val rateMap = if (feed.ratesToUsd.isNotEmpty()) feed.ratesToUsd else balances.associate { it.currencyCode.uppercase(Locale.US) to it.rateToUsd }
        PredictiveCashFlowEngine.calculateForecast(
            balances = balances,
            invoices = invs,
            teamMembers = members,
            entities = ents,
            shipments = shps,
            transactions = txs,
            horizon = horizon,
            scenario = scenario,
            customRates = if (rateMap.isNotEmpty()) rateMap else PredictiveCashFlowEngine.defaultRatesToUsd
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PredictiveCashFlowEngine.calculateForecast(emptyList(), emptyList())
    )

    fun setCashFlowHorizon(horizon: CashFlowHorizon) {
        _cashFlowHorizon.value = horizon
    }

    fun setCashFlowScenario(scenario: CashFlowScenario) {
        _cashFlowScenario.value = scenario
    }

    fun exportCashForecastCsv(): String {
        val report = predictiveCashFlowReport.value
        val sb = StringBuilder()
        sb.append("DayIndex,Date,ProjectedBalanceUsd,InflowUsd,OutflowUsd,NetFlowUsd,MilestoneEvent\n")
        report.dailyPoints.forEach { pt ->
            sb.append("${pt.dayIndex},\"${pt.dateFormatted}\",${pt.projectedBalanceUsd},${pt.inflowUsd},${pt.outflowUsd},${pt.netDailyFlowUsd},\"${pt.milestoneTitle ?: ""}\"\n")
        }
        return sb.toString()
    }

    val macroIndicators: List<MacroIndicator> = repository.getMacroIndicators()
    val regionalHubs: List<RegionalHub> = repository.getRegionalHubs()

    private val _currentTab = MutableStateFlow(OSNavigationTab.COCKPIT)
    val currentTab: StateFlow<OSNavigationTab> = _currentTab.asStateFlow()

    private val _selectedHub = MutableStateFlow<RegionalHub?>(null)
    val selectedHub: StateFlow<RegionalHub?> = _selectedHub.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _selectedBaseCurrency = MutableStateFlow("USD")
    val selectedBaseCurrency: StateFlow<String> = _selectedBaseCurrency.asStateFlow()

    // Currency Conversion Calculator Modal State
    private val _calculatorModalVisible = MutableStateFlow(false)
    val calculatorModalVisible: StateFlow<Boolean> = _calculatorModalVisible.asStateFlow()

    private val _calculatorAmount = MutableStateFlow("10000")
    val calculatorAmount: StateFlow<String> = _calculatorAmount.asStateFlow()

    private val _calculatorFromCurrency = MutableStateFlow("USD")
    val calculatorFromCurrency: StateFlow<String> = _calculatorFromCurrency.asStateFlow()

    private val _calculatorToCurrency = MutableStateFlow("EUR")
    val calculatorToCurrency: StateFlow<String> = _calculatorToCurrency.asStateFlow()

    private val _conversionQuoteState = MutableStateFlow<ConversionUiState>(ConversionUiState.Idle)
    val conversionQuoteState: StateFlow<ConversionUiState> = _conversionQuoteState.asStateFlow()

    private val bankMetadataMap = mapOf(
        "USD" to BankMeta("JPMorgan Chase Bank, N.A. (New York)", "US-CHAS-***-8821", "CHASUS33", "Fedwire ABA 021000021", "Tier 1 - Instant Liquidity", 4.85),
        "EUR" to BankMeta("Deutsche Bank AG (Frankfurt Corporate)", "DE-DEUT-***-0914", "DEUTDEFF", "Bundesbank / SEPA RT1", "Tier 1 - Instant Clearing", 3.40),
        "GBP" to BankMeta("Barclays Bank PLC (London 1 Churchill)", "GB-BARC-***-4912", "BARCGB22", "CHAPS / Sort Code 20-00-00", "Tier 1 - Faster Payments", 4.65),
        "SGD" to BankMeta("DBS Bank Ltd / OCBC Treasury (Singapore)", "SG-DBSS-***-3301", "DBSSSGSG", "FAST & MEPS Clearing", "Tier 1 - Real-Time Net", 3.80),
        "CHF" to BankMeta("UBS Switzerland AG (Zurich Paradeplatz)", "CH-UBSW-***-2290", "UBSWCHZH", "SIC High-Value Vault", "Tier 1 - Sovereign Reserve", 1.25),
        "JPY" to BankMeta("Mitsubishi UFJ Financial Group (Tokyo)", "JP-BOTK-***-5512", "BOTKJPJT", "BOJ-NET Settlement", "Tier 2 - Liquidity Buffer", 0.40),
        "BRL" to BankMeta("Banco Itaú BBA S.A. (São Paulo)", "BR-ITAU-***-7102", "ITAUBRSP", "BACEN / PIX Commercial Rail", "Tier 1 - Real-Time PIX", 10.75),
        "AUD" to BankMeta("ANZ Banking Group (Sydney Treasury)", "AU-ANZB-***-4401", "ANZBAU3S", "RBA Austraclear & NPP", "Tier 1 - Instant Liquidity", 4.35),
        "CAD" to BankMeta("Royal Bank of Canada (Toronto HQ)", "CA-ROYC-***-1189", "ROYCCAT2", "Lynx Large Value System", "Tier 1 - Overnight Repo", 3.90)
    )

    val treasuryConversionSummary: StateFlow<TreasuryConversionSummary> = combine(
        fxBalances,
        selectedBaseCurrency,
        liveCurrencyFeed
    ) { balances, baseCurr, feed ->
        val liveRates = feed.ratesToUsd

        // Effective USD exchange rate: prefers high-precision live financial API rate, falls back to Room record
        fun getEffectiveUsdRate(currencyCode: String, fallback: Double): Double {
            val live = liveRates[currencyCode.uppercase(Locale.US)]
            return if (live != null && live > 0.0) live else fallback
        }

        val baseRecord = balances.find { it.currencyCode == baseCurr } ?: balances.find { it.currencyCode == "USD" }
        val baseUsdRate = getEffectiveUsdRate(baseCurr, baseRecord?.rateToUsd ?: 1.0)

        val totalBaseValuation = balances.sumOf { b ->
            val effectiveRateToUsd = getEffectiveUsdRate(b.currencyCode, b.rateToUsd)
            val balanceInUsd = b.balance * effectiveRateToUsd
            if (baseUsdRate > 0.0) balanceInUsd / baseUsdRate else balanceInUsd
        }

        val details = balances.map { b ->
            val effectiveRateToUsd = getEffectiveUsdRate(b.currencyCode, b.rateToUsd)
            val meta = bankMetadataMap[b.currencyCode] ?: BankMeta(
                institution = "${b.currencyName} Commercial Custody",
                maskedAccount = "${b.currencyCode}-VAULT-***-001",
                swiftBic = "${b.currencyCode}XXMM1",
                routing = "ISO 20022 Direct",
                tier = "Tier 1 - Sovereign Liquidity",
                yieldApy = 3.50
            )

            // Rate of 1 unit of 'b' expressed in 'baseCurr'
            val rateInBase = if (baseUsdRate > 0.0) effectiveRateToUsd / baseUsdRate else 1.0
            val convertedBal = b.balance * rateInBase
            val share = if (totalBaseValuation > 0.0) (convertedBal / totalBaseValuation) * 100.0 else 0.0

            CurrencyAccountDetail(
                currencyCode = b.currencyCode,
                currencyName = b.currencyName,
                symbol = b.symbol,
                nativeBalance = b.balance,
                rateToUsd = effectiveRateToUsd,
                dailyChangePercent = b.dailyChangePercent,
                bankInstitution = meta.institution,
                accountMasked = meta.maskedAccount,
                swiftBic = meta.swiftBic,
                routingCode = meta.routing,
                liquidityTier = meta.tier,
                yieldApy = meta.yieldApy,
                convertedBalanceInBase = convertedBal,
                conversionRateToBase = rateInBase,
                portfolioSharePercent = share
            )
        }

        val weightedYield = if (totalBaseValuation > 0.0) {
            details.sumOf { it.convertedBalanceInBase * it.yieldApy } / totalBaseValuation
        } else 3.8

        // Calculate 24h impact based on dailyChangePercent of each currency
        val netImpactAmount = details.sumOf { acc ->
            acc.convertedBalanceInBase * (acc.dailyChangePercent / 100.0)
        }
        val netImpactPercent = if (totalBaseValuation > 0.0) (netImpactAmount / totalBaseValuation) * 100.0 else 0.0

        TreasuryConversionSummary(
            baseCurrency = baseCurr,
            totalConsolidatedBalance = totalBaseValuation,
            accountsCount = details.size,
            net24hImpactAmount = netImpactAmount,
            net24hImpactPercent = netImpactPercent,
            weightedYieldApy = weightedYield,
            accounts = details
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TreasuryConversionSummary(
            baseCurrency = "USD",
            totalConsolidatedBalance = 0.0,
            accountsCount = 0,
            net24hImpactAmount = 0.0,
            net24hImpactPercent = 0.0,
            weightedYieldApy = 4.0,
            accounts = emptyList()
        )
    )

    val executiveKpis: StateFlow<ExecutiveKpis> = combine(
        entities,
        invoices,
        teamMembers,
        shipments,
        fxBalances
    ) { ent, inv, team, ship, fx ->
        val totalCash = fx.sumOf { it.balance * it.rateToUsd }
        val pendingInv = inv.filter { it.status != "PAID" }.sumOf { invoice ->
            val rate = fx.find { it.currencyCode == invoice.currency }?.rateToUsd ?: 1.0
            invoice.amount * rate
        }
        val monthlyPayroll = team.sumOf { member ->
            val rate = fx.find { it.currencyCode == member.currency }?.rateToUsd ?: 1.0
            member.monthlyCompensation * rate
        }
        val cargoInTransit = ship.filter { it.customsStatus != "CLEARED" }.sumOf { item ->
            val rate = fx.find { it.currencyCode == item.currency }?.rateToUsd ?: 1.0
            item.cargoValue * rate
        }
        val avgCompliance = if (ent.isNotEmpty()) ent.map { it.complianceScore }.average().toInt() else 95

        ExecutiveKpis(
            consolidatedCashUsd = totalCash,
            pendingInvoicesUsd = pendingInv,
            activeEntitiesCount = ent.size,
            globalTeamCount = team.size,
            monthlyPayrollRunUsd = monthlyPayroll,
            inTransitCargoUsd = cargoInTransit,
            complianceHealthScore = avgCompliance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExecutiveKpis())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    fun selectTab(tab: OSNavigationTab) {
        _currentTab.value = tab
    }

    fun selectHub(hub: RegionalHub?) {
        _selectedHub.value = hub
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun createEntity(
        name: String,
        jurisdiction: String,
        countryCode: String,
        entityType: String,
        taxId: String,
        baseCurrency: String,
        operatingCapital: Double,
        filingDeadline: String,
        localDirector: String
    ) {
        viewModelScope.launch {
            val entity = EntityRecord(
                name = name,
                jurisdiction = jurisdiction,
                countryCode = countryCode.uppercase(Locale.US),
                entityType = entityType,
                taxId = taxId,
                status = "GOOD_STANDING",
                baseCurrency = baseCurrency,
                operatingCapital = operatingCapital,
                annualFilingDeadline = filingDeadline,
                localDirector = localDirector,
                complianceScore = 95
            )
            repository.insertEntity(entity)
            _userMessage.value = "New corporate entity incorporated: $name ($jurisdiction)"
        }
    }

    fun updateEntityStatus(entity: EntityRecord, newStatus: String) {
        viewModelScope.launch {
            repository.updateEntity(entity.copy(status = newStatus))
            _userMessage.value = "Operational status for ${entity.name} updated to ${newStatus.replace('_', ' ')}"
        }
    }

    fun createInvoice(
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val count = invoices.value.size + 1
            val invoiceNumber = "INV-2026-${String.format(Locale.US, "%04d", 891 + count)}"
            val invoice = InvoiceRecord(
                invoiceNumber = invoiceNumber,
                issuingEntityName = issuingEntity,
                clientName = clientName,
                clientCountry = clientCountry,
                issueDate = "Today",
                dueDate = dueDate,
                amount = amount,
                currency = currency,
                taxRatePercent = taxPercent,
                status = "PENDING",
                serviceDescription = description
            )
            repository.insertInvoice(invoice)
            val offline = repository.isOfflineMode.value
            _userMessage.value = if (offline) {
                "Invoice $invoiceNumber saved locally in Room SQLite (Offline Mode • Pending Sync)"
            } else {
                "Invoice $invoiceNumber issued to $clientName ($currency $amount)"
            }
        }
    }

    fun markInvoiceAsPaid(invoiceId: Long, invoiceNumber: String) {
        viewModelScope.launch {
            repository.markInvoicePaid(invoiceId)
            _userMessage.value = "Settlement verified for $invoiceNumber — funds cleared in Treasury."
        }
    }

    fun updateInvoiceStatus(invoiceId: Long, newStatus: String) {
        viewModelScope.launch {
            val inv = invoices.value.find { it.id == invoiceId }
            repository.updateInvoiceStatus(invoiceId, newStatus)
            val invName = inv?.invoiceNumber ?: "#$invoiceId"
            _userMessage.value = "Invoice $invName status updated to ${newStatus.replace('_', ' ')}"
        }
    }

    fun addTeamMember(
        name: String,
        role: String,
        country: String,
        countryCode: String,
        employmentType: String,
        salary: Double,
        currency: String,
        taxJurisdiction: String
    ) {
        viewModelScope.launch {
            val member = TeamMemberRecord(
                fullName = name,
                role = role,
                country = country,
                countryCode = countryCode.uppercase(Locale.US),
                employmentType = employmentType,
                monthlyCompensation = salary,
                currency = currency,
                status = "ACTIVE",
                taxJurisdiction = taxJurisdiction,
                lastPaidDate = "Pending Next Cycle"
            )
            repository.insertTeamMember(member)
            _userMessage.value = "Global employee onboarded: $name ($role, $country)"
        }
    }

    fun runGlobalPayroll() {
        viewModelScope.launch {
            val count = repository.executeGlobalPayrollRun()
            _userMessage.value = "Global payroll executed successfully for $count multinational team members!"
        }
    }

    fun executeBulkPayrollDisbursement(
        scheduledDate: String,
        fundingCurrency: String,
        batches: List<PayrollCurrencyDisbursementBatch>,
        selectedMemberIds: Set<Long>,
        onSuccess: (BulkPayrollDisbursementReceipt) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val receipt = repository.executeBulkMultiCurrencyPayroll(
                    scheduledDate = scheduledDate,
                    fundingCurrency = fundingCurrency,
                    batches = batches,
                    selectedMemberIds = selectedMemberIds
                )
                _userMessage.value = "Disbursed ${String.format(Locale.US, "%,.2f", receipt.totalFundingOutflow)} ${receipt.fundingVaultCurrency} across ${batches.size} currencies (${receipt.batchReferenceId})"
                onSuccess(receipt)
            } catch (e: Exception) {
                _userMessage.value = "Payroll execution failed: ${e.message}"
            }
        }
    }

    suspend fun fetchLiveRate(from: String, to: String): Double {
        return repository.fetchLiveConversionRate(from, to, 1.0).exchangeRate
    }

    fun remediateComplianceAlert(alertId: String, newStatus: ComplianceAlertStatus) {
        _complianceOverrides.value = _complianceOverrides.value + (alertId to newStatus)
        _userMessage.value = "Compliance Action Executed: Alert $alertId set to ${newStatus.label}"
    }

    fun simulateComplianceRiskTransaction() {
        viewModelScope.launch {
            val randomId = (1000..9999).random()
            val simulatedTx = TreasuryTransactionRecord(
                date = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US).format(Date()),
                recipient = "Al-Zahrani Maritime Arbitrage Ltd. (Unverified)",
                senderOrCounterparty = "OmniGlobal Treasury Escrow Outbound",
                amount = 48500.0,
                currency = "USD",
                isCredit = false,
                category = "COMMERCIAL_SETTLEMENT",
                status = "PENDING",
                referenceCode = "SWIFT-MT103-TEST-$randomId",
                note = "Urgent transshipment clearance fee to unverified maritime shadow fleet intermediary"
            )
            repository.insertTransaction(simulatedTx)
            _userMessage.value = "CRITICAL ALERT FLAGGED: Potential OFAC Sanctions / Shadow Fleet match on SWIFT-MT103-TEST-$randomId!"
        }
    }

    fun dispatchShipment(
        origin: String,
        destination: String,
        carrier: String,
        incoterm: String,
        cargoDescription: String,
        cargoValue: Double,
        currency: String,
        eta: String
    ) {
        viewModelScope.launch {
            val count = shipments.value.size + 1
            val tracking = "WBOS-EXP-${String.format(Locale.US, "%04d", 7000 + count)}"
            val shipment = ShipmentRecord(
                trackingCode = tracking,
                origin = origin,
                destination = destination,
                carrier = carrier,
                incoterm = incoterm,
                cargoDescription = cargoDescription,
                cargoValue = cargoValue,
                currency = currency,
                customsStatus = "IN_TRANSIT",
                estimatedArrival = eta
            )
            repository.insertShipment(shipment)
            _userMessage.value = "Freight dispatched: $tracking via $carrier ($incoterm)"
        }
    }

    fun selectBaseCurrency(currencyCode: String) {
        _selectedBaseCurrency.value = currencyCode
        _userMessage.value = "Conversion base currency set to $currencyCode"
    }

    fun depositFunds(currencyCode: String, amount: Double) {
        viewModelScope.launch {
            val success = repository.depositFunds(currencyCode, amount)
            if (success) {
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
                _userMessage.value = "Deposited $formatted $currencyCode into Treasury Vault"
            }
        }
    }

    fun adjustBalance(currencyCode: String, newBalance: Double) {
        viewModelScope.launch {
            val success = repository.adjustFxBalance(currencyCode, newBalance)
            if (success) {
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(newBalance)
                _userMessage.value = "Account balance updated: $currencyCode $formatted"
            }
        }
    }

    fun simulateLiveMarketTicks() {
        viewModelScope.launch {
            repository.simulateMarketRateTicks()
            _userMessage.value = "Live Interbank FX quotes refreshed with real-time ticks."
        }
    }

    fun addCurrencyVault(
        code: String,
        name: String,
        symbol: String,
        balance: Double,
        rateToUsd: Double
    ) {
        viewModelScope.launch {
            val record = FxBalanceRecord(
                currencyCode = code.uppercase(Locale.US),
                currencyName = name,
                symbol = symbol,
                balance = balance,
                rateToUsd = rateToUsd,
                dailyChangePercent = 0.0
            )
            repository.addCurrencyVault(record)
            _userMessage.value = "New Multi-Currency Vault created: $code ($name)"
        }
    }

    fun executeFxSwap(fromCurr: String, toCurr: String, amount: Double, rate: Double) {
        viewModelScope.launch {
            val success = repository.executeFxSwap(fromCurr, toCurr, amount, rate)
            if (success) {
                val converted = amount * rate
                val fmt = NumberFormat.getNumberInstance(Locale.US).format(converted)
                _userMessage.value = "FX Swap Executed: $amount $fromCurr -> $fmt $toCurr (SWIFT MT103 Ref #SWF-${(100000..999999).random()})"
            } else {
                _userMessage.value = "FX Transfer failed: Insufficient $fromCurr liquidity vault balance."
            }
        }
    }

    fun recordTreasuryTransaction(
        recipient: String,
        senderOrCounterparty: String,
        amount: Double,
        currency: String,
        isCredit: Boolean,
        category: String = "COMMERCIAL_SETTLEMENT",
        note: String = ""
    ) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
            val nowStr = dateFormat.format(Date())
            val prefix = when (currency) {
                "EUR" -> "SEPA-RT1"
                "GBP" -> "BACS-FPS"
                "SGD" -> "FAST-MEPS"
                "CHF" -> "SIC-RTGS"
                "BRL" -> "PIX-BACEN"
                else -> "SWIFT-MT103"
            }
            val refCode = "$prefix-${(10000..99999).random()}"
            val tx = TreasuryTransactionRecord(
                date = nowStr,
                recipient = recipient.trim(),
                senderOrCounterparty = senderOrCounterparty.trim(),
                amount = amount,
                currency = currency.uppercase(Locale.US),
                isCredit = isCredit,
                category = category,
                status = "SETTLED",
                referenceCode = refCode,
                note = note.trim()
            )
            repository.insertTransaction(tx)
            if (isCredit) {
                repository.depositFunds(currency, amount)
            } else {
                repository.deductFunds(currency, amount)
            }
            val formattedAmt = String.format(Locale.US, "%,.2f", amount)
            _userMessage.value = "${if (isCredit) "Inflow Credit" else "Outflow Debit"} of $currency $formattedAmt recorded: $refCode"
        }
    }

    fun openConversionCalculator(from: String = "USD", to: String = "EUR", amount: Double? = null) {
        _calculatorFromCurrency.value = from
        _calculatorToCurrency.value = to
        if (amount != null && amount > 0.0) {
            _calculatorAmount.value = String.format(Locale.US, "%.0f", amount)
        }
        _calculatorModalVisible.value = true
        calculateConversion()
    }

    fun closeConversionCalculator() {
        _calculatorModalVisible.value = false
    }

    fun setCalculatorAmount(amountStr: String) {
        _calculatorAmount.value = amountStr
        calculateConversion()
    }

    fun setCalculatorFromCurrency(code: String) {
        _calculatorFromCurrency.value = code
        calculateConversion()
    }

    fun setCalculatorToCurrency(code: String) {
        _calculatorToCurrency.value = code
        calculateConversion()
    }

    fun swapCalculatorCurrencies() {
        val oldFrom = _calculatorFromCurrency.value
        val oldTo = _calculatorToCurrency.value
        _calculatorFromCurrency.value = oldTo
        _calculatorToCurrency.value = oldFrom
        calculateConversion()
    }

    fun recordAdminAction(
        actionType: AuditActionType,
        module: AuditLogModule,
        description: String,
        amount: Double? = null,
        currency: String? = null,
        sourceJurisdiction: String = "Global Sovereign Enterprise Hub",
        destinationJurisdiction: String? = null,
        actorUsername: String = "compliance.officer@omniglobal.ch"
    ) {
        viewModelScope.launch {
            repository.recordAuditLog(
                module = module,
                actionType = actionType,
                actorUsername = actorUsername,
                actorRole = "Corporate Compliance Officer",
                actorIpAddress = "10.8.0.88 (VPN-Admin)",
                sourceJurisdiction = sourceJurisdiction,
                destinationJurisdiction = destinationJurisdiction,
                financialAmount = amount,
                currency = currency,
                description = description,
                complianceStandard = "SOX Sec 404 • Corporate Governance Audit"
            )
            _userMessage.value = "Administrative action logged & cryptographically certified: ${actionType.label}"
        }
    }

    fun verifyAuditChainIntegrity(): AuditChainVerificationResult {
        return AuditCryptographicEngine.verifyChainIntegrity(auditLogs.value)
    }

    fun exportAuditTrailCsv(): String {
        val logs = auditLogs.value
        val sb = StringBuilder()
        sb.append("LogId,Timestamp,Module,ActionType,Actor,SourceJurisdiction,DestinationJurisdiction,Amount,Currency,Status,ComplianceStandard,CryptographicHash\n")
        logs.forEach { log ->
            sb.append("${log.logId},\"${log.timestampFormatted}\",${log.module},${log.actionType},\"${log.actorUsername}\",\"${log.sourceJurisdiction}\",\"${log.destinationJurisdiction ?: ""}\",${log.financialAmount ?: 0.0},${log.currency ?: ""},${log.status},\"${log.complianceStandard}\",${log.cryptographicHash}\n")
        }
        return sb.toString()
    }

    fun exportSupplierDirectoryCsv(): String {
        val suppliersList = suppliers.value
        val csv = SupplierAuditExportEngine.generateCsv(suppliersList)
        viewModelScope.launch {
            repository.recordAuditLog(
                module = AuditLogModule.TAX_COMPLIANCE,
                actionType = AuditActionType.TAX_RETURN_FILED,
                sourceJurisdiction = "Global Compliance Controller Desk",
                description = "Supplier directory exported to CSV format for audit compliance (${suppliersList.size} suppliers)",
                complianceStandard = "SOX Sec 404 • OECD Due Diligence"
            )
        }
        _userMessage.value = "Exported ${suppliersList.size} suppliers to CSV"
        return csv
    }

    fun exportSupplierAuditDossier(): String {
        val suppliersList = suppliers.value
        val report = SupplierAuditExportEngine.generateAuditDossierText(suppliersList)
        viewModelScope.launch {
            repository.recordAuditLog(
                module = AuditLogModule.TAX_COMPLIANCE,
                actionType = AuditActionType.TAX_RETURN_FILED,
                sourceJurisdiction = "Global Compliance Controller Desk",
                description = "Statutory supplier audit dossier generated for regulatory submission (${suppliersList.size} suppliers)",
                complianceStandard = "OECD Due Diligence • US FCPA • UK Bribery Act"
            )
        }
        _userMessage.value = "Generated Statutory Supplier Audit Dossier (${suppliersList.size} suppliers)"
        return report
    }

    fun refreshCalculatorRates() {
        calculateConversion(forceToast = true)
    }

    private fun calculateConversion(forceToast: Boolean = false) {
        val amount = _calculatorAmount.value.toDoubleOrNull() ?: 0.0
        val from = _calculatorFromCurrency.value
        val to = _calculatorToCurrency.value

        _conversionQuoteState.value = ConversionUiState.Loading

        viewModelScope.launch {
            try {
                val quote = repository.fetchLiveConversionRate(from, to, amount)
                _conversionQuoteState.value = ConversionUiState.Success(quote)
                if (forceToast) {
                    val rateFmt = String.format(Locale.US, "%.4f", quote.exchangeRate)
                    _userMessage.value = "Updated real-time rate: 1 $from = $rateFmt $to (${if (quote.isLiveApi) "Live API" else "Cached"})"
                }
            } catch (e: Exception) {
                _conversionQuoteState.value = ConversionUiState.Error(e.message ?: "Failed to calculate conversion rate")
            }
        }
    }
}

class WorldBusinessViewModelFactory(
    private val repository: WorldBusinessRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorldBusinessViewModel::class.java)) {
            return WorldBusinessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
