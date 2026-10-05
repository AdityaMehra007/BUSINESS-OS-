package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.local.WorldBusinessDatabase
import com.example.worldbusiness.data.local.RoomTreasuryAggregation
import com.example.worldbusiness.data.local.RoomPayrollCurrencyAggregation
import com.example.worldbusiness.data.local.RoomPayrollJurisdictionAggregation
import com.example.worldbusiness.data.local.RoomPayrollEmploymentTypeAggregation
import com.example.worldbusiness.data.local.RoomPayrollTotalsAggregation
import com.example.worldbusiness.data.model.AuditActionType
import com.example.worldbusiness.data.model.AuditLogModule
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.AuditLogStatus
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.MacroIndicator
import com.example.worldbusiness.data.model.RegionalHub
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import com.example.worldbusiness.data.model.ConversionQuote
import com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch
import com.example.worldbusiness.data.model.BulkPayrollDisbursementReceipt
import com.example.worldbusiness.data.remote.CurrencyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorldBusinessRepository(private val database: WorldBusinessDatabase) {
    private val entityDao = database.entityDao()
    private val invoiceDao = database.invoiceDao()
    private val teamMemberDao = database.teamMemberDao()
    private val shipmentDao = database.shipmentDao()
    private val fxBalanceDao = database.fxBalanceDao()
    private val treasuryTransactionDao = database.treasuryTransactionDao()
    private val auditLogDao = database.auditLogDao()
    private val globalSupplierDao = database.globalSupplierDao()
    private val currencyApiService: CurrencyApiService = CurrencyApiService.create()

    val invoiceSyncEngine: InvoiceOfflineSyncEngine by lazy { InvoiceOfflineSyncEngine(invoiceDao, this) }

    val allEntities: Flow<List<EntityRecord>> = entityDao.getAllEntities()
    val allInvoices: Flow<List<InvoiceRecord>> = invoiceDao.getAllInvoices()
    val pendingInvoiceSyncCount: Flow<Int> = invoiceDao.getPendingSyncCount()
    val isOfflineMode: StateFlow<Boolean> get() = invoiceSyncEngine.isOfflineMode
    val invoiceSyncState: StateFlow<SyncOperationState> get() = invoiceSyncEngine.syncState
    val lastInvoiceSyncTimestamp: StateFlow<String> get() = invoiceSyncEngine.lastSyncTimestamp
    val allTeamMembers: Flow<List<TeamMemberRecord>> = teamMemberDao.getAllTeamMembers()
    val allShipments: Flow<List<ShipmentRecord>> = shipmentDao.getAllShipments()
    val allBalances: Flow<List<FxBalanceRecord>> = fxBalanceDao.getAllBalances()
    val roomTreasuryTotals: Flow<RoomTreasuryAggregation> = fxBalanceDao.getConsolidatedTreasuryTotals()
    val roomPayrollCurrencies: Flow<List<RoomPayrollCurrencyAggregation>> = teamMemberDao.getPayrollCurrencyAggregations()
    val roomPayrollJurisdictions: Flow<List<RoomPayrollJurisdictionAggregation>> = teamMemberDao.getPayrollJurisdictionAggregations()
    val roomPayrollEmploymentTypes: Flow<List<RoomPayrollEmploymentTypeAggregation>> = teamMemberDao.getPayrollEmploymentTypeAggregations()
    val roomPayrollTotals: Flow<RoomPayrollTotalsAggregation> = teamMemberDao.getPayrollTotalsAggregation()
    val allTransactions: Flow<List<TreasuryTransactionRecord>> = treasuryTransactionDao.getAllTransactions()
    val allAuditLogs: Flow<List<AuditLogRecord>> = auditLogDao.getAllLogs()
    val allSuppliers: Flow<List<GlobalSupplierRecord>> = globalSupplierDao.getAllSuppliers()
    val activeSuppliers: Flow<List<GlobalSupplierRecord>> = globalSupplierDao.getActiveSuppliers()

    suspend fun recordAuditLog(
        module: AuditLogModule,
        actionType: AuditActionType,
        actorUsername: String = "cfo.alex@omniglobal.ch",
        actorRole: String = "VP Global Treasury & Compliance",
        actorIpAddress: String = "10.8.0.42 (VPN-ZRH)",
        sourceJurisdiction: String,
        destinationJurisdiction: String? = null,
        financialAmount: Double? = null,
        currency: String? = null,
        description: String,
        complianceStandard: String = "SOX Sec 404 • EU MiFID II",
        metadataJson: String = "{}"
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date(now))
        val logId = "AUD-2026-${(10000..99999).random()}"

        val prevHash = auditLogDao.getRecentLogs(1).firstOrNull()?.firstOrNull()?.cryptographicHash
            ?: AuditCryptographicEngine.GENESIS_HASH

        val cryptoHash = AuditCryptographicEngine.generateLogHash(
            previousHash = prevHash,
            timestamp = now,
            logId = logId,
            module = module.name,
            actionType = actionType.name,
            actorUsername = actorUsername,
            amount = financialAmount,
            currency = currency,
            sourceJurisdiction = sourceJurisdiction,
            destinationJurisdiction = destinationJurisdiction
        )

        val log = AuditLogRecord(
            logId = logId,
            timestamp = now,
            timestampFormatted = formattedDate,
            module = module.name,
            actionType = actionType.name,
            actorUsername = actorUsername,
            actorRole = actorRole,
            actorIpAddress = actorIpAddress,
            sourceJurisdiction = sourceJurisdiction,
            destinationJurisdiction = destinationJurisdiction,
            currency = currency,
            financialAmount = financialAmount,
            status = AuditLogStatus.VERIFIED_IMMUTABLE.name,
            complianceStandard = complianceStandard,
            previousHash = prevHash,
            cryptographicHash = cryptoHash,
            description = description,
            metadataJson = metadataJson
        )
        auditLogDao.insertLog(log)
    }

    suspend fun insertTransaction(transaction: TreasuryTransactionRecord): Long = withContext(Dispatchers.IO) {
        val result = treasuryTransactionDao.insertTransaction(transaction)
        recordAuditLog(
            module = AuditLogModule.TREASURY,
            actionType = AuditActionType.TREASURY_TRANSFER,
            sourceJurisdiction = "Consolidated Treasury Pool",
            destinationJurisdiction = transaction.recipient,
            financialAmount = transaction.amount,
            currency = transaction.currency,
            description = "Treasury transaction recorded: ${transaction.recipient} (${transaction.amount} ${transaction.currency}) - ${transaction.category}",
            complianceStandard = "SOX Sec 404 • FinCEN BSA"
        )
        result
    }

    suspend fun insertEntity(entity: EntityRecord): Long = withContext(Dispatchers.IO) {
        val result = entityDao.insertEntity(entity)
        recordAuditLog(
            module = AuditLogModule.ENTITIES,
            actionType = AuditActionType.ENTITY_CREATED,
            sourceJurisdiction = entity.jurisdiction,
            financialAmount = entity.operatingCapital,
            currency = entity.baseCurrency,
            description = "New corporate entity incorporated: ${entity.name} (${entity.entityType}) in ${entity.jurisdiction}",
            complianceStandard = "Delaware DGCL / Swiss OR / ACRA"
        )
        result
    }

    suspend fun updateEntity(entity: EntityRecord) = withContext(Dispatchers.IO) {
        entityDao.updateEntity(entity)
        recordAuditLog(
            module = AuditLogModule.ENTITIES,
            actionType = AuditActionType.ENTITY_STATUS_UPDATED,
            sourceJurisdiction = entity.jurisdiction,
            description = "Entity status updated to ${entity.status} for ${entity.name} (Compliance Score: ${entity.complianceScore})",
            complianceStandard = "OECD BEPS Action 13"
        )
    }

    suspend fun insertInvoice(invoice: InvoiceRecord): Long = withContext(Dispatchers.IO) {
        invoiceSyncEngine.saveInvoice(invoice)
    }

    suspend fun insertInvoiceOffline(invoice: InvoiceRecord): Long = withContext(Dispatchers.IO) {
        invoiceSyncEngine.saveInvoice(invoice, forceOffline = true)
    }

    suspend fun syncLocalInvoices(): InvoiceSyncResult = withContext(Dispatchers.IO) {
        invoiceSyncEngine.syncPendingInvoices()
    }

    fun setOfflineMode(enabled: Boolean) {
        invoiceSyncEngine.setOfflineMode(enabled)
    }

    suspend fun markInvoicePaid(invoiceId: Long) = withContext(Dispatchers.IO) {
        invoiceDao.updateStatus(invoiceId, "PAID")
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
        val nowStr = dateFormat.format(Date())
        treasuryTransactionDao.insertTransaction(
            TreasuryTransactionRecord(
                date = nowStr,
                recipient = "OmniGlobal Sovereign Treasury Pool",
                senderOrCounterparty = "Enterprise Client Invoiced",
                amount = 175000.0,
                currency = "USD",
                isCredit = true,
                category = "COMMERCIAL_SETTLEMENT",
                status = "SETTLED",
                referenceCode = "SWIFT-MT103-${(10000..99999).random()}",
                note = "Cross-border Invoice #$invoiceId settlement cleared"
            )
        )
        recordAuditLog(
            module = AuditLogModule.COMMERCIAL,
            actionType = AuditActionType.INVOICE_PAID,
            sourceJurisdiction = "Client Commercial Account",
            destinationJurisdiction = "OmniGlobal Sovereign Treasury Pool",
            financialAmount = 175000.0,
            currency = "USD",
            description = "Invoice #$invoiceId marked as PAID. Cleared via SWIFT MT103 settlement.",
            complianceStandard = "SWIFT Customer Security Programme (CSP)"
        )
    }

    suspend fun updateInvoiceStatus(invoiceId: Long, newStatus: String) = withContext(Dispatchers.IO) {
        if (newStatus == "PAID" || newStatus == "SETTLED") {
            markInvoicePaid(invoiceId)
        } else {
            invoiceDao.updateStatus(invoiceId, newStatus)
            recordAuditLog(
                module = AuditLogModule.COMMERCIAL,
                actionType = AuditActionType.INVOICE_STATUS_UPDATED,
                sourceJurisdiction = "Cross-Border Commercial Desk",
                description = "Invoice #$invoiceId settlement status transitioned to $newStatus",
                complianceStandard = "ISO 20022 Cross-Border Status Reporting"
            )
        }
    }

    suspend fun insertTeamMember(member: TeamMemberRecord): Long = withContext(Dispatchers.IO) {
        teamMemberDao.insertMember(member)
    }

    suspend fun executeGlobalPayrollRun(): Int = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val todayStr = dateFormat.format(Date())
        teamMemberDao.markAllAsPaid(todayStr)
        val count = teamMemberDao.getCount()
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
        treasuryTransactionDao.insertTransaction(
            TreasuryTransactionRecord(
                date = timeFormat.format(Date()),
                recipient = "Global Workforce Multi-Jurisdiction Clearing",
                senderOrCounterparty = "OmniGlobal Consolidated Payroll Account",
                amount = 118950.0,
                currency = "USD",
                isCredit = false,
                category = "GLOBAL_PAYROLL",
                status = "SETTLED",
                referenceCode = "BATCH-PAY-${(10000..99999).random()}",
                note = "Batch payroll disbursement for $count multinational team members"
            )
        )
        count
    }

    suspend fun executeBulkMultiCurrencyPayroll(
        scheduledDate: String,
        fundingCurrency: String,
        batches: List<PayrollCurrencyDisbursementBatch>,
        selectedMemberIds: Set<Long>
    ): BulkPayrollDisbursementReceipt = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss 'UTC'", Locale.US)
        val nowStr = timeFormat.format(Date())
        val randomBatchSuffix = (10000..99999).random()
        val masterBatchRef = "BATCH-PAY-2026-MULTI-$randomBatchSuffix"

        val totalFundingOutflow = batches.sumOf { it.fundingCurrencyEquivalent }
        val totalTaxesWithheldFunding = batches.sumOf { batch ->
            if (batch.netPayableLocalAmount > 0) {
                batch.taxWithheldLocalAmount * batch.exchangeRateToFunding
            } else 0.0
        }

        // 1. Deduct funding currency from source vault
        val fundingVault = fxBalanceDao.getByCode(fundingCurrency)
        if (fundingVault != null) {
            val updatedBal = (fundingVault.balance - totalFundingOutflow).coerceAtLeast(0.0)
            fxBalanceDao.updateBalance(fundingVault.copy(balance = updatedBal))
        }

        // 2. Mark team members as paid
        if (selectedMemberIds.isNotEmpty()) {
            teamMemberDao.markMembersAsPaid(selectedMemberIds.toList(), scheduledDate)
        } else {
            teamMemberDao.markAllAsPaid(scheduledDate)
        }

        // 3. Insert individual currency disbursement transactions into the treasury ledger
        val clearingRefs = mutableListOf<String>()
        batches.forEach { batch ->
            val refCode = "WIRE-${batch.currency}-$randomBatchSuffix"
            clearingRefs.add(refCode)

            treasuryTransactionDao.insertTransaction(
                TreasuryTransactionRecord(
                    date = nowStr,
                    recipient = "${batch.currency} Global Payroll Clearing (${batch.clearingRail})",
                    senderOrCounterparty = "OmniGlobal Treasury ($fundingCurrency Operating Vault)",
                    amount = batch.fundingCurrencyEquivalent,
                    currency = fundingCurrency,
                    isCredit = false,
                    category = "GLOBAL_PAYROLL",
                    status = "SETTLED",
                    referenceCode = refCode,
                    note = "Multi-Currency Payroll Run: Disbursed ${String.format(Locale.US, "%,.2f", batch.netPayableLocalAmount)} ${batch.currency} for ${batch.recipientCount} staff members via ${batch.clearingRail} @ live rate ${String.format(Locale.US, "%.4f", batch.exchangeRateToFunding)}"
                )
            )
        }

        // 4. Record certified audit trail log
        val netPayoutsMap = batches.associate { it.currency to it.netPayableLocalAmount }
        val auditDescription = "Bulk multinational payroll executed: $masterBatchRef across ${batches.size} local currencies (${batches.joinToString { it.currency }}). Total Treasury outflow: ${String.format(Locale.US, "%,.2f", totalFundingOutflow)} $fundingCurrency for ${selectedMemberIds.size} employees."
        recordAuditLog(
            module = AuditLogModule.WORKFORCE,
            actionType = AuditActionType.PAYROLL_EXECUTED,
            sourceJurisdiction = "Global Payroll Clearing Hub",
            destinationJurisdiction = batches.joinToString { it.countryCode },
            currency = fundingCurrency,
            financialAmount = totalFundingOutflow,
            description = auditDescription,
            complianceStandard = "ISO 20022 Cross-Border Bulk STP • Multi-Jurisdiction Statutory Withholding"
        )

        // 5. Generate cryptographic hash for receipt
        val cryptoHash = AuditCryptographicEngine.computeSha256("$masterBatchRef|$nowStr|$totalFundingOutflow|$fundingCurrency")

        BulkPayrollDisbursementReceipt(
            batchReferenceId = masterBatchRef,
            executionTimestamp = nowStr,
            scheduledDate = scheduledDate,
            fundingVaultCurrency = fundingCurrency,
            totalFundingOutflow = totalFundingOutflow,
            totalNetLocalPayouts = netPayoutsMap,
            totalTaxesWithheldFundingEquivalent = totalTaxesWithheldFunding,
            processedEmployeesCount = selectedMemberIds.size,
            clearingRailReferences = clearingRefs,
            cryptographicHash = cryptoHash
        )
    }

    suspend fun insertShipment(shipment: ShipmentRecord): Long = withContext(Dispatchers.IO) {
        shipmentDao.insertShipment(shipment)
    }

    suspend fun updateShipment(shipment: ShipmentRecord) = withContext(Dispatchers.IO) {
        shipmentDao.updateShipment(shipment)
    }

    suspend fun insertSupplier(supplier: GlobalSupplierRecord): Long = withContext(Dispatchers.IO) {
        val id = globalSupplierDao.insertSupplier(supplier)
        recordAuditLog(
            module = AuditLogModule.COMMERCIAL,
            actionType = AuditActionType.ENTITY_CREATED,
            sourceJurisdiction = supplier.country,
            description = "New global supplier enrolled: ${supplier.legalName} (${supplier.supplierCode}) - Payment Terms: ${supplier.paymentTermsDescription}",
            complianceStandard = "OECD Due Diligence • FCPA / UK Bribery Act"
        )
        id
    }

    suspend fun updateSupplier(supplier: GlobalSupplierRecord) = withContext(Dispatchers.IO) {
        globalSupplierDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: GlobalSupplierRecord) = withContext(Dispatchers.IO) {
        globalSupplierDao.deleteSupplier(supplier)
    }

    suspend fun deleteSupplierById(id: Long): Int = withContext(Dispatchers.IO) {
        globalSupplierDao.deleteSupplierById(id)
    }

    suspend fun updateSupplierCompliance(id: Long, newStatus: String, notes: String) = withContext(Dispatchers.IO) {
        globalSupplierDao.updateComplianceStatus(id, newStatus, notes)
        recordAuditLog(
            module = AuditLogModule.TAX_COMPLIANCE,
            actionType = AuditActionType.ENTITY_STATUS_UPDATED,
            sourceJurisdiction = "Global Compliance Officer Desk",
            description = "Supplier #$id compliance standing updated to $newStatus. Notes: $notes",
            complianceStandard = "ISO 37001 Anti-Bribery • W-8BEN-E"
        )
    }

    suspend fun updateSupplierPaymentTerms(id: Long, termsDays: Int, description: String, currency: String) = withContext(Dispatchers.IO) {
        globalSupplierDao.updatePaymentTerms(id, termsDays, description, currency)
    }

    fun searchSuppliers(query: String): Flow<List<GlobalSupplierRecord>> = globalSupplierDao.searchSuppliers(query)
    fun getSuppliersByCompliance(status: String): Flow<List<GlobalSupplierRecord>> = globalSupplierDao.getSuppliersByCompliance(status)

    suspend fun executeFxSwap(
        fromCurrency: String,
        toCurrency: String,
        amountFrom: Double,
        rate: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceBalance = fxBalanceDao.getByCode(fromCurrency) ?: return@withContext false
        val destBalance = fxBalanceDao.getByCode(toCurrency) ?: return@withContext false

        if (sourceBalance.balance < amountFrom) return@withContext false

        val amountTo = amountFrom * rate
        fxBalanceDao.updateBalance(sourceBalance.copy(balance = sourceBalance.balance - amountFrom))
        fxBalanceDao.updateBalance(destBalance.copy(balance = destBalance.balance + amountTo))

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
        val nowStr = dateFormat.format(Date())
        val ref = "SWIFT-MT103-${(100000..999999).random()}"

        // Log outflow on sell currency
        treasuryTransactionDao.insertTransaction(
            TreasuryTransactionRecord(
                date = nowStr,
                recipient = "Interbank FX Clearing Rail ($toCurrency Pool)",
                senderOrCounterparty = "$fromCurrency Operating Vault",
                amount = amountFrom,
                currency = fromCurrency,
                isCredit = false,
                category = "FX_INTERBANK",
                status = "SETTLED",
                referenceCode = ref,
                note = "FX Swap conversion leg: Sold $fromCurrency for $toCurrency"
            )
        )

        // Log inflow on buy currency
        treasuryTransactionDao.insertTransaction(
            TreasuryTransactionRecord(
                date = nowStr,
                recipient = "$toCurrency Liquidity Vault",
                senderOrCounterparty = "Interbank FX Liquidity Provider",
                amount = amountTo,
                currency = toCurrency,
                isCredit = true,
                category = "FX_INTERBANK",
                status = "SETTLED",
                referenceCode = ref,
                note = "FX Swap proceeds: Received $toCurrency @ rate ${String.format(Locale.US, "%.4f", rate)}"
            )
        )

        true
    }

    suspend fun adjustFxBalance(currencyCode: String, newBalance: Double): Boolean = withContext(Dispatchers.IO) {
        val current = fxBalanceDao.getByCode(currencyCode) ?: return@withContext false
        fxBalanceDao.updateBalance(current.copy(balance = newBalance.coerceAtLeast(0.0)))
        true
    }

    suspend fun depositFunds(currencyCode: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val current = fxBalanceDao.getByCode(currencyCode) ?: return@withContext false
        fxBalanceDao.updateBalance(current.copy(balance = current.balance + amount))

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm 'UTC'", Locale.US)
        treasuryTransactionDao.insertTransaction(
            TreasuryTransactionRecord(
                date = dateFormat.format(Date()),
                recipient = "$currencyCode Vault Reserve Account",
                senderOrCounterparty = "Corporate Capital Transfer",
                amount = amount,
                currency = currencyCode,
                isCredit = true,
                category = "CAPITAL_INJECTION",
                status = "SETTLED",
                referenceCode = "DEP-FED-${(10000..99999).random()}",
                note = "Direct treasury reserve deposit injection"
            )
        )
        true
    }

    suspend fun deductFunds(currencyCode: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val current = fxBalanceDao.getByCode(currencyCode) ?: return@withContext false
        val updated = (current.balance - amount).coerceAtLeast(0.0)
        fxBalanceDao.updateBalance(current.copy(balance = updated))
        true
    }

    suspend fun addCurrencyVault(vault: FxBalanceRecord): Boolean = withContext(Dispatchers.IO) {
        fxBalanceDao.insertBalance(vault)
        true
    }

    suspend fun simulateMarketRateTicks(): Unit = withContext(Dispatchers.IO) {
        val current = fxBalanceDao.getByCode("USD") ?: return@withContext
        // Slightly vary non-USD currencies by random tick -0.3% to +0.3%
        val currencies = listOf("EUR", "GBP", "SGD", "CHF", "JPY", "BRL")
        currencies.forEach { code ->
            val record = fxBalanceDao.getByCode(code)
            if (record != null) {
                val tickPercent = ((-30..30).random() / 10000.0) // -0.3% to +0.3%
                val newRate = record.rateToUsd * (1.0 + tickPercent)
                val newChange = (record.dailyChangePercent + (tickPercent * 100)).coerceIn(-5.0, 5.0)
                fxBalanceDao.updateBalance(record.copy(rateToUsd = newRate, dailyChangePercent = newChange))
            }
        }
    }

    suspend fun fetchLiveConversionRate(fromCurrency: String, toCurrency: String, amount: Double): ConversionQuote = withContext(Dispatchers.IO) {
        val from = fromCurrency.uppercase(Locale.US)
        val to = toCurrency.uppercase(Locale.US)
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss 'UTC'", Locale.US)

        if (from == to) {
            val nowStr = timeFormat.format(Date())
            return@withContext ConversionQuote(
                amount = amount,
                fromCurrency = from,
                toCurrency = to,
                convertedAmount = amount,
                exchangeRate = 1.0,
                inverseRate = 1.0,
                isLiveApi = true,
                provider = "Exact Sovereign Match (1:1)",
                timestamp = nowStr
            )
        }

        try {
            val response = currencyApiService.getLatestRates(from)
            val rate = response.rates?.get(to)
            if (rate != null && rate > 0.0) {
                val converted = amount * rate
                val lastUpdated = response.timeLastUpdateUtc ?: timeFormat.format(Date())
                return@withContext ConversionQuote(
                    amount = amount,
                    fromCurrency = from,
                    toCurrency = to,
                    convertedAmount = converted,
                    exchangeRate = rate,
                    inverseRate = if (rate > 0.0) 1.0 / rate else 0.0,
                    isLiveApi = true,
                    provider = "Open Exchange Rates API (Real-Time)",
                    timestamp = lastUpdated
                )
            }
        } catch (_: Exception) {
            // Graceful fallback to Room database rates below
        }

        // Fallback: Compute cross-rate using Room FxBalanceRecord
        val fromBal = fxBalanceDao.getByCode(from)
        val toBal = fxBalanceDao.getByCode(to)
        val fromUsd = fromBal?.rateToUsd ?: 1.0
        val toUsd = toBal?.rateToUsd ?: 1.0

        val fallbackRate = if (toUsd > 0.0) fromUsd / toUsd else 1.0
        val converted = amount * fallbackRate
        val nowStr = timeFormat.format(Date())

        ConversionQuote(
            amount = amount,
            fromCurrency = from,
            toCurrency = to,
            convertedAmount = converted,
            exchangeRate = fallbackRate,
            inverseRate = if (fallbackRate > 0.0) 1.0 / fallbackRate else 0.0,
            isLiveApi = false,
            provider = "Treasury Reserve Benchmark (Cached)",
            timestamp = nowStr
        )
    }

    fun getMacroIndicators(): List<MacroIndicator> {
        return listOf(
            MacroIndicator(
                title = "OECD Pillar Two Minimum Tax",
                value = "15.0%",
                change = "Enforced 2026",
                isPositive = true,
                category = "TAX_OECD",
                description = "Global minimum corporate rate implemented across 140+ jurisdictions."
            ),
            MacroIndicator(
                title = "Fed Funds Target Rate",
                value = "4.25% - 4.50%",
                change = "-25 bps",
                isPositive = true,
                category = "CENTRAL_BANKS",
                description = "FOMC terminal rate trajectory stable; dollar liquidity easing."
            ),
            MacroIndicator(
                title = "ECB Main Refinancing Rate",
                value = "3.15%",
                change = "-25 bps",
                isPositive = true,
                category = "CENTRAL_BANKS",
                description = "European Central Bank deposit facility rate accommodates cross-border loans."
            ),
            MacroIndicator(
                title = "Global Trade Freight Index (FBX)",
                value = "$3,420 / FEU",
                change = "-4.2%",
                isPositive = true,
                category = "CORRIDOR",
                description = "Asia-Europe and Transpacific route rates stabilizing."
            ),
            MacroIndicator(
                title = "Cross-Border Settlement Velocity",
                value = "T+0 / FedNow",
                change = "Real-time",
                isPositive = true,
                category = "GLOBAL_GDP",
                description = "Direct multi-corridor ISO 20022 liquidity settlement active."
            )
        )
    }

    fun getRegionalHubs(): List<RegionalHub> {
        return listOf(
            RegionalHub(
                code = "AMER",
                name = "Americas HQ",
                city = "New York / Delaware",
                coordinatesNormX = 0.26f,
                coordinatesNormY = 0.35f,
                activeEntityCount = 2,
                activeHeadcount = 48,
                revenueContributionPercent = 42,
                localStatus = "PEAK_HOURS",
                localTimeStr = "10:24 AM EDT",
                activeCurrency = "USD"
            ),
            RegionalHub(
                code = "EMEA",
                name = "EMEA Gateway",
                city = "London / Dublin / Zurich",
                coordinatesNormX = 0.49f,
                coordinatesNormY = 0.28f,
                activeEntityCount = 3,
                activeHeadcount = 62,
                revenueContributionPercent = 33,
                localStatus = "PEAK_HOURS",
                localTimeStr = "03:24 PM BST",
                activeCurrency = "EUR / GBP / CHF"
            ),
            RegionalHub(
                code = "APAC",
                name = "APAC Hub",
                city = "Singapore / Tokyo",
                coordinatesNormX = 0.82f,
                coordinatesNormY = 0.55f,
                activeEntityCount = 2,
                activeHeadcount = 35,
                revenueContributionPercent = 19,
                localStatus = "EVENING_WRAP",
                localTimeStr = "10:24 PM SGT",
                activeCurrency = "SGD / JPY"
            ),
            RegionalHub(
                code = "LATAM",
                name = "LatAm Operations",
                city = "São Paulo",
                coordinatesNormX = 0.34f,
                coordinatesNormY = 0.72f,
                activeEntityCount = 1,
                activeHeadcount = 14,
                revenueContributionPercent = 6,
                localStatus = "ACTIVE",
                localTimeStr = "11:24 AM BRT",
                activeCurrency = "BRL / USD"
            )
        )
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        if (entityDao.getCount() == 0) {
            val entities = listOf(
                EntityRecord(
                    name = "OmniGlobal Holdings Inc.",
                    jurisdiction = "United States (Delaware)",
                    countryCode = "US",
                    entityType = "C-Corporation",
                    taxId = "EIN: 84-2910482",
                    status = "GOOD_STANDING",
                    baseCurrency = "USD",
                    operatingCapital = 4850000.0,
                    annualFilingDeadline = "Mar 01, 2027",
                    localDirector = "Sarah Sterling (Managing Principal)",
                    complianceScore = 98
                ),
                EntityRecord(
                    name = "OmniGlobal UK & EMEA Ltd.",
                    jurisdiction = "United Kingdom",
                    countryCode = "GB",
                    entityType = "Private Limited (Ltd)",
                    taxId = "VAT: GB 928 1102 33",
                    status = "GOOD_STANDING",
                    baseCurrency = "GBP",
                    operatingCapital = 2150000.0,
                    annualFilingDeadline = "Dec 31, 2026",
                    localDirector = "Alistair Vance (Resident Director)",
                    complianceScore = 96
                ),
                EntityRecord(
                    name = "OmniGlobal APAC Pte. Ltd.",
                    jurisdiction = "Singapore",
                    countryCode = "SG",
                    entityType = "Private Limited (Pte. Ltd.)",
                    taxId = "UEN: 202391024K",
                    status = "GOOD_STANDING",
                    baseCurrency = "SGD",
                    operatingCapital = 1820000.0,
                    annualFilingDeadline = "Nov 30, 2026",
                    localDirector = "Mei-Ling Tan (Nominee Director)",
                    complianceScore = 100
                ),
                EntityRecord(
                    name = "OmniGlobal DACH GmbH",
                    jurisdiction = "Switzerland (Zurich)",
                    countryCode = "CH",
                    entityType = "Gesellschaft mit beschränkter Haftung",
                    taxId = "CHE-119.290.112 MWST",
                    status = "GOOD_STANDING",
                    baseCurrency = "CHF",
                    operatingCapital = 1450000.0,
                    annualFilingDeadline = "Oct 15, 2026",
                    localDirector = "Beat Oberholzer (Managing Director)",
                    complianceScore = 94
                ),
                EntityRecord(
                    name = "OmniGlobal LatAm Tecnologia Ltda.",
                    jurisdiction = "Brazil (São Paulo)",
                    countryCode = "BR",
                    entityType = "Sociedade Limitada",
                    taxId = "CNPJ: 42.119.290/0001-44",
                    status = "FILING_DUE",
                    baseCurrency = "BRL",
                    operatingCapital = 620000.0,
                    annualFilingDeadline = "Oct 30, 2026",
                    localDirector = "Rodrigo Santos (Administrator)",
                    complianceScore = 88
                )
            )
            entityDao.insertAll(entities)
        }

        if (fxBalanceDao.getCount() == 0) {
            val fxBalances = listOf(
                FxBalanceRecord(
                    currencyCode = "USD",
                    currencyName = "US Dollar (Consolidated Treasury)",
                    symbol = "$",
                    balance = 3420850.0,
                    rateToUsd = 1.0,
                    dailyChangePercent = 0.0
                ),
                FxBalanceRecord(
                    currencyCode = "EUR",
                    currencyName = "Euro (SEPA Clearing Pool)",
                    symbol = "€",
                    balance = 1680500.0,
                    rateToUsd = 1.0925,
                    dailyChangePercent = 0.28
                ),
                FxBalanceRecord(
                    currencyCode = "GBP",
                    currencyName = "British Pound (Faster Payments)",
                    symbol = "£",
                    balance = 940200.0,
                    rateToUsd = 1.3040,
                    dailyChangePercent = -0.15
                ),
                FxBalanceRecord(
                    currencyCode = "SGD",
                    currencyName = "Singapore Dollar (FAST Settlement)",
                    symbol = "S$",
                    balance = 1450000.0,
                    rateToUsd = 0.7710,
                    dailyChangePercent = 0.12
                ),
                FxBalanceRecord(
                    currencyCode = "CHF",
                    currencyName = "Swiss Franc (SIC High-Value Vault)",
                    symbol = "CHF",
                    balance = 820000.0,
                    rateToUsd = 1.1730,
                    dailyChangePercent = 0.45
                ),
                FxBalanceRecord(
                    currencyCode = "JPY",
                    currencyName = "Japanese Yen (BOJ Net Clearing)",
                    symbol = "¥",
                    balance = 184500000.0,
                    rateToUsd = 0.0068,
                    dailyChangePercent = -0.32
                ),
                FxBalanceRecord(
                    currencyCode = "BRL",
                    currencyName = "Brazilian Real (PIX Commercial Gateway)",
                    symbol = "R$",
                    balance = 2950000.0,
                    rateToUsd = 0.1840,
                    dailyChangePercent = 0.05
                )
            )
            fxBalanceDao.insertAll(fxBalances)
        }

        if (invoiceDao.getCount() == 0) {
            val invoices = listOf(
                InvoiceRecord(
                    invoiceNumber = "INV-2026-0891",
                    issuingEntityName = "OmniGlobal Holdings Inc.",
                    clientName = "Aether Pharma Group AG",
                    clientCountry = "Switzerland",
                    issueDate = "Sep 18, 2026",
                    dueDate = "Oct 18, 2026",
                    amount = 285000.0,
                    currency = "USD",
                    taxRatePercent = 0.0,
                    status = "PENDING",
                    serviceDescription = "Cross-border Enterprise Cloud Architecture License & SLA"
                ),
                InvoiceRecord(
                    invoiceNumber = "INV-2026-0890",
                    issuingEntityName = "OmniGlobal UK & EMEA Ltd.",
                    clientName = "Nordic CleanTech Solutions Oy",
                    clientCountry = "Finland",
                    issueDate = "Sep 15, 2026",
                    dueDate = "Oct 15, 2026",
                    amount = 142000.0,
                    currency = "EUR",
                    taxRatePercent = 0.0, // Reverse charge EU B2B
                    status = "PAID",
                    serviceDescription = "Global Grid Predictive Optimization Platform - Q3"
                ),
                InvoiceRecord(
                    invoiceNumber = "INV-2026-0889",
                    issuingEntityName = "OmniGlobal APAC Pte. Ltd.",
                    clientName = "Tokyo Robotics Consortium",
                    clientCountry = "Japan",
                    issueDate = "Sep 01, 2026",
                    dueDate = "Sep 30, 2026",
                    amount = 320000.0,
                    currency = "SGD",
                    taxRatePercent = 9.0, // GST
                    status = "PAID",
                    serviceDescription = "Multi-Hub Autonomous Hardware Integration Gateway"
                ),
                InvoiceRecord(
                    invoiceNumber = "INV-2026-0888",
                    issuingEntityName = "OmniGlobal Holdings Inc.",
                    clientName = "Stratos Defense Aerospace Inc.",
                    clientCountry = "United States",
                    issueDate = "Aug 20, 2026",
                    dueDate = "Sep 20, 2026",
                    amount = 510000.0,
                    currency = "USD",
                    taxRatePercent = 8.875,
                    status = "OVERDUE",
                    serviceDescription = "Satellite Telemetry Real-time Ingestion Pipeline"
                ),
                InvoiceRecord(
                    invoiceNumber = "INV-2026-0887",
                    issuingEntityName = "OmniGlobal DACH GmbH",
                    clientName = "Bavaria Automotive Engineering SE",
                    clientCountry = "Germany",
                    issueDate = "Sep 22, 2026",
                    dueDate = "Oct 22, 2026",
                    amount = 195000.0,
                    currency = "EUR",
                    taxRatePercent = 19.0,
                    status = "PENDING",
                    serviceDescription = "Embedded Autonomous Safety Verification Suite"
                )
            )
            invoiceDao.insertAll(invoices)
        }

        if (teamMemberDao.getCount() == 0) {
            val team = listOf(
                TeamMemberRecord(
                    fullName = "Dr. Elena Rostova",
                    role = "VP Global System Architecture",
                    country = "United Kingdom",
                    countryCode = "GB",
                    employmentType = "Direct Subsidiary",
                    monthlyCompensation = 16500.0,
                    currency = "GBP",
                    status = "ACTIVE",
                    taxJurisdiction = "HMRC Pay-As-You-Earn (PAYE)",
                    lastPaidDate = "Aug 31, 2026"
                ),
                TeamMemberRecord(
                    fullName = "Marcus Chen",
                    role = "Head of APAC Commercial Operations",
                    country = "Singapore",
                    countryCode = "SG",
                    employmentType = "Direct Subsidiary",
                    monthlyCompensation = 22000.0,
                    currency = "SGD",
                    status = "ACTIVE",
                    taxJurisdiction = "IRAS / CPF Scheme",
                    lastPaidDate = "Aug 31, 2026"
                ),
                TeamMemberRecord(
                    fullName = "Klaus Lindemann",
                    role = "Principal Cryptographic Engineer",
                    country = "Switzerland",
                    countryCode = "CH",
                    employmentType = "Direct Subsidiary",
                    monthlyCompensation = 18500.0,
                    currency = "CHF",
                    status = "ACTIVE",
                    taxJurisdiction = "Swiss Canton Zurich Withholding",
                    lastPaidDate = "Aug 31, 2026"
                ),
                TeamMemberRecord(
                    fullName = "Amara Okafor",
                    role = "Global Tax & Transfer Pricing Director",
                    country = "United States",
                    countryCode = "US",
                    employmentType = "Direct Subsidiary",
                    monthlyCompensation = 24000.0,
                    currency = "USD",
                    status = "ACTIVE",
                    taxJurisdiction = "IRS / New York State Dept of Tax",
                    lastPaidDate = "Aug 31, 2026"
                ),
                TeamMemberRecord(
                    fullName = "Mateo Silva",
                    role = "Senior Freight & Logistics Lead",
                    country = "Brazil",
                    countryCode = "BR",
                    employmentType = "Full-Time (EOR)",
                    monthlyCompensation = 38000.0,
                    currency = "BRL",
                    status = "ACTIVE",
                    taxJurisdiction = "Receita Federal / CLT",
                    lastPaidDate = "Aug 31, 2026"
                ),
                TeamMemberRecord(
                    fullName = "Yuki Tanaka",
                    role = "Specialized AI Hardware Contractor",
                    country = "Japan",
                    countryCode = "JP",
                    employmentType = "B2B Contractor",
                    monthlyCompensation = 1850000.0,
                    currency = "JPY",
                    status = "ACTIVE",
                    taxJurisdiction = "Japan National Tax Agency (WHT exempt)",
                    lastPaidDate = "Aug 31, 2026"
                )
            )
            teamMemberDao.insertAll(team)
        }

        if (shipmentDao.getCount() == 0) {
            val shipments = listOf(
                ShipmentRecord(
                    trackingCode = "WBOS-FRT-9921",
                    origin = "Shenzhen Port (SZX), China",
                    destination = "Rotterdam Terminal (RTM), Netherlands",
                    carrier = "Maersk Line (Vessel: Triple-E Madrid)",
                    incoterm = "DDP (Delivered Duty Paid)",
                    cargoDescription = "48x Edge AI Accelerator Racks & Quantum Sensors",
                    cargoValue = 1420000.0,
                    currency = "USD",
                    customsStatus = "IN_TRANSIT",
                    estimatedArrival = "Oct 04, 2026"
                ),
                ShipmentRecord(
                    trackingCode = "WBOS-AIR-8012",
                    origin = "Frankfurt CargoCity (FRA), Germany",
                    destination = "Singapore Changi Airfreight (SIN)",
                    carrier = "Lufthansa Cargo (Boeing 777F)",
                    incoterm = "CIF (Cost, Insurance & Freight)",
                    cargoDescription = "High-Purity Silicon Wafers & Optical Transceivers",
                    cargoValue = 890000.0,
                    currency = "EUR",
                    customsStatus = "CLEARED",
                    estimatedArrival = "Oct 01, 2026"
                ),
                ShipmentRecord(
                    trackingCode = "WBOS-SEA-3410",
                    origin = "Santos Container Terminal (SSZ), Brazil",
                    destination = "Port of New York / Newark (EWR)",
                    carrier = "CMA CGM (Vessel: Pegasus)",
                    incoterm = "FOB (Free on Board)",
                    cargoDescription = "Sustainable Agri-Commodity Raw Biomass for Polymers",
                    cargoValue = 340000.0,
                    currency = "USD",
                    customsStatus = "PORT_INSPECTION",
                    estimatedArrival = "Oct 12, 2026"
                ),
                ShipmentRecord(
                    trackingCode = "WBOS-AIR-4109",
                    origin = "Tokyo Haneda Logistics (HND), Japan",
                    destination = "London Heathrow Freight Hub (LHR)",
                    carrier = "ANA Cargo (Express Direct)",
                    incoterm = "DDP (Delivered Duty Paid)",
                    cargoDescription = "Precision Micro-Actuators for Satellite Arrays",
                    cargoValue = 620000.0,
                    currency = "USD",
                    customsStatus = "CLEARED",
                    estimatedArrival = "Oct 02, 2026"
                )
            )
            shipmentDao.insertAll(shipments)
        }

        if (treasuryTransactionDao.getCount() == 0) {
            val transactions = listOf(
                TreasuryTransactionRecord(
                    date = "Sep 30, 2026 • 15:45 UTC",
                    recipient = "OmniGlobal Holdings Inc. (JPMorgan Vault)",
                    senderOrCounterparty = "Aether Pharma Group AG",
                    amount = 285000.0,
                    currency = "USD",
                    isCredit = true,
                    category = "COMMERCIAL_SETTLEMENT",
                    status = "SETTLED",
                    referenceCode = "SWIFT-MT103-88219",
                    note = "Enterprise Cloud Architecture License Settlement"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 30, 2026 • 13:10 UTC",
                    recipient = "Nordic CleanTech Solutions Oy",
                    senderOrCounterparty = "OmniGlobal UK & EMEA Ltd.",
                    amount = 142000.0,
                    currency = "EUR",
                    isCredit = false,
                    category = "COMMERCIAL_INVOICE",
                    status = "CLEARED",
                    referenceCode = "SEPA-RT1-99201",
                    note = "Clean Grid Hardware Escrow Release"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 30, 2026 • 11:30 UTC",
                    recipient = "HMRC Pay-As-You-Earn (UK Staff Payroll)",
                    senderOrCounterparty = "OmniGlobal UK & EMEA Ltd.",
                    amount = 68450.0,
                    currency = "GBP",
                    isCredit = false,
                    category = "GLOBAL_PAYROLL",
                    status = "SETTLED",
                    referenceCode = "BACS-PAY-44102",
                    note = "Monthly Engineering & Architecture Compensation Batch"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 29, 2026 • 18:20 UTC",
                    recipient = "OmniGlobal APAC Pte. Ltd. (DBS Vault)",
                    senderOrCounterparty = "Tokyo Robotics Consortium",
                    amount = 320000.0,
                    currency = "SGD",
                    isCredit = true,
                    category = "COMMERCIAL_SETTLEMENT",
                    status = "SETTLED",
                    referenceCode = "FAST-MEPS-22019",
                    note = "Autonomous Gateway Delivery Milestone"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 29, 2026 • 14:05 UTC",
                    recipient = "Maersk Shipping Line Logistics",
                    senderOrCounterparty = "OmniGlobal Holdings Inc.",
                    amount = 50000.0,
                    currency = "USD",
                    isCredit = false,
                    category = "SUPPLY_FREIGHT",
                    status = "CLEARED",
                    referenceCode = "SWIFT-MT103-77301",
                    note = "Transpacific Sea-Freight Customs Duty & Clearance"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 28, 2026 • 16:40 UTC",
                    recipient = "OmniGlobal DACH GmbH (UBS Vault)",
                    senderOrCounterparty = "Institutional Sovereign Wealth Fund",
                    amount = 150000.0,
                    currency = "CHF",
                    isCredit = true,
                    category = "CAPITAL_INJECTION",
                    status = "SETTLED",
                    referenceCode = "SIC-RTGS-33109",
                    note = "Swiss Operating Capital Tranche A"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 28, 2026 • 10:15 UTC",
                    recipient = "Receita Federal do Brasil / Mateo Silva",
                    senderOrCounterparty = "OmniGlobal LatAm Ltda.",
                    amount = 38000.0,
                    currency = "BRL",
                    isCredit = false,
                    category = "GLOBAL_PAYROLL",
                    status = "SETTLED",
                    referenceCode = "PIX-BACEN-55192",
                    note = "LatAm Logistics Management Monthly Compensation"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 27, 2026 • 17:00 UTC",
                    recipient = "Yuki Tanaka AI Consulting Services",
                    senderOrCounterparty = "OmniGlobal APAC Pte. Ltd.",
                    amount = 1850000.0,
                    currency = "JPY",
                    isCredit = false,
                    category = "GLOBAL_PAYROLL",
                    status = "CLEARED",
                    referenceCode = "BOJ-NET-88192",
                    note = "Specialized AI Hardware Design Contractor Fee"
                ),
                TreasuryTransactionRecord(
                    date = "Sep 27, 2026 • 09:30 UTC",
                    recipient = "OmniGlobal Holdings Inc. (JPMorgan Reserve)",
                    senderOrCounterparty = "Interbank Global Liquidity Provider",
                    amount = 500000.0,
                    currency = "USD",
                    isCredit = true,
                    category = "FX_INTERBANK",
                    status = "SETTLED",
                    referenceCode = "FEDWIRE-02100-994",
                    note = "Cross-Currency Treasury Optimization Liquidity"
                )
            )
            treasuryTransactionDao.insertAll(transactions)
        }

        if (auditLogDao.countLogs() == 0) {
            val initialLogs = mutableListOf<AuditLogRecord>()
            var prevHash = AuditCryptographicEngine.GENESIS_HASH

            val seedDefs = listOf(
                Triple(
                    "AUD-2026-0001",
                    AuditLogModule.ENTITIES to AuditActionType.ENTITY_CREATED,
                    "OmniGlobal Holdings Inc. incorporated as Delaware C-Corporation (Authorized Capital: $45M USD)" to 45000000.0
                ),
                Triple(
                    "AUD-2026-0002",
                    AuditLogModule.ENTITIES to AuditActionType.ENTITY_CREATED,
                    "OmniGlobal DACH GmbH registered in Commercial Register of Canton Zurich (Capital: CHF 18.5M)" to 18500000.0
                ),
                Triple(
                    "AUD-2026-0003",
                    AuditLogModule.ENTITIES to AuditActionType.ENTITY_CREATED,
                    "OmniGlobal Singapore Pte. Ltd. incorporated under ACRA BizFile (Capital: SGD 22M)" to 22000000.0
                ),
                Triple(
                    "AUD-2026-0004",
                    AuditLogModule.TREASURY to AuditActionType.TREASURY_TRANSFER,
                    "SWIFT MT103 Commercial Inbound Settlement: Aether Pharma Group AG ($285,000 USD)" to 285000.0
                ),
                Triple(
                    "AUD-2026-0005",
                    AuditLogModule.COMMERCIAL to AuditActionType.INVOICE_PAID,
                    "Cross-border EU B2B Reverse Charge Settlement Cleared: Nordic CleanTech (€142,000 EUR)" to 142000.0
                ),
                Triple(
                    "AUD-2026-0006",
                    AuditLogModule.TREASURY to AuditActionType.FX_SWAP_EXECUTED,
                    "Interbank FX Swap Execution: Sold $150,000 USD to buy 127,950 CHF via Swiss RTGS SIC" to 150000.0
                ),
                Triple(
                    "AUD-2026-0007",
                    AuditLogModule.LOGISTICS to AuditActionType.SHIPMENT_DISPATCHED,
                    "International Airfreight Dispatched: FRA CargoCity to Singapore Changi (€890,000 EUR Wafers)" to 890000.0
                ),
                Triple(
                    "AUD-2026-0008",
                    AuditLogModule.WORKFORCE to AuditActionType.PAYROLL_EXECUTED,
                    "Consolidated Monthly Global Payroll Batch Settlement: 146 cross-border staff ($813,678 USD)" to 813678.0
                ),
                Triple(
                    "AUD-2026-0009",
                    AuditLogModule.TAX_COMPLIANCE to AuditActionType.TAX_RETURN_FILED,
                    "Statutory Tax Filing Verification: US Form 1120 & FinCEN BOI XML transmission" to 148500.0
                ),
                Triple(
                    "AUD-2026-0010",
                    AuditLogModule.SYSTEM_ADMIN to AuditActionType.SECURITY_KEY_ROTATED,
                    "Enterprise HSM Cryptographic Master Key Rotation & Merkle Root Chain Certified" to 0.0
                )
            )

            val baseTime = 1790841600000L - (10 * 86400000L)
            seedDefs.forEachIndexed { index, (logId, moduleAction, descAmount) ->
                val (module, actionType) = moduleAction
                val (description, amount) = descAmount
                val logTime = baseTime + (index * 86400000L)
                val timeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date(logTime))

                val cryptoHash = AuditCryptographicEngine.generateLogHash(
                    previousHash = prevHash,
                    timestamp = logTime,
                    logId = logId,
                    module = module.name,
                    actionType = actionType.name,
                    actorUsername = "system.audit.engine@omniglobal.ch",
                    amount = if (amount > 0.0) amount else null,
                    currency = "USD",
                    sourceJurisdiction = "Global Sovereign Enterprise Hub",
                    destinationJurisdiction = "Multi-Jurisdictional Desk"
                )

                val record = AuditLogRecord(
                    logId = logId,
                    timestamp = logTime,
                    timestampFormatted = timeFormatted,
                    module = module.name,
                    actionType = actionType.name,
                    actorUsername = "system.audit.engine@omniglobal.ch",
                    actorRole = "Global Compliance Controller",
                    actorIpAddress = "10.8.0.1 (HSM-Root)",
                    sourceJurisdiction = "Global Sovereign Enterprise Hub",
                    destinationJurisdiction = "Multi-Jurisdictional Desk",
                    currency = "USD",
                    financialAmount = if (amount > 0.0) amount else null,
                    status = AuditLogStatus.VERIFIED_IMMUTABLE.name,
                    complianceStandard = "SOX Sec 404 • EU MiFID II • Basel III",
                    previousHash = prevHash,
                    cryptographicHash = cryptoHash,
                    description = description,
                    metadataJson = "{\"automated\":true,\"auditTier\":\"LEVEL_1_ENTERPRISE\"}"
                )
                initialLogs.add(record)
                prevHash = cryptoHash
            }

            auditLogDao.insertLogs(initialLogs)
        }

        if (globalSupplierDao.getCount() == 0) {
            val suppliers = listOf(
                GlobalSupplierRecord(
                    supplierCode = "SUP-DE-0042",
                    legalName = "Siemens AG Industrial Systems",
                    tradingName = "Siemens Industrial",
                    country = "Germany",
                    countryCode = "DE",
                    taxRegistrationNumber = "DE129274202",
                    businessCategory = "MANUFACTURING",
                    primaryContactName = "Dr. Klaus Weber",
                    primaryContactEmail = "klaus.weber@siemens-supply.de",
                    primaryContactPhone = "+49 89 636 00",
                    physicalAddress = "Werner-von-Siemens-Straße 1, 80333 Munich, Germany",
                    websiteUrl = "https://www.siemens.com/global-supply",
                    paymentTermsDays = 30,
                    paymentTermsDescription = "NET_30",
                    preferredCurrency = "EUR",
                    settlementRail = "SEPA",
                    bankName = "Deutsche Bank AG",
                    bankSwiftBic = "DEUTDEDD",
                    bankIbanOrAccountNumber = "DE89370400440532013000",
                    discountEarlyPaymentPercent = 2.0,
                    complianceStatus = "COMPLIANT",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 92,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "VERIFIED",
                    lastAuditDate = "2026-08-15",
                    complianceDocExpiryDate = "2027-08-15",
                    complianceOfficerNotes = "Full ISO 9001:2015 & ISO 14001 on file. Annual OECD supply chain audit passed.",
                    riskLevel = "LOW",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-TW-0108",
                    legalName = "Taiwan Semiconductor Manufacturing Co. (TSMC)",
                    tradingName = "TSMC Foundry Services",
                    country = "Taiwan",
                    countryCode = "TW",
                    taxRegistrationNumber = "TW22099131",
                    businessCategory = "RAW_MATERIALS",
                    primaryContactName = "Mei-Ling Chang",
                    primaryContactEmail = "ml_chang@tsmc-corporate.tw",
                    primaryContactPhone = "+886 3 563 6688",
                    physicalAddress = "8 Li-Hsin Rd. 6, Hsinchu Science Park, Taiwan",
                    websiteUrl = "https://www.tsmc.com",
                    paymentTermsDays = 45,
                    paymentTermsDescription = "NET_45",
                    preferredCurrency = "USD",
                    settlementRail = "SWIFT",
                    bankName = "Mega International Commercial Bank",
                    bankSwiftBic = "ICBCTWTP",
                    bankIbanOrAccountNumber = "TW0900118920194820",
                    discountEarlyPaymentPercent = 1.5,
                    complianceStatus = "COMPLIANT",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 89,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "VERIFIED",
                    lastAuditDate = "2026-07-20",
                    complianceDocExpiryDate = "2027-07-20",
                    complianceOfficerNotes = "Critical semiconductor supplier. US CHIPS Act dual-use clearance active.",
                    riskLevel = "LOW",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-JP-0089",
                    legalName = "Daikin Applied Thermal Technologies",
                    tradingName = "Daikin Tech",
                    country = "Japan",
                    countryCode = "JP",
                    taxRegistrationNumber = "JP9010001008821",
                    businessCategory = "MANUFACTURING",
                    primaryContactName = "Kenji Sato",
                    primaryContactEmail = "kenji.sato@daikin-supply.co.jp",
                    primaryContactPhone = "+81 6 6373 4351",
                    physicalAddress = "Umeda Center Bldg, 2-4-12 Nakazaki-Nishi, Kita-ku, Osaka, Japan",
                    websiteUrl = "https://www.daikin.com",
                    paymentTermsDays = 60,
                    paymentTermsDescription = "NET_60",
                    preferredCurrency = "JPY",
                    settlementRail = "SWIFT",
                    bankName = "Mitsubishi UFJ Financial Group",
                    bankSwiftBic = "BOTKJPJT",
                    bankIbanOrAccountNumber = "JP19000188920114",
                    discountEarlyPaymentPercent = 1.0,
                    complianceStatus = "COMPLIANT",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 91,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "VERIFIED",
                    lastAuditDate = "2026-06-10",
                    complianceDocExpiryDate = "2027-06-10",
                    complianceOfficerNotes = "Cryogenic data center cooling systems. METI certified.",
                    riskLevel = "LOW",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-CH-0014",
                    legalName = "STMicroelectronics N.V. Corporate Supply",
                    tradingName = "ST Microelectronics",
                    country = "Switzerland",
                    countryCode = "CH",
                    taxRegistrationNumber = "CHE-105.890.112 MWST",
                    businessCategory = "IT_SERVICES",
                    primaryContactName = "Laurent Blanc",
                    primaryContactEmail = "laurent.blanc@st-europe.ch",
                    primaryContactPhone = "+41 22 929 29 29",
                    physicalAddress = "39 Chemin du Champ des Filles, 1228 Plan-les-Ouates, Geneva, Switzerland",
                    websiteUrl = "https://www.st.com",
                    paymentTermsDays = 30,
                    paymentTermsDescription = "NET_30",
                    preferredCurrency = "CHF",
                    settlementRail = "SWIFT",
                    bankName = "UBS Switzerland AG",
                    bankSwiftBic = "UBSWCHZH",
                    bankIbanOrAccountNumber = "CH930024024018820194",
                    discountEarlyPaymentPercent = 2.0,
                    complianceStatus = "COMPLIANT",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 95,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "VERIFIED",
                    lastAuditDate = "2026-09-01",
                    complianceDocExpiryDate = "2027-09-01",
                    complianceOfficerNotes = "Swiss OR compliant. Zero VAT cross-border service treaty active.",
                    riskLevel = "LOW",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-SG-0231",
                    legalName = "Flex Ltd. High-Reliability Assemblies",
                    tradingName = "Flex Global Logistics",
                    country = "Singapore",
                    countryCode = "SG",
                    taxRegistrationNumber = "UEN: 199002645H",
                    businessCategory = "LOGISTICS_FREIGHT",
                    primaryContactName = "Cheryl Ng",
                    primaryContactEmail = "cheryl.ng@flex-sg.com",
                    primaryContactPhone = "+65 6861 6888",
                    physicalAddress = "2 Changi South Lane, Singapore 486123",
                    websiteUrl = "https://www.flex.com",
                    paymentTermsDays = 30,
                    paymentTermsDescription = "NET_30",
                    preferredCurrency = "SGD",
                    settlementRail = "SWIFT",
                    bankName = "DBS Bank Ltd",
                    bankSwiftBic = "DBSSSGSG",
                    bankIbanOrAccountNumber = "SG9900010928172645",
                    discountEarlyPaymentPercent = 1.0,
                    complianceStatus = "COMPLIANT",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 88,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "VERIFIED",
                    lastAuditDate = "2026-05-18",
                    complianceDocExpiryDate = "2027-05-18",
                    complianceOfficerNotes = "Secure bonded warehouse certified by Singapore Customs.",
                    riskLevel = "LOW",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-BR-0077",
                    legalName = "Embraer S.A. Aerostructures Division",
                    tradingName = "Embraer Aero",
                    country = "Brazil",
                    countryCode = "BR",
                    taxRegistrationNumber = "CNPJ: 60.701.190/0001-04",
                    businessCategory = "MANUFACTURING",
                    primaryContactName = "Eduardo Ramos",
                    primaryContactEmail = "eduardo.ramos@embraer.com.br",
                    primaryContactPhone = "+55 12 3927 1000",
                    physicalAddress = "Av. Brigadeiro Faria Lima 2170, São José dos Campos, SP, Brazil",
                    websiteUrl = "https://www.embraer.com",
                    paymentTermsDays = 60,
                    paymentTermsDescription = "NET_60",
                    preferredCurrency = "USD",
                    settlementRail = "SWIFT",
                    bankName = "Banco Itaú BBA S.A.",
                    bankSwiftBic = "ITAUBRSP",
                    bankIbanOrAccountNumber = "BR99034100019283746501",
                    discountEarlyPaymentPercent = 0.0,
                    complianceStatus = "AUDIT_PENDING",
                    w8BenOrTaxFormFiled = true,
                    iso9001Certified = true,
                    esgRatingScore = 82,
                    antiBriberyPactSigned = true,
                    kycVerificationStatus = "IN_REVIEW",
                    lastAuditDate = "2025-10-14",
                    complianceDocExpiryDate = "2026-11-15",
                    complianceOfficerNotes = "Annual Recertification in progress. WHT 15% applicable on cross-border wire.",
                    riskLevel = "MEDIUM",
                    isActive = true
                ),
                GlobalSupplierRecord(
                    supplierCode = "SUP-CN-0319",
                    legalName = "Foxconn Industrial Interconnect Logistics",
                    tradingName = "FII Logistics",
                    country = "China",
                    countryCode = "CN",
                    taxRegistrationNumber = "91440300MA5DC4KM69",
                    businessCategory = "LOGISTICS_FREIGHT",
                    primaryContactName = "Zhang Wei",
                    primaryContactEmail = "zhang.wei@fii-global.com",
                    primaryContactPhone = "+86 755 2812 9988",
                    physicalAddress = "Foxconn Science Park, Longhua, Shenzhen, China",
                    websiteUrl = "https://www.fii-foxconn.com",
                    paymentTermsDays = 60,
                    paymentTermsDescription = "NET_60",
                    preferredCurrency = "USD",
                    settlementRail = "SWIFT",
                    bankName = "Bank of China",
                    bankSwiftBic = "BKCHCNBJ",
                    bankIbanOrAccountNumber = "CN1040001928374655",
                    discountEarlyPaymentPercent = 0.0,
                    complianceStatus = "DOCUMENTATION_EXPIRED",
                    w8BenOrTaxFormFiled = false,
                    iso9001Certified = true,
                    esgRatingScore = 74,
                    antiBriberyPactSigned = false,
                    kycVerificationStatus = "IN_REVIEW",
                    lastAuditDate = "2025-06-20",
                    complianceDocExpiryDate = "2026-06-20",
                    complianceOfficerNotes = "URGENT: Form W-8BEN-E expired on June 2026. Anti-bribery renewal pending.",
                    riskLevel = "HIGH",
                    isActive = true
                )
            )
            globalSupplierDao.insertAll(suppliers)
        }
    }
}
