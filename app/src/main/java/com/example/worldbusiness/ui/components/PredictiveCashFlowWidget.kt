package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.worldbusiness.data.model.CashFlowHorizon
import com.example.worldbusiness.data.model.CashFlowScenario
import com.example.worldbusiness.data.model.DailyCashFlowPoint
import com.example.worldbusiness.data.model.PredictiveCashFlowReport
import com.example.worldbusiness.data.model.ScheduledCashEvent
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Predictive Analytics Dashboard Widget: Calculates future cash flow requirements
 * based on current multi-currency treasury balances and outstanding global commercial invoices.
 */
@Composable
fun PredictiveCashFlowWidget(
    report: PredictiveCashFlowReport,
    onSelectHorizon: (CashFlowHorizon) -> Unit,
    onSelectScenario: (CashFlowScenario) -> Unit,
    onExportCsv: () -> String,
    onNavigateToInvoices: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var showExportModal by remember { mutableStateOf(false) }
    var exportedCsvData by remember { mutableStateOf("") }

    val activePoint = selectedPointIndex?.let { idx ->
        if (idx in report.dailyPoints.indices) report.dailyPoints[idx] else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("predictive_cash_flow_widget"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Horizon Pill, Health Status Badge
            PredictiveHeader(
                report = report,
                onExportClick = {
                    exportedCsvData = onExportCsv()
                    showExportModal = true
                }
            )

            // Horizon & Scenario Selector Chips
            PredictiveControlsRow(
                currentHorizon = report.horizon,
                currentScenario = report.scenario,
                onSelectHorizon = onSelectHorizon,
                onSelectScenario = onSelectScenario
            )

            // 4-Pillar Executive Summary Metric Strip
            PredictiveKpiStrip(report = report)

            // Interactive Predictive Canvas Trajectory Chart
            PredictiveTrajectoryChart(
                points = report.dailyPoints,
                currentLiquidBalance = report.currentLiquidCashUsd,
                minBufferUsd = report.minWorkingCapitalBufferUsd,
                selectedIndex = selectedPointIndex,
                onSelectPoint = { selectedPointIndex = it }
            )

            // Scrubber Tooltip when user drags or taps a day
            activePoint?.let { pt ->
                ActivePointDetailCard(point = pt)
            }

            // Executive Recommendation & Working Capital Alert
            ExecutiveRecommendationBanner(report = report)

            // Chronological Upcoming Cash Events Timeline
            UpcomingCashEventsSection(
                events = report.upcomingEvents.take(5),
                onNavigateToInvoices = onNavigateToInvoices
            )
        }
    }

    // CSV Export Modal
    if (showExportModal) {
        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export", tint = CyanAccent)
                    Text(
                        text = "PREDICTIVE CASH FLOW CSV READY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exported ${report.horizon.label} daily cash flow forecast and milestone event projections:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevated,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Box(modifier = Modifier.padding(8.dp).horizontalScroll(rememberScrollState())) {
                            Text(text = exportedCsvData, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
                ) {
                    Text(text = "CLOSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * Top Header with Title, Status Pill, and Export Action.
 */
@Composable
private fun PredictiveHeader(
    report: PredictiveCashFlowReport,
    onExportClick: () -> Unit
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
                    .size(36.dp)
                    .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = "Predictive Analytics",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "PREDICTIVE CASH FLOW & RUNWAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Forward Liquidity & Global Invoice Clearing",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = report.liquidityHealthStatus.color.copy(alpha = 0.15f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(report.liquidityHealthStatus.color, report.liquidityHealthStatus.color.copy(alpha = 0.4f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(report.liquidityHealthStatus.color, CircleShape))
                    Text(
                        text = report.liquidityHealthStatus.badgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = report.liquidityHealthStatus.color,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = onExportClick,
                modifier = Modifier.size(28.dp).testTag("btn_export_forecast_csv")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export CSV",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Horizon & Stress Scenario Selector Row.
 */
@Composable
private fun PredictiveControlsRow(
    currentHorizon: CashFlowHorizon,
    currentScenario: CashFlowScenario,
    onSelectHorizon: (CashFlowHorizon) -> Unit,
    onSelectScenario: (CashFlowScenario) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Horizon selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CashFlowHorizon.values().forEach { horizon ->
                val isSelected = currentHorizon == horizon
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectHorizon(horizon) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("horizon_chip_${horizon.days}")
                ) {
                    Text(
                        text = horizon.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Stress Scenario Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCENARIO:",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            CashFlowScenario.values().forEach { scenario ->
                val isSelected = currentScenario == scenario
                Box(
                    modifier = Modifier
                        .background(if (isSelected) GoldAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) GoldAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectScenario(scenario) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("scenario_chip_${scenario.name.lowercase(Locale.US)}")
                ) {
                    Text(
                        text = scenario.shortName,
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
 * 4-Pillar Executive Summary Metric Strip.
 */
@Composable
private fun PredictiveKpiStrip(report: PredictiveCashFlowReport) {
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "STARTING CASH", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$${numberFmt.format((report.currentLiquidCashUsd / 1000000.0 * 10).roundToInt() / 10.0)}M",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "EXP. INFLOWS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "+$${numberFmt.format((report.totalExpectedInflowsUsd / 1000.0).roundToInt())}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "COMMITTED OUT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "-$${numberFmt.format((report.totalCommittedOutflowsUsd / 1000.0).roundToInt())}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = RoseNegative,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "NET RUNWAY", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "${String.format(Locale.US, "%.1f", report.runwayMonths)} Mo",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = if (report.runwayMonths >= 12.0) EmeraldPositive else GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Interactive D3/Canvas Predictive Trajectory Chart.
 */
@Composable
private fun PredictiveTrajectoryChart(
    points: List<DailyCashFlowPoint>,
    currentLiquidBalance: Double,
    minBufferUsd: Double,
    selectedIndex: Int?,
    onSelectPoint: (Int) -> Unit
) {
    if (points.isEmpty()) return

    val minBal = points.minOfOrNull { it.projectedBalanceUsd } ?: currentLiquidBalance
    val maxBal = points.maxOfOrNull { it.projectedBalanceUsd } ?: currentLiquidBalance
    val bufferThreshold = minBufferUsd

    // Scale range with 10% breathing room
    val yMin = min(minBal, bufferThreshold) * 0.92
    val yMax = max(maxBal, currentLiquidBalance) * 1.05
    val yRange = if (yMax - yMin > 0.0) yMax - yMin else 1.0

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    val idx = (frac * (points.size - 1)).roundToInt()
                    onSelectPoint(idx)
                }
            }
            .pointerInput(points) {
                detectDragGestures { change, _ ->
                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    val idx = (frac * (points.size - 1)).roundToInt()
                    onSelectPoint(idx)
                }
            }
            .testTag("predictive_trajectory_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {
            val width = size.width
            val height = size.height

            // 1. Draw Working Capital Buffer (Dotted Line)
            val bufferY = height - (((bufferThreshold - yMin) / yRange).toFloat() * height).coerceIn(0f, height)
            drawLine(
                color = GoldAccent.copy(alpha = 0.5f),
                start = Offset(0f, bufferY),
                end = Offset(width, bufferY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            // 2. Build Monotone Line & Area Paths
            val curvePath = Path()
            val fillPath = Path()

            val stepX = width / (points.size - 1).toFloat()

            points.forEachIndexed { index, pt ->
                val x = index * stepX
                val normalizedY = ((pt.projectedBalanceUsd - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                val y = height - (normalizedY * height)

                if (index == 0) {
                    curvePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevIndex = index - 1
                    val prevX = prevIndex * stepX
                    val prevNormY = ((points[prevIndex].projectedBalanceUsd - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    val prevY = height - (prevNormY * height)

                    // Cubic Bézier control points
                    val cx1 = prevX + (stepX / 2f)
                    val cy1 = prevY
                    val cx2 = prevX + (stepX / 2f)
                    val cy2 = y

                    curvePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Draw Area Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyanAccent.copy(alpha = 0.28f),
                        EmeraldPositive.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                )
            )

            // Draw Curve Line
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(CyanAccent, EmeraldPositive)
                ),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 3. Draw Milestone Event Nodes
            points.forEachIndexed { index, pt ->
                if (pt.isMilestoneEvent) {
                    val x = index * stepX
                    val normalizedY = ((pt.projectedBalanceUsd - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    val y = height - (normalizedY * height)

                    val milestoneColor = if (pt.inflowUsd > 0) EmeraldPositive else RoseNegative

                    drawCircle(
                        color = milestoneColor.copy(alpha = pulseAlpha),
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = milestoneColor,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // 4. Draw Selected Crosshair Guide
            selectedIndex?.let { idx ->
                if (idx in points.indices) {
                    val selectedPt = points[idx]
                    val x = idx * stepX
                    val normY = ((selectedPt.projectedBalanceUsd - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    val y = height - (normY * height)

                    // Vertical guide
                    drawLine(
                        color = CyanAccent.copy(alpha = 0.7f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Active cursor dot
                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = CyanAccent,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Buffer legend overlay in top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.width(10.dp).height(2.dp).background(GoldAccent))
            Text(
                text = "Safe Buffer: $${(minBufferUsd / 1000000.0 * 10).roundToInt() / 10.0}M",
                fontSize = 8.sp,
                color = GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Scrubber Detail Card showing exact values on tapped/dragged day.
 */
@Composable
private fun ActivePointDetailCard(point: DailyCashFlowPoint) {
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyanAccent, BorderSubtle))),
        modifier = Modifier.fillMaxWidth().testTag("active_point_tooltip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DATE: ${point.dateFormatted} (DAY ${point.dayIndex})",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Projected: $${numberFmt.format(point.projectedBalanceUsd.roundToInt())} USD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (point.netDailyFlowUsd != 0.0) {
                    val isPositive = point.netDailyFlowUsd > 0
                    Text(
                        text = "${if (isPositive) "+" else ""}$${numberFmt.format(point.netDailyFlowUsd.roundToInt())} USD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) EmeraldPositive else RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (point.milestoneTitle != null) {
                    Text(
                        text = point.milestoneTitle,
                        fontSize = 8.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Executive Recommendation Banner.
 */
@Composable
private fun ExecutiveRecommendationBanner(report: PredictiveCashFlowReport) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(report.liquidityHealthStatus.color.copy(alpha = 0.08f))
            .border(1.dp, report.liquidityHealthStatus.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Recommendation",
                tint = report.liquidityHealthStatus.color,
                modifier = Modifier.size(16.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TREASURY EXECUTIVE DIRECTIVE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = report.liquidityHealthStatus.color,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = report.statusRecommendation,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Upcoming Critical Cash Events Timeline.
 */
@Composable
private fun UpcomingCashEventsSection(
    events: List<ScheduledCashEvent>,
    onNavigateToInvoices: (() -> Unit)? = null
) {
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPCOMING CASH EVENTS & INVOICES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )

            if (onNavigateToInvoices != null) {
                Text(
                    text = "View Invoices ➔",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onNavigateToInvoices() }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            events.forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (event.isInflow) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (event.isInflow) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = if (event.isInflow) "Inflow" else "Outflow",
                                tint = if (event.isInflow) EmeraldPositive else RoseNegative,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Column {
                            Text(
                                text = event.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${event.date} • ${event.counterpartyOrSource}",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (event.isInflow) "+" else "-"}$${numberFmt.format(event.amountUsd.roundToInt())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (event.isInflow) EmeraldPositive else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(event.confidenceProbability * 100).toInt()}% Conf.",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
