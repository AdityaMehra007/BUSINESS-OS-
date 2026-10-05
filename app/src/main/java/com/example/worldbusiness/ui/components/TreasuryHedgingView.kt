package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.HedgingPortfolioSummary
import com.example.worldbusiness.data.model.HedgingStrategyType
import com.example.worldbusiness.data.model.HedgingSuggestion
import com.example.worldbusiness.data.model.HedgingUrgency
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.CurrencyHedgingEngine
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Automated Rule-Based Currency Hedging System View for Treasury Module.
 * Suggests hedging strategies based on historical exchange rate volatility and invoice due dates.
 */
@Composable
fun TreasuryHedgingView(
    invoices: List<InvoiceRecord>,
    balances: List<FxBalanceRecord>,
    onExecuteHedge: (HedgingSuggestion) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var executedHedgeIds by remember { mutableStateOf(setOf<String>()) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var hedgeToConfirm by remember { mutableStateOf<HedgingSuggestion?>(null) }
    var showScenarioSimulator by remember { mutableStateOf(false) }

    val suggestions = remember(invoices, balances, executedHedgeIds) {
        CurrencyHedgingEngine.generateHedgingSuggestions(invoices, balances).map {
            if (executedHedgeIds.contains(it.id)) it.copy(isExecuted = true) else it
        }
    }

    val portfolioSummary = remember(suggestions) {
        CurrencyHedgingEngine.computePortfolioSummary(suggestions)
    }

    val filteredSuggestions = when (selectedFilter) {
        "CRITICAL" -> suggestions.filter { it.urgency == HedgingUrgency.CRITICAL || it.urgency == HedgingUrgency.HIGH }
        "FORWARD" -> suggestions.filter { it.strategyType == HedgingStrategyType.FORWARD_CONTRACT }
        "COLLAR" -> suggestions.filter { it.strategyType == HedgingStrategyType.ZERO_COST_COLLAR }
        "NATURAL" -> suggestions.filter { it.strategyType == HedgingStrategyType.NATURAL_MATCH }
        else -> suggestions
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("treasury_hedging_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Hedging Portfolio Summary Card
        HedgingPortfolioCockpitCard(
            summary = portfolioSummary,
            onOpenSimulator = { showScenarioSimulator = true }
        )

        // Filter Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "ALL" to "All Strategies (${suggestions.size})",
                "CRITICAL" to "Critical / High (${portfolioSummary.criticalUrgencyCount + portfolioSummary.highUrgencyCount})",
                "FORWARD" to "Forward Locks",
                "COLLAR" to "Collars",
                "NATURAL" to "Natural Vault Matches"
            ).forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("filter_hedge_$key")
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Render Hedging Suggestion Cards
        if (filteredSuggestions.isNotEmpty()) {
            filteredSuggestions.forEach { suggestion ->
                HedgingStrategyCard(
                    suggestion = suggestion,
                    onExecuteClick = { hedgeToConfirm = suggestion }
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = EmeraldPositive, modifier = Modifier.size(32.dp))
                    Text(
                        text = "All Cross-Border Payables Insulated Against FX Volatility",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "No unhedged currency risk detected for current filter criteria.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Hedge Confirmation Execution Dialog
    hedgeToConfirm?.let { suggestion ->
        HedgeExecutionConfirmationDialog(
            suggestion = suggestion,
            onDismiss = { hedgeToConfirm = null },
            onConfirm = {
                executedHedgeIds = executedHedgeIds + suggestion.id
                onExecuteHedge(suggestion)
                hedgeToConfirm = null
            }
        )
    }

    // What-If Volatility & Horizon Scenario Simulator Dialog
    if (showScenarioSimulator) {
        HedgingScenarioSimulatorDialog(
            onDismiss = { showScenarioSimulator = false }
        )
    }
}

/**
 * Top Cockpit Card displaying aggregate portfolio unhedged exposure, VaR, and coverage.
 */
@Composable
private fun HedgingPortfolioCockpitCard(
    summary: HedgingPortfolioSummary,
    onOpenSimulator: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(GoldAccent.copy(alpha = 0.5f), BorderSubtle)
        ))
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Hedging Engine",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AUTOMATED FX HEDGING ENGINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Rule-based Volatility & Maturity Analysis",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onOpenSimulator,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_open_hedging_simulator")
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Simulate", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SIMULATOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // 3-Metric KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "UNHEDGED FOREIGN EXPOSURE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(summary.totalUnhedgedExposureUsd.roundToInt())} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PORTFOLIO 95% VaR", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(summary.totalValueAtRiskUsd.roundToInt())} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "RECOMMENDED COVERAGE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${summary.recommendedCoverageRatioPercent.roundToInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Coverage Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { (summary.recommendedCoverageRatioPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = EmeraldPositive,
                    trackColor = SurfaceElevated
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Historical Realized Volatility: ${String.format(Locale.US, "%.1f", summary.averageVolatilityPercent)}% Annualized",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${summary.criticalUrgencyCount} Critical • ${summary.highUrgencyCount} High Risk",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.criticalUrgencyCount > 0) RoseNegative else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Individual Card representing an automated Rule-Based Hedging Strategy.
 */
@Composable
private fun HedgingStrategyCard(
    suggestion: HedgingSuggestion,
    onExecuteClick: () -> Unit
) {
    val urgencyColor = when (suggestion.urgency) {
        HedgingUrgency.CRITICAL -> RoseNegative
        HedgingUrgency.HIGH -> GoldAccent
        HedgingUrgency.MODERATE -> CyanAccent
        HedgingUrgency.LOW -> EmeraldPositive
    }

    val strategyBadgeColor = when (suggestion.strategyType) {
        HedgingStrategyType.FORWARD_CONTRACT -> CyanAccent
        HedgingStrategyType.ZERO_COST_COLLAR -> GoldAccent
        HedgingStrategyType.LAYERED_TRANCHES -> Color(0xFFB388FF)
        HedgingStrategyType.SYNTHETIC_SWAP -> Color(0xFF80D8FF)
        HedgingStrategyType.NATURAL_MATCH -> EmeraldPositive
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hedging_card_${suggestion.id.lowercase(Locale.US)}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
            if (suggestion.isExecuted) {
                listOf(EmeraldPositive.copy(alpha = 0.8f), BorderSubtle)
            } else {
                listOf(urgencyColor.copy(alpha = 0.6f), BorderSubtle)
            }
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Strategy Type, Urgency Badge, Execution Status
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
                            .background(strategyBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(0.8.dp, strategyBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = suggestion.strategyType.shortCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = strategyBadgeColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = suggestion.strategyType.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (suggestion.isExecuted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPositive.copy(alpha = 0.2f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.5f))))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Executed", tint = EmeraldPositive, modifier = Modifier.size(12.dp))
                            Text(text = "HEDGE ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = urgencyColor.copy(alpha = 0.15f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(urgencyColor, urgencyColor.copy(alpha = 0.4f))))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(urgencyColor, CircleShape))
                            Text(text = suggestion.urgency.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = urgencyColor, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Invoice Telemetry & Counterparty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${suggestion.invoiceNumber} • ${suggestion.counterparty}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Due Date: ${suggestion.dueDateString} (${suggestion.daysToDueDate} Days to Maturity)",
                        fontSize = 10.sp,
                        color = if (suggestion.daysToDueDate <= 15) RoseNegative else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${NumberFormat.getNumberInstance(Locale.US).format(suggestion.exposureAmount.roundToInt())} ${suggestion.currency}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "≈ $${NumberFormat.getNumberInstance(Locale.US).format(suggestion.exposureUsd.roundToInt())} USD",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))

            // Quantitative Pricing & Volatility Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "VOLATILITY (90D)", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", suggestion.historicalVolatilityAnnualized)}% σ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (suggestion.historicalVolatilityAnnualized >= 12.0) RoseNegative else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "RATES (SPOT vs FWD)", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${String.format(Locale.US, "%.4f", suggestion.spotRate)} → ${String.format(Locale.US, "%.4f", suggestion.forwardRate)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "95% VaR AT RISK", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "-$${NumberFormat.getNumberInstance(Locale.US).format(suggestion.valueAtRisk95Usd.roundToInt())}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Collar / Strike Corridor Range (if applicable)
            if (suggestion.collarFloorRate != null && suggestion.collarCapRate != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLLAR CORRIDOR:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Floor: ${String.format(Locale.US, "%.4f", suggestion.collarFloorRate)} | Cap: ${String.format(Locale.US, "%.4f", suggestion.collarCapRate)} (Zero Net Premium)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Rule-based Justification Callout
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚙️ ${suggestion.ruleTriggeredName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = suggestion.ruleExplanation,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            // Action Steps
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                suggestion.actionSteps.forEach { step ->
                    Text(
                        text = "• $step",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Execute Hedge Action Button
            if (!suggestion.isExecuted) {
                Button(
                    onClick = onExecuteClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (suggestion.urgency == HedgingUrgency.CRITICAL) RoseNegative else CyanAccent,
                        contentColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_execute_hedge_${suggestion.id.lowercase(Locale.US)}")
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXECUTE ${suggestion.strategyType.shortCode} HEDGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Confirmation Modal for executing a derivative contract / hedge.
 */
@Composable
private fun HedgeExecutionConfirmationDialog(
    suggestion: HedgingSuggestion,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = CyanAccent)
                    Text(
                        text = "EXECUTE ${suggestion.strategyType.shortCode} CONTRACT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConfirmRow("Target Obligation", "${suggestion.invoiceNumber} (${suggestion.counterparty})")
                        ConfirmRow("Notional Exposure", "${NumberFormat.getNumberInstance(Locale.US).format(suggestion.exposureAmount.roundToInt())} ${suggestion.currency}")
                        ConfirmRow("USD Equivalent", "$${NumberFormat.getNumberInstance(Locale.US).format(suggestion.exposureUsd.roundToInt())}")
                        ConfirmRow("Guaranteed Forward Rate", String.format(Locale.US, "%.4f", suggestion.forwardRate))
                        ConfirmRow("Value Settlement Date", suggestion.dueDateString)
                        ConfirmRow("Protected Downside VaR", "$${NumberFormat.getNumberInstance(Locale.US).format(suggestion.valueAtRisk95Usd.roundToInt())} USD")
                        ConfirmRow("Clearing Desk Execution", "ISDA Master Agreement • OTC Bilateral")
                    }
                }

                Text(
                    text = "Confirming this transaction locks the enterprise currency rate and binds treasury liquidity reserves on value date delivery.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "CONFIRM & BIND HEDGE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Interactive What-If Scenario Simulator Dialog:
 * Treasury managers can slide volatility and horizon to see the rule-based engine dynamically switch recommendations.
 */
@Composable
private fun HedgingScenarioSimulatorDialog(
    onDismiss: () -> Unit
) {
    var selectedSimCurrency by remember { mutableStateOf("EUR") }
    var simAmount by remember { mutableStateOf(200000.0) }
    var simDays by remember { mutableStateOf(30f) }
    var simVolatility by remember { mutableStateOf(10f) }

    val simulatedSuggestion = remember(selectedSimCurrency, simAmount, simDays, simVolatility) {
        val fakeInvoice = InvoiceRecord(
            id = 9999,
            invoiceNumber = "SIM-2026-X",
            issuingEntityName = "OmniGlobal Treasury Desk",
            clientName = "Simulation Counterparty Ltd.",
            clientCountry = "Global",
            issueDate = "Oct 01, 2026",
            dueDate = "Simulated Maturity",
            amount = simAmount,
            currency = selectedSimCurrency,
            taxRatePercent = 0.0,
            status = "PENDING",
            serviceDescription = "Simulated Cross-Border Exposure"
        )
        val rate = CurrencyHedgingEngine.defaultFxRates[selectedSimCurrency] ?: 1.0
        val exposureUsd = simAmount * rate

        // Re-evaluate using user-tweaked volatility & days
        when {
            simVolatility >= 12.0 && simDays <= 30 -> {
                "Fixed Forward Contract (Rule 1: High Volatility & Imminent Maturity)"
            }
            (selectedSimCurrency == "BRL" || selectedSimCurrency == "CHF") && simDays >= 40 -> {
                "Synthetic Currency Swap (Rule 4: Sovereign Yield Parity Carry)"
            }
            simDays > 60 && exposureUsd >= 200000.0 -> {
                "Layered Dynamic Tranches (Rule 3: Extended Horizon & Large Notional)"
            }
            simVolatility in 7.0..12.0 -> {
                "Zero-Cost Collar Corridor (Rule 2: Moderate Volatility Range Forward)"
            }
            else -> {
                "Spot Timing Optimization (Rule 5: Low Sovereign Volatility Peg)"
            }
        }
    }

    val simulatedVaR = remember(simAmount, simDays, simVolatility, selectedSimCurrency) {
        val rate = CurrencyHedgingEngine.defaultFxRates[selectedSimCurrency] ?: 1.0
        val exposureUsd = simAmount * rate
        val timeFactor = kotlin.math.sqrt(simDays.coerceAtLeast(1f) / 365.0)
        exposureUsd * 1.645 * (simVolatility / 100.0) * timeFactor
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Simulator", tint = CyanAccent)
                    Text(
                        text = "FX SCENARIO SIMULATOR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Currency Selector
                Text(text = "SELECT EXPOSURE CURRENCY:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("EUR", "GBP", "CHF", "SGD", "JPY", "BRL").forEach { curr ->
                        val isSel = selectedSimCurrency == curr
                        Box(
                            modifier = Modifier
                                .background(if (isSel) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSel) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedSimCurrency = curr }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = curr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Days to Due Date Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "TIME TO INVOICE DUE DATE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "${simDays.roundToInt()} Days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = simDays,
                        onValueChange = { simDays = it },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }

                // Volatility Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "MARKET VOLATILITY (ANNUALIZED σ)", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "${simVolatility.roundToInt()}% σ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (simVolatility >= 12f) RoseNegative else GoldAccent, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = simVolatility,
                        onValueChange = { simVolatility = it },
                        valueRange = 3f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (simVolatility >= 12f) RoseNegative else GoldAccent,
                            activeTrackColor = if (simVolatility >= 12f) RoseNegative else GoldAccent
                        )
                    )
                }

                // Dynamic Output Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                        listOf(CyanAccent.copy(alpha = 0.6f), BorderSubtle)
                    )),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "AUTOMATED STRATEGY SUGGESTION:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = simulatedSuggestion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Text(
                            text = "Simulated 95% VaR: $${NumberFormat.getNumberInstance(Locale.US).format(simulatedVaR.roundToInt())} USD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text(text = "DONE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
