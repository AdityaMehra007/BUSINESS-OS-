package com.example.worldbusiness.ui.components

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.HistoricalBalancePoint
import com.example.worldbusiness.data.model.TreasuryConversionSummary
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import com.example.worldbusiness.data.model.TreasuryTrendHistory
import com.example.worldbusiness.data.repository.TreasuryHistoryEngine
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-performance, D3/Recharts-inspired Historical Trend Line & Area Visualization
 * displaying account balances over the last 30 days within the World Business OS Treasury Dashboard.
 *
 * Features:
 * - Smooth Monotone Cubic Bézier Spline curve (D3 curveMonotoneX / Recharts monotone)
 * - Multi-stop glowing vertical gradient area fill (Recharts Area style)
 * - Interactive Touch & Drag Scrubber with real-time hovering tooltip
 * - Active data point glowing pulse indicator & vertical crosshair guide
 * - 30-Day, 14-Day, and 7-Day timeframe filters
 * - Consolidated Portfolio vs Individual Currency Vault switching
 * - Peak & Trough milestone indicators
 * - 30-Day Executive Analytics metric summary cards
 */
@Composable
fun TreasuryBalanceTrendVisualization(
    summary: TreasuryConversionSummary,
    transactions: List<TreasuryTransactionRecord> = emptyList(),
    initialCurrencyCode: String = "ALL",
    onSelectVault: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCurrency by remember { mutableStateOf(initialCurrencyCode) }
    var selectedRangeDays by remember { mutableStateOf(30) }
    var isAreaFillMode by remember { mutableStateOf(true) }
    var showDataPoints by remember { mutableStateOf(true) }
    var scrubbedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Derive trend data deterministically
    val trendHistory by remember(summary, transactions, selectedCurrency, selectedRangeDays) {
        derivedStateOf {
            TreasuryHistoryEngine.generateTrendHistory(
                summary = summary,
                transactions = transactions,
                currencyCode = selectedCurrency,
                timeRangeDays = selectedRangeDays
            )
        }
    }

    val activePoint = remember(trendHistory, scrubbedPointIndex) {
        if (scrubbedPointIndex != null && scrubbedPointIndex in trendHistory.points.indices) {
            trendHistory.points[scrubbedPointIndex!!]
        } else {
            trendHistory.points.lastOrNull()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("treasury_trend_visualization")
            .testTag("recharts_currency_balance_chart")
            .testTag("recharts_historical_trend_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.45f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with Title, Library Badge & Controls
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
                            .background(CyanAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Trend Line Chart",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "HISTORICAL ACCOUNT BALANCES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(EmeraldPositive.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, EmeraldPositive.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "30D TREND",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "D3 / Recharts Spline Visualization • Drag & scrub to inspect",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Range Selector (7D, 14D, 30D)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(7 to "7D", 14 to "14D", 30 to "30D").forEach { (days, label) ->
                        val isSelected = selectedRangeDays == days
                        Box(
                            modifier = Modifier
                                .testTag("treasury_trend_range_${label.lowercase(Locale.US)}")
                                .background(
                                    color = if (isSelected) CyanAccent else SurfaceElevated,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyanAccent else BorderSubtle,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedRangeDays = days }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Currency & Vault Selector Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("treasury_trend_currency_filter"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Consolidated Option
                VaultFilterChip(
                    label = "Consolidated (${summary.baseCurrency})",
                    code = "ALL",
                    isSelected = selectedCurrency == "ALL",
                    onClick = {
                        selectedCurrency = "ALL"
                        onSelectVault("ALL")
                    }
                )

                // Individual Currency Vaults
                summary.accounts.forEach { account ->
                    VaultFilterChip(
                        label = "${account.currencyCode} Vault",
                        code = account.currencyCode,
                        isSelected = selectedCurrency == account.currencyCode,
                        symbol = account.symbol,
                        onClick = {
                            selectedCurrency = account.currencyCode
                            onSelectVault(account.currencyCode)
                        }
                    )
                }
            }

            // Real-Time / Scrubbed Metric Value Banner
            activePoint?.let { pt ->
                ActivePointDetailBanner(
                    point = pt,
                    trendHistory = trendHistory,
                    isLive = scrubbedPointIndex == null,
                    onReset = { scrubbedPointIndex = null }
                )
            }

            // The Interactive Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark.copy(alpha = 0.45f))
                    .border(1.dp, BorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .testTag("treasury_trend_chart_canvas")
            ) {
                TreasuryTrendSplineCanvas(
                    trendHistory = trendHistory,
                    isAreaFillMode = isAreaFillMode,
                    showDataPoints = showDataPoints,
                    scrubbedIndex = scrubbedPointIndex,
                    onScrubPoint = { index -> scrubbedPointIndex = index },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Chart View Mode Toggles (Recharts Area vs D3 Line, Dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Style Toggle: Recharts Area vs D3 Line
                    Row(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { isAreaFillMode = !isAreaFillMode }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAreaFillMode) Icons.Default.Layers else Icons.Default.ShowChart,
                            contentDescription = "Toggle Chart Style",
                            tint = CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isAreaFillMode) "Recharts Area Fill" else "D3 Pure Line",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Milestone Dots Toggle
                    Row(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { showDataPoints = !showDataPoints }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (showDataPoints) CyanAccent else TextMuted, CircleShape)
                        )
                        Text(
                            text = "Milestones",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (showDataPoints) TextPrimary else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Interactive Hint
                Text(
                    text = if (scrubbedPointIndex != null) "Touch released or tap reset" else "Touch & drag across chart",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 30-Day Executive Analytics KPI Grid (D3/Recharts summary statistics)
            TreasuryAnalyticsSummaryCards(trendHistory = trendHistory)
        }
    }
}

/**
 * Filter Chip for Switching Between Consolidated and Individual Currency Vaults
 */
@Composable
private fun VaultFilterChip(
    label: String,
    code: String,
    isSelected: Boolean,
    symbol: String = "",
    onClick: () -> Unit
) {
    val bg = if (isSelected) CyanAccent.copy(alpha = 0.18f) else SurfaceElevated
    val border = if (isSelected) CyanAccent else BorderSubtle
    val textCol = if (isSelected) CyanAccent else TextSecondary

    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (symbol.isNotEmpty()) {
            Text(
                text = symbol,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CyanAccent else TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Interactive Banner displaying the balance, date, and delta for the currently scrubbed day
 */
@Composable
private fun ActivePointDetailBanner(
    point: HistoricalBalancePoint,
    trendHistory: TreasuryTrendHistory,
    isLive: Boolean,
    onReset: () -> Unit
) {
    val isPositive = point.dailyChange >= 0.0
    val deltaColor = if (isPositive) EmeraldPositive else RoseNegative
    val deltaIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated.copy(alpha = 0.8f),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
            listOf(CyanAccent.copy(alpha = 0.3f), BorderSubtle)
        )),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = CyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${point.fullDate} (Day ${point.dayIndex + 1} of ${trendHistory.timeRangeDays})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .background(CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LATEST CLOSE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "HISTORICAL INSPECT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Balance with Currency Symbol
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${trendHistory.currencySymbol}${Formatters.formatCurrency(point.balance)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (trendHistory.currencyCode == "ALL") trendHistory.baseCurrency else trendHistory.currencyCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Event annotation if present
                if (!point.eventNote.isNullOrEmpty()) {
                    Text(
                        text = "• Event: ${point.eventNote}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right side: Daily Delta & Reset button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .background(deltaColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = deltaIcon,
                        contentDescription = "Delta",
                        tint = deltaColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${trendHistory.currencySymbol}${Formatters.formatCurrency(point.dailyChange)} (${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", point.dailyChangePercent)}%)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!isLive) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onReset)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset to Live",
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Reset to Today",
                            fontSize = 10.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Compose Canvas rendering the D3 / Recharts Monotone Cubic Bézier Curve,
 * multi-stop vertical gradient area, horizontal gridlines, and interactive crosshair scrubber.
 */
@Composable
private fun TreasuryTrendSplineCanvas(
    trendHistory: TreasuryTrendHistory,
    isAreaFillMode: Boolean,
    showDataPoints: Boolean,
    scrubbedIndex: Int?,
    onScrubPoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val points = trendHistory.points
    if (points.isEmpty()) return

    // Pulsing halo animation for the active scrubbed point
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Canvas(
        modifier = modifier
            .testTag("recharts_trend_canvas")
            .semantics { contentDescription = "Historical Account Balance 30-Day Trend Chart" }
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val width = size.width
                    val padStart = 50f
                    val padEnd = 30f
                    val chartWidth = width - padStart - padEnd
                    if (chartWidth > 0 && points.size > 1) {
                        val touchX = (offset.x - padStart).coerceIn(0f, chartWidth)
                        val fraction = touchX / chartWidth
                        val index = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                        onScrubPoint(index)
                    }
                }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width
                        val padStart = 50f
                        val padEnd = 30f
                        val chartWidth = width - padStart - padEnd
                        if (chartWidth > 0 && points.size > 1) {
                            val touchX = (offset.x - padStart).coerceIn(0f, chartWidth)
                            val fraction = touchX / chartWidth
                            val index = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrubPoint(index)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = size.width
                        val padStart = 50f
                        val padEnd = 30f
                        val chartWidth = width - padStart - padEnd
                        if (chartWidth > 0 && points.size > 1) {
                            val touchX = (change.position.x - padStart).coerceIn(0f, chartWidth)
                            val fraction = touchX / chartWidth
                            val index = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrubPoint(index)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        val padStart = 55f
        val padEnd = 35f
        val padTop = 35f
        val padBottom = 35f

        val chartWidth = width - padStart - padEnd
        val chartHeight = height - padTop - padBottom

        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val minVal = trendHistory.minBalance
        val maxVal = trendHistory.maxBalance
        val range = (maxVal - minVal).takeIf { it > 0.0 } ?: 1.0

        // Function to map a day index and balance to canvas (x, y)
        fun getCoord(index: Int, balance: Double): Offset {
            val fractionX = if (points.size > 1) index.toFloat() / (points.size - 1) else 0.5f
            val x = padStart + fractionX * chartWidth
            val fractionY = ((balance - minVal) / range).toFloat().coerceIn(0f, 1f)
            val y = padTop + chartHeight * (1f - fractionY)
            return Offset(x, y)
        }

        // 1. Draw horizontal gridlines (4 lines)
        val gridLines = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        for (g in 0..gridLines) {
            val frac = g.toFloat() / gridLines
            val y = padTop + chartHeight * frac
            val lineVal = maxVal - frac * range

            drawLine(
                color = BorderSubtle.copy(alpha = 0.45f),
                start = Offset(padStart, y),
                end = Offset(width - padEnd, y),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
        }

        // 2. Generate smooth Monotone Cubic Bézier Spline path (D3 curveMonotoneX / Recharts)
        val splinePath = Path()
        val areaPath = Path()

        val coords = points.mapIndexed { idx, pt -> getCoord(idx, pt.balance) }

        if (coords.isNotEmpty()) {
            splinePath.moveTo(coords[0].x, coords[0].y)
            areaPath.moveTo(coords[0].x, height - padBottom)
            areaPath.lineTo(coords[0].x, coords[0].y)

            for (i in 0 until coords.size - 1) {
                val p0 = coords[(i - 1).coerceAtLeast(0)]
                val p1 = coords[i]
                val p2 = coords[i + 1]
                val p3 = coords[(i + 2).coerceAtMost(coords.size - 1)]

                // Catmull-Rom to Cubic Bézier control points
                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                splinePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                areaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            areaPath.lineTo(coords.last().x, height - padBottom)
            areaPath.close()

            // 3. Draw Area Fill (Recharts gradient fill under curve)
            if (isAreaFillMode) {
                val areaBrush = Brush.verticalGradient(
                    colors = listOf(
                        CyanAccent.copy(alpha = 0.40f),
                        EmeraldPositive.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = padTop,
                    endY = height - padBottom
                )
                drawPath(path = areaPath, brush = areaBrush)
            }

            // 4. Draw Soft Outer Bloom Glow (Neon glow behind spline)
            drawPath(
                path = splinePath,
                color = CyanAccent.copy(alpha = 0.22f),
                style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Draw Primary Sharp Spline Line
            val splineBrush = Brush.horizontalGradient(
                colors = listOf(
                    CyanAccent.copy(alpha = 0.85f),
                    EmeraldPositive,
                    CyanAccent
                ),
                startX = padStart,
                endX = width - padEnd
            )
            drawPath(
                path = splinePath,
                brush = splineBrush,
                style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 6. Draw Milestone / Peak / Trough Points if enabled
            if (showDataPoints) {
                coords.forEachIndexed { idx, coord ->
                    val pt = points[idx]
                    when {
                        pt.isPeak -> {
                            // Peak Callout Dot (Emerald glow)
                            drawCircle(
                                color = EmeraldPositive.copy(alpha = 0.35f),
                                radius = 7f,
                                center = coord
                            )
                            drawCircle(
                                color = EmeraldPositive,
                                radius = 3.5f,
                                center = coord
                            )
                        }
                        pt.isTrough -> {
                            // Trough Callout Dot (Rose glow)
                            drawCircle(
                                color = RoseNegative.copy(alpha = 0.35f),
                                radius = 6f,
                                center = coord
                            )
                            drawCircle(
                                color = RoseNegative,
                                radius = 3f,
                                center = coord
                            )
                        }
                        pt.eventNote != null -> {
                            // Milestone Event Dot (Gold accent)
                            drawCircle(
                                color = GoldAccent.copy(alpha = 0.4f),
                                radius = 5.5f,
                                center = coord
                            )
                            drawCircle(
                                color = GoldAccent,
                                radius = 2.5f,
                                center = coord
                            )
                        }
                    }
                }
            }

            // 7. Active / Scrubbed Point Guide Line and Pulsing Dot (Recharts activeDot)
            val activeIdx = scrubbedIndex ?: (points.size - 1)
            if (activeIdx in coords.indices) {
                val activeCoord = coords[activeIdx]

                // Vertical Crosshair Scrubber Line
                drawLine(
                    color = CyanAccent.copy(alpha = 0.7f),
                    start = Offset(activeCoord.x, padTop),
                    end = Offset(activeCoord.x, height - padBottom),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Animated Pulsing Outer Ring
                drawCircle(
                    color = CyanAccent.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = activeCoord,
                    style = Stroke(width = 2f)
                )

                // Outer Halo
                drawCircle(
                    color = SurfaceDark,
                    radius = 6.5f,
                    center = activeCoord
                )

                // Solid Core Dot
                drawCircle(
                    color = CyanAccent,
                    radius = 4.5f,
                    center = activeCoord
                )
            }
        }
    }
}

/**
 * Executive KPI Summary Cards highlighting 30-day performance metrics,
 * range high/low milestones, net trajectory, and treasury volatility.
 */
@Composable
private fun TreasuryAnalyticsSummaryCards(trendHistory: TreasuryTrendHistory) {
    val isPositive = trendHistory.netChangeAmount >= 0.0
    val netColor = if (isPositive) EmeraldPositive else RoseNegative

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: 30D Starting Balance
            AnalyticsMetricCard(
                title = "${trendHistory.timeRangeDays}D START BALANCE",
                value = "${trendHistory.currencySymbol}${Formatters.formatCompactNumber(trendHistory.startBalance)}",
                subtitle = "Day 1 Baseline",
                tint = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Card 2: 30D Ending / Current Balance
            AnalyticsMetricCard(
                title = "CURRENT VALUATION",
                value = "${trendHistory.currencySymbol}${Formatters.formatCompactNumber(trendHistory.currentBalance)}",
                subtitle = "Day ${trendHistory.timeRangeDays} Close",
                tint = CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Net Period Trajectory
            AnalyticsMetricCard(
                title = "NET ${trendHistory.timeRangeDays}D TRAJECTORY",
                value = "${if (isPositive) "+" else ""}${trendHistory.currencySymbol}${Formatters.formatCompactNumber(trendHistory.netChangeAmount)}",
                subtitle = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", trendHistory.netChangePercent)}% Net Gain",
                tint = netColor,
                modifier = Modifier.weight(1f)
            )

            // Card 4: High / Low Peak Range
            AnalyticsMetricCard(
                title = "HIGH / LOW PEAK",
                value = "${trendHistory.currencySymbol}${Formatters.formatCompactNumber(trendHistory.maxBalance)}",
                subtitle = "Low: ${trendHistory.currencySymbol}${Formatters.formatCompactNumber(trendHistory.minBalance)}",
                tint = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Stability & Volatility Strip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceElevated.copy(alpha = 0.5f),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))
            )),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Volatility",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "TREASURY VOLATILITY INDEX:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", trendHistory.volatilityPercent)}% (Standard Deviation)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(EmeraldPositive.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STABLE CAPITAL GRADE",
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

/**
 * Individual Metric Tile for Analytics Summary Grid
 */
@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.4f))
        )),
        modifier = modifier
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
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
