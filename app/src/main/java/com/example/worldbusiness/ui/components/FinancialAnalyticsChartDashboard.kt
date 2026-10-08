package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FinancialAnalyticsOverview
import com.example.worldbusiness.data.model.FinancialChartType
import com.example.worldbusiness.data.model.FinancialTimeRange
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.FinancialAnalyticsEngine
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enterprise Recharts-Inspired Financial Analytics Visualization Dashboard.
 * Visualizes Monthly Revenue, Tax Liabilities, and Invoice Settlement Trends
 * with 60 FPS Compose Canvas rendering, interactive crosshairs, and multi-series breakdowns.
 */
@Composable
fun FinancialAnalyticsChartDashboard(
    invoices: List<InvoiceRecord>,
    entities: List<EntityRecord>,
    fxBalances: List<FxBalanceRecord>,
    modifier: Modifier = Modifier,
    onNavigateToInvoices: (() -> Unit)? = null
) {
    var selectedRange by remember { mutableStateOf(FinancialTimeRange.RANGE_6M) }
    var selectedChartType by remember { mutableStateOf(FinancialChartType.REVENUE) }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    val analytics = remember(invoices, entities, fxBalances, selectedRange) {
        FinancialAnalyticsEngine.generateAnalytics(invoices, entities, fxBalances, selectedRange)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_analytics_dashboard"),
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
            // Header: Title & Time Range Switcher
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
                            .background(SurfaceElevated)
                            .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Recharts Financial Analytics",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FINANCIAL ANALYTICS & TRENDS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Recharts-Inspired Visualization • Multi-Currency Ledger",
                            fontSize = 9.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Range Selector (6M, 12M, YTD)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FinancialTimeRange.values().forEach { range ->
                        val isSelected = selectedRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CyanAccent else Color.Transparent)
                                .clickable {
                                    selectedRange = range
                                    scrubbedIndex = null
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("btn_range_${range.label.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range.label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Chart Type Segmented Switcher (Revenue, Tax, Invoices, Composite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FinancialChartType.values().forEach { chartType ->
                    val isSelected = selectedChartType == chartType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanAccent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, if (isSelected) CyanAccent.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedChartType = chartType
                                scrubbedIndex = null
                            }
                            .padding(vertical = 6.dp)
                            .testTag("chart_tab_${chartType.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chartType.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CyanAccent else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Dynamic Chart Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                RechartsInteractiveCanvas(
                    analytics = analytics,
                    chartType = selectedChartType,
                    scrubbedIndex = scrubbedIndex,
                    onScrub = { scrubbedIndex = it },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Tooltip inspection banner if user touches/scrubs the chart
            AnimatedVisibility(
                visible = scrubbedIndex != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                scrubbedIndex?.let { idx ->
                    ScrubbedDataInspectionBanner(
                        analytics = analytics,
                        chartType = selectedChartType,
                        index = idx
                    )
                }
            }

            // Legend Strip
            ChartLegendRow(chartType = selectedChartType)

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))

            // Executive 4-Pillar Financial Summary Strip
            FinancialSummaryKpiStrip(
                analytics = analytics,
                onNavigateToInvoices = onNavigateToInvoices
            )
        }
    }
}

/**
 * High-performance 60 FPS Recharts-style Canvas Renderer.
 * Supports smooth Bézier curves, stacked bars, horizontal gridlines, and touch scrubbing crosshairs.
 */
@Composable
private fun RechartsInteractiveCanvas(
    analytics: FinancialAnalyticsOverview,
    chartType: FinancialChartType,
    scrubbedIndex: Int?,
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = analytics.revenueSeries.size
    if (count == 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Canvas(
        modifier = modifier
            .testTag("recharts_financial_canvas")
            .semantics { contentDescription = "Recharts Financial Analytics Interactive Graph" }
            .pointerInput(count) {
                detectTapGestures { offset ->
                    val padLeft = 45f
                    val padRight = 20f
                    val chartW = size.width - padLeft - padRight
                    if (chartW > 0 && count > 1) {
                        val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                        val fraction = touchX / chartW
                        val idx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                        onScrub(idx)
                    }
                }
            }
            .pointerInput(count) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val padLeft = 45f
                        val padRight = 20f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && count > 1) {
                            val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                            val idx = ((touchX / chartW) * (count - 1)).roundToInt().coerceIn(0, count - 1)
                            onScrub(idx)
                        }
                    },
                    onDrag = { change, _ ->
                        val padLeft = 45f
                        val padRight = 20f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && count > 1) {
                            val touchX = (change.position.x - padLeft).coerceIn(0f, chartW)
                            val idx = ((touchX / chartW) * (count - 1)).roundToInt().coerceIn(0, count - 1)
                            onScrub(idx)
                        }
                    }
                )
            }
    ) {
        val padLeft = 45f
        val padRight = 20f
        val padTop = 15f
        val padBottom = 26f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom
        if (chartW <= 0 || chartH <= 0) return@Canvas

        // 1. Compute dynamic Max value for scaling
        val maxValue = when (chartType) {
            FinancialChartType.REVENUE -> {
                analytics.revenueSeries.maxOfOrNull { it.grossRevenue }?.times(1.15) ?: 1_000_000.0
            }
            FinancialChartType.TAX -> {
                analytics.taxSeries.maxOfOrNull { it.totalLiability }?.times(1.20) ?: 300_000.0
            }
            FinancialChartType.INVOICES -> {
                analytics.invoiceSeries.maxOfOrNull { it.totalVolume }?.times(1.15) ?: 1_000_000.0
            }
            FinancialChartType.COMPOSITE -> {
                analytics.revenueSeries.maxOfOrNull { it.grossRevenue }?.times(1.15) ?: 1_000_000.0
            }
        }.coerceAtLeast(100_000.0)

        // 2. Draw Horizontal Gridlines & Y-Axis Labels
        val gridLines = 4
        val gridPaint = android.graphics.Paint().apply {
            color = Color(0xFF64748B).toArgb()
            textSize = 20f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        for (i in 0..gridLines) {
            val yFrac = i.toFloat() / gridLines
            val yPos = padTop + (chartH * (1f - yFrac))
            val lineVal = maxValue * yFrac

            // Dotted horizontal gridline
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.6f),
                start = Offset(padLeft, yPos),
                end = Offset(padLeft + chartW, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Y-axis tick text
            val labelText = formatCompactK(lineVal)
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                padLeft - 6f,
                yPos + 6f,
                gridPaint
            )
        }

        // 3. Draw Renderers Based on Selected Chart Type
        when (chartType) {
            FinancialChartType.REVENUE -> {
                drawRevenueAreaSpline(
                    series = analytics.revenueSeries,
                    maxValue = maxValue,
                    padLeft = padLeft,
                    padTop = padTop,
                    chartW = chartW,
                    chartH = chartH
                )
            }
            FinancialChartType.TAX -> {
                drawTaxStackedBars(
                    series = analytics.taxSeries,
                    maxValue = maxValue,
                    padLeft = padLeft,
                    padTop = padTop,
                    chartW = chartW,
                    chartH = chartH
                )
            }
            FinancialChartType.INVOICES -> {
                drawInvoiceStatusArea(
                    series = analytics.invoiceSeries,
                    maxValue = maxValue,
                    padLeft = padLeft,
                    padTop = padTop,
                    chartW = chartW,
                    chartH = chartH
                )
            }
            FinancialChartType.COMPOSITE -> {
                drawCompositeRevenueAndTax(
                    revenueSeries = analytics.revenueSeries,
                    taxSeries = analytics.taxSeries,
                    maxValue = maxValue,
                    padLeft = padLeft,
                    padTop = padTop,
                    chartW = chartW,
                    chartH = chartH
                )
            }
        }

        // 4. Draw X-Axis Month Labels
        val xLabelPaint = android.graphics.Paint().apply {
            color = Color(0xFF94A3B8).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (i in 0 until count) {
            val xPos = padLeft + (chartW * (i.toFloat() / (count - 1).coerceAtLeast(1)))
            val label = analytics.revenueSeries[i].monthShort
            drawContext.canvas.nativeCanvas.drawText(
                label,
                xPos,
                size.height - 4f,
                xLabelPaint
            )
        }

        // 5. Draw Touch / Scrub Crosshair and Halo Indicator
        scrubbedIndex?.let { sIdx ->
            if (sIdx in 0 until count) {
                val scrubX = padLeft + (chartW * (sIdx.toFloat() / (count - 1).coerceAtLeast(1)))

                // Vertical Crosshair Line
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(scrubX, padTop),
                    end = Offset(scrubX, padTop + chartH),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )

                // Highlight Dot on top line
                val valTop = when (chartType) {
                    FinancialChartType.REVENUE -> analytics.revenueSeries[sIdx].grossRevenue
                    FinancialChartType.TAX -> analytics.taxSeries[sIdx].totalLiability
                    FinancialChartType.INVOICES -> analytics.invoiceSeries[sIdx].totalVolume
                    FinancialChartType.COMPOSITE -> analytics.revenueSeries[sIdx].grossRevenue
                }
                val dotY = padTop + (chartH * (1f - (valTop / maxValue).toFloat().coerceIn(0f, 1f)))

                // Pulsing Halo
                drawCircle(
                    color = CyanAccent.copy(alpha = 0.35f),
                    radius = pulseRadius,
                    center = Offset(scrubX, dotY)
                )
                // Solid Center
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(scrubX, dotY)
                )
                drawCircle(
                    color = CyanAccent,
                    radius = 2.5f,
                    center = Offset(scrubX, dotY)
                )
            }
        }
    }
}

/**
 * Monotone Cubic Bézier Spline with Gradient Fill for Monthly Revenue
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRevenueAreaSpline(
    series: List<com.example.worldbusiness.data.model.MonthlyRevenueData>,
    maxValue: Double,
    padLeft: Float,
    padTop: Float,
    chartW: Float,
    chartH: Float
) {
    val count = series.size
    if (count < 2) return

    val splinePath = Path()
    val areaPath = Path()
    val opexPath = Path()

    fun getX(idx: Int) = padLeft + (chartW * (idx.toFloat() / (count - 1)))
    fun getY(value: Double) = padTop + (chartH * (1f - (value / maxValue).toFloat().coerceIn(0f, 1f)))

    // Build Gross Revenue Spline
    val firstX = getX(0)
    val firstY = getY(series[0].grossRevenue)

    splinePath.moveTo(firstX, firstY)
    areaPath.moveTo(firstX, padTop + chartH)
    areaPath.lineTo(firstX, firstY)

    for (i in 0 until count - 1) {
        val x0 = getX(i)
        val y0 = getY(series[i].grossRevenue)
        val x1 = getX(i + 1)
        val y1 = getY(series[i + 1].grossRevenue)

        val cpX1 = x0 + (x1 - x0) / 2f
        val cpY1 = y0
        val cpX2 = x0 + (x1 - x0) / 2f
        val cpY2 = y1

        splinePath.cubicTo(cpX1, cpY1, cpX2, cpY2, x1, y1)
        areaPath.cubicTo(cpX1, cpY1, cpX2, cpY2, x1, y1)
    }

    areaPath.lineTo(getX(count - 1), padTop + chartH)
    areaPath.close()

    // 1. Draw Shaded Gradient Area
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                CyanAccent.copy(alpha = 0.40f),
                CyanAccent.copy(alpha = 0.12f),
                Color.Transparent
            ),
            startY = padTop,
            endY = padTop + chartH
        )
    )

    // 2. Draw Main Spline Line
    drawPath(
        path = splinePath,
        color = CyanAccent,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // 3. Draw Operating Expenses Comparison Line (Muted Dashed)
    opexPath.moveTo(firstX, getY(series[0].operatingExpenses))
    for (i in 0 until count - 1) {
        val x0 = getX(i)
        val y0 = getY(series[i].operatingExpenses)
        val x1 = getX(i + 1)
        val y1 = getY(series[i + 1].operatingExpenses)

        val cpX1 = x0 + (x1 - x0) / 2f
        val cpY1 = y0
        val cpX2 = x0 + (x1 - x0) / 2f
        val cpY2 = y1
        opexPath.cubicTo(cpX1, cpY1, cpX2, cpY2, x1, y1)
    }

    drawPath(
        path = opexPath,
        color = RoseNegative.copy(alpha = 0.75f),
        style = Stroke(
            width = 2f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    )
}

/**
 * Clustered / Stacked Bars for Tax Liabilities
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTaxStackedBars(
    series: List<com.example.worldbusiness.data.model.MonthlyTaxLiabilityData>,
    maxValue: Double,
    padLeft: Float,
    padTop: Float,
    chartW: Float,
    chartH: Float
) {
    val count = series.size
    val barWidth = (chartW / count) * 0.55f

    series.forEachIndexed { i, tax ->
        val centerX = padLeft + (chartW * (i.toFloat() / (count - 1).coerceAtLeast(1)))
        val barLeft = centerX - (barWidth / 2f)

        val vatH = (chartH * (tax.vatGstAmount / maxValue).toFloat()).coerceAtLeast(2f)
        val citH = (chartH * (tax.corporateTaxAmount / maxValue).toFloat()).coerceAtLeast(2f)
        val whtH = (chartH * (tax.withholdingTaxAmount / maxValue).toFloat()).coerceAtLeast(2f)

        val baseY = padTop + chartH

        // 1. VAT/GST Segment (Gold)
        val vatY = baseY - vatH
        drawRoundRect(
            color = GoldAccent,
            topLeft = Offset(barLeft, vatY),
            size = Size(barWidth, vatH),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // 2. Corporate Tax Segment (Cyan)
        val citY = vatY - citH
        drawRoundRect(
            color = CyanAccent,
            topLeft = Offset(barLeft, citY),
            size = Size(barWidth, citH),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // 3. Withholding Tax Segment (Rose)
        val whtY = citY - whtH
        drawRoundRect(
            color = RoseNegative,
            topLeft = Offset(barLeft, whtY),
            size = Size(barWidth, whtH),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

/**
 * Invoice Status Trend Area & Multi-Series Flow
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInvoiceStatusArea(
    series: List<com.example.worldbusiness.data.model.InvoiceStatusTrendData>,
    maxValue: Double,
    padLeft: Float,
    padTop: Float,
    chartW: Float,
    chartH: Float
) {
    val count = series.size
    if (count < 2) return

    val paidPath = Path()
    val clearingPath = Path()

    fun getX(idx: Int) = padLeft + (chartW * (idx.toFloat() / (count - 1)))
    fun getY(value: Double) = padTop + (chartH * (1f - (value / maxValue).toFloat().coerceIn(0f, 1f)))

    paidPath.moveTo(getX(0), getY(series[0].paidVolume))
    clearingPath.moveTo(getX(0), getY(series[0].paidVolume + series[0].inClearingVolume))

    for (i in 0 until count - 1) {
        val x0 = getX(i)
        val x1 = getX(i + 1)
        val midX = x0 + (x1 - x0) / 2f

        val py0 = getY(series[i].paidVolume)
        val py1 = getY(series[i + 1].paidVolume)
        paidPath.cubicTo(midX, py0, midX, py1, x1, py1)

        val cy0 = getY(series[i].paidVolume + series[i].inClearingVolume)
        val cy1 = getY(series[i + 1].paidVolume + series[i + 1].inClearingVolume)
        clearingPath.cubicTo(midX, cy0, midX, cy1, x1, cy1)
    }

    // Draw In-Clearing Upper Line
    drawPath(
        path = clearingPath,
        color = CyanAccent,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Draw Paid Primary Line
    drawPath(
        path = paidPath,
        color = EmeraldPositive,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

/**
 * Dual-Axis Composite: Revenue Columns + Tax Liabilities Spline Line
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompositeRevenueAndTax(
    revenueSeries: List<com.example.worldbusiness.data.model.MonthlyRevenueData>,
    taxSeries: List<com.example.worldbusiness.data.model.MonthlyTaxLiabilityData>,
    maxValue: Double,
    padLeft: Float,
    padTop: Float,
    chartW: Float,
    chartH: Float
) {
    val count = revenueSeries.size
    val barWidth = (chartW / count) * 0.45f

    // 1. Draw Revenue Bars
    revenueSeries.forEachIndexed { i, rev ->
        val centerX = padLeft + (chartW * (i.toFloat() / (count - 1).coerceAtLeast(1)))
        val barLeft = centerX - (barWidth / 2f)
        val barH = (chartH * (rev.grossRevenue / maxValue).toFloat()).coerceAtLeast(4f)
        val barY = (padTop + chartH) - barH

        drawRoundRect(
            color = CyanAccent.copy(alpha = 0.35f),
            topLeft = Offset(barLeft, barY),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // 2. Draw Tax Spline Line overlay
    if (taxSeries.size >= 2) {
        val taxPath = Path()
        fun getX(idx: Int) = padLeft + (chartW * (idx.toFloat() / (count - 1)))
        fun getY(valTax: Double) = padTop + (chartH * (1f - (valTax / maxValue).toFloat().coerceIn(0f, 1f)))

        taxPath.moveTo(getX(0), getY(taxSeries[0].totalLiability))
        for (i in 0 until count - 1) {
            val x0 = getX(i)
            val y0 = getY(taxSeries[i].totalLiability)
            val x1 = getX(i + 1)
            val y1 = getY(taxSeries[i + 1].totalLiability)
            val midX = x0 + (x1 - x0) / 2f
            taxPath.cubicTo(midX, y0, midX, y1, x1, y1)
        }

        drawPath(
            path = taxPath,
            color = GoldAccent,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Floating / Scrubbed Detail Tooltip Banner
 */
@Composable
private fun ScrubbedDataInspectionBanner(
    analytics: FinancialAnalyticsOverview,
    chartType: FinancialChartType,
    index: Int
) {
    val rev = analytics.revenueSeries.getOrNull(index) ?: return
    val tax = analytics.taxSeries.getOrNull(index)
    val inv = analytics.invoiceSeries.getOrNull(index)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scrubbed_tooltip_card"),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariantDark,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyanAccent, BorderSubtle)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INSPECTION: ${rev.month.uppercase()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = when (chartType) {
                        FinancialChartType.REVENUE -> "Gross: ${formatFullCurrency(rev.grossRevenue)} • Net: ${formatFullCurrency(rev.netRevenue)}"
                        FinancialChartType.TAX -> "Total Tax: ${formatFullCurrency(tax?.totalLiability ?: 0.0)} (Status: ${tax?.complianceStatus})"
                        FinancialChartType.INVOICES -> "Paid: ${formatFullCurrency(inv?.paidVolume ?: 0.0)} • In Clearing: ${formatFullCurrency(inv?.inClearingVolume ?: 0.0)}"
                        FinancialChartType.COMPOSITE -> "Revenue: ${formatFullCurrency(rev.grossRevenue)} • Total Tax: ${formatFullCurrency(tax?.totalLiability ?: 0.0)}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // MoM Trend Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isPositive = rev.growthRatePercent >= 0
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = "Growth",
                    tint = if (isPositive) EmeraldPositive else RoseNegative,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", rev.growthRatePercent)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) EmeraldPositive else RoseNegative,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Dynamic Legend Row matching the active chart mode
 */
@Composable
private fun ChartLegendRow(chartType: FinancialChartType) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (chartType) {
            FinancialChartType.REVENUE -> {
                LegendItem(color = CyanAccent, label = "Gross Revenue")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = RoseNegative.copy(alpha = 0.75f), label = "Operating Expenses", isDashed = true)
            }
            FinancialChartType.TAX -> {
                LegendItem(color = GoldAccent, label = "VAT/GST")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = CyanAccent, label = "Corporate Tax")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = RoseNegative, label = "WHT Deduction")
            }
            FinancialChartType.INVOICES -> {
                LegendItem(color = EmeraldPositive, label = "Settled (Paid)")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem(color = CyanAccent, label = "In Interbank Clearing")
            }
            FinancialChartType.COMPOSITE -> {
                LegendItem(color = CyanAccent.copy(alpha = 0.6f), label = "Gross Revenue (Bars)")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = GoldAccent, label = "Total Tax Liabilities (Line)")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = if (isDashed) 14.dp else 10.dp, height = if (isDashed) 2.dp else 10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Executive Summary KPI cards below the visualization
 */
@Composable
private fun FinancialSummaryKpiStrip(
    analytics: FinancialAnalyticsOverview,
    onNavigateToInvoices: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card 1: Trailing Revenue
        SummaryPillCard(
            modifier = Modifier.weight(1f),
            title = "TRAILING REVENUE",
            value = analytics.formatRevenue(),
            subtext = "+${analytics.yoyRevenueGrowthPercent}% YoY",
            valueColor = CyanAccent,
            icon = Icons.Default.TrendingUp,
            iconTint = EmeraldPositive
        )

        // Card 2: Tax Liabilities
        SummaryPillCard(
            modifier = Modifier.weight(1f),
            title = "TAX ACCRUALS",
            value = analytics.formatTax(),
            subtext = "Statutory Reserves",
            valueColor = GoldAccent,
            icon = Icons.Default.AccountBalance,
            iconTint = GoldAccent
        )

        // Card 3: In Clearing / DSO
        SummaryPillCard(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onNavigateToInvoices != null) { onNavigateToInvoices?.invoke() },
            title = "SETTLEMENT VELOCITY",
            value = "${analytics.averageDsoDays} Days DSO",
            subtext = "${analytics.formatInClearing()} In-Flight",
            valueColor = EmeraldPositive,
            icon = Icons.Default.Speed,
            iconTint = EmeraldPositive
        )
    }
}

@Composable
private fun SummaryPillCard(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                fontSize = 8.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatCompactK(value: Double): String {
    return when {
        value >= 1_000_000 -> "$${String.format(Locale.US, "%.1f", value / 1_000_000)}M"
        value >= 1_000 -> "$${(value / 1_000).roundToInt()}k"
        else -> "$${value.roundToInt()}"
    }
}

private fun formatFullCurrency(value: Double): String {
    return "$${NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(value.roundToInt())}"
}
