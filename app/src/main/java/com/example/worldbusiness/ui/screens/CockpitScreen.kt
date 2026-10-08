package com.example.worldbusiness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.AuditLogSummaryStats
import com.example.worldbusiness.data.model.CashFlowHorizon
import com.example.worldbusiness.data.model.CashFlowScenario
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.MacroIndicator
import com.example.worldbusiness.data.model.PredictiveCashFlowReport
import com.example.worldbusiness.data.model.RegionalHub
import com.example.worldbusiness.ui.ExecutiveKpis
import com.example.worldbusiness.ui.OSNavigationTab
import com.example.worldbusiness.ui.components.AuditLogDashboardWidget
import com.example.worldbusiness.ui.components.FinancialAnalyticsChartDashboard
import com.example.worldbusiness.ui.components.Formatters
import com.example.worldbusiness.ui.components.InteractiveWorldMap
import com.example.worldbusiness.ui.components.JurisdictionTaxDeadlinesWidget
import com.example.worldbusiness.ui.components.PredictiveCashFlowWidget

@Composable
fun CockpitScreen(
    kpis: ExecutiveKpis,
    regionalHubs: List<RegionalHub>,
    selectedHub: RegionalHub?,
    macroIndicators: List<MacroIndicator>,
    entities: List<EntityRecord> = emptyList(),
    invoices: List<InvoiceRecord> = emptyList(),
    fxBalances: List<FxBalanceRecord> = emptyList(),
    auditLogs: List<AuditLogRecord> = emptyList(),
    auditSummaryStats: AuditLogSummaryStats = AuditLogSummaryStats(0, 0, 0, 0.0, 100.0, 0, true),
    predictiveReport: PredictiveCashFlowReport? = null,
    onSelectCashFlowHorizon: (CashFlowHorizon) -> Unit = {},
    onSelectCashFlowScenario: (CashFlowScenario) -> Unit = {},
    onExportCashForecastCsv: () -> String = { "" },
    onSelectHub: (RegionalHub?) -> Unit,
    onNavigateTab: (OSNavigationTab) -> Unit,
    onOpenCalculator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Consolidated Enterprise Treasury Hero Card
        item {
            ConsolidatedTreasuryCard(kpis = kpis, onNavigateToTreasury = { onNavigateTab(OSNavigationTab.TREASURY) })
        }

        // Recharts-Inspired Financial Analytics Visualizer: Monthly Revenue, Tax Liabilities & Invoice Status Trends
        item {
            FinancialAnalyticsChartDashboard(
                invoices = invoices,
                entities = entities,
                fxBalances = fxBalances,
                onNavigateToInvoices = { onNavigateTab(OSNavigationTab.COMMERCIAL) }
            )
        }

        // Predictive Cash Flow & Runway Analytics Dashboard Widget
        predictiveReport?.let { report ->
            item {
                PredictiveCashFlowWidget(
                    report = report,
                    onSelectHorizon = onSelectCashFlowHorizon,
                    onSelectScenario = onSelectCashFlowScenario,
                    onExportCsv = onExportCashForecastCsv,
                    onNavigateToInvoices = { onNavigateTab(OSNavigationTab.COMMERCIAL) }
                )
            }
        }

        // Interactive Global Radar
        item {
            InteractiveWorldMap(
                hubs = regionalHubs,
                selectedHub = selectedHub,
                onHubSelected = onSelectHub
            )
        }

        // Executive 4-Pillar Grid
        item {
            ExecutivePillarGrid(kpis = kpis, onNavigateTab = onNavigateTab)
        }

        // Upcoming Tax Filing Deadlines & Regulatory Reporting Requirements Widget
        item {
            JurisdictionTaxDeadlinesWidget(
                entities = entities,
                onNavigateToEntities = { onNavigateTab(OSNavigationTab.ENTITIES) }
            )
        }

        // Centralized Compliance Audit Log Module Widget
        item {
            AuditLogDashboardWidget(
                recentLogs = auditLogs,
                summaryStats = auditSummaryStats,
                onNavigateToAuditLog = { onNavigateTab(OSNavigationTab.AUDIT) }
            )
        }

        // Quick Global Operations Launcher
        item {
            QuickOperationsBar(
                onNavigateTab = onNavigateTab,
                onOpenCalculator = onOpenCalculator
            )
        }

        // Macro Regulatory & Central Bank Radar
        item {
            MacroStreamSection(macroIndicators = macroIndicators)
        }
    }
}

@Composable
private fun ConsolidatedTreasuryCard(
    kpis: ExecutiveKpis,
    onNavigateToTreasury: () -> Unit
) {
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
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onNavigateToTreasury() }
            .padding(18.dp)
            .testTag("consolidated_treasury_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPositive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONSOLIDATED GLOBAL TREASURY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EmeraldPositive.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Up",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "+2.48% 24H",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Formatters.formatUsd(kpis.consolidatedCashUsd),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Converted Base USD across 7 Sovereign Vaults (US, UK, EU, SG, CH, JP, BR)",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TreasurySubStat(label = "PENDING RECEIVABLES", value = Formatters.formatUsd(kpis.pendingInvoicesUsd), color = GoldAccent)
                TreasurySubStat(label = "MONTHLY PAYROLL", value = Formatters.formatUsd(kpis.monthlyPayrollRunUsd), color = CyanAccent)
                TreasurySubStat(label = "CARGO IN TRANSIT", value = Formatters.formatUsd(kpis.inTransitCargoUsd), color = EmeraldPositive)
            }
        }
    }
}

@Composable
private fun TreasurySubStat(label: String, value: String, color: Color) {
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
private fun ExecutivePillarGrid(
    kpis: ExecutiveKpis,
    onNavigateTab: (OSNavigationTab) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiPillarCard(
                title = "SUBSIDIARIES",
                value = "${kpis.activeEntitiesCount} Entities",
                subtitle = "Good Standing",
                icon = Icons.Default.AccountBalance,
                color = CyanAccent,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab(OSNavigationTab.ENTITIES) }
            )
            KpiPillarCard(
                title = "GLOBAL TEAM",
                value = "${kpis.globalTeamCount} Hires",
                subtitle = "5 Jurisdictions",
                icon = Icons.Default.Group,
                color = GoldAccent,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab(OSNavigationTab.WORKFORCE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiPillarCard(
                title = "SUPPLY LOGISTICS",
                value = Formatters.formatUsd(kpis.inTransitCargoUsd),
                subtitle = "Active Corridors",
                icon = Icons.Default.LocalShipping,
                color = EmeraldPositive,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab(OSNavigationTab.LOGISTICS) }
            )
            KpiPillarCard(
                title = "OECD COMPLIANCE",
                value = "${kpis.complianceHealthScore}%",
                subtitle = "Pillar Two Pass",
                icon = Icons.Default.FactCheck,
                color = VioletAccentColor,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab(OSNavigationTab.ENTITIES) }
            )
        }
    }
}

val VioletAccentColor = Color(0xFFA78BFA)

@Composable
private fun KpiPillarCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("kpi_card_${title.lowercase()}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun QuickOperationsBar(
    onNavigateTab: (OSNavigationTab) -> Unit,
    onOpenCalculator: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "EXECUTIVE QUICK DISPATCH",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                label = "FX CALC",
                icon = Icons.Default.CurrencyExchange,
                modifier = Modifier.width(82.dp),
                onClick = onOpenCalculator
            )
            QuickActionButton(
                label = "FX SWAP",
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.width(82.dp),
                onClick = { onNavigateTab(OSNavigationTab.TREASURY) }
            )
            QuickActionButton(
                label = "INVOICE",
                icon = Icons.Default.Receipt,
                modifier = Modifier.width(82.dp),
                onClick = { onNavigateTab(OSNavigationTab.COMMERCIAL) }
            )
            QuickActionButton(
                label = "PAYROLL",
                icon = Icons.Default.Payments,
                modifier = Modifier.width(82.dp),
                onClick = { onNavigateTab(OSNavigationTab.WORKFORCE) }
            )
            QuickActionButton(
                label = "FREIGHT",
                icon = Icons.Default.LocalShipping,
                modifier = Modifier.width(82.dp),
                onClick = { onNavigateTab(OSNavigationTab.LOGISTICS) }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MacroStreamSection(macroIndicators: List<MacroIndicator>) {
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
                text = "MACRO & REGULATORY RADAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "LIVE OECD/CENTRAL BANK",
                fontSize = 9.sp,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            macroIndicators.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = item.description,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = item.value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = item.change,
                            fontSize = 9.sp,
                            color = if (item.isPositive) EmeraldPositive else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
