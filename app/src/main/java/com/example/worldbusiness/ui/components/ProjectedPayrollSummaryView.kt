package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.worldbusiness.data.local.RoomPayrollCurrencyAggregation
import com.example.worldbusiness.data.local.RoomPayrollEmploymentTypeAggregation
import com.example.worldbusiness.data.local.RoomPayrollJurisdictionAggregation
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.ThresholdCategory
import java.util.Locale

/**
 * Summary view to display projected global payroll expenses for the current month,
 * aggregating multi-currency compensation, statutory tax withholdings, and employer obligations
 * directly from the Room database.
 */
@Composable
fun ProjectedPayrollSummaryView(
    summary: GlobalPayrollSummary,
    balances: List<FxBalanceRecord>,
    teamMembers: List<TeamMemberRecord>,
    roomCurrencies: List<RoomPayrollCurrencyAggregation> = emptyList(),
    roomJurisdictions: List<RoomPayrollJurisdictionAggregation> = emptyList(),
    roomEmploymentTypes: List<RoomPayrollEmploymentTypeAggregation> = emptyList(),
    thresholdAlerts: List<RegulatoryThresholdAlert> = emptyList(),
    onAcknowledgeThresholdAlert: (String) -> Unit = {},
    onFileThresholdReport: (String) -> Unit = {},
    onRunPayroll: () -> Unit = {},
    onNavigateToDisbursement: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("CURRENCIES") }

    // Rates map
    val rateMap = remember(balances) {
        balances.associate { it.currencyCode.uppercase(Locale.US) to it.rateToUsd }
    }

    // Estimate employer statutory contributions (approx 8.8% blended)
    val estimatedEmployerTaxUsd = summary.totalGrossUsd * 0.088
    val totalProjectedCorporateCostUsd = summary.totalGrossUsd + estimatedEmployerTaxUsd

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("projected_global_payroll_summary"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header: Projected Current Month Global Payroll Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("projected_payroll_hero_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(GoldAccent.copy(alpha = 0.6f), BorderSubtle))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Meta Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Projected Payroll",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PROJECTED GLOBAL PAYROLL EXPENSES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OCT 2026",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "Current Month Cycle • Aggregate Forecast from Room SQLite",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Room DB Badge
                    Box(
                        modifier = Modifier
                            .testTag("room_payroll_aggregation_badge")
                            .background(CyanAccent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Room Database",
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ROOM v5 DB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f))

                // Main Total Projected Amount
                Column {
                    Text(
                        text = "TOTAL PROJECTED CORPORATE COMMITMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${Formatters.formatCurrency(totalProjectedCorporateCostUsd)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("projected_payroll_total_usd")
                        )
                        Text(
                            text = "USD EQUIVALENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = "Includes gross worker compensation + statutory employer social taxes across ${summary.jurisdictionCount} jurisdictions",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // 3 Primary Metric KPI Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Projected Gross
                    PayrollKpiCard(
                        title = "PROJECTED GROSS",
                        value = "$${Formatters.formatCompactNumber(summary.totalGrossUsd)}",
                        sub = "${summary.totalEmployees} Team Members",
                        tint = TextPrimary,
                        tag = "projected_payroll_gross_kpi",
                        modifier = Modifier.weight(1f)
                    )

                    // Statutory Taxes & Withholding
                    PayrollKpiCard(
                        title = "TAXES & DEDUCTIONS",
                        value = "$${Formatters.formatCompactNumber(summary.totalTaxWithheldUsd)}",
                        sub = "${String.format(Locale.US, "%.1f", summary.overallEffectiveTaxRate)}% WHT Rate",
                        tint = RoseNegative,
                        tag = "projected_payroll_tax_kpi",
                        modifier = Modifier.weight(1f)
                    )

                    // Net Payout Disbursed
                    PayrollKpiCard(
                        title = "NET TAKE-HOME",
                        value = "$${Formatters.formatCompactNumber(summary.totalNetPayUsd)}",
                        sub = "Worker Bank Payout",
                        tint = EmeraldPositive,
                        tag = "projected_payroll_net_kpi",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRunPayroll,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_execute_projected_payroll")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Execute Run",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXECUTE PAYROLL RUN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToDisbursement,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        modifier = Modifier.testTag("btn_goto_bulk_disbursement")
                    ) {
                        Text(
                            text = "DISBURSEMENT ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Regulatory Compliance Notification: Payroll Expenses Approaching Local Reporting Thresholds
        val payrollAlerts = remember(thresholdAlerts) {
            thresholdAlerts.filter { it.category == ThresholdCategory.PAYROLL_EXPENSES }
        }

        if (payrollAlerts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payroll_threshold_alert_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                contentDescription = "Statutory Alert",
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "REGULATORY PAYROLL EXPENSE THRESHOLD NOTIFICATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "${payrollAlerts.size} Monitored",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                payrollAlerts.forEach { alert ->
                    RegulatoryThresholdAlertCard(
                        alert = alert,
                        onAcknowledge = onAcknowledgeThresholdAlert,
                        onFileReport = onFileThresholdReport
                    )
                }
            }
        }

        // Section Selector Tabs (Currencies, Jurisdictions, Employment Types)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "CURRENCIES" to "Currency Breakdown",
                "JURISDICTIONS" to "Statutory Jurisdictions",
                "EMPLOYMENT" to "Employment Types"
            ).forEach { (key, label) ->
                val isSelected = selectedSection == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent else SurfaceElevated)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { selectedSection = key }
                        .padding(vertical = 8.dp)
                        .testTag("tab_projected_$key"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Selected Section Content
        when (selectedSection) {
            "CURRENCIES" -> {
                ProjectedCurrencyBreakdownCard(
                    roomCurrencies = roomCurrencies,
                    teamMembers = teamMembers,
                    rateMap = rateMap,
                    totalGrossUsd = summary.totalGrossUsd
                )
            }
            "JURISDICTIONS" -> {
                ProjectedJurisdictionsCard(
                    jurisdictions = summary.jurisdictions,
                    roomJurisdictions = roomJurisdictions
                )
            }
            "EMPLOYMENT" -> {
                ProjectedEmploymentTypesCard(
                    teamMembers = teamMembers,
                    roomEmploymentTypes = roomEmploymentTypes,
                    rateMap = rateMap,
                    totalGrossUsd = summary.totalGrossUsd
                )
            }
        }
    }
}

@Composable
private fun PayrollKpiCard(
    title: String,
    value: String,
    sub: String,
    tint: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f)))
        ),
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.4.sp
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Aggregated Multi-Currency Projected Payroll Expenses Card
 */
@Composable
private fun ProjectedCurrencyBreakdownCard(
    roomCurrencies: List<RoomPayrollCurrencyAggregation>,
    teamMembers: List<TeamMemberRecord>,
    rateMap: Map<String, Double>,
    totalGrossUsd: Double
) {
    // If roomCurrencies is populated from Room DAO, use it, otherwise derive from teamMembers
    val currenciesData = remember(roomCurrencies, teamMembers, rateMap) {
        if (roomCurrencies.isNotEmpty()) {
            roomCurrencies.map { agg ->
                val rate = rateMap[agg.currency.uppercase(Locale.US)] ?: 1.0
                val usdVal = agg.totalLocalGross * rate
                val pct = if (totalGrossUsd > 0.0) (usdVal / totalGrossUsd) * 100.0 else 0.0
                CurrencyPayrollItem(
                    currency = agg.currency,
                    headcount = agg.memberCount,
                    totalLocal = agg.totalLocalGross,
                    rateToUsd = rate,
                    usdEquivalent = usdVal,
                    sharePercent = pct
                )
            }.sortedByDescending { it.usdEquivalent }
        } else {
            teamMembers.groupBy { it.currency }.map { (curr, members) ->
                val localSum = members.sumOf { it.monthlyCompensation }
                val rate = rateMap[curr.uppercase(Locale.US)] ?: 1.0
                val usdVal = localSum * rate
                val pct = if (totalGrossUsd > 0.0) (usdVal / totalGrossUsd) * 100.0 else 0.0
                CurrencyPayrollItem(
                    currency = curr,
                    headcount = members.size,
                    totalLocal = localSum,
                    rateToUsd = rate,
                    usdEquivalent = usdVal,
                    sharePercent = pct
                )
            }.sortedByDescending { it.usdEquivalent }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_payroll_currency_aggregations"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Currencies",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ROOM DATABASE: MULTI-CURRENCY PAYROLL BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${currenciesData.size} Currencies",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Aggregated monthly salary liabilities grouped by native worker payroll currency and converted to USD.",
                fontSize = 10.sp,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            currenciesData.forEach { item ->
                val currColor = Formatters.getCurrencyColor(item.currency)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(currColor.copy(alpha = 0.4f), BorderSubtle))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(currColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, currColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.currency,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = currColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${item.headcount} Team Member${if (item.headcount > 1) "s" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "1 ${item.currency} = $${String.format(Locale.US, "%.4f", item.rateToUsd)} USD",
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${Formatters.formatCurrency(item.usdEquivalent)} USD",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${Formatters.formatCurrency(item.totalLocal)} ${item.currency}",
                                    fontSize = 10.sp,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Progress Bar representing share of total payroll
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SurfaceDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (item.sharePercent / 100.0).toFloat().coerceIn(0.01f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(currColor)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", item.sharePercent)}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Statutory Jurisdictions Aggregation Card
 */
@Composable
private fun ProjectedJurisdictionsCard(
    jurisdictions: List<com.example.worldbusiness.data.model.JurisdictionSummary>,
    roomJurisdictions: List<RoomPayrollJurisdictionAggregation>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_payroll_jurisdiction_aggregations"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Jurisdictions",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ROOM DATABASE: STATUTORY JURISDICTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${jurisdictions.size} Jurisdictions",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Statutory withholding obligations aggregated across sovereign tax agencies (IRS, HMRC, CPF, etc.)",
                fontSize = 10.sp,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            jurisdictions.forEach { jur ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = jur.countryCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text(
                                    text = jur.countryName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${jur.employeeCount} Staff • ${jur.currency} • ${String.format(Locale.US, "%.1f", jur.averageEffectiveTaxRate)}% Avg WHT",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${Formatters.formatCompactNumber(jur.totalGrossUsd)} Gross",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "-$${Formatters.formatCompactNumber(jur.totalTaxWithheldUsd)} Tax",
                                fontSize = 10.sp,
                                color = RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Employment Types Aggregation Card
 */
@Composable
private fun ProjectedEmploymentTypesCard(
    teamMembers: List<TeamMemberRecord>,
    roomEmploymentTypes: List<RoomPayrollEmploymentTypeAggregation>,
    rateMap: Map<String, Double>,
    totalGrossUsd: Double
) {
    val typesData = remember(roomEmploymentTypes, teamMembers, rateMap) {
        if (roomEmploymentTypes.isNotEmpty()) {
            roomEmploymentTypes.map { agg ->
                val usdVal = agg.totalLocalGross // If normalized in query or estimate
                val pct = if (totalGrossUsd > 0.0) (usdVal / totalGrossUsd) * 100.0 else 0.0
                EmploymentModelItem(agg.employmentType, agg.memberCount, usdVal, pct)
            }
        } else {
            teamMembers.groupBy { it.employmentType }.map { (type, members) ->
                val usdVal = members.sumOf { m ->
                    val rate = rateMap[m.currency.uppercase(Locale.US)] ?: 1.0
                    m.monthlyCompensation * rate
                }
                val pct = if (totalGrossUsd > 0.0) (usdVal / totalGrossUsd) * 100.0 else 0.0
                EmploymentModelItem(type, members.size, usdVal, pct)
            }.sortedByDescending { it.totalUsd }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_payroll_employment_aggregations"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Employment Models",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ROOM DATABASE: EMPLOYMENT MODEL DISTRIBUTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "Breakdown of monthly payroll allocation across Direct Subsidiaries, EOR entities, and B2B Contractors.",
                fontSize = 10.sp,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            typesData.forEach { model ->
                val badgeColor = when {
                    model.type.contains("Subsidiary", ignoreCase = true) -> CyanAccent
                    model.type.contains("EOR", ignoreCase = true) -> GoldAccent
                    else -> EmeraldPositive
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(badgeColor.copy(alpha = 0.35f), BorderSubtle))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = model.type,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${model.count} Staff",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", model.sharePercent)}% of global monthly payroll run",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "$${Formatters.formatCurrency(model.totalUsd)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private data class CurrencyPayrollItem(
    val currency: String,
    val headcount: Int,
    val totalLocal: Double,
    val rateToUsd: Double,
    val usdEquivalent: Double,
    val sharePercent: Double
)

private data class EmploymentModelItem(
    val type: String,
    val count: Int,
    val totalUsd: Double,
    val sharePercent: Double
)
