package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.worldbusiness.data.local.WorldBusinessDatabase
import com.example.worldbusiness.data.model.AuditActionType
import com.example.worldbusiness.data.model.AuditLogModule
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.AuditLogStatus
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.AuditCryptographicEngine
import com.example.worldbusiness.data.repository.WorldBusinessRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: WorldBusinessDatabase
  private lateinit var repository: WorldBusinessRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, WorldBusinessDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = WorldBusinessRepository(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("World Business OS", appName)
  }

  @Test
  fun `test initial seed and entity count`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val entities = repository.allEntities.first()
    assertTrue(entities.isNotEmpty())
    assertEquals(5, entities.size)

    val usEntity = entities.find { it.countryCode == "US" }
    assertNotNull(usEntity)
    assertEquals("OmniGlobal Holdings Inc.", usEntity?.name)
  }

  @Test
  fun `test fx balances seeded`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val balances = repository.allBalances.first()
    assertTrue(balances.isNotEmpty())
    val usd = balances.find { it.currencyCode == "USD" }
    assertNotNull(usd)
  }

  @Test
  fun `test deposit funds into eur vault`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val initialEur = repository.allBalances.first().find { it.currencyCode == "EUR" }
    assertNotNull(initialEur)
    val prevBalance = initialEur!!.balance

    repository.depositFunds("EUR", 250000.0)
    val updatedEur = repository.allBalances.first().find { it.currencyCode == "EUR" }
    assertNotNull(updatedEur)
    assertEquals(prevBalance + 250000.0, updatedEur!!.balance, 0.01)
  }

  @Test
  fun `test adjust fx balance and conversion`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    repository.adjustFxBalance("GBP", 1000000.0)
    val updatedGbp = repository.allBalances.first().find { it.currencyCode == "GBP" }
    assertNotNull(updatedGbp)
    assertEquals(1000000.0, updatedGbp!!.balance, 0.01)
  }

  @Test
  fun `test treasury transactions seeded with credits and debits`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val transactions = repository.allTransactions.first()
    assertTrue(transactions.isNotEmpty())

    val credits = transactions.filter { it.isCredit }
    val debits = transactions.filter { !it.isCredit }
    assertTrue("Should have credit transactions", credits.isNotEmpty())
    assertTrue("Should have debit transactions", debits.isNotEmpty())

    val firstTx = transactions.first()
    assertNotNull(firstTx.date)
    assertNotNull(firstTx.recipient)
    assertTrue(firstTx.amount > 0.0)
    assertTrue(firstTx.currency.isNotBlank())
  }

  @Test
  fun `test insert and query new treasury transaction`() = runBlocking {
    val tx = com.example.worldbusiness.data.model.TreasuryTransactionRecord(
      date = "Sep 30, 2026 • 16:00 UTC",
      recipient = "Tokyo Tech Supply Co.",
      senderOrCounterparty = "OmniGlobal Holdings",
      amount = 75000.0,
      currency = "JPY",
      isCredit = false,
      category = "SUPPLY_FREIGHT",
      status = "SETTLED",
      referenceCode = "BOJ-NET-99120",
      note = "Electronic components settlement"
    )
    repository.insertTransaction(tx)
    val list = repository.allTransactions.first()
    val found = list.find { it.referenceCode == "BOJ-NET-99120" }
    assertNotNull(found)
    assertEquals("Tokyo Tech Supply Co.", found?.recipient)
    assertEquals(75000.0, found?.amount ?: 0.0, 0.01)
    assertEquals("JPY", found?.currency)
    assertEquals(false, found?.isCredit)
  }

  @Test
  fun `test currency conversion calculator logic same currency`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val quote = repository.fetchLiveConversionRate("USD", "USD", 5000.0)
    assertEquals(5000.0, quote.amount, 0.01)
    assertEquals(5000.0, quote.convertedAmount, 0.01)
    assertEquals(1.0, quote.exchangeRate, 0.0001)
    assertEquals(1.0, quote.inverseRate, 0.0001)
  }

  @Test
  fun `test currency conversion calculator logic cross currencies`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val quote = repository.fetchLiveConversionRate("USD", "EUR", 10000.0)
    assertEquals(10000.0, quote.amount, 0.01)
    assertTrue("Rate must be positive", quote.exchangeRate > 0.0)
    assertTrue("Converted amount must be positive", quote.convertedAmount > 0.0)
    assertTrue("Inverse rate must be positive", quote.inverseRate > 0.0)
    assertNotNull(quote.provider)
  }

  @Test
  fun `test currency converter viewModel actions`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val viewModel = com.example.worldbusiness.ui.WorldBusinessViewModel(repository)

    viewModel.openConversionCalculator("GBP", "JPY", 2500.0)
    assertEquals(true, viewModel.calculatorModalVisible.value)
    assertEquals("2500", viewModel.calculatorAmount.value)
    assertEquals("GBP", viewModel.calculatorFromCurrency.value)
    assertEquals("JPY", viewModel.calculatorToCurrency.value)

    viewModel.swapCalculatorCurrencies()
    assertEquals("JPY", viewModel.calculatorFromCurrency.value)
    assertEquals("GBP", viewModel.calculatorToCurrency.value)

    viewModel.setCalculatorAmount("50000")
    assertEquals("50000", viewModel.calculatorAmount.value)

    viewModel.closeConversionCalculator()
    assertEquals(false, viewModel.calculatorModalVisible.value)
  }

  @Test
  fun `test 30-day historical balance trend generator consolidated`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val balances = repository.allBalances.first()
    val summary = com.example.worldbusiness.data.model.TreasuryConversionSummary(
      baseCurrency = "USD",
      totalConsolidatedBalance = 18500000.0,
      accountsCount = balances.size,
      net24hImpactAmount = 12000.0,
      net24hImpactPercent = 0.65,
      weightedYieldApy = 4.1,
      accounts = balances.map {
        com.example.worldbusiness.data.model.CurrencyAccountDetail(
          currencyCode = it.currencyCode,
          currencyName = it.currencyName,
          symbol = it.symbol,
          nativeBalance = it.balance,
          rateToUsd = it.rateToUsd,
          dailyChangePercent = it.dailyChangePercent,
          bankInstitution = "Global Custody",
          accountMasked = "ACC-01",
          swiftBic = "BIC01",
          routingCode = "ISO-01",
          liquidityTier = "Tier 1",
          yieldApy = 3.5,
          convertedBalanceInBase = it.balance * it.rateToUsd,
          conversionRateToBase = it.rateToUsd,
          portfolioSharePercent = 20.0
        )
      }
    )

    val trend30 = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = summary,
      currencyCode = "ALL",
      timeRangeDays = 30
    )

    assertEquals(30, trend30.points.size)
    assertEquals("ALL", trend30.currencyCode)
    assertEquals("$", trend30.currencySymbol)
    assertEquals("USD", trend30.baseCurrency)
    assertEquals(30, trend30.timeRangeDays)
    assertTrue("Start balance should be positive", trend30.startBalance > 0.0)
    assertEquals(18500000.0, trend30.currentBalance, 1.0)
    assertTrue("Max balance should be >= min balance", trend30.maxBalance >= trend30.minBalance)
    assertTrue("Points should have peak marked", trend30.points.any { it.isPeak })

    // Test 7-day and 14-day ranges
    val trend7 = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = summary,
      currencyCode = "ALL",
      timeRangeDays = 7
    )
    assertEquals(7, trend7.points.size)

    val trend14 = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = summary,
      currencyCode = "ALL",
      timeRangeDays = 14
    )
    assertEquals(14, trend14.points.size)
  }

  @Test
  fun `test 30-day historical balance trend for specific currency vault`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val balances = repository.allBalances.first()
    val eurAccount = balances.find { it.currencyCode == "EUR" }!!

    val summary = com.example.worldbusiness.data.model.TreasuryConversionSummary(
      baseCurrency = "USD",
      totalConsolidatedBalance = 18500000.0,
      accountsCount = balances.size,
      net24hImpactAmount = 12000.0,
      net24hImpactPercent = 0.65,
      weightedYieldApy = 4.1,
      accounts = listOf(
        com.example.worldbusiness.data.model.CurrencyAccountDetail(
          currencyCode = eurAccount.currencyCode,
          currencyName = eurAccount.currencyName,
          symbol = eurAccount.symbol,
          nativeBalance = eurAccount.balance,
          rateToUsd = eurAccount.rateToUsd,
          dailyChangePercent = eurAccount.dailyChangePercent,
          bankInstitution = "BNP Paribas",
          accountMasked = "EUR-01",
          swiftBic = "BNPA01",
          routingCode = "SEPA-01",
          liquidityTier = "Tier 1",
          yieldApy = 3.65,
          convertedBalanceInBase = eurAccount.balance * eurAccount.rateToUsd,
          conversionRateToBase = eurAccount.rateToUsd,
          portfolioSharePercent = 100.0
        )
      )
    )

    val eurTrend = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = summary,
      currencyCode = "EUR",
      timeRangeDays = 30
    )

    assertEquals(30, eurTrend.points.size)
    assertEquals("EUR", eurTrend.currencyCode)
    assertEquals("€", eurTrend.currencySymbol)
    assertEquals(eurAccount.balance, eurTrend.currentBalance, 1.0)
    assertTrue("Average balance should be positive", eurTrend.averageBalance > 0.0)
  }

  @Test
  fun `test multi-jurisdiction tax withholdings and local net pay per employee`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val members = repository.allTeamMembers.first()
    val balances = repository.allBalances.first()
    assertTrue(members.isNotEmpty())

    // 1. Verify US Employee (Amara Okafor)
    val usEmployee = members.find { it.countryCode == "US" }!!
    val usDetail = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateEmployeePayroll(usEmployee, 1.0)
    assertEquals(24000.0, usDetail.grossPayLocal, 0.01)
    assertEquals("USD", usDetail.currency)
    assertEquals("$", usDetail.currencySymbol)
    assertTrue("Income tax should be positive", usDetail.incomeTaxLocal > 0.0)
    assertTrue("Social security should be positive", usDetail.socialSecurityLocal > 0.0)
    assertTrue("Total withheld should be positive", usDetail.totalTaxWithheldLocal > 0.0)
    assertEquals(usDetail.grossPayLocal - usDetail.totalTaxWithheldLocal, usDetail.netPayLocal, 0.01)
    assertTrue("Net pay must be less than gross pay", usDetail.netPayLocal < usDetail.grossPayLocal)
    assertTrue("Effective tax rate must be between 20% and 50%", usDetail.effectiveTaxRatePercent in 20.0..50.0)

    // 2. Verify UK Employee (Dr. Helena Vance)
    val ukEmployee = members.find { it.countryCode == "GB" }!!
    val gbpRate = balances.find { it.currencyCode == "GBP" }?.rateToUsd ?: 1.30
    val ukDetail = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateEmployeePayroll(ukEmployee, gbpRate)
    assertEquals(16500.0, ukDetail.grossPayLocal, 0.01)
    assertEquals("GBP", ukDetail.currency)
    assertEquals("£", ukDetail.currencySymbol)
    assertEquals(ukDetail.grossPayLocal - ukDetail.totalTaxWithheldLocal, ukDetail.netPayLocal, 0.01)
    assertTrue("UK deductions should include PAYE, NIC, Pension", ukDetail.deductions.size >= 3)

    // 3. Verify Singapore Employee (Marcus Chen)
    val sgEmployee = members.find { it.countryCode == "SG" }!!
    val sgdRate = balances.find { it.currencyCode == "SGD" }?.rateToUsd ?: 0.77
    val sgDetail = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateEmployeePayroll(sgEmployee, sgdRate)
    assertEquals(22000.0, sgDetail.grossPayLocal, 0.01)
    assertEquals("SGD", sgDetail.currency)
    assertEquals("S$", sgDetail.currencySymbol)
    assertEquals(sgDetail.grossPayLocal - sgDetail.totalTaxWithheldLocal, sgDetail.netPayLocal, 0.01)
    assertTrue("SGD net pay must be positive", sgDetail.netPayLocal > 0.0)

    // 4. Verify Japan B2B Contractor (Yuki Tanaka)
    val jpContractor = members.find { it.countryCode == "JP" }!!
    val jpyRate = balances.find { it.currencyCode == "JPY" }?.rateToUsd ?: 0.0069
    val jpDetail = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateEmployeePayroll(jpContractor, jpyRate)
    assertEquals(1850000.0, jpDetail.grossPayLocal, 0.01)
    assertEquals("JPY", jpDetail.currency)
    assertEquals("¥", jpDetail.currencySymbol)
    assertEquals(0.0, jpDetail.totalTaxWithheldLocal, 0.01)
    assertEquals(jpDetail.grossPayLocal, jpDetail.netPayLocal, 0.01) // 0% WHT for B2B Contractor

    // 5. Verify Global Summary
    val globalSummary = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateGlobalPayrollSummary(members, balances)
    assertEquals(members.size, globalSummary.totalEmployees)
    assertTrue("Total gross USD must be positive", globalSummary.totalGrossUsd > 0.0)
    assertTrue("Total withheld USD must be positive", globalSummary.totalTaxWithheldUsd > 0.0)
    assertTrue("Total net USD must be positive", globalSummary.totalNetPayUsd > 0.0)
    assertEquals(globalSummary.totalGrossUsd - globalSummary.totalTaxWithheldUsd, globalSummary.totalNetPayUsd, 0.01)
    assertTrue("Jurisdictions must be represented", globalSummary.jurisdictions.isNotEmpty())
  }

  @Test
  fun `test supply chain tracking real time locations and ETAs`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val shipments = repository.allShipments.first()
    assertTrue("Shipments should not be empty", shipments.isNotEmpty())

    // 1. Verify Ocean Freight Vessel (WBOS-FRT-9921)
    val oceanShipment = shipments.find { it.trackingCode == "WBOS-FRT-9921" }!!
    val oceanDetail = com.example.worldbusiness.data.repository.SupplyChainTrackingEngine.buildShipmentDetail(oceanShipment)
    assertEquals(com.example.worldbusiness.data.model.TransportMode.OCEAN_VESSEL, oceanDetail.transportMode)
    assertTrue("Real-time location must be present", oceanDetail.currentRealTimeLocation.isNotBlank())
    assertTrue("Location must mention corridor", oceanDetail.currentRealTimeLocation.contains("Red Sea") || oceanDetail.currentRealTimeLocation.contains("Strait"))
    assertNotNull(oceanDetail.gpsCoordinates.latitude)
    assertNotNull(oceanDetail.gpsCoordinates.longitude)
    assertTrue("Formatted GPS string must exist", oceanDetail.gpsCoordinates.formatted.isNotBlank())
    assertEquals(oceanShipment.estimatedArrival, oceanDetail.estimatedArrivalDate)
    assertTrue("Days remaining should be positive", oceanDetail.etaDaysRemaining > 0)
    assertEquals("ON_SCHEDULE", oceanDetail.etaStatus)
    assertTrue("Milestones must exist", oceanDetail.milestones.size >= 4)
    assertTrue("Should have a current milestone", oceanDetail.milestones.any { it.isCurrent })

    // 2. Verify Air Freight (WBOS-AIR-8012)
    val airShipment = shipments.find { it.trackingCode == "WBOS-AIR-8012" }!!
    val airDetail = com.example.worldbusiness.data.repository.SupplyChainTrackingEngine.buildShipmentDetail(airShipment)
    assertEquals(com.example.worldbusiness.data.model.TransportMode.AIR_FREIGHT, airDetail.transportMode)
    assertTrue(airDetail.currentRealTimeLocation.isNotBlank())
    assertTrue(airDetail.speedTelemetry.contains("km/h") || airDetail.speedTelemetry.contains("knots"))
    assertEquals(airShipment.estimatedArrival, airDetail.estimatedArrivalDate)

    // 3. Verify Overview Stats
    val allDetails = shipments.map { com.example.worldbusiness.data.repository.SupplyChainTrackingEngine.buildShipmentDetail(it) }
    val stats = com.example.worldbusiness.data.repository.SupplyChainTrackingEngine.buildOverviewStats(allDetails)
    assertEquals(shipments.size, stats.totalActiveShipments)
    assertTrue("Total cargo value should be positive", stats.totalCargoValueUsd > 0.0)
    assertTrue("On schedule rate should be high", stats.onScheduleRatePercent > 50.0)
  }

  @Test
  fun `test real time treasury compliance alert system`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val transactions = repository.allTransactions.first()
    assertTrue("Treasury transactions should be seeded", transactions.isNotEmpty())

    // 1. Initial Analysis
    val alerts = com.example.worldbusiness.data.repository.TreasuryComplianceEngine.analyzeTransactions(transactions)
    assertTrue("Compliance alerts should be generated", alerts.isNotEmpty())

    // 2. Check FINMA Swiss High-Value Rule
    val swissAlert = alerts.find { it.jurisdictionCode == "CH" }
    assertNotNull("Swiss high-value capital inflow should trigger FINMA alert", swissAlert)
    assertEquals(com.example.worldbusiness.data.model.ComplianceSeverity.HIGH, swissAlert!!.severity)
    assertEquals("FINMA-AMLA-GWG-EDD-06", swissAlert.statutoryRuleCode)
    assertTrue("Should mention UBO / Form A", swissAlert.requiredFiling.contains("Form A"))

    // 3. Check FinCEN CTR Rule
    val usAlert = alerts.find { it.statutoryRuleCode.contains("FINCEN") }
    assertNotNull("Large USD transfer should trigger FinCEN CTR audit", usAlert)
    assertEquals(com.example.worldbusiness.data.model.ComplianceRiskType.LARGE_VALUE_CTR, usAlert!!.riskType)

    // 4. Test Simulated High-Risk Sanction Violation
    val highRiskTx = com.example.worldbusiness.data.model.TreasuryTransactionRecord(
      date = "Oct 01, 2026",
      recipient = "Al-Zahrani Maritime Arbitrage Ltd.",
      senderOrCounterparty = "OmniGlobal Treasury Escrow Outbound",
      amount = 48500.0,
      currency = "USD",
      isCredit = false,
      category = "COMMERCIAL_SETTLEMENT",
      status = "PENDING",
      referenceCode = "SWIFT-MT103-TEST-9999",
      note = "Urgent transshipment clearance fee to unverified maritime intermediary"
    )
    val updatedAlerts = com.example.worldbusiness.data.repository.TreasuryComplianceEngine.analyzeTransactions(transactions + highRiskTx)
    val criticalAlert = updatedAlerts.find { it.severity == com.example.worldbusiness.data.model.ComplianceSeverity.CRITICAL }
    assertNotNull("Critical OFAC alert must be flagged immediately", criticalAlert)
    assertTrue("Critical alert should target SDN / Sanctioned entity", criticalAlert!!.title.contains("OFAC") || criticalAlert.title.contains("CRITICAL"))
    assertEquals(96, criticalAlert.riskScore)

    // 5. Test Structuring / Smurfing Rule
    val structuringTx = com.example.worldbusiness.data.model.TreasuryTransactionRecord(
      date = "Oct 01, 2026",
      recipient = "Anonymous Tech Vendor",
      senderOrCounterparty = "OmniGlobal Sub",
      amount = 9850.0,
      currency = "USD",
      isCredit = false,
      category = "COMMERCIAL_SETTLEMENT",
      status = "PENDING",
      referenceCode = "WIRE-9850",
      note = "Split transfer"
    )
    val structuringAlerts = com.example.worldbusiness.data.repository.TreasuryComplianceEngine.analyzeTransactions(listOf(structuringTx))
    val structAlert = structuringAlerts.find { it.riskType == com.example.worldbusiness.data.model.ComplianceRiskType.AML_STRUCTURING }
    assertNotNull("Structuring transfer must be flagged as critical BSA violation", structAlert)
    assertEquals(com.example.worldbusiness.data.model.ComplianceSeverity.CRITICAL, structAlert!!.severity)

    // 6. Test Remediation Override
    val overrides = mapOf(swissAlert.id to com.example.worldbusiness.data.model.ComplianceAlertStatus.RESOLVED)
    val remediatedAlerts = com.example.worldbusiness.data.repository.TreasuryComplianceEngine.analyzeTransactions(transactions, overrides)
    val remediatedSwiss = remediatedAlerts.find { it.id == swissAlert.id }!!
    assertEquals(com.example.worldbusiness.data.model.ComplianceAlertStatus.RESOLVED, remediatedSwiss.status)

    // 7. Verify Stats Computation
    val stats = com.example.worldbusiness.data.repository.TreasuryComplianceEngine.computeOverviewStats(remediatedAlerts, transactions.size)
    assertTrue("Health score should be calculated", stats.complianceHealthScore in 1..100)
    assertTrue("Monitored jurisdictions should be at least 5", stats.monitoredJurisdictionsCount >= 5)
  }

  @Test
  fun `test D3 global logistics great circle arc visualization engine`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val shipments = repository.allShipments.first()
    assertTrue("Shipments must be present for D3 radar", shipments.isNotEmpty())

    // 1. Build D3 routes
    val d3Routes = shipments.map { record ->
      val detail = com.example.worldbusiness.data.repository.SupplyChainTrackingEngine.buildShipmentDetail(record, 0)
      com.example.worldbusiness.data.repository.D3GeodesicMath.buildRoute(detail)
    }
    assertEquals(shipments.size, d3Routes.size)

    // 2. Validate Great-Circle Geodesic Interpolation
    val firstRoute = d3Routes.first()
    assertTrue("Route must contain interpolated spherical waypoints", firstRoute.waypoints.size >= 10)
    assertEquals(firstRoute.originPort.point.latitude, firstRoute.waypoints.first().latitude, 0.1)
    assertEquals(firstRoute.destinationPort.point.latitude, firstRoute.waypoints.last().latitude, 0.1)

    // 3. Validate Port resolution
    val fraRoute = d3Routes.find { it.trackingCode == "WBOS-AIR-8012" }!!
    assertEquals("FRA", fraRoute.originPort.code)
    assertEquals("SIN", fraRoute.destinationPort.code)
    assertEquals(com.example.worldbusiness.data.model.TransportMode.AIR_FREIGHT, fraRoute.transportMode)

    // 4. Validate Canvas Projection
    val (px, py) = com.example.worldbusiness.data.repository.D3GeodesicMath.projectToCanvas(
      firstRoute.currentPosition,
      canvasWidth = 1000f,
      canvasHeight = 500f
    )
    assertTrue("Projected X coordinate should be within bounds", px in 0f..1000f)
    assertTrue("Projected Y coordinate should be within bounds", py in 0f..500f)

    // 5. Validate Strategic Choke Points
    val chokePoints = com.example.worldbusiness.data.repository.D3GeodesicMath.GLOBAL_CHOKE_POINTS
    assertTrue("Should monitor at least 5 global choke points", chokePoints.size >= 5)
    assertNotNull(chokePoints.find { it.id == "SUEZ" })
    assertNotNull(chokePoints.find { it.id == "MALACCA" })
  }

  @Test
  fun `test multinational corporate entities hierarchical tree visualization engine`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val entities: List<EntityRecord> = repository.allEntities.first()
    assertTrue("Entities must be seeded", entities.isNotEmpty())

    // 1. Build hierarchy tree
    val tree = com.example.worldbusiness.data.repository.CorporateHierarchyEngine.buildHierarchy(entities)
    assertEquals(1, tree.size) // Root node
    val root = tree.first()

    // 2. Validate Root / Ultimate Parent Entity (UPE)
    assertEquals(0, root.tierLevel)
    assertTrue("Root must be Holdings parent", root.entity.name.contains("Holdings", ignoreCase = true))
    assertEquals("US", root.entity.countryCode)
    assertNull(root.parentId)
    assertEquals(100.0, root.ownershipPercentage, 0.01)

    // 3. Validate Tier 1 Regional Hub Subsidiaries
    assertTrue("Root should have regional operating subsidiaries", root.children.isNotEmpty())
    val emeaNode = root.children.find { it.entity.name.contains("EMEA") || it.entity.countryCode == "GB" }
    assertNotNull("EMEA holding subsidiary must be present under Holdings", emeaNode)
    assertEquals(1, emeaNode!!.tierLevel)
    assertEquals(root.entity.id, emeaNode.parentId)

    // 4. Validate Tier 2 Operating Subsidiary (DACH GmbH under EMEA)
    val dachNode = emeaNode.children.find { it.entity.countryCode == "CH" }
    assertNotNull("DACH GmbH must be nested under EMEA", dachNode)
    assertEquals(2, dachNode!!.tierLevel)
    assertEquals(emeaNode.entity.id, dachNode.parentId)
    assertEquals("GOOD_STANDING", dachNode.entity.status)

    // 5. Validate Tree Statistics
    val stats = com.example.worldbusiness.data.repository.CorporateHierarchyEngine.computeTreeStats(entities)
    assertEquals(entities.size, stats.totalEntities)
    assertTrue("Consolidated capital should be positive", stats.consolidatedOperatingCapitalUsd > 0.0)
    assertTrue("Average compliance score should be between 80 and 100", stats.averageComplianceScore in 80..100)
    assertTrue("Should report at least 3 entities in good standing", stats.goodStandingCount >= 3)

    // 6. Test Collapsed Node Filtering
    val collapsedTree = com.example.worldbusiness.data.repository.CorporateHierarchyEngine.buildHierarchy(
      entities,
      collapsedNodeIds = setOf(emeaNode.entity.id)
    )
    val collapsedEmea = collapsedTree.first().children.find { it.entity.id == emeaNode.entity.id }!!
    assertFalse("EMEA node should be collapsed when in collapsed set", collapsedEmea.isExpanded)
  }

  @Test
  fun `test paper invoice camera ocr structured data extractor engine`() {
    val samples = com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine.SAMPLE_INVOICES
    assertTrue("Sample paper invoices must be available", samples.size >= 4)

    // 1. Test German Cross-Border Engineering Invoice (EUR with 19% MwSt & comma numbers)
    val germanSample = samples.find { it.id == "SAMPLE_DE_SIEMENS" }!!
    val germanResult = com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine.extractFromText(germanSample.rawText)
    assertEquals("Bavaria Automotive Engineering SE", germanResult.vendorName)
    assertEquals("EUR", germanResult.currency)
    assertEquals("INV-2026-0887", germanResult.invoiceNumber)
    assertEquals(232050.0, germanResult.totalAmount, 0.5)
    assertEquals(19.0, germanResult.taxRatePercent, 0.1)
    assertTrue("Should detect high OCR confidence", germanResult.confidenceScore >= 90)
    assertTrue("Should extract IBAN & BIC routing", germanResult.bankDetails.contains("DE89"))

    // 2. Test Swiss Biomedical Pharma Invoice (CHF with 0% Export exemption)
    val swissSample = samples.find { it.id == "SAMPLE_CH_NOVARTIS" }!!
    val swissResult = com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine.extractFromText(swissSample.rawText)
    assertEquals("Aether Pharma Group AG", swissResult.vendorName)
    assertEquals("CHF", swissResult.currency)
    assertEquals("INV-2026-0891", swissResult.invoiceNumber)
    assertEquals(285000.0, swissResult.totalAmount, 0.5)
    assertEquals(0.0, swissResult.taxRatePercent, 0.01)

    // 3. Test Singapore Robotics Hardware Invoice (SGD with 9% GST)
    val sgSample = samples.find { it.id == "SAMPLE_SG_ROBOTICS" }!!
    val sgResult = com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine.extractFromText(sgSample.rawText)
    assertTrue(sgResult.vendorName.contains("Tokyo Robotics"))
    assertEquals("SGD", sgResult.currency)
    assertEquals(348800.0, sgResult.totalAmount, 0.5)
    assertEquals(9.0, sgResult.taxRatePercent, 0.1)

    // 4. Test US Defense Aerospace Invoice (USD)
    val usSample = samples.find { it.id == "SAMPLE_US_STRATOS" }!!
    val usResult = com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine.extractFromText(usSample.rawText)
    assertEquals("Stratos Defense Aerospace Inc.", usResult.vendorName)
    assertEquals("USD", usResult.currency)
    assertEquals(510000.0, usResult.totalAmount, 0.5)
    assertEquals("INV-2026-0888", usResult.invoiceNumber)
  }

  @Test
  fun `test automated rule based currency hedging strategy engine`() {
    val sampleInvoices = listOf(
      InvoiceRecord(
        id = 101,
        invoiceNumber = "INV-2026-EUR-1",
        issuingEntityName = "OmniGlobal DACH GmbH",
        clientName = "Bavaria Automotive SE",
        clientCountry = "Germany",
        issueDate = "Sep 22, 2026",
        dueDate = "Oct 22, 2026", // 21 days horizon
        amount = 195000.0,
        currency = "EUR",
        taxRatePercent = 19.0,
        status = "PENDING",
        serviceDescription = "Autonomous Verification"
      ),
      InvoiceRecord(
        id = 102,
        invoiceNumber = "INV-2026-BRL-1",
        issuingEntityName = "OmniGlobal LatAm Ltda",
        clientName = "Petrobras Energy SA",
        clientCountry = "Brazil",
        issueDate = "Aug 15, 2026",
        dueDate = "Nov 25, 2026", // 55 days horizon
        amount = 850000.0,
        currency = "BRL",
        taxRatePercent = 0.0,
        status = "PENDING",
        serviceDescription = "Offshore Telemetry"
      ),
      InvoiceRecord(
        id = 103,
        invoiceNumber = "INV-2026-USD-1",
        issuingEntityName = "OmniGlobal Holdings Inc.",
        clientName = "Stratos Aerospace Inc.",
        clientCountry = "United States",
        issueDate = "Sep 01, 2026",
        dueDate = "Oct 01, 2026",
        amount = 300000.0,
        currency = "USD", // Base currency - should NOT require FX hedge
        taxRatePercent = 0.0,
        status = "PENDING",
        serviceDescription = "Cloud SLA"
      )
    )

    val balances = listOf(
      FxBalanceRecord(currencyCode = "USD", currencyName = "US Dollar", symbol = "$", balance = 3000000.0, rateToUsd = 1.0, dailyChangePercent = 0.0),
      FxBalanceRecord(currencyCode = "EUR", currencyName = "Euro", symbol = "€", balance = 50000.0, rateToUsd = 1.0925, dailyChangePercent = 0.2), // Does NOT cover 195k EUR
      FxBalanceRecord(currencyCode = "BRL", currencyName = "Brazilian Real", symbol = "R$", balance = 10000.0, rateToUsd = 0.1840, dailyChangePercent = -0.3)
    )

    // 1. Generate suggestions
    val suggestions = com.example.worldbusiness.data.repository.CurrencyHedgingEngine.generateHedgingSuggestions(
      invoices = sampleInvoices,
      balances = balances
    )

    // USD invoice must be excluded (no FX risk)
    assertEquals(2, suggestions.size)

    // 2. Validate EUR invoice rule (Rule 2: Moderate Volatility 8.9% with 21D horizon -> ZERO_COST_COLLAR)
    val eurHedge = suggestions.find { it.currency == "EUR" }!!
    assertEquals(com.example.worldbusiness.data.model.HedgingStrategyType.ZERO_COST_COLLAR, eurHedge.strategyType)
    assertNotNull(eurHedge.collarFloorRate)
    assertNotNull(eurHedge.collarCapRate)
    assertTrue("Collar cap should be greater than spot rate", eurHedge.collarCapRate!! > eurHedge.spotRate)
    assertTrue("Collar floor should be lower than spot rate", eurHedge.collarFloorRate!! < eurHedge.spotRate)
    assertTrue("VaR should be positive", eurHedge.valueAtRisk95Usd > 1000.0)

    // 3. Validate BRL invoice rule (Rule 4: Sovereign Yield Parity Synthetic Swap for high carry differential)
    val brlHedge = suggestions.find { it.currency == "BRL" }!!
    assertEquals(com.example.worldbusiness.data.model.HedgingStrategyType.SYNTHETIC_SWAP, brlHedge.strategyType)
    assertEquals(com.example.worldbusiness.data.model.HedgingUrgency.HIGH, brlHedge.urgency)
    assertTrue("BRL forward rate should reflect positive forward carry", brlHedge.forwardRate > brlHedge.spotRate)

    // 4. Test Natural Balance Sheet Match (Rule 0) when vault has sufficient funds
    val highBalanceVaults = listOf(
      FxBalanceRecord(currencyCode = "EUR", currencyName = "Euro", symbol = "€", balance = 500000.0, rateToUsd = 1.0925, dailyChangePercent = 0.2)
    )
    val naturalHedges = com.example.worldbusiness.data.repository.CurrencyHedgingEngine.generateHedgingSuggestions(
      invoices = listOf(sampleInvoices.first()),
      balances = highBalanceVaults
    )
    assertEquals(1, naturalHedges.size)
    assertEquals(com.example.worldbusiness.data.model.HedgingStrategyType.NATURAL_MATCH, naturalHedges.first().strategyType)
    assertEquals(com.example.worldbusiness.data.model.HedgingUrgency.LOW, naturalHedges.first().urgency)

    // 5. Test Portfolio Summary computation
    val summary = com.example.worldbusiness.data.repository.CurrencyHedgingEngine.computePortfolioSummary(suggestions)
    assertTrue("Total unhedged exposure should be greater than zero", summary.totalUnhedgedExposureUsd > 0.0)
    assertTrue("Total VaR should be greater than zero", summary.totalValueAtRiskUsd > 0.0)
    assertEquals(2, summary.activeSuggestionsCount)
  }

  @Test
  fun `test corporate tax filing deadlines and regulatory compliance engine`() {
    val entities = listOf(
      EntityRecord(
        id = 1,
        name = "OmniGlobal Holdings Inc.",
        jurisdiction = "United States (Delaware)",
        countryCode = "US",
        entityType = "C-Corp",
        taxId = "US-EIN-98-4421903",
        status = "GOOD_STANDING",
        baseCurrency = "USD",
        operatingCapital = 45000000.0,
        annualFilingDeadline = "2026-10-15",
        localDirector = "Sarah Jenkins",
        complianceScore = 98
      ),
      EntityRecord(
        id = 2,
        name = "OmniGlobal DACH GmbH",
        jurisdiction = "Switzerland (Zurich)",
        countryCode = "CH",
        entityType = "GmbH",
        taxId = "CHE-105.890.112 MWST",
        status = "GOOD_STANDING",
        baseCurrency = "CHF",
        operatingCapital = 18500000.0,
        annualFilingDeadline = "2026-11-30",
        localDirector = "Beat Meier",
        complianceScore = 95
      ),
      EntityRecord(
        id = 3,
        name = "OmniGlobal Singapore Pte. Ltd.",
        jurisdiction = "Singapore",
        countryCode = "SG",
        entityType = "Pte. Ltd.",
        taxId = "UEN: 202109842K",
        status = "GOOD_STANDING",
        baseCurrency = "SGD",
        operatingCapital = 22000000.0,
        annualFilingDeadline = "2026-10-20",
        localDirector = "Wei Ming Tan",
        complianceScore = 97
      )
    )

    // 1. Pull statutory deadlines
    val deadlines = com.example.worldbusiness.data.repository.CorporateTaxComplianceEngine.generateDeadlines(entities)
    assertTrue("Must generate multiple statutory deadlines across jurisdictions", deadlines.size >= 6)

    // 2. Test US Corporate Income Tax (Form 1120 due Oct 15)
    val usCit = deadlines.find { it.countryCode == "US" && it.statutoryFormCode == "IRS-1120" }
    assertNotNull("US Form 1120 deadline must be present", usCit)
    assertEquals(14, usCit!!.daysRemaining)
    assertEquals("Internal Revenue Service (IRS)", usCit.statutoryAuthority)
    assertTrue("Should have non-zero tax liability estimate", (usCit.estimatedTaxLiabilityUsd ?: 0.0) > 0.0)
    assertTrue("Should have penal code reference", usCit.penalCodeReference.contains("IRC Sec. 6651"))

    // 3. Test Swiss Cantonal CIT & Federal MWST
    val swissCit = deadlines.find { it.countryCode == "CH" && it.statutoryFormCode == "ESTV-ZH-2026" }
    assertNotNull("Swiss CIT must be present", swissCit)
    assertEquals("Switzerland", swissCit!!.countryName)
    assertTrue("ESTV Swiss tax authority must match", swissCit.statutoryAuthority.contains("ESTV"))

    val swissVat = deadlines.find { it.countryCode == "CH" && it.statutoryFormCode == "MWST-Q3" }
    assertNotNull("Swiss MWST must be present", swissVat)
    assertEquals(com.example.worldbusiness.data.model.TaxRequirementType.VAT_GST_SALES, swissVat!!.statutoryCategory)

    // 4. Test Singapore IRAS CIT and ACRA Annual Return
    val sgCit = deadlines.find { it.countryCode == "SG" && it.statutoryFormCode == "IRAS-Form-CS" }
    assertNotNull("Singapore IRAS CIT must be present", sgCit)
    val sgAcra = deadlines.find { it.countryCode == "SG" && it.statutoryFormCode == "ACRA-AR-XBRL" }
    assertNotNull("ACRA XBRL Annual Return must be present", sgAcra)
    assertEquals(19, sgAcra!!.daysRemaining)

    // 5. Test Overview Metrics Aggregation
    val overview = com.example.worldbusiness.data.repository.CorporateTaxComplianceEngine.computeOverview(deadlines, entities)
    assertEquals(deadlines.size, overview.totalFilingDeadlines)
    assertTrue("Should have upcoming deadlines in next 30 days", overview.upcomingIn30DaysCount >= 2)
    assertTrue("Total estimated tax due should exceed $100,000", overview.totalEstimatedTaxDueUsd > 100000.0)
    assertTrue("Penalty exposure should be tracked", overview.totalStatutoryPenaltyAtRiskUsd > 10000.0)
    assertEquals(3, overview.jurisdictionsCoveredCount)
  }

  @Test
  fun `test centralized audit log cryptographic chain and automated accountability`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, WorldBusinessDatabase::class.java).build()
    val testRepo = WorldBusinessRepository(inMemoryDb)

    // 1. Initial empty state has 0 logs
    assertEquals(0, inMemoryDb.auditLogDao().countLogs())

    // 2. Perform actions: insert Entity and insert Invoice
    val entity = EntityRecord(
      name = "OmniGlobal Deutschland GmbH",
      jurisdiction = "Germany (Bavaria)",
      countryCode = "DE",
      entityType = "GmbH",
      taxId = "DE-123456789",
      status = "GOOD_STANDING",
      baseCurrency = "EUR",
      operatingCapital = 5000000.0,
      annualFilingDeadline = "2026-10-31",
      localDirector = "Hans Gruber",
      complianceScore = 96
    )
    testRepo.insertEntity(entity)

    val invoice = InvoiceRecord(
      invoiceNumber = "INV-2026-DE-991",
      issuingEntityName = "OmniGlobal Deutschland GmbH",
      clientName = "BMW Group AG",
      clientCountry = "Germany",
      issueDate = "2026-10-01",
      dueDate = "2026-10-31",
      amount = 750000.0,
      currency = "EUR",
      taxRatePercent = 19.0,
      status = "PENDING",
      serviceDescription = "Autonomous Fleet Telemetry"
    )
    testRepo.insertInvoice(invoice)

    // 3. Verify automated audit logs were generated
    val logs = testRepo.allAuditLogs.first()
    assertEquals(2, logs.size)

    val entityLog = logs.find { it.actionType == AuditActionType.ENTITY_CREATED.name }
    assertNotNull("Entity creation must be auto-logged", entityLog)
    assertEquals(AuditLogModule.ENTITIES.name, entityLog!!.module)
    assertTrue("Description must reference entity", entityLog.description.contains("OmniGlobal Deutschland GmbH"))

    val invoiceLog = logs.find { it.actionType == AuditActionType.INVOICE_CREATED.name }
    assertNotNull("Invoice creation must be auto-logged", invoiceLog)
    assertEquals(AuditLogModule.COMMERCIAL.name, invoiceLog!!.module)
    assertEquals(750000.0, invoiceLog.financialAmount)
    assertEquals("EUR", invoiceLog.currency)

    // 4. Test SHA-256 Cryptographic Chain Verification
    val chainResult = AuditCryptographicEngine.verifyChainIntegrity(logs)
    assertTrue("Cryptographic chain must be valid and tamper-evident", chainResult.isValid)
    assertEquals(2, chainResult.totalRecordsVerified)
    assertNotNull(chainResult.rootHash)
    assertNotNull(chainResult.latestHash)

    // 5. Tamper Detection Test: Modifying an audit record's payload invalidates the chain
    val tamperedLog = invoiceLog.copy(financialAmount = 9999999.0) // Tampered amount
    val tamperedLogs = listOf(entityLog, tamperedLog)
    val tamperedResult = AuditCryptographicEngine.verifyChainIntegrity(tamperedLogs)
    assertFalse("Chain verification must fail when payload is tampered", tamperedResult.isValid)
    assertEquals(tamperedLog.logId, tamperedResult.compromisedRecordId)

    inMemoryDb.close()
  }

  @Test
  fun `test hierarchical entity search bar matching by name and id with ancestor auto-expansion`() {
    val entities = listOf(
      EntityRecord(
        id = 1,
        name = "OmniGlobal Holdings Inc.",
        jurisdiction = "United States (Delaware)",
        countryCode = "US",
        entityType = "C-Corporation",
        taxId = "EIN: 84-2910482",
        status = "GOOD_STANDING",
        baseCurrency = "USD",
        operatingCapital = 45000000.0,
        annualFilingDeadline = "Mar 01, 2027",
        localDirector = "Sarah Sterling",
        complianceScore = 98
      ),
      EntityRecord(
        id = 2,
        name = "OmniGlobal EMEA Holding Ltd.",
        jurisdiction = "United Kingdom (London)",
        countryCode = "GB",
        entityType = "Holding Co.",
        taxId = "GB-VAT-902-1481-99",
        status = "GOOD_STANDING",
        baseCurrency = "GBP",
        operatingCapital = 25000000.0,
        annualFilingDeadline = "Dec 31, 2026",
        localDirector = "Alistair Vance",
        complianceScore = 96
      ),
      EntityRecord(
        id = 3,
        name = "OmniGlobal DACH GmbH",
        jurisdiction = "Switzerland (Zurich)",
        countryCode = "CH",
        entityType = "GmbH",
        taxId = "CHE-105.890.112 MWST",
        status = "GOOD_STANDING",
        baseCurrency = "CHF",
        operatingCapital = 18500000.0,
        annualFilingDeadline = "Nov 30, 2026",
        localDirector = "Beat Meier",
        complianceScore = 95
      ),
      EntityRecord(
        id = 4,
        name = "OmniGlobal APAC Pte. Ltd.",
        jurisdiction = "Singapore",
        countryCode = "SG",
        entityType = "Pte. Ltd.",
        taxId = "UEN: 202109842K",
        status = "GOOD_STANDING",
        baseCurrency = "SGD",
        operatingCapital = 22000000.0,
        annualFilingDeadline = "Oct 20, 2026",
        localDirector = "Wei Ming Tan",
        complianceScore = 97
      )
    )

    val engine = com.example.worldbusiness.data.repository.CorporateHierarchyEngine

    // 1. Test matching by subsidiary Name
    assertTrue("Should match by partial name", engine.matchesQuery(entities[2], "DACH"))
    assertTrue("Should match case-insensitively", engine.matchesQuery(entities[3], "singapore"))

    // 2. Test matching by subsidiary ID (#ID, plain ID, id:ID)
    assertTrue("Should match plain ID 3", engine.matchesQuery(entities[2], "3"))
    assertTrue("Should match formatted #3", engine.matchesQuery(entities[2], "#3"))
    assertTrue("Should match id:3", engine.matchesQuery(entities[2], "id:3"))
    assertTrue("Should match formatted #1", engine.matchesQuery(entities[0], "#1"))
    assertFalse("Should not match different ID", engine.matchesQuery(entities[0], "#99"))

    // 3. Test matching by Tax ID & Registry
    assertTrue("Should match Swiss VAT number", engine.matchesQuery(entities[2], "CHE-105"))
    assertTrue("Should match US EIN", engine.matchesQuery(entities[0], "84-2910482"))
    assertTrue("Should match Singapore UEN", engine.matchesQuery(entities[3], "202109842K"))

    // 4. Test Ancestor Auto-Expansion
    val tree = engine.buildHierarchy(entities)
    val ancestorsForDach = engine.findAncestorsOfMatchingNodes(tree, "DACH")
    assertTrue("Must auto-expand root Holdings (#1)", ancestorsForDach.contains(1L))
    assertTrue("Must auto-expand EMEA Holding (#2) under which DACH sits", ancestorsForDach.contains(2L))

    // 5. Test Ancestor Auto-Expansion by ID
    val ancestorsForSingapore = engine.findAncestorsOfMatchingNodes(tree, "#4")
    assertTrue("Must auto-expand root for Singapore subsidiary", ancestorsForSingapore.contains(1L))

    // 6. Non-matching query returns empty ancestors
    val nonExistentAncestors = engine.findAncestorsOfMatchingNodes(tree, "NonExistentSubsidiaryXYZ")
    assertTrue("Non-matching query should yield empty ancestors", nonExistentAncestors.isEmpty())
  }

  @Test
  fun `test cross-border invoice multi-currency tax calculations and status tracking`() {
    val calculator = com.example.worldbusiness.data.repository.CrossBorderTaxCalculator

    // 1. Verify Supported Currencies with Live FX Rates
    val currencies = calculator.supportedCurrencies
    assertTrue("Must support USD, EUR, GBP, SGD, CHF, JPY, BRL", currencies.size >= 7)
    assertEquals(1.0, calculator.getCurrencyOption("USD").rateToUsd, 0.001)
    assertTrue("EUR rate to USD must be > 1.0", calculator.getCurrencyOption("EUR").rateToUsd > 1.0)
    assertTrue("GBP rate to USD must be > 1.0", calculator.getCurrencyOption("GBP").rateToUsd > 1.0)

    // 2. Test B2B Reverse Charge Calculation (0% VAT, EU / UK / Swiss B2B cross-border)
    val ukReverseCharge = calculator.calculate(
      subtotal = 100000.0,
      currencyCode = "GBP",
      clientCountry = "United Kingdom",
      isReverseChargeApplied = true
    )
    assertEquals(0.0, ukReverseCharge.vatRatePercent, 0.001)
    assertEquals(0.0, ukReverseCharge.vatAmount, 0.001)
    assertEquals(100000.0, ukReverseCharge.netReceivable, 0.001)
    assertTrue("Must mention Reverse Charge in compliance note", ukReverseCharge.complianceNote.contains("Reverse Charge"))
    assertTrue("Must calculate equivalent USD", ukReverseCharge.equivalentUsd > 120000.0)

    // 3. Test Standard Statutory VAT Calculation (UK 20% VAT)
    val ukStandardVat = calculator.calculate(
      subtotal = 50000.0,
      currencyCode = "GBP",
      clientCountry = "United Kingdom",
      isReverseChargeApplied = false
    )
    assertEquals(20.0, ukStandardVat.vatRatePercent, 0.001)
    assertEquals(10000.0, ukStandardVat.vatAmount, 0.001)
    assertEquals(60000.0, ukStandardVat.grossTotal, 0.001)
    assertEquals(60000.0, ukStandardVat.netReceivable, 0.001)

    // 4. Test Swiss MWST Rate Lookup (8.1% VAT)
    val swissCalculation = calculator.calculate(
      subtotal = 200000.0,
      currencyCode = "CHF",
      clientCountry = "Switzerland",
      isReverseChargeApplied = false
    )
    assertEquals(8.1, swissCalculation.vatRatePercent, 0.001)
    assertEquals(16200.0, swissCalculation.vatAmount, 0.001)
    assertEquals(216200.0, swissCalculation.grossTotal, 0.001)

    // 5. Test Brazil Cross-Border Withholding Tax Deduction (15% WHT)
    val brazilCalculation = calculator.calculate(
      subtotal = 80000.0,
      currencyCode = "USD",
      clientCountry = "Brazil",
      isReverseChargeApplied = false
    )
    assertEquals(15.0, brazilCalculation.whtRatePercent, 0.001)
    assertEquals(12000.0, brazilCalculation.whtAmount, 0.001)
    assertEquals(84000.0, brazilCalculation.grossTotal, 0.001) // 80k + 5% ISS (4k)
    assertEquals(72000.0, brazilCalculation.netReceivable, 0.001) // 84k - 12k WHT

    // 6. Test Lifecycle Status Stage Transitions
    val draftStage = com.example.worldbusiness.data.model.InvoiceStatusStage.fromString("DRAFT")
    assertEquals(0, draftStage.stepIndex)
    val pendingStage = com.example.worldbusiness.data.model.InvoiceStatusStage.fromString("PENDING")
    assertEquals(1, pendingStage.stepIndex)
    val inClearingStage = com.example.worldbusiness.data.model.InvoiceStatusStage.fromString("IN_CLEARING")
    assertEquals(2, inClearingStage.stepIndex)
    val paidStage = com.example.worldbusiness.data.model.InvoiceStatusStage.fromString("PAID")
    assertEquals(3, paidStage.stepIndex)
  }

  @Test
  fun `test real-time currency conversion service and dynamic invoice calculation`() {
    val calculator = com.example.worldbusiness.data.repository.CrossBorderTaxCalculator

    // 1. Test LiveCurrencyFeed Multi-Currency Conversions
    val dynamicRates = mapOf(
      "USD" to 1.0,
      "EUR" to 1.1200, // Live updated EUR/USD rate
      "GBP" to 1.3250, // Live updated GBP/USD rate
      "SGD" to 0.7850,
      "CHF" to 1.1800,
      "JPY" to 0.0070,
      "BRL" to 0.1900
    )

    val feed = com.example.worldbusiness.data.model.LiveCurrencyFeed(
      baseCurrency = "USD",
      ratesToUsd = dynamicRates,
      lastUpdatedUtc = "2026-10-03 12:00 UTC",
      providerName = "Financial Feed API",
      status = com.example.worldbusiness.data.model.CurrencySyncStatus.LIVE
    )

    assertEquals(1.1200, feed.getRateToUsd("EUR"), 0.0001)
    assertEquals(1.3250, feed.getRateToUsd("GBP"), 0.0001)

    // Convert 50,000 EUR to USD
    val eurToUsd = feed.convert(50000.0, "EUR", "USD")
    assertEquals(56000.0, eurToUsd, 0.01)

    // Convert 100,000 USD to GBP
    val usdToGbp = feed.convert(100000.0, "USD", "GBP")
    assertEquals(100000.0 / 1.3250, usdToGbp, 0.01)

    // 2. Test Dynamic Invoicing Calculation with Live FX Rates Injected
    val dynamicInvoiceCalc = calculator.calculate(
      subtotal = 100000.0,
      currencyCode = "EUR",
      clientCountry = "Germany",
      isReverseChargeApplied = true,
      liveRatesToUsd = dynamicRates
    )

    assertEquals(0.0, dynamicInvoiceCalc.vatRatePercent, 0.001)
    assertEquals(100000.0, dynamicInvoiceCalc.netReceivable, 0.001)
    // Converted to USD using the live 1.1200 rate instead of default 1.0925
    assertEquals(112000.0, dynamicInvoiceCalc.equivalentUsd, 0.01)

    // 3. Test Currency Option with Live Rate
    val eurOption = calculator.getCurrencyOption("EUR", dynamicRates)
    assertEquals(1.1200, eurOption.rateToUsd, 0.0001)
    assertEquals("€", eurOption.symbol)
    assertEquals("🇪🇺", eurOption.flag)
  }

  @Test
  fun `test global supplier room schema and dao`() = runBlocking {
    val dao = database.globalSupplierDao()

    // 1. Verify Seed Initialization
    repository.seedInitialDataIfNeeded()
    val initialCount = dao.getCount()
    assertTrue("Initial seed must contain at least 7 global suppliers", initialCount >= 7)

    // 2. Test Contact Information Fields
    val siemens = dao.getSupplierByCode("SUP-DE-0042").first()
    assertNotNull("Siemens supplier record must exist", siemens)
    assertEquals("Siemens AG Industrial Systems", siemens?.legalName)
    assertEquals("Germany", siemens?.country)
    assertEquals("DE", siemens?.countryCode)
    assertEquals("Dr. Klaus Weber", siemens?.primaryContactName)
    assertEquals("klaus.weber@siemens-supply.de", siemens?.primaryContactEmail)
    assertEquals("+49 89 636 00", siemens?.primaryContactPhone)
    assertEquals("Werner-von-Siemens-Straße 1, 80333 Munich, Germany", siemens?.physicalAddress)

    // 3. Test Payment Terms Fields
    assertEquals(30, siemens?.paymentTermsDays)
    assertEquals("NET_30", siemens?.paymentTermsDescription)
    assertEquals("EUR", siemens?.preferredCurrency)
    assertEquals("SEPA", siemens?.settlementRail)
    assertEquals("Deutsche Bank AG", siemens?.bankName)
    assertEquals("DEUTDEDD", siemens?.bankSwiftBic)
    assertEquals("DE89370400440532013000", siemens?.bankIbanOrAccountNumber)
    assertEquals(2.0, siemens?.discountEarlyPaymentPercent ?: 0.0, 0.01)

    // 4. Test Compliance Documentation Fields
    assertEquals("COMPLIANT", siemens?.complianceStatus)
    assertTrue("W-8BEN/Tax form must be filed", siemens?.w8BenOrTaxFormFiled == true)
    assertTrue("ISO 9001 must be certified", siemens?.iso9001Certified == true)
    assertTrue("ESG rating score must be > 80", (siemens?.esgRatingScore ?: 0) >= 90)
    assertTrue("Anti-Bribery declaration must be signed", siemens?.antiBriberyPactSigned == true)
    assertEquals("VERIFIED", siemens?.kycVerificationStatus)
    assertEquals("LOW", siemens?.riskLevel)

    // 5. Test Compliance Status Filtering & Counts
    val compliantCount = dao.getCompliantCount()
    assertTrue("Compliant count must be at least 5", compliantCount >= 5)

    val pendingAuditCount = dao.getPendingAuditCount()
    assertTrue("Pending audit count must be at least 1 (Embraer)", pendingAuditCount >= 1)

    val expiredOrNonCompliant = dao.getNonCompliantOrExpiredCount()
    assertTrue("Expired documentation count must be at least 1 (Foxconn)", expiredOrNonCompliant >= 1)

    val auditPendingList = dao.getSuppliersByCompliance("AUDIT_PENDING").first()
    assertTrue("Must find Embraer in AUDIT_PENDING list", auditPendingList.any { it.supplierCode == "SUP-BR-0077" })

    // 6. Test Multi-Field Search
    val searchByName = dao.searchSuppliers("Semiconductor").first()
    assertTrue("Must find TSMC by partial legal name", searchByName.any { it.supplierCode == "SUP-TW-0108" })

    val searchByTaxId = dao.searchSuppliers("CHE-105").first()
    assertTrue("Must find STMicroelectronics by Swiss VAT number", searchByTaxId.any { it.supplierCode == "SUP-CH-0014" })

    val searchByContact = dao.searchSuppliers("Laurent Blanc").first()
    assertTrue("Must find supplier by primary contact person", searchByContact.any { it.supplierCode == "SUP-CH-0014" })

    // 7. Test Insertion of New Global Supplier
    val newSupplier = com.example.worldbusiness.data.model.GlobalSupplierRecord(
      supplierCode = "SUP-US-0991",
      legalName = "Honeywell Aerospace Technologies Inc.",
      tradingName = "Honeywell Aerospace",
      country = "United States",
      countryCode = "US",
      taxRegistrationNumber = "US-EIN-22-1928401",
      businessCategory = "MANUFACTURING",
      primaryContactName = "Rebecca Vance",
      primaryContactEmail = "rebecca.vance@honeywell-supply.com",
      primaryContactPhone = "+1 704 627 6200",
      physicalAddress = "855 S Mint St, Charlotte, NC 28202, United States",
      websiteUrl = "https://aerospace.honeywell.com",
      paymentTermsDays = 45,
      paymentTermsDescription = "NET_45",
      preferredCurrency = "USD",
      settlementRail = "FEDWIRE",
      bankName = "JPMorgan Chase Bank, N.A.",
      bankSwiftBic = "CHASUS33",
      bankIbanOrAccountNumber = "US990210000219920194",
      discountEarlyPaymentPercent = 1.0,
      complianceStatus = "COMPLIANT",
      w8BenOrTaxFormFiled = true,
      iso9001Certified = true,
      esgRatingScore = 94,
      antiBriberyPactSigned = true,
      kycVerificationStatus = "VERIFIED",
      lastAuditDate = "2026-09-15",
      complianceDocExpiryDate = "2027-09-15",
      complianceOfficerNotes = "DoD CMMC Level 3 Security Cleared",
      riskLevel = "LOW",
      isActive = true
    )

    val insertedId = repository.insertSupplier(newSupplier)
    assertTrue("Inserted supplier ID must be > 0", insertedId > 0)
    assertEquals(initialCount + 1, dao.getCount())

    // 8. Test Status and Terms Mutation
    dao.updateComplianceStatus(insertedId, "AUDIT_PENDING", "Routine annual recertification scheduled")
    val updatedSupplier = dao.getSupplierByIdSync(insertedId)
    assertEquals("AUDIT_PENDING", updatedSupplier?.complianceStatus)
    assertEquals("Routine annual recertification scheduled", updatedSupplier?.complianceOfficerNotes)

    dao.updatePaymentTerms(insertedId, 60, "NET_60", "USD")
    val termsUpdatedSupplier = dao.getSupplierByIdSync(insertedId)
    assertEquals(60, termsUpdatedSupplier?.paymentTermsDays)
    assertEquals("NET_60", termsUpdatedSupplier?.paymentTermsDescription)

    // 9. Test Deletion
    val deletedRows = dao.deleteSupplierById(insertedId)
    assertEquals(1, deletedRows)
    assertNull("Deleted supplier must not be found", dao.getSupplierByIdSync(insertedId))
    assertEquals(initialCount, dao.getCount())
  }

  @Test
  fun `test supplier compliance status bar metrics and supply chain risk calculations`() = runBlocking {
    val dao = database.globalSupplierDao()
    repository.seedInitialDataIfNeeded()
    val suppliers = dao.getAllSuppliers().first()

    assertTrue("Must have at least 7 seeded suppliers", suppliers.size >= 7)

    val total = suppliers.size
    val verified = suppliers.count { it.complianceStatus.equals("COMPLIANT", ignoreCase = true) }
    val expiringOrPending = suppliers.count {
      it.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) ||
      it.riskLevel.equals("MEDIUM", ignoreCase = true)
    }
    val nonCompliant = suppliers.count {
      it.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) ||
      it.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) ||
      it.riskLevel.equals("HIGH", ignoreCase = true)
    }

    // 1. Check Color-Coded Tiers
    assertEquals("Green (Verified) count must equal 5", 5, verified)
    assertEquals("Amber (Expiring / Audit Pending) count must equal 1", 1, expiringOrPending)
    assertEquals("Red (Non-Compliant / Expired Doc) count must equal 1", 1, nonCompliant)
    assertEquals("Sum of categories must equal total", total, verified + expiringOrPending + nonCompliant)

    // 2. Check Proportional Status Bar Ratios
    val verifiedRatio = verified.toFloat() / total.toFloat()
    val expiringRatio = expiringOrPending.toFloat() / total.toFloat()
    val nonCompliantRatio = nonCompliant.toFloat() / total.toFloat()
    assertEquals(1.0f, verifiedRatio + expiringRatio + nonCompliantRatio, 0.001f)

    // 3. Check Overall Health Index
    val healthScore = ((verified * 100 + expiringOrPending * 50) / total).coerceIn(0, 100)
    assertTrue("Health index must be between 70 and 90", healthScore in 70..90)

    // 4. Check Statutory Filing Rates
    val w8BenRate = ((suppliers.count { it.w8BenOrTaxFormFiled }.toDouble() / total) * 100).toInt()
    val isoRate = ((suppliers.count { it.iso9001Certified }.toDouble() / total) * 100).toInt()
    val antiBriberyRate = ((suppliers.count { it.antiBriberyPactSigned }.toDouble() / total) * 100).toInt()

    assertTrue("W-8BEN rate must be >= 80%", w8BenRate >= 80)
    assertEquals("ISO 9001 rate must be 100% for initial partners", 100, isoRate)
    assertTrue("Anti-Bribery pact rate must be >= 80%", antiBriberyRate >= 80)

    // 5. Check ESG Average
    val avgEsg = suppliers.map { it.esgRatingScore }.average()
    assertTrue("Average ESG score must be >= 80.0", avgEsg >= 80.0)
  }

  @Test
  fun `test supplier detail view contact info payment terms and associated invoices`() = runBlocking {
    val dao = database.globalSupplierDao()
    val invoiceDao = database.invoiceDao()
    repository.seedInitialDataIfNeeded()

    val siemens = dao.getSupplierByCode("SUP-DE-0042").first()
    assertNotNull("Siemens supplier record must exist", siemens)

    // 1. Verify Full Contact Information Available
    assertEquals("Dr. Klaus Weber", siemens?.primaryContactName)
    assertEquals("klaus.weber@siemens-supply.de", siemens?.primaryContactEmail)
    assertEquals("+49 89 636 00", siemens?.primaryContactPhone)
    assertEquals("Werner-von-Siemens-Straße 1, 80333 Munich, Germany", siemens?.physicalAddress)
    assertEquals("https://www.siemens.com/global-supply", siemens?.websiteUrl)
    assertEquals("DE129274202", siemens?.taxRegistrationNumber)
    assertEquals("MANUFACTURING", siemens?.businessCategory)
    assertEquals("Germany", siemens?.country)
    assertEquals("DE", siemens?.countryCode)

    // 2. Verify Full Payment Terms & International Banking Rails
    assertEquals(30, siemens?.paymentTermsDays)
    assertEquals("NET_30", siemens?.paymentTermsDescription)
    assertEquals("EUR", siemens?.preferredCurrency)
    assertEquals("SEPA", siemens?.settlementRail)
    assertEquals("Deutsche Bank AG", siemens?.bankName)
    assertEquals("DEUTDEDD", siemens?.bankSwiftBic)
    assertEquals("DE89370400440532013000", siemens?.bankIbanOrAccountNumber)
    assertEquals(2.0, siemens?.discountEarlyPaymentPercent ?: 0.0, 0.01)

    // 3. Insert and Verify Associated Invoices for this Supplier
    val sampleInvoice1 = com.example.worldbusiness.data.model.InvoiceRecord(
      invoiceNumber = "INV-2026-SUP-01",
      issuingEntityName = "OmniGlobal Holdings Inc.",
      clientName = "Siemens AG Industrial Systems",
      clientCountry = "Germany",
      issueDate = "Sep 10, 2026",
      dueDate = "Oct 10, 2026",
      amount = 140000.0,
      currency = "EUR",
      taxRatePercent = 0.0,
      status = "PAID",
      serviceDescription = "Procurement of Edge Turbines & Sensors (SUP-DE-0042)"
    )
    val sampleInvoice2 = com.example.worldbusiness.data.model.InvoiceRecord(
      invoiceNumber = "INV-2026-SUP-02",
      issuingEntityName = "OmniGlobal DACH GmbH",
      clientName = "Siemens AG Industrial Systems",
      clientCountry = "Germany",
      issueDate = "Oct 01, 2026",
      dueDate = "Nov 01, 2026",
      amount = 75000.0,
      currency = "EUR",
      taxRatePercent = 19.0,
      status = "PENDING",
      serviceDescription = "Factory Automation Hardware Integration (SUP-DE-0042)"
    )
    invoiceDao.insertAll(listOf(sampleInvoice1, sampleInvoice2))

    val allInvoices = invoiceDao.getAllInvoices().first()
    val associatedInvoices = allInvoices.filter {
      it.clientName.contains("Siemens", ignoreCase = true) ||
      it.serviceDescription.contains("SUP-DE-0042", ignoreCase = true)
    }

    assertEquals("Must find 2 associated invoices for Siemens", 2, associatedInvoices.size)
    val totalBilled = associatedInvoices.sumOf { it.amount }
    val paidBilled = associatedInvoices.filter { it.status == "PAID" }.sumOf { it.amount }
    val pendingBilled = associatedInvoices.filter { it.status == "PENDING" }.sumOf { it.amount }

    assertEquals(215000.0, totalBilled, 0.01)
    assertEquals(140000.0, paidBilled, 0.01)
    assertEquals(75000.0, pendingBilled, 0.01)
  }

  @Test
  fun `test supplier audit export engine generates compliant csv and statutory dossier`() = runBlocking {
    val dao = database.globalSupplierDao()
    repository.seedInitialDataIfNeeded()
    val suppliers = dao.getAllSuppliers().first()

    assertTrue("Must have at least 7 suppliers", suppliers.size >= 7)

    // 1. Test CSV Export Generation
    val csv = com.example.worldbusiness.data.repository.SupplierAuditExportEngine.generateCsv(suppliers)
    assertNotNull("CSV output must not be null", csv)
    assertTrue("CSV must have content", csv.isNotBlank())

    val csvLines = csv.trim().lines()
    assertEquals("CSV must have 1 header line + 7 supplier lines", suppliers.size + 1, csvLines.size)

    // Verify CSV Headers
    val header = csvLines.first()
    assertTrue("Header must contain Supplier Code", header.contains("Supplier Code"))
    assertTrue("Header must contain Legal Entity Name", header.contains("Legal Entity Name"))
    assertTrue("Header must contain Compliance Status", header.contains("Compliance Status"))
    assertTrue("Header must contain Payment Terms Days", header.contains("Payment Terms Days"))
    assertTrue("Header must contain Settlement Currency", header.contains("Settlement Currency"))
    assertTrue("Header must contain W8BEN Tax Form Filed", header.contains("W8BEN Tax Form Filed"))
    assertTrue("Header must contain Beneficiary Bank", header.contains("Beneficiary Bank"))

    // Verify Data Row Content
    val siemensCsvLine = csvLines.firstOrNull { it.contains("SUP-DE-0042") }
    assertNotNull("Siemens line must be present in CSV", siemensCsvLine)
    assertTrue("Siemens CSV must contain legal name", siemensCsvLine!!.contains("Siemens AG Industrial Systems"))
    assertTrue("Siemens CSV must contain Net 30", siemensCsvLine.contains("NET_30"))
    assertTrue("Siemens CSV must contain EUR", siemensCsvLine.contains("EUR"))
    assertTrue("Siemens CSV must contain Deutsche Bank AG", siemensCsvLine.contains("Deutsche Bank AG"))
    assertTrue("Siemens CSV must contain COMPLIANT", siemensCsvLine.contains("COMPLIANT"))

    // 2. Test Statutory Audit Dossier Generation
    val dossier = com.example.worldbusiness.data.repository.SupplierAuditExportEngine.generateAuditDossierText(suppliers)
    assertNotNull("Dossier output must not be null", dossier)
    assertTrue("Dossier must contain audit title", dossier.contains("STATUTORY SUPPLIER DIRECTORY AUDIT DOSSIER"))
    assertTrue("Dossier must mention OECD Due Diligence", dossier.contains("OECD Due Diligence Guidance"))
    assertTrue("Dossier must mention US FCPA", dossier.contains("US FCPA"))
    assertTrue("Dossier must mention SOX Sec 404", dossier.contains("SOX Sec 404"))
    assertTrue("Dossier must summarize verified suppliers", dossier.contains("Verified & Compliant (Green)"))
    assertTrue("Dossier must summarize W-8BEN coverage", dossier.contains("IRS Form W-8BEN-E Coverage"))
    assertTrue("Dossier must contain Siemens item block", dossier.contains("SUPPLIER CODE: SUP-DE-0042"))
    assertTrue("Dossier must contain TSMC item block", dossier.contains("SUPPLIER CODE: SUP-TW-0108"))
    assertTrue("Dossier must end with digital certification seal", dossier.contains("DIGITALLY CERTIFIED FOR REGULATORY PURPOSES"))
  }

  @Test
  fun `test d3 currency volatility heatmap dataset metrics and hedging recommendations`() {
    val pairs = com.example.worldbusiness.ui.components.D3VolatilityDataSet.ALL_PAIRS
    assertEquals("Must contain 10 major currency pairs", 10, pairs.size)

    // 1. Verify Key Pairs Present
    val symbols = pairs.map { it.pairSymbol }
    assertTrue(symbols.contains("EUR/USD"))
    assertTrue(symbols.contains("GBP/USD"))
    assertTrue(symbols.contains("USD/JPY"))
    assertTrue(symbols.contains("USD/CHF"))
    assertTrue(symbols.contains("AUD/USD"))
    assertTrue(symbols.contains("USD/CAD"))
    assertTrue(symbols.contains("USD/SGD"))
    assertTrue(symbols.contains("GBP/JPY"))

    // 2. Verify Volatility Across Time Horizons
    pairs.forEach { p ->
      assertTrue("${p.pairSymbol} 1W vol must be positive", p.vol1W in 3.0..25.0)
      assertTrue("${p.pairSymbol} 1M vol must be positive", p.vol1M in 3.0..25.0)
      assertTrue("${p.pairSymbol} 3M vol must be positive", p.vol3M in 3.0..25.0)
      assertTrue("${p.pairSymbol} 1Y vol must be positive", p.vol1Y in 3.0..25.0)
      assertTrue("${p.pairSymbol} Implied vol must be positive", p.impliedVol in 3.0..30.0)
      assertTrue("${p.pairSymbol} Daily pips must be positive", p.dailyPipsRange > 0.0)
      assertTrue("${p.pairSymbol} VaR 95% per million must be > $5,000", p.var95PerMillion > 5000.0)
    }

    // 3. Verify Extreme Volatility Tiers
    val usdjpy = pairs.first { it.pairSymbol == "USD/JPY" }
    assertEquals(com.example.worldbusiness.data.model.HedgingUrgency.CRITICAL, usdjpy.urgency)
    assertEquals(com.example.worldbusiness.data.model.HedgingStrategyType.LAYERED_TRANCHES, usdjpy.recommendedStrategy)
    assertTrue("USD/JPY 3M vol must be > 13.0%", usdjpy.vol3M > 13.0)

    val usdsgd = pairs.first { it.pairSymbol == "USD/SGD" }
    assertEquals(com.example.worldbusiness.data.model.HedgingUrgency.LOW, usdsgd.urgency)
    assertTrue("USD/SGD 3M vol must be < 6.0%", usdsgd.vol3M < 6.0)

    // 4. Verify Horizon Helper Function
    assertEquals(usdjpy.vol1W, usdjpy.getVolForHorizon(com.example.worldbusiness.ui.components.VolatilityTimeHorizon.ONE_WEEK), 0.001)
    assertEquals(usdjpy.vol1M, usdjpy.getVolForHorizon(com.example.worldbusiness.ui.components.VolatilityTimeHorizon.ONE_MONTH), 0.001)
    assertEquals(usdjpy.vol3M, usdjpy.getVolForHorizon(com.example.worldbusiness.ui.components.VolatilityTimeHorizon.THREE_MONTHS), 0.001)
    assertEquals(usdjpy.vol1Y, usdjpy.getVolForHorizon(com.example.worldbusiness.ui.components.VolatilityTimeHorizon.ONE_YEAR), 0.001)
  }

  @Test
  fun `test bulk multi currency payroll scheduling conversion and disbursement`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val memberDao = database.teamMemberDao()
    val txDao = database.treasuryTransactionDao()
    val fxDao = database.fxBalanceDao()
    val auditDao = database.auditLogDao()

    val members = memberDao.getAllTeamMembers().first()
    assertTrue("Must have seeded multinational hires", members.isNotEmpty())

    val initialUsdBal = fxDao.getByCode("USD")?.balance ?: 0.0
    assertTrue("USD vault must have positive balance", initialUsdBal > 50000.0)

    val sampleBatches = listOf(
      com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch(
        currency = "EUR",
        countryCode = "DE",
        clearingRail = "SEPA Instant SCT (Europe)",
        recipientCount = 2,
        grossLocalAmount = 18300.0,
        taxWithheldLocalAmount = 5600.0,
        netPayableLocalAmount = 12700.0,
        exchangeRateToFunding = 1.0900,
        fundingCurrencyEquivalent = 12700.0 * 1.0900,
        isApiLive = true,
        rateProvider = "Open Exchange Rates API",
        rateTimestamp = "14:00:00 UTC",
        memberIds = members.take(2).map { it.id }
      ),
      com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch(
        currency = "GBP",
        countryCode = "GB",
        clearingRail = "BACS / Faster Payments (UK)",
        recipientCount = 1,
        grossLocalAmount = 8200.0,
        taxWithheldLocalAmount = 2400.0,
        netPayableLocalAmount = 5800.0,
        exchangeRateToFunding = 1.3050,
        fundingCurrencyEquivalent = 5800.0 * 1.3050,
        isApiLive = true,
        rateProvider = "Open Exchange Rates API",
        rateTimestamp = "14:00:00 UTC",
        memberIds = listOf(members.first().id)
      )
    )

    val memberIdsToPay = sampleBatches.flatMap { it.memberIds }.toSet()
    val scheduledDate = "Oct 31, 2026"

    val receipt = repository.executeBulkMultiCurrencyPayroll(
      scheduledDate = scheduledDate,
      fundingCurrency = "USD",
      batches = sampleBatches,
      selectedMemberIds = memberIdsToPay
    )

    // 1. Verify Receipt Integrity
    assertNotNull("Receipt must be produced", receipt)
    assertTrue("Receipt batch ref must start with prefix", receipt.batchReferenceId.startsWith("BATCH-PAY-2026-MULTI-"))
    assertEquals("USD", receipt.fundingVaultCurrency)
    assertEquals(scheduledDate, receipt.scheduledDate)
    assertTrue("Crypto hash must be computed", receipt.cryptographicHash.isNotBlank())
    assertEquals(2, receipt.totalNetLocalPayouts.size)
    assertEquals(12700.0, receipt.totalNetLocalPayouts["EUR"] ?: 0.0, 0.01)
    assertEquals(5800.0, receipt.totalNetLocalPayouts["GBP"] ?: 0.0, 0.01)

    // 2. Verify Member Records Marked as Paid
    val updatedMembers = memberDao.getAllTeamMembers().first()
    val paidMembers = updatedMembers.filter { memberIdsToPay.contains(it.id) }
    paidMembers.forEach { m ->
      assertEquals("PAID_THIS_CYCLE", m.status)
      assertEquals(scheduledDate, m.lastPaidDate)
    }

    // 3. Verify Treasury Outflow Ledger Records
    val transactions = txDao.getAllTransactions().first()
    val payrollTxs = transactions.filter { it.category == "GLOBAL_PAYROLL" && it.referenceCode.startsWith("WIRE-") }
    assertTrue("Must have at least 2 wire tx records created", payrollTxs.size >= 2)

    val eurWire = payrollTxs.find { it.recipient.contains("EUR") }
    assertNotNull("Must find EUR clearing wire", eurWire)
    assertTrue("EUR wire must reference SEPA", eurWire!!.note.contains("SEPA"))

    // 4. Verify Vault Balance Deducted
    val finalUsdBal = fxDao.getByCode("USD")?.balance ?: 0.0
    assertEquals(initialUsdBal - receipt.totalFundingOutflow, finalUsdBal, 0.01)

    // 5. Verify Cryptographic Audit Log
    val auditLogs = auditDao.getAllLogs().first()
    val payrollAudit = auditLogs.find { it.module == "WORKFORCE" && it.actionType == "PAYROLL_EXECUTED" }
    assertNotNull("Must find certified payroll audit log", payrollAudit)
    assertTrue("Audit must mention multi-currency batch", payrollAudit!!.description.contains(receipt.batchReferenceId))
  }

  @Test
  fun `test real time currency conversion service and treasury invoicing integration`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    val currencyService = com.example.worldbusiness.data.repository.RealTimeCurrencyService()
    val feed = currencyService.currencyFeed.value

    // 1. Verify Feed Baseline & Available Currencies
    assertNotNull("Currency feed must be initialized", feed)
    assertTrue("Feed must contain USD rate 1.0", feed.getRateToUsd("USD") == 1.0)
    assertTrue("Feed must contain EUR rate > 0", feed.getRateToUsd("EUR") > 0.9)
    assertTrue("Feed must contain GBP rate > 0", feed.getRateToUsd("GBP") > 1.1)
    assertTrue("Feed must contain JPY rate > 0", feed.getRateToUsd("JPY") in 0.005..0.01)
    assertTrue("Feed must contain CHF rate > 0", feed.getRateToUsd("CHF") in 1.0..1.3)
    assertTrue("Feed must contain SGD rate > 0", feed.getRateToUsd("SGD") in 0.7..0.9)

    // 2. Cross-Rate Calculations & Reciprocal Precision
    val eurToUsd = feed.getCrossRate("EUR", "USD")
    val usdToEur = feed.getCrossRate("USD", "EUR")
    assertEquals("Reciprocal cross rates must multiply to ~1.0", 1.0, eurToUsd * usdToEur, 0.001)

    val gbpToEur = feed.getCrossRate("GBP", "EUR")
    assertTrue("GBP/EUR cross rate should be > 1.10", gbpToEur > 1.10)

    // 3. Multi-Currency Conversion Logic
    val converted100EurToUsd = feed.convert(100.0, "EUR", "USD")
    assertEquals(100.0 * feed.getRateToUsd("EUR"), converted100EurToUsd, 0.001)

    val converted500GbpToSgd = feed.convert(500.0, "GBP", "SGD")
    val expectedSgd = 500.0 * (feed.getRateToUsd("GBP") / feed.getRateToUsd("SGD"))
    assertEquals(expectedSgd, converted500GbpToSgd, 0.001)

    // 4. Live Quote Service Generation
    val quote = currencyService.fetchQuote("EUR", "USD", 10000.0)
    assertNotNull("Quote must not be null", quote)
    assertEquals(10000.0, quote.amount, 0.001)
    assertEquals("EUR", quote.fromCurrency)
    assertEquals("USD", quote.toCurrency)
    assertTrue("Exchange rate must match EUR/USD", quote.exchangeRate > 0.9)
    assertTrue("Converted amount must be > $9,000 USD", quote.convertedAmount > 9000.0)
    assertEquals(1.0 / quote.exchangeRate, quote.inverseRate, 0.001)

    // 5. Invoicing Cross-Border Integration
    val invoiceDao = database.invoiceDao()
    val invoices = invoiceDao.getAllInvoices().first()
    assertTrue("Must have seeded invoices", invoices.isNotEmpty())

    val sampleInvoice = invoices.first()
    val invoiceUsdEquivalent = feed.convert(sampleInvoice.amount, sampleInvoice.currency, "USD")
    assertTrue("Converted invoice value in USD must be positive", invoiceUsdEquivalent > 0.0)

    // 6. Treasury Multi-Currency Revaluation Integration
    val fxDao = database.fxBalanceDao()
    val balances = fxDao.getAllBalances().first()
    assertTrue("Must have multi-currency vaults", balances.size >= 5)

    val totalConsolidatedUsd = balances.sumOf { vault ->
      feed.convert(vault.balance, vault.currencyCode, "USD")
    }
    assertTrue("Consolidated treasury balance across all vaults must exceed $1M USD", totalConsolidatedUsd > 1000000.0)
  }

  @Test
  fun `test room database offline invoice storage and sync`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val invoiceDao = database.invoiceDao()
    val auditDao = database.auditLogDao()
    val syncEngine = repository.invoiceSyncEngine

    // 1. Verify Seeded Invoices Exist in Room Database
    val initialInvoices = invoiceDao.getAllInvoices().first()
    assertTrue("Initial Room database must contain seeded invoices", initialInvoices.isNotEmpty())
    val initialPendingCount = invoiceDao.getPendingSyncInvoices().size
    assertEquals("Initial seeded invoices should all be SYNCED", 0, initialPendingCount)

    // 2. Enable Offline Access Mode
    syncEngine.setOfflineMode(true)
    assertTrue("Offline mode must be active", syncEngine.isOfflineMode.value)

    // 3. Create Cross-Border Invoices while Offline in Room
    val offlineInvoice1 = com.example.worldbusiness.data.model.InvoiceRecord(
      invoiceNumber = "INV-OFFLINE-001",
      issuingEntityName = "OmniGlobal UK & EMEA Ltd.",
      clientName = "Geneva BioTech Innovations SA",
      clientCountry = "Switzerland",
      issueDate = "Oct 04, 2026",
      dueDate = "Nov 04, 2026",
      amount = 75000.0,
      currency = "CHF",
      taxRatePercent = 8.1,
      status = "PENDING",
      serviceDescription = "Cross-border Clinical Informatics Licensing",
      isSynced = false,
      syncStatus = "PENDING_SYNC"
    )

    val offlineInvoice2 = com.example.worldbusiness.data.model.InvoiceRecord(
      invoiceNumber = "INV-OFFLINE-002",
      issuingEntityName = "OmniGlobal APAC Pte. Ltd.",
      clientName = "Kyoto Precision Semiconductor Ltd.",
      clientCountry = "Japan",
      issueDate = "Oct 04, 2026",
      dueDate = "Nov 15, 2026",
      amount = 18500000.0,
      currency = "JPY",
      taxRatePercent = 10.0,
      status = "PENDING",
      serviceDescription = "Automated Silicon Verification Pipeline",
      isSynced = false,
      syncStatus = "PENDING_SYNC"
    )

    val id1 = repository.insertInvoiceOffline(offlineInvoice1)
    val id2 = repository.insertInvoiceOffline(offlineInvoice2)
    assertTrue("Invoice 1 Room ID must be generated", id1 > 0)
    assertTrue("Invoice 2 Room ID must be generated", id2 > 0)

    // 4. Verify Local Room Offline Persistence & Pending Queue
    val pendingInvoices = invoiceDao.getPendingSyncInvoices()
    assertEquals("Room must queue exactly 2 pending offline invoices", 2, pendingInvoices.size)

    val pendingCount = invoiceDao.getPendingSyncCount().first()
    assertEquals("Pending count flow must report 2 items", 2, pendingCount)

    val storedInv1 = invoiceDao.getInvoiceById(id1)
    assertNotNull("Stored invoice 1 must be retrievable locally from Room", storedInv1)
    assertFalse("Stored invoice 1 must be marked not synced", storedInv1!!.isSynced)
    assertEquals("PENDING_SYNC", storedInv1.syncStatus)
    assertTrue("Offline created flag must be true", storedInv1.offlineCreated)

    // 5. Verify Offline Invoices Can Be Queried Offline
    val allInvoices = invoiceDao.getAllInvoices().first()
    val foundOffline = allInvoices.filter { it.invoiceNumber.startsWith("INV-OFFLINE-") }
    assertEquals("Both offline invoices must be accessible in local Room store", 2, foundOffline.size)

    // 6. Restore Online Mode & Execute Synchronization
    syncEngine.setOfflineMode(false)
    assertFalse("Offline mode must be deactivated", syncEngine.isOfflineMode.value)

    val syncResult = repository.syncLocalInvoices()
    assertEquals("Must successfully sync 2 invoices", 2, syncResult.syncedCount)
    assertEquals("Failed sync count should be 0", 0, syncResult.failedCount)
    assertTrue("Synced invoice list must contain INV-OFFLINE-001", syncResult.syncedInvoiceNumbers.contains("INV-OFFLINE-001"))
    assertTrue("Synced invoice list must contain INV-OFFLINE-002", syncResult.syncedInvoiceNumbers.contains("INV-OFFLINE-002"))

    // 7. Verify Invoices in Room are Now Marked SYNCED
    val remainingPending = invoiceDao.getPendingSyncInvoices()
    assertEquals("Pending queue must be completely clear after sync", 0, remainingPending.size)

    val syncedInv1 = invoiceDao.getInvoiceById(id1)
    assertNotNull(syncedInv1)
    assertTrue("Invoice 1 isSynced must be true", syncedInv1!!.isSynced)
    assertEquals("SYNCED", syncedInv1.syncStatus)
    assertNotNull("Last synced timestamp must be populated", syncedInv1.lastSyncedAt)

    // 8. Verify Audit Trail for Offline Persistence and Ledger Sync
    val auditLogs = auditDao.getAllLogs().first()
    val syncAudit = auditLogs.find { it.module == "COMMERCIAL" && it.description.contains("INV-OFFLINE-001") && it.description.contains("synchronized") }
    assertNotNull("Cryptographic audit trail must record invoice synchronization", syncAudit)
  }

  @Test
  fun `test treasury multi currency room summaries and projected payroll aggregations`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    // 1. Test Room Database Multi-Currency Balances Aggregation
    val fxDao = database.fxBalanceDao()
    val balances = fxDao.getAllBalances().first()
    assertTrue("Room database must contain multi-currency balances", balances.size >= 5)

    val treasuryTotals = fxDao.getConsolidatedTreasuryTotals().first()
    assertTrue("Total vaults count from Room aggregation must be >= 5", treasuryTotals.totalVaultsCount >= 5)
    assertTrue("Consolidated USD total in Room must exceed 1,000,000", treasuryTotals.totalConsolidatedUsd > 1000000.0)
    assertTrue("Highest vault balance in USD must be positive", treasuryTotals.highestBalanceUsd > 0.0)

    // 2. Test Recharts Historical Currency Balance Trends Engine
    val conversionSummary = com.example.worldbusiness.data.model.TreasuryConversionSummary(
      baseCurrency = "USD",
      totalConsolidatedBalance = treasuryTotals.totalConsolidatedUsd,
      accountsCount = balances.size,
      net24hImpactAmount = 14250.0,
      net24hImpactPercent = 0.85,
      weightedYieldApy = 4.15,
      accounts = balances.map { b ->
        com.example.worldbusiness.data.model.CurrencyAccountDetail(
          currencyCode = b.currencyCode,
          currencyName = b.currencyName,
          symbol = b.symbol,
          nativeBalance = b.balance,
          rateToUsd = b.rateToUsd,
          dailyChangePercent = b.dailyChangePercent,
          bankInstitution = "Tier 1 Sovereign Custody",
          accountMasked = "${b.currencyCode}-***",
          swiftBic = "${b.currencyCode}XXMM1",
          routingCode = "ISO 20022",
          liquidityTier = "Tier 1",
          yieldApy = 4.0,
          convertedBalanceInBase = b.balance * b.rateToUsd,
          conversionRateToBase = b.rateToUsd,
          portfolioSharePercent = 15.0
        )
      }
    )

    val trend30d = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = conversionSummary,
      transactions = emptyList(),
      currencyCode = "ALL",
      timeRangeDays = 30
    )
    assertEquals("Recharts trend history must contain 30 daily points", 30, trend30d.points.size)
    assertTrue("Recharts trend 30D high must exceed 30D low", trend30d.maxBalance >= trend30d.minBalance)
    assertTrue("Recharts trend current balance must be positive", trend30d.currentBalance > 0.0)

    // Test individual currency trend (e.g. EUR)
    val trendEur = com.example.worldbusiness.data.repository.TreasuryHistoryEngine.generateTrendHistory(
      summary = conversionSummary,
      transactions = emptyList(),
      currencyCode = "EUR",
      timeRangeDays = 14
    )
    assertEquals("EUR Recharts trend must contain 14 daily points", 14, trendEur.points.size)
    assertEquals("EUR", trendEur.currencyCode)

    // 3. Test Room Database Projected Global Payroll Aggregations
    val teamDao = database.teamMemberDao()
    val allMembers = teamDao.getAllTeamMembers().first()
    assertTrue("Room database must contain team members", allMembers.isNotEmpty())

    val payrollTotals = teamDao.getPayrollTotalsAggregation().first()
    assertTrue("Total headcount in Room must be >= 5", payrollTotals.totalHeadcount >= 5)
    assertTrue("Total currencies in Room must be >= 3", payrollTotals.totalCurrenciesCount >= 3)

    val currencyAggregations = teamDao.getPayrollCurrencyAggregations().first()
    assertTrue("Must have currency aggregations from Room", currencyAggregations.isNotEmpty())
    val usdAgg = currencyAggregations.find { it.currency == "USD" }
    assertNotNull("USD payroll aggregation must exist in Room", usdAgg)
    assertTrue("USD local gross must be positive", usdAgg!!.totalLocalGross > 0.0)

    val jurisdictionAggregations = teamDao.getPayrollJurisdictionAggregations().first()
    assertTrue("Must have jurisdiction aggregations from Room", jurisdictionAggregations.isNotEmpty())

    val employmentAggregations = teamDao.getPayrollEmploymentTypeAggregations().first()
    assertTrue("Must have employment type aggregations from Room", employmentAggregations.isNotEmpty())

    // 4. Test Global Payroll Engine Aggregation
    val globalSummary = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateGlobalPayrollSummary(
      members = allMembers,
      fxBalances = balances
    )
    assertTrue("Total gross USD must exceed 50,000", globalSummary.totalGrossUsd > 50000.0)
    assertTrue("Total tax withheld USD must be positive", globalSummary.totalTaxWithheldUsd > 0.0)
    assertTrue("Total net pay USD must be positive", globalSummary.totalNetPayUsd > 0.0)
    assertTrue("Jurisdiction count must match aggregated countries", globalSummary.jurisdictionCount >= 3)
  }

  @Test
  fun `test regulatory compliance notification system for payroll and cross-border thresholds`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    val teamDao = database.teamMemberDao()
    val fxDao = database.fxBalanceDao()
    val txDao = database.treasuryTransactionDao()

    val members = teamDao.getAllTeamMembers().first()
    val balances = fxDao.getAllBalances().first()
    val transactions = txDao.getAllTransactions().first()

    val payrollSummary = com.example.worldbusiness.data.repository.PayrollTaxEngine.calculateGlobalPayrollSummary(
      members = members,
      fxBalances = balances
    )

    // 1. Evaluate Regulatory Thresholds
    val alerts = com.example.worldbusiness.data.repository.RegulatoryThresholdEngine.evaluateThresholds(
      teamMembers = members,
      balances = balances,
      transactions = transactions,
      payrollSummary = payrollSummary
    )

    assertTrue("Regulatory alerts must be generated for monitored obligations", alerts.isNotEmpty())

    // 2. Validate Payroll Reporting Threshold Alerts
    val payrollAlerts = alerts.filter { it.category == com.example.worldbusiness.data.model.ThresholdCategory.PAYROLL_EXPENSES }
    assertTrue("Must have regulatory payroll expense threshold alerts", payrollAlerts.isNotEmpty())
    
    // Check for US or UK or SG payroll alerts
    val usOrUkPayrollAlert = payrollAlerts.find { it.jurisdictionCode == "US" || it.jurisdictionCode == "GB" || it.jurisdictionCode == "SG" }
    assertNotNull("Must monitor major statutory payroll jurisdictions", usOrUkPayrollAlert)
    assertTrue("Utilization percent must be calculated", usOrUkPayrollAlert!!.utilizationPercent > 0.0)
    assertNotNull("Required statutory filing form must be specified", usOrUkPayrollAlert.requiredFilingForm)
    assertTrue("Required filing form must not be blank", usOrUkPayrollAlert.requiredFilingForm.isNotBlank())
    assertNotNull("Reporting deadline must be specified", usOrUkPayrollAlert.reportingDeadline)

    // 3. Validate Cross-Border Wire Transfer Threshold Alerts
    val wireAlerts = alerts.filter { it.category == com.example.worldbusiness.data.model.ThresholdCategory.CROSS_BORDER_TRANSFER }
    assertTrue("Must have cross-border transfer threshold alerts", wireAlerts.isNotEmpty())

    val wireAlert = wireAlerts.first()
    assertTrue("Wire threshold limit must be positive", wireAlert.thresholdLimit > 0.0)
    assertTrue("Wire current amount must be positive", wireAlert.currentAmount > 0.0)
    assertNotNull("Regulatory body must be identified", wireAlert.regulatoryBody)
    assertNotNull("Statutory reference citation must be provided", wireAlert.statutoryReference)

    // 4. Test Acknowledgment and Status Overrides
    val targetAlertId = alerts.first().id
    val overriddenAlerts = com.example.worldbusiness.data.repository.RegulatoryThresholdEngine.evaluateThresholds(
      teamMembers = members,
      balances = balances,
      transactions = transactions,
      payrollSummary = payrollSummary,
      statusOverrides = mapOf(targetAlertId to com.example.worldbusiness.data.model.ThresholdAlertStatus.ACKNOWLEDGED)
    )
    val acknowledged = overriddenAlerts.find { it.id == targetAlertId }
    assertNotNull(acknowledged)
    assertEquals(com.example.worldbusiness.data.model.ThresholdAlertStatus.ACKNOWLEDGED, acknowledged!!.status)

    // 5. Test Regulatory Threshold Statistics Computation
    val stats = com.example.worldbusiness.data.repository.RegulatoryThresholdEngine.computeStats(overriddenAlerts)
    assertEquals("Total alerts count must match alerts list size", overriddenAlerts.size, stats.totalAlertsCount)
    assertTrue("Must have tracked payroll alerts count", stats.payrollAlertsCount > 0)
    assertTrue("Must have tracked transfer alerts count", stats.transferAlertsCount > 0)
    assertEquals("Acknowledged count must reflect the updated status", 1, stats.acknowledgedCount)
  }

  @Test
  fun `test cross-border invoicing formatting with Room entities and regional tax calculation`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    val entityDao = database.entityDao()
    val fxDao = database.fxBalanceDao()
    val invoiceDao = database.invoiceDao()

    val entities = entityDao.getAllEntities().first()
    val balances = fxDao.getAllBalances().first()
    val invoices = invoiceDao.getAllInvoices().first()

    assertTrue("Room database must contain entities", entities.isNotEmpty())
    assertTrue("Room database must contain fxBalances", balances.isNotEmpty())
    assertTrue("Room database must contain seeded invoices", invoices.isNotEmpty())

    // 1. Test Formatted Invoice Generation for German Client (Reverse Charge EU Directive 2006/112/EC)
    val testInvoice = invoices.first()
    val formattedInvoice = com.example.worldbusiness.data.repository.FormattedInvoiceEngine.buildFormattedInvoice(
      invoice = testInvoice,
      entities = entities,
      fxBalances = balances
    )

    assertNotNull("Formatted cross-border invoice must be generated", formattedInvoice)
    assertEquals(testInvoice.invoiceNumber, formattedInvoice.invoiceNumber)
    assertNotNull("Issuing corporate entity must be populated from Room", formattedInvoice.issuingEntity)
    assertTrue("Customs export certification must be present", formattedInvoice.customsCertification.isNotBlank())
    assertTrue("Bank institution clearing rail must be resolved", formattedInvoice.clearingRail.isNotBlank())
    assertTrue("SWIFT/BIC must be populated", formattedInvoice.swiftBic.isNotBlank())

    // 2. Test Regional Tax Calculation: Reverse Charge for EU B2B
    val germanTax = com.example.worldbusiness.data.repository.CrossBorderTaxCalculator.calculate(
      subtotal = 100000.0,
      currencyCode = "EUR",
      clientCountry = "Germany",
      isReverseChargeApplied = true
    )
    assertEquals("VAT rate on reverse charge should be 0%", 0.0, germanTax.vatRatePercent, 0.001)
    assertEquals("VAT amount on reverse charge should be 0", 0.0, germanTax.vatAmount, 0.001)
    assertEquals(100000.0, germanTax.grossTotal, 0.001)
    assertTrue("Reverse charge flag must be true", germanTax.isReverseCharge)
    assertTrue("Tax citation must reference EU Directive or UStG", germanTax.complianceNote.contains("2006/112/EC") || germanTax.complianceNote.contains("EU") || germanTax.complianceNote.contains("USt"))

    // 3. Test Regional Tax Calculation: Domestic / Non-Reverse Charge (e.g. UK VAT 20%)
    val ukStandardTax = com.example.worldbusiness.data.repository.CrossBorderTaxCalculator.calculate(
      subtotal = 50000.0,
      currencyCode = "GBP",
      clientCountry = "United Kingdom",
      isReverseChargeApplied = false
    )
    assertEquals(20.0, ukStandardTax.vatRatePercent, 0.001)
    assertEquals(10000.0, ukStandardTax.vatAmount, 0.001)
    assertEquals(60000.0, ukStandardTax.grossTotal, 0.001)

    // 4. Test Regional Tax Calculation: Switzerland MWST 8.1%
    val swissTax = com.example.worldbusiness.data.repository.CrossBorderTaxCalculator.calculate(
      subtotal = 80000.0,
      currencyCode = "CHF",
      clientCountry = "Switzerland",
      isReverseChargeApplied = false
    )
    assertEquals(8.1, swissTax.vatRatePercent, 0.001)
    assertEquals(6480.0, swissTax.vatAmount, 0.001)
    assertEquals(86480.0, swissTax.grossTotal, 0.001)

    // 5. Test Custom Line Items formatted with HSN/SAC codes
    val customItems = listOf(
      com.example.worldbusiness.data.model.CrossBorderInvoiceItem("Global Cloud Infra Architecture", 2.0, 30000.0, "998313", 0.0),
      com.example.worldbusiness.data.model.CrossBorderInvoiceItem("SWIFT ISO 20022 Multi-Rail Integration", 1.0, 40000.0, "998313", 0.0)
    )
    val customDoc = com.example.worldbusiness.data.repository.FormattedInvoiceEngine.buildFormattedInvoice(
      invoice = testInvoice.copy(amount = 100000.0),
      entities = entities,
      fxBalances = balances,
      customLineItems = customItems
    )
    assertEquals(2, customDoc.lineItems.size)
    assertEquals(100000.0, customDoc.subtotal, 0.001)
  }

  @Test
  fun `test multi-currency conversion helper for treasury valuation, swaps, and cross-border invoicing`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    val fxDao = database.fxBalanceDao()
    val balances = fxDao.getAllBalances().first()
    assertTrue("Room database must provide fx balances for multi-currency calculations", balances.isNotEmpty())

    val liveRates = mapOf(
      "USD" to 1.0,
      "EUR" to 1.10,
      "GBP" to 1.30,
      "CHF" to 1.15,
      "SGD" to 0.75,
      "JPY" to 0.0067
    )

    // 1. Core Real-Time Multi-Currency Conversion
    val conversion = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.convert(
      amount = 10000.0,
      fromCurrency = "USD",
      toCurrency = "EUR",
      liveRatesToUsd = liveRates,
      spreadBps = 4.0
    )
    // 1 USD = (1.0 / 1.10) EUR = 0.90909 EUR => 10000 USD ≈ 9090.91 EUR
    assertEquals("USD", conversion.sourceCurrency)
    assertEquals("EUR", conversion.targetCurrency)
    assertTrue("Converted EUR amount must be between 9080 and 9100", conversion.convertedAmount in 9080.0..9100.0)
    assertTrue("Mid market rate must be positive", conversion.midMarketRate > 0.0)
    assertTrue("Bid rate must be less than Ask rate", conversion.bidRate < conversion.askRate)
    assertTrue("Spread cost must be positive", conversion.spreadCostAmount > 0.0)
    assertTrue("Formatted text must contain EUR symbol", conversion.roundedFormattedText.contains("€") || conversion.roundedFormattedText.contains("EUR"))

    // Test JPY zero-decimal rounding
    val jpyConversion = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.convert(
      amount = 100.0,
      fromCurrency = "USD",
      toCurrency = "JPY",
      liveRatesToUsd = liveRates
    )
    // 100 USD @ ~149 JPY = 14925 JPY (no decimal fraction)
    assertEquals(jpyConversion.convertedAmount, Math.floor(jpyConversion.convertedAmount), 0.0001)

    // 2. Treasury Consolidated Multi-Vault Valuation
    val treasuryValuation = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateTreasuryValuation(
      balances = balances,
      baseCurrency = "USD",
      liveRatesToUsd = liveRates
    )
    assertEquals("USD", treasuryValuation.baseCurrency)
    assertEquals("$", treasuryValuation.baseCurrencySymbol)
    assertTrue("Total valuation must exceed 1,000,000 USD", treasuryValuation.totalValuationInBase > 1_000_000.0)
    assertEquals(balances.size, treasuryValuation.vaultCount)
    assertTrue("Must have vault breakdowns", treasuryValuation.vaultBreakdowns.isNotEmpty())
    assertNotNull("Top currency exposure must be determined", treasuryValuation.topCurrencyExposure)
    assertTrue("Top exposure percentage must be positive", treasuryValuation.topExposurePercent > 0.0)

    // 3. Treasury Swap Execution Quote with Wholesale Spread
    val swapQuote = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateSwapExecutionQuote(
      fromCurrency = "USD",
      toCurrency = "EUR",
      amount = 500000.0,
      liveRatesToUsd = liveRates,
      isInstitutionalWholesale = true
    )
    assertEquals("USD", swapQuote.fromCurrency)
    assertEquals("EUR", swapQuote.toCurrency)
    assertTrue("Execution rate must be lower than mid-rate due to spread", swapQuote.executionRate < swapQuote.marketMidRate)
    assertTrue("To amount received must be positive", swapQuote.toAmountReceived > 0.0)
    assertTrue("Clearing rail must be SEPA Instant or Target2", swapQuote.clearingRail.contains("SEPA") || swapQuote.clearingRail.contains("Target2"))

    // 4. Treasury Vault Portfolio Rebalancing
    val targetAllocations = mapOf(
      "USD" to 50.0,
      "EUR" to 25.0,
      "GBP" to 15.0,
      "CHF" to 10.0
    )
    val rebalanceOrders = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculatePortfolioRebalance(
      balances = balances,
      targetAllocationsPercent = targetAllocations,
      baseCurrency = "USD",
      liveRatesToUsd = liveRates
    )
    assertTrue("Rebalancing orders must be generated", rebalanceOrders.isNotEmpty())
    val usdOrder = rebalanceOrders.find { it.currencyCode == "USD" }
    assertNotNull(usdOrder)
    assertNotNull(usdOrder!!.action)

    // 5. Invoicing Multi-Currency Quotation with Real-Time Conversion & Hedging Volatility Buffer
    val invoicePricing = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateInvoiceMultiCurrencyPricing(
      invoiceNumber = "INV-TEST-001",
      subtotal = 100000.0,
      originalCurrency = "USD",
      settlementCurrency = "EUR",
      vatRatePercent = 19.0,
      paymentTermsDays = 30,
      liveRatesToUsd = liveRates,
      includeVolatilityBuffer = true
    )
    assertEquals("INV-TEST-001", invoicePricing.invoiceNumber)
    assertEquals(100000.0, invoicePricing.originalAmount, 0.01)
    assertEquals(19000.0, invoicePricing.originalVatAmount, 0.01)
    assertEquals(119000.0, invoicePricing.originalGrossTotal, 0.01)
    assertTrue("Settlement amount in EUR must be calculated", invoicePricing.settlementAmount > 0.0)
    assertTrue("Hedging buffer percent must be 1.5% for Net 30", invoicePricing.hedgingBufferPercent == 1.5)
    assertTrue("Hedging buffer amount must be positive", invoicePricing.hedgingBufferAmount > 0.0)
    assertTrue("Total with buffer must exceed settlement gross total", invoicePricing.totalWithHedgingBuffer > invoicePricing.settlementGrossTotal)
    assertTrue("Jurisdiction citation must reference EU Directive", invoicePricing.taxJurisdictionCitation.contains("2006/112/EC") || invoicePricing.taxJurisdictionCitation.contains("EU"))

    // 6. Dual-Currency Line Items Computation
    val lineItems = listOf(
      com.example.worldbusiness.data.model.CrossBorderInvoiceItem("Architecture SLA", 2.0, 25000.0, "998313", 0.0),
      com.example.worldbusiness.data.model.CrossBorderInvoiceItem("Clearing Engine Gateway", 1.0, 50000.0, "998313", 0.0)
    )
    val dualLineItems = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateDualCurrencyLineItems(
      items = lineItems,
      originalCurrency = "USD",
      settlementCurrency = "GBP",
      liveRatesToUsd = liveRates
    )
    assertEquals(2, dualLineItems.size)
    assertTrue("Original total must match", dualLineItems.sumOf { it.originalTotal } == 100000.0)
    assertTrue("Settlement total in GBP must be positive", dualLineItems.sumOf { it.settlementTotal } > 0.0)

    // 7. Settlement Realized FX Gain/Loss Analysis
    // Invoice issued at 1 EUR = 1.08 USD, settled at 1 EUR = 1.12 USD (EUR appreciated -> USD gain)
    val fxImpactGain = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateSettlementFxGainLoss(
      invoiceNumber = "INV-SETTLE-001",
      invoiceAmount = 100000.0,
      invoiceCurrency = "EUR",
      settlementCurrency = "USD",
      issuedRateToUsd = 1.08,
      settledRateToUsd = 1.12
    )
    assertTrue("Must be classified as FX Gain", fxImpactGain.isGain)
    assertEquals(4000.0, fxImpactGain.realizedGainLossAmountUsd, 0.01)
    assertTrue("Accounting entry must reference ASC 830 or FX Operating Reserve", fxImpactGain.accountingEntry.contains("ASC 830") || fxImpactGain.accountingEntry.contains("Gain"))

    // 8. Value-at-Risk (VaR) Estimation
    val var95 = com.example.worldbusiness.data.repository.MultiCurrencyConversionHelper.calculateTreasuryValueAtRisk(
      balances = balances,
      baseCurrency = "USD",
      liveRatesToUsd = liveRates
    )
    assertTrue("Treasury 95% 1-day VaR must be positive", var95 > 0.0)
  }

  @Test
  fun `test firebase enterprise firestore configuration and repository instantiation`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
      com.google.firebase.FirebaseApp.initializeApp(context)
    }
    val databaseId = context.getString(com.example.R.string.firestore_database_id)
    assertNotNull("Firestore database ID must be provisioned in firebase_applet_config.xml", databaseId)
    assertTrue("Firestore database ID must be non-empty", databaseId.isNotBlank())
    assertEquals("ai-studio-android-worldbus-b89fa850-f80a-4b2e-9598-338661fcdfca", databaseId)

    // Instantiate FirebaseSyncRepository with custom database ID
    val syncRepo = com.example.worldbusiness.data.remote.FirebaseSyncRepository(context)
    assertNotNull("FirebaseSyncRepository must be created", syncRepo)
    assertFalse("Default unauthenticated state before Google Sign-In", syncRepo.isAuthenticated)
  }

  @Test
  fun `test cross-border invoice data model and vector pdf generation engine`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

    // 1. Test Data Model Creation and Computations
    val sampleDoc = com.example.worldbusiness.data.model.CrossBorderInvoiceDocument.createSampleInvoice(
      invoiceNumber = "INV-2026-CH-TEST",
      currency = "CHF",
      currencySymbol = "CHF",
      subtotal = 100000.0,
      vatRate = 8.1
    )

    assertEquals("INV-2026-CH-TEST", sampleDoc.invoiceNumber)
    assertEquals("Apex Global Treasury AG", sampleDoc.exporter.entityName)
    assertEquals("Stellar Logistics GmbH", sampleDoc.client.clientName)
    assertEquals("CHF", sampleDoc.currency)
    assertEquals(100000.0, sampleDoc.subtotal, 0.001)
    assertEquals(8100.0, sampleDoc.taxAmount, 0.001)
    assertEquals(108100.0, sampleDoc.netReceivable, 0.001)
    assertTrue("Total quantity must be 2.0", sampleDoc.totalQuantity == 2.0)
    assertTrue("Formatted subtotal must contain CHF", sampleDoc.formattedSubtotal().contains("CHF"))
    assertTrue("Formatted net receivable must contain 108,100", sampleDoc.formattedNetReceivable().contains("108,100"))
    assertTrue("USD equivalent must contain USD", sampleDoc.formattedUsdEquivalent().contains("USD"))

    // 2. Test PDF Document Generation on Android
    val pdfResult = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine.generateAndSaveInvoicePdf(
      context = context,
      invoiceDoc = sampleDoc
    )

    assertNotNull("PDF result must not be null", pdfResult)
    assertTrue("PDF file must exist on storage", pdfResult.file.exists())
    assertTrue("PDF file size must be greater than 0 bytes", pdfResult.fileSizeBytes > 0)
    assertEquals(1, pdfResult.pageCount)
    assertEquals("INV-2026-CH-TEST", pdfResult.invoiceNumber)
    assertTrue("SHA-256 checksum must be 64 hex characters", pdfResult.sha256Checksum.length == 64)

    // 3. Test Listing Saved Invoices
    val savedList = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine.listGeneratedInvoices(context)
    assertTrue("Saved invoices archive must include the generated file", savedList.any { it.name == pdfResult.file.name })

    // 4. Test Intents
    val viewIntent = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine.createViewPdfIntent(context, pdfResult.file)
    assertEquals(android.content.Intent.ACTION_VIEW, viewIntent.action)
    assertEquals("application/pdf", viewIntent.type)

    val shareIntent = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine.createSharePdfIntent(context, pdfResult.file)
    assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
    assertEquals("application/pdf", shareIntent.type)
  }

  @Test
  fun `test financial analytics engine generates monthly revenue, tax liabilities, and invoice status trends`() {
    val sampleEntities = listOf(
      com.example.worldbusiness.data.model.EntityRecord(
        id = 1L,
        name = "Apex Global Corp (US)",
        jurisdiction = "United States",
        countryCode = "US",
        entityType = "Delaware C-Corp",
        taxId = "US-EIN-9923841",
        status = "ACTIVE",
        baseCurrency = "USD",
        operatingCapital = 10000000.0,
        annualFilingDeadline = "2026-10-15",
        localDirector = "Sarah Jenkins",
        complianceScore = 98
      ),
      com.example.worldbusiness.data.model.EntityRecord(
        id = 2L,
        name = "Apex Europe B.V.",
        jurisdiction = "Netherlands",
        countryCode = "NL",
        entityType = "Besloten Vennootschap",
        taxId = "NL-VAT-882710",
        status = "ACTIVE",
        baseCurrency = "EUR",
        operatingCapital = 5000000.0,
        annualFilingDeadline = "2026-11-30",
        localDirector = "Maarten Van Der Berg",
        complianceScore = 95
      )
    )

    val sampleFxBalances = listOf(
      com.example.worldbusiness.data.model.FxBalanceRecord(
        currencyCode = "USD",
        currencyName = "US Dollar",
        symbol = "$",
        balance = 5000000.0,
        rateToUsd = 1.0,
        dailyChangePercent = 0.0
      ),
      com.example.worldbusiness.data.model.FxBalanceRecord(
        currencyCode = "EUR",
        currencyName = "Euro",
        symbol = "€",
        balance = 3000000.0,
        rateToUsd = 1.09,
        dailyChangePercent = 0.25
      ),
      com.example.worldbusiness.data.model.FxBalanceRecord(
        currencyCode = "GBP",
        currencyName = "British Pound",
        symbol = "£",
        balance = 2000000.0,
        rateToUsd = 1.28,
        dailyChangePercent = -0.15
      )
    )

    val sampleInvoices = listOf(
      com.example.worldbusiness.data.model.InvoiceRecord(
        id = 1L,
        invoiceNumber = "INV-2026-001",
        issuingEntityName = "Apex Global Corp (US)",
        clientName = "Stripe Inc",
        clientCountry = "United States",
        issueDate = "2026-09-01",
        dueDate = "2026-10-31",
        amount = 500000.0,
        currency = "USD",
        taxRatePercent = 0.0,
        status = "PAID",
        serviceDescription = "Q3 Enterprise API Infrastructure Licensing"
      ),
      com.example.worldbusiness.data.model.InvoiceRecord(
        id = 2L,
        invoiceNumber = "INV-2026-002",
        issuingEntityName = "Apex Europe B.V.",
        clientName = "Siemens AG",
        clientCountry = "Germany",
        issueDate = "2026-09-15",
        dueDate = "2026-11-15",
        amount = 750000.0,
        currency = "EUR",
        taxRatePercent = 19.0,
        status = "IN_CLEARING",
        serviceDescription = "Industrial SaaS Deployment Phase 2"
      ),
      com.example.worldbusiness.data.model.InvoiceRecord(
        id = 3L,
        invoiceNumber = "INV-2026-003",
        issuingEntityName = "Apex Global Corp (US)",
        clientName = "Barclays PLC",
        clientCountry = "United Kingdom",
        issueDate = "2026-09-20",
        dueDate = "2026-11-30",
        amount = 350000.0,
        currency = "GBP",
        taxRatePercent = 20.0,
        status = "ISSUED",
        serviceDescription = "Treasury Management Consulting Services"
      )
    )

    // 1. Test 6-Month Range Analytics
    val analytics6M = com.example.worldbusiness.data.repository.FinancialAnalyticsEngine.generateAnalytics(
      invoices = sampleInvoices,
      entities = sampleEntities,
      fxBalances = sampleFxBalances,
      timeRange = com.example.worldbusiness.data.model.FinancialTimeRange.RANGE_6M
    )

    assertEquals(6, analytics6M.revenueSeries.size)
    assertEquals(6, analytics6M.taxSeries.size)
    assertEquals(6, analytics6M.invoiceSeries.size)
    assertTrue("Total gross revenue must be positive", analytics6M.totalGrossRevenue > 0)
    assertTrue("Total tax liabilities must be positive", analytics6M.totalTaxLiabilities > 0)
    assertTrue("Average DSO must be within reasonable bounds (20-60 days)", analytics6M.averageDsoDays in 20..60)
    assertTrue("Annualized run rate must exceed 6M revenue", analytics6M.currentRunRateAnnualized > analytics6M.totalGrossRevenue)

    // Check revenue series calculations
    val firstMonthRev = analytics6M.revenueSeries.first()
    assertTrue("Operating expenses must be positive", firstMonthRev.operatingExpenses > 0)
    assertEquals(firstMonthRev.grossRevenue - firstMonthRev.operatingExpenses, firstMonthRev.netRevenue, 0.01)
    assertTrue("Profit margin must be positive", firstMonthRev.profitMarginPercent > 0)

    // Check tax series calculations
    val firstMonthTax = analytics6M.taxSeries.first()
    assertTrue("VAT/GST must be positive", firstMonthTax.vatGstAmount > 0)
    assertTrue("WHT must be positive", firstMonthTax.withholdingTaxAmount > 0)
    assertTrue("CIT must be positive", firstMonthTax.corporateTaxAmount > 0)
    assertEquals(
      firstMonthTax.vatGstAmount + firstMonthTax.withholdingTaxAmount + firstMonthTax.corporateTaxAmount,
      firstMonthTax.totalLiability,
      0.01
    )

    // Check invoice status trends
    val lastMonthInv = analytics6M.invoiceSeries.last()
    assertTrue("Paid volume must be positive", lastMonthInv.paidVolume > 0)
    assertTrue("In clearing volume must be positive", lastMonthInv.inClearingVolume > 0)
    assertTrue("Collection efficiency must be between 0 and 100", lastMonthInv.collectionEfficiencyPercent in 0.0..100.0)
    assertTrue("DSO days must be positive", lastMonthInv.dsoDays > 0)

    // 2. Test 12-Month & YTD Ranges
    val analytics12M = com.example.worldbusiness.data.repository.FinancialAnalyticsEngine.generateAnalytics(
      invoices = sampleInvoices,
      entities = sampleEntities,
      fxBalances = sampleFxBalances,
      timeRange = com.example.worldbusiness.data.model.FinancialTimeRange.RANGE_12M
    )
    assertEquals(12, analytics12M.revenueSeries.size)
    assertEquals(12, analytics12M.taxSeries.size)
    assertEquals(12, analytics12M.invoiceSeries.size)

    val analyticsYtd = com.example.worldbusiness.data.repository.FinancialAnalyticsEngine.generateAnalytics(
      invoices = sampleInvoices,
      entities = sampleEntities,
      fxBalances = sampleFxBalances,
      timeRange = com.example.worldbusiness.data.model.FinancialTimeRange.RANGE_YTD
    )
    assertEquals(10, analyticsYtd.revenueSeries.size)

    // 3. Test Formatted Currency Outputs
    assertTrue("Formatted revenue must start with $", analytics6M.formatRevenue().startsWith("$"))
    assertTrue("Formatted tax must start with $", analytics6M.formatTax().startsWith("$"))
    assertTrue("Formatted clearing must start with $", analytics6M.formatInClearing().startsWith("$"))
  }

  @Test
  fun `test logistics dashboard Room persistence, real-time status updates, and estimated delivery date calculations`() = runBlocking {
    val engine = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine
    val hubs = engine.GLOBAL_TRADE_HUBS

    val shaHub = hubs.first { it.code == "SHA" }
    val rtmHub = hubs.first { it.code == "RTM" }
    val laxHub = hubs.first { it.code == "LAX" }
    val fraHub = hubs.first { it.code == "FRA" }

    // 1. Test Great-Circle Distance Calculation
    val distanceShaToRtm = engine.calculateHaversineDistanceKm(
      shaHub.latitude, shaHub.longitude,
      rtmHub.latitude, rtmHub.longitude
    )
    assertTrue("Distance between Shanghai and Rotterdam should be between 8,500 and 10,500 km", distanceShaToRtm in 8500.0..10500.0)

    // 2. Test Estimated Delivery Date Calculations across Freight Modes
    // Ocean Container SHA -> RTM
    val oceanEstimate = engine.calculateDeliveryEstimate(
      originHub = shaHub,
      destinationHub = rtmHub,
      freightMode = com.example.worldbusiness.data.model.LogisticsFreightMode.OCEAN_CONTAINER,
      clearanceCategory = com.example.worldbusiness.data.model.CustomsClearanceCategory.STANDARD_GENERAL
    )
    assertTrue("Ocean container transit days should be between 14 and 35 days", oceanEstimate.totalTransitDays in 14..35)
    assertTrue("Ocean container nautical miles should be positive", oceanEstimate.nauticalMiles > 4000.0)
    assertTrue("Delivery confidence should be high", oceanEstimate.deliveryConfidencePercent >= 85)
    assertTrue("Calculated arrival date string should not be blank", oceanEstimate.calculatedEstimatedDeliveryDate.isNotBlank())
    assertTrue("Explanation must mention hubs", oceanEstimate.formulaExplanation.contains("Shanghai") && oceanEstimate.formulaExplanation.contains("Rotterdam"))

    // Air Cargo SHA -> FRA
    val airEstimate = engine.calculateDeliveryEstimate(
      originHub = shaHub,
      destinationHub = fraHub,
      freightMode = com.example.worldbusiness.data.model.LogisticsFreightMode.AIR_CARGO,
      clearanceCategory = com.example.worldbusiness.data.model.CustomsClearanceCategory.HIGH_TECH_DUAL_USE
    )
    assertTrue("Air cargo total transit days should be between 2 and 6 days", airEstimate.totalTransitDays in 2..6)
    assertTrue("Air cargo cruising hours should be under 24 hours", airEstimate.baseTransitHours < 24.0)

    // Express Courier LAX -> FRA
    val courierEstimate = engine.calculateDeliveryEstimate(
      originHub = laxHub,
      destinationHub = fraHub,
      freightMode = com.example.worldbusiness.data.model.LogisticsFreightMode.EXPRESS_COURIER
    )
    assertTrue("Express courier total transit should be under 5 days", courierEstimate.totalTransitDays <= 5)
    assertEquals(98, courierEstimate.deliveryConfidencePercent)

    // 3. Test Dynamic ETA Recalibration based on Status
    val baseShipment = com.example.worldbusiness.data.model.ShipmentRecord(
      id = 99L,
      trackingCode = "WBOS-TEST-9901",
      origin = "Shanghai (SHA), China",
      destination = "Rotterdam (RTM), Netherlands",
      carrier = "Maersk Line",
      incoterm = "CIF",
      cargoDescription = "Precision Robotic Actuators",
      cargoValue = 450000.0,
      currency = "USD",
      customsStatus = "IN_TRANSIT",
      estimatedArrival = "Oct 22, 2026"
    )

    // When held for port inspection, ETA must be recalibrated with inspection buffer
    val inspectionEta = engine.recalibrateShipmentEta(baseShipment, "PORT_INSPECTION")
    assertTrue("Inspection ETA must not be blank", inspectionEta.isNotBlank())

    // When cleared, ETA must be expedited for final last-mile delivery
    val clearedEta = engine.recalibrateShipmentEta(baseShipment, "CLEARED")
    assertTrue("Cleared ETA must not be blank", clearedEta.isNotBlank())

    // 4. Test Room Database Persistence via Repository
    val newShipment = com.example.worldbusiness.data.model.ShipmentRecord(
      trackingCode = "WBOS-ROOM-8821",
      origin = "Port of Singapore (SIN)",
      destination = "Port Newark (NYC), USA",
      carrier = "CMA CGM",
      incoterm = "DDP",
      cargoDescription = "High-Density Server Motherboards",
      cargoValue = 1250000.0,
      currency = "USD",
      customsStatus = "IN_TRANSIT",
      estimatedArrival = oceanEstimate.calculatedEstimatedDeliveryDate
    )

    val insertedId = repository.insertShipment(newShipment)
    assertTrue("Inserted shipment ID must be positive", insertedId > 0)

    val shipmentList = repository.allShipments.first()
    val foundShipment = shipmentList.find { it.trackingCode == "WBOS-ROOM-8821" }
    assertNotNull("Shipment must be retrievable from Room database", foundShipment)
    assertEquals("IN_TRANSIT", foundShipment!!.customsStatus)
    assertEquals("CMA CGM", foundShipment.carrier)

    // Test Real-Time Status Update in Room Database
    repository.updateShipmentStatus(insertedId, "PORT_INSPECTION", "Oct 28, 2026")
    val updatedList = repository.allShipments.first()
    val updatedShipment = updatedList.find { it.id == insertedId }
    assertNotNull(updatedShipment)
    assertEquals("PORT_INSPECTION", updatedShipment!!.customsStatus)
    assertEquals("Oct 28, 2026", updatedShipment.estimatedArrival)

    // Test Audit Log Generation for the Shipment Status Transition
    val auditLogs = repository.allAuditLogs.first()
    val statusAudit = auditLogs.find { it.module == com.example.worldbusiness.data.model.AuditLogModule.LOGISTICS.name }
    assertNotNull("Logistics audit log must be recorded", statusAudit)
  }

  @Test
  fun `test enterprise ledger compliance snapshot export and zero-trust integrity verification`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val entities = repository.allEntities.first()
    val invoices = repository.allInvoices.first()
    val balances = repository.allBalances.first()
    val shipments = repository.allShipments.first()
    val auditLogs = repository.allAuditLogs.first()

    val snapshot = com.example.worldbusiness.data.repository.DatabaseComplianceExportEngine.generateSnapshot(
      entities = entities,
      invoices = invoices,
      balances = balances,
      shipments = shipments,
      auditLogs = auditLogs
    )

    assertNotNull("Snapshot must not be null", snapshot)
    assertTrue("Total records in snapshot must be positive", snapshot.totalRecordsCount > 0)
    assertTrue("Manifest checksum must be 64-char SHA-256", snapshot.sha256ManifestChecksum.length == 64)
    assertTrue("Raw JSON must contain system metrics", snapshot.rawJsonPayload.contains("systemMetrics"))
    assertTrue("Raw JSON must contain entities", snapshot.rawJsonPayload.contains("entities"))
    assertTrue("Raw JSON must contain vaults", snapshot.rawJsonPayload.contains("vaults"))
    assertTrue("Chain integrity must be verified as valid", snapshot.chainIntegrityValid)
    assertNotNull("Root hash must not be null", snapshot.rootHash)
    assertNotNull("Head hash must not be null", snapshot.latestHash)
  }

  @Test
  fun `test executive logistics corridor stream and ETA recalibration engine`() = runBlocking {
    repository.seedInitialDataIfNeeded()

    // 1. Validate Geodesic & Modal Calculation for Prime Trade Corridor
    val shanghaiHub = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine.findHubByKeyword("Shanghai")
    val rotterdamHub = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine.findHubByKeyword("Rotterdam")

    val oceanEstimate = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine.calculateDeliveryEstimate(
      originHub = shanghaiHub,
      destinationHub = rotterdamHub,
      freightMode = com.example.worldbusiness.data.model.LogisticsFreightMode.OCEAN_CONTAINER
    )

    assertTrue("Great circle distance must be positive", oceanEstimate.geodesicDistanceKm > 8000.0)
    assertTrue("Total transit days for ocean must be realistic", oceanEstimate.totalTransitDays >= 15)
    assertTrue("Delivery confidence must be high", oceanEstimate.deliveryConfidencePercent >= 85)
    assertNotNull("Calculated ETA must be generated", oceanEstimate.calculatedEstimatedDeliveryDate)

    // 2. Validate Air Cargo Mode Speed Profile
    val airEstimate = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine.calculateDeliveryEstimate(
      originHub = shanghaiHub,
      destinationHub = rotterdamHub,
      freightMode = com.example.worldbusiness.data.model.LogisticsFreightMode.AIR_CARGO
    )
    assertTrue("Air transit days must be faster than ocean", airEstimate.totalTransitDays < oceanEstimate.totalTransitDays)

    // 3. Test Room Database Real-Time Recalibration
    val shipments = repository.allShipments.first()
    assertTrue("Seed shipments must exist in Room", shipments.isNotEmpty())
    val targetShipment = shipments.first()

    val revisedEta = com.example.worldbusiness.data.repository.LogisticsDeliveryCalculatorEngine.recalibrateShipmentEta(
      currentRecord = targetShipment,
      newStatus = "PORT_INSPECTION"
    )
    assertNotNull("Revised ETA must not be null", revisedEta)

    repository.updateShipmentStatus(targetShipment.id, "PORT_INSPECTION", revisedEta)
    val updatedShipments = repository.allShipments.first()
    val updated = updatedShipments.find { it.id == targetShipment.id }

    assertNotNull("Updated shipment must be found in Room", updated)
    assertEquals("PORT_INSPECTION", updated!!.customsStatus)
    assertEquals(revisedEta, updated.estimatedArrival)
  }

  @Test
  fun `test UI navigation tab routing and state flow transitions`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val viewModel = com.example.worldbusiness.ui.WorldBusinessViewModel(repository)

    // Verify initial tab is COCKPIT
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.COCKPIT, viewModel.currentTab.value)

    // Test transition to ENTITIES tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.ENTITIES)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.ENTITIES, viewModel.currentTab.value)

    // Test transition to TREASURY tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.TREASURY)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.TREASURY, viewModel.currentTab.value)

    // Test transition to COMMERCIAL tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.COMMERCIAL)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.COMMERCIAL, viewModel.currentTab.value)

    // Test transition to WORKFORCE tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.WORKFORCE)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.WORKFORCE, viewModel.currentTab.value)

    // Test transition to LOGISTICS tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.LOGISTICS)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.LOGISTICS, viewModel.currentTab.value)

    // Test transition to AUDIT tab
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.AUDIT)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.AUDIT, viewModel.currentTab.value)

    // Test return to COCKPIT
    viewModel.selectTab(com.example.worldbusiness.ui.OSNavigationTab.COCKPIT)
    assertEquals(com.example.worldbusiness.ui.OSNavigationTab.COCKPIT, viewModel.currentTab.value)

    // Test Currency Calculator Modal open/close state
    assertEquals(false, viewModel.calculatorModalVisible.value)
    viewModel.openConversionCalculator()
    assertEquals(true, viewModel.calculatorModalVisible.value)
    viewModel.closeConversionCalculator()
    assertEquals(false, viewModel.calculatorModalVisible.value)
  }

  @Test
  fun `test cross border invoice PDF document generation and download export`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    val exporter = com.example.worldbusiness.data.model.CorporateExporterProfile(
        entityName = "Helvetia Global Asset Management AG",
        jurisdiction = "Zurich, Switzerland",
        physicalAddress = "Bahnhofstrasse 45, 8001 Zurich, Switzerland",
        countryCode = "CH",
        vatTaxId = "CHE-102.345.678 MWST",
        eoriCustomsNumber = "EORI-CH-91823",
        registrationNumber = "REG-101",
        legalRepresentative = "Dr. Beatrix von Haller",
        bankInstitution = "UBS Switzerland AG",
        swiftBic = "UBSWCHZH80A",
        ibanOrAccount = "CH93 0024 0240 1234 5678 9",
        primaryClearingRail = "SIC RTGS Rail"
    )

    val client = com.example.worldbusiness.data.model.CrossBorderClientProfile(
        clientName = "Siemens Energy AG",
        destinationCountry = "Germany",
        destinationCountryCode = "DE",
        billingAddress = "Otto-Hahn-Ring 6, 81739 Munich, Germany",
        shippingAddress = "Otto-Hahn-Ring 6, 81739 Munich, Germany",
        clientVatGstId = "DE 302 918 201",
        contactEmail = "invoicing@siemens-energy.com",
        contactPhone = "+49 89 636 00"
    )

    val items = listOf(
        com.example.worldbusiness.data.model.CrossBorderInvoiceItem(
            description = "Enterprise Cloud Architecture License",
            quantity = 1.0,
            unitPrice = 50000.0,
            hsnSacCode = "998313",
            taxRatePercent = 0.0
        )
    )

    val invoiceDoc = com.example.worldbusiness.data.model.CrossBorderInvoiceDocument(
        invoiceNumber = "INV-2026-TEST-9999",
        issueDate = "2026-10-10",
        dueDate = "2026-11-10",
        paymentTerms = "Net 30 Days",
        status = com.example.worldbusiness.data.model.CrossBorderInvoiceStatus.ISSUED,
        incoterm = com.example.worldbusiness.data.model.CrossBorderIncoterm.DAP,
        exporter = exporter,
        client = client,
        currency = "EUR",
        currencySymbol = "€",
        exchangeRateToUsd = 1.08,
        lineItems = items,
        subtotal = 50000.0,
        taxName = "0% Reverse Charge VAT",
        taxRatePercent = 0.0,
        taxAmount = 0.0,
        isReverseCharge = true,
        grossTotal = 50000.0,
        netReceivable = 50000.0,
        equivalentUsdAmount = 54000.0,
        statutoryComplianceNote = "Art. 196 EU VAT Directive"
    )

    // 1. Generate & Save PDF
    val pdfResult = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
        .generateAndSaveInvoicePdf(context, invoiceDoc)

    assertNotNull(pdfResult)
    assertTrue(pdfResult.file.exists())
    assertTrue(pdfResult.file.length() > 0)
    assertEquals("INV-2026-TEST-9999", pdfResult.invoiceNumber)
    assertTrue(pdfResult.sha256Checksum.isNotBlank())

    // 2. Test Download Export to public downloads directory
    val downloadedCopy = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
        .exportToDownloadsFolder(context, pdfResult.file)
    assertNotNull(downloadedCopy)
    assertTrue(downloadedCopy!!.exists())
    assertTrue(downloadedCopy.length() > 0)

    // 3. Test Intent creation for Viewing and Sharing PDF
    val viewIntent = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
        .createViewPdfIntent(context, pdfResult.file)
    assertEquals(android.content.Intent.ACTION_VIEW, viewIntent.action)
    assertEquals("application/pdf", viewIntent.type)

    val shareIntent = com.example.worldbusiness.data.repository.CrossBorderInvoicePdfEngine
        .createSharePdfIntent(context, pdfResult.file)
    assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
    assertEquals("application/pdf", shareIntent.type)
  }

  @Test
  fun `test Recharts invoice currency amounts aggregation over 30 days`() = runBlocking {
    repository.seedInitialDataIfNeeded()
    val invoices = repository.getAllInvoices().first()
    assertTrue(invoices.isNotEmpty())

    // Group invoices by currency
    val grouped = invoices.groupBy { it.currency }
    assertTrue(grouped.containsKey("USD"))
    assertTrue(grouped.containsKey("EUR"))

    val usdTotal = grouped["USD"]?.sumOf { it.amount } ?: 0.0
    val eurTotal = grouped["EUR"]?.sumOf { it.amount } ?: 0.0
    assertTrue(usdTotal > 0.0)
    assertTrue(eurTotal > 0.0)
  }
}
