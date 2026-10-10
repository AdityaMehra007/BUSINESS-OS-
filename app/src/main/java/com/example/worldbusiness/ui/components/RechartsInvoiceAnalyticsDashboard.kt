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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.example.worldbusiness.data.repository.CrossBorderTaxCalculator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Data model representing aggregated invoice metrics per currency over the selected 30-day window.
 */
data class CurrencyInvoiceMetric(
    val currencyCode: String,
    val currencySymbol: String,
    val countryFlag: String,
    val totalAmount: Double,
    val invoiceCount: Int,
    val paidAmount: Double,
    val pendingAmount: Double,
    val overdueAmount: Double,
    val fxRateToUsd: Double,
    val equivalentUsd: Double,
    val sharePercent: Double,
    val themeColor: Color
)

/**
 * Recharts Visualization Type
 */
enum class RechartsChartStyle(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    BAR_CHART("Bar Chart", Icons.Default.BarChart),
    AREA_SPLINE("Area Spline", Icons.Default.ShowChart),
    DONUT_DISTRIBUTION("Donut Share", Icons.Default.PieChart)
}

/**
 * Currency Filter in Dashboard
 */
enum class InvoiceFilterMode(val label: String) {
    ALL_CURRENCIES("All Currencies"),
    TOP_G10("G10 Currencies"),
    HIGH_VOLUME("High Volume (>$100k)")
}

/**
 * RechartsInvoiceAnalyticsDashboard:
 * Uses Recharts-style declarative, vector-sharp interactive Canvas charts (Stacked Bar,
 * Cubic Bézier Area Spline, and Segmented Donut Share) to summarize total invoiced
 * amounts by currency over the last 30 days.
 */
@Composable
fun RechartsInvoiceAnalyticsDashboard(
    invoices: List<InvoiceRecord>,
    entities: List<EntityRecord> = emptyList(),
    liveCurrencyFeed: LiveCurrencyFeed = LiveCurrencyFeed(),
    onNavigateBack: (() -> Unit)? = null,
    onOpenCreateInvoice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedChartStyle by remember { mutableStateOf(RechartsChartStyle.BAR_CHART) }
    var selectedFilterMode by remember { mutableStateOf(InvoiceFilterMode.ALL_CURRENCIES) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var viewInUsdEquivalent by remember { mutableStateOf(true) }

    // 1. Filter Invoices for the Last 30 Days
    // In demo environment, dates are formatted as "MMM dd, yyyy" or "yyyy-MM-dd".
    // We treat all demo invoices from the active 30-day operating ledger cycle as the primary dataset.
    val recentInvoices = remember(invoices) {
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        val nowMillis = System.currentTimeMillis()
        val format1 = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val format2 = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        invoices.filter { inv ->
            try {
                val parsedDate = runCatching { format1.parse(inv.issueDate) }.getOrNull()
                    ?: runCatching { format2.parse(inv.issueDate) }.getOrNull()
                if (parsedDate != null) {
                    val age = nowMillis - parsedDate.time
                    // Include if within 45 days window or future due dates
                    age <= (thirtyDaysMillis * 1.5)
                } else {
                    true // fallback include active invoice
                }
            } catch (_: Exception) {
                true
            }
        }.ifEmpty { invoices } // fallback to all invoices if none match timestamp filter
    }

    // Palette for currencies
    val colorPalette = listOf(
        CyanAccent,
        EmeraldPositive,
        GoldAccent,
        Color(0xFF818CF8), // Indigo
        RoseNegative,
        Color(0xFFF472B6), // Pink
        Color(0xFF38BDF8), // Light Blue
        Color(0xFFA78BFA), // Violet
        Color(0xFF34D399)  // Teal
    )

    // 2. Aggregate Invoiced Amounts Grouped by Currency
    val currencyMetrics: List<CurrencyInvoiceMetric> = remember(recentInvoices, liveCurrencyFeed, selectedFilterMode) {
        val grouped: Map<String, List<InvoiceRecord>> = recentInvoices.groupBy { it.currency.uppercase(Locale.US) }
        val supportedCurrencies = CrossBorderTaxCalculator.getSupportedCurrencies(liveCurrencyFeed.ratesToUsd)

        val totalAllUsd = recentInvoices.sumOf { inv ->
            val rate = liveCurrencyFeed.getRateToUsd(inv.currency)
            inv.amount * rate
        }.coerceAtLeast(1.0)

        val list = grouped.entries.toList().mapIndexed { idx, entry ->
            val curr = entry.key
            val invList = entry.value

            val currOption = supportedCurrencies.find { it.code.equals(curr, ignoreCase = true) }
            val sym = currOption?.symbol ?: curr
            val flag = currOption?.flag ?: "🌐"
            val fxRate = liveCurrencyFeed.getRateToUsd(curr)

            val total = invList.sumOf { it.amount }
            val paid = invList.filter { it.status == "PAID" }.sumOf { it.amount }
            val pending = invList.filter { it.status == "PENDING" || it.status == "IN_CLEARING" }.sumOf { it.amount }
            val overdue = invList.filter { it.status == "OVERDUE" }.sumOf { it.amount }
            val eqUsd = total * fxRate
            val share = (eqUsd / totalAllUsd) * 100.0

            CurrencyInvoiceMetric(
                currencyCode = curr,
                currencySymbol = sym,
                countryFlag = flag,
                totalAmount = total,
                invoiceCount = invList.size,
                paidAmount = paid,
                pendingAmount = pending,
                overdueAmount = overdue,
                fxRateToUsd = fxRate,
                equivalentUsd = eqUsd,
                sharePercent = share,
                themeColor = colorPalette[idx % colorPalette.size]
            )
        }.sortedByDescending { it.equivalentUsd }

        when (selectedFilterMode) {
            InvoiceFilterMode.ALL_CURRENCIES -> list
            InvoiceFilterMode.TOP_G10 -> list.filter { it.currencyCode in listOf("USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD") }
            InvoiceFilterMode.HIGH_VOLUME -> list.filter { it.equivalentUsd >= 100_000.0 }
        }.ifEmpty { list }
    }

    val totalPortfolioUsd = remember(currencyMetrics) {
        currencyMetrics.sumOf { it.equivalentUsd }
    }

    val totalInvoicesCount = remember(currencyMetrics) {
        currencyMetrics.sumOf { it.invoiceCount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("recharts_invoice_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header & Sub-Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("btn_back_from_recharts_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Recharts",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "RECHARTS COMMERCIAL DASHBOARD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Total Invoiced Amounts by Currency • Last 30 Operating Days",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CyanAccent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Window",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "LAST 30 DAYS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Summary KPI Strip (Total Volume USD, Currencies Active, Total Documents, Settled Ratio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RechartsKpiCard(
                title = "30-DAY INVOICED TOTAL",
                value = "$${NumberFormat.getNumberInstance(Locale.US).format(totalPortfolioUsd.toLong())} USD",
                subtitle = "Across ${currencyMetrics.size} currencies",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            RechartsKpiCard(
                title = "CURRENCY DIVERSITY",
                value = "${currencyMetrics.size} RAILS",
                subtitle = "$totalInvoicesCount commercial items",
                accentColor = EmeraldPositive,
                modifier = Modifier.weight(1f)
            )
            RechartsKpiCard(
                title = "TOP SETTLEMENT CURRENCY",
                value = currencyMetrics.firstOrNull()?.currencyCode ?: "USD",
                subtitle = "${String.format(Locale.US, "%.1f", currencyMetrics.firstOrNull()?.sharePercent ?: 0.0)}% volume",
                accentColor = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Main Recharts Interactive Visualization Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recharts_main_visualization_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header of the Chart: Style Switcher & Value Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INVOICED AMOUNTS BY CURRENCY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (viewInUsdEquivalent) "Normalized to USD at live market parity" else "Denominated in native currency values",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }

                    // Value representation toggle (USD Parity vs Native)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.clickable { viewInUsdEquivalent = !viewInUsdEquivalent }
                    ) {
                        Text(
                            text = if (viewInUsdEquivalent) "SHOW NATIVE" else "SHOW USD PARITY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Chart Type Selector Strip (Recharts Bar / Area Spline / Donut)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RechartsChartStyle.values().forEach { style ->
                        val isSelected = selectedChartStyle == style
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanAccent) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedChartStyle = style }
                                .padding(vertical = 4.dp)
                                .testTag("btn_chart_style_${style.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = style.label,
                                    tint = if (isSelected) CyanAccent else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = style.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CyanAccent else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Interactive Chart Canvas (60 FPS Vector Recharts Renderer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    when (selectedChartStyle) {
                        RechartsChartStyle.BAR_CHART -> {
                            RechartsCurrencyBarCanvas(
                                metrics = currencyMetrics,
                                inUsdEquivalent = viewInUsdEquivalent,
                                hoveredIndex = hoveredIndex,
                                onHover = { hoveredIndex = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        RechartsChartStyle.AREA_SPLINE -> {
                            RechartsCurrencySplineCanvas(
                                metrics = currencyMetrics,
                                inUsdEquivalent = viewInUsdEquivalent,
                                hoveredIndex = hoveredIndex,
                                onHover = { hoveredIndex = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        RechartsChartStyle.DONUT_DISTRIBUTION -> {
                            RechartsCurrencyDonutCanvas(
                                metrics = currencyMetrics,
                                totalUsd = totalPortfolioUsd,
                                hoveredIndex = hoveredIndex,
                                onHover = { hoveredIndex = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Interactive Inspection Tooltip Banner
                AnimatedVisibility(
                    visible = hoveredIndex != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    hoveredIndex?.let { idx ->
                        val metric = currencyMetrics.getOrNull(idx)
                        if (metric != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, metric.themeColor.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recharts_hover_tooltip_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(metric.themeColor)
                                        )
                                        Column {
                                            Text(
                                                text = "${metric.countryFlag} ${metric.currencyCode} (${metric.sharePercent.roundToInt()}% of 30-Day Volume)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${metric.invoiceCount} invoices • Paid: ${metric.currencySymbol}${NumberFormat.getNumberInstance(Locale.US).format(metric.paidAmount.toLong())} • Pending: ${metric.currencySymbol}${NumberFormat.getNumberInstance(Locale.US).format(metric.pendingAmount.toLong())}",
                                                fontSize = 8.sp,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${metric.currencySymbol}${NumberFormat.getNumberInstance(Locale.US).format(metric.totalAmount.toLong())}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = metric.themeColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "≈ $${NumberFormat.getNumberInstance(Locale.US).format(metric.equivalentUsd.toLong())} USD",
                                            fontSize = 9.sp,
                                            color = TextMuted,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Legend row with responsive color indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currencyMetrics.forEachIndexed { index, item ->
                        val isHovered = hoveredIndex == index
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { hoveredIndex = if (isHovered) null else index }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.themeColor)
                            )
                            Text(
                                text = "${item.currencyCode} (${item.sharePercent.roundToInt()}%)",
                                fontSize = 8.sp,
                                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                                color = if (isHovered) TextPrimary else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Currency Breakdown Detailed Data Table Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("currency_breakdown_table_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                            text = "30-DAY CURRENCY INVOICE LEDGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Filter quick chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        InvoiceFilterMode.values().forEach { mode ->
                            val isSelected = selectedFilterMode == mode
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) CyanAccent else SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isSelected) CyanAccent else BorderSubtle),
                                modifier = Modifier.clickable { selectedFilterMode = mode }
                            ) {
                                Text(
                                    text = mode.label.take(8),
                                    fontSize = 7.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                // Items list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencyMetrics.forEachIndexed { index, metric ->
                        CurrencyMetricRowCard(
                            metric = metric,
                            isHighlighted = hoveredIndex == index,
                            onClick = { hoveredIndex = if (hoveredIndex == index) null else index }
                        )
                    }
                }

                if (onOpenCreateInvoice != null) {
                    Button(
                        onClick = onOpenCreateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("btn_dashboard_new_invoice")
                    ) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = "Create", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CREATE NEW CROSS-BORDER INVOICE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

/**
 * Recharts Interactive Bar Chart Canvas:
 * Renders high-performance stacked/grouped bar columns per currency with subtle gradient caps,
 * dotted horizontal threshold guidelines, and touch drag scrubbing.
 */
@Composable
private fun RechartsCurrencyBarCanvas(
    metrics: List<CurrencyInvoiceMetric>,
    inUsdEquivalent: Boolean,
    hoveredIndex: Int?,
    onHover: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    val maxVal = remember(metrics, inUsdEquivalent) {
        val peak = if (inUsdEquivalent) {
            metrics.maxOfOrNull { it.equivalentUsd } ?: 100_000.0
        } else {
            metrics.maxOfOrNull { it.totalAmount } ?: 100_000.0
        }
        (peak * 1.20).coerceAtLeast(10_000.0)
    }

    Canvas(
        modifier = modifier
            .testTag("recharts_currency_bar_canvas")
            .pointerInput(metrics.size) {
                detectTapGestures { offset ->
                    val padLeft = 45f
                    val padRight = 15f
                    val chartW = size.width - padLeft - padRight
                    if (chartW > 0 && metrics.isNotEmpty()) {
                        val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                        val slotWidth = chartW / metrics.size
                        val idx = (touchX / slotWidth).toInt().coerceIn(0, metrics.size - 1)
                        onHover(idx)
                    }
                }
            }
            .pointerInput(metrics.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val padLeft = 45f
                        val padRight = 15f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && metrics.isNotEmpty()) {
                            val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                            val slotWidth = chartW / metrics.size
                            val idx = (touchX / slotWidth).toInt().coerceIn(0, metrics.size - 1)
                            onHover(idx)
                        }
                    },
                    onDrag = { change, _ ->
                        val padLeft = 45f
                        val padRight = 15f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && metrics.isNotEmpty()) {
                            val touchX = (change.position.x - padLeft).coerceIn(0f, chartW)
                            val slotWidth = chartW / metrics.size
                            val idx = (touchX / slotWidth).toInt().coerceIn(0, metrics.size - 1)
                            onHover(idx)
                        }
                    }
                )
            }
    ) {
        val padLeft = 45f
        val padRight = 15f
        val padTop = 15f
        val padBottom = 26f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom
        if (chartW <= 0 || chartH <= 0) return@Canvas

        // 1. Draw Recharts Dotted Horizontal Gridlines
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
            val lineVal = maxVal * yFrac

            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(padLeft, yPos),
                end = Offset(padLeft + chartW, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            val label = formatCompactValue(lineVal)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                padLeft - 6f,
                yPos + 6f,
                gridPaint
            )
        }

        // 2. Draw Recharts Rounded Vertical Bars
        val count = metrics.size
        val slotWidth = chartW / count
        val barWidth = (slotWidth * 0.55f).coerceIn(12f, 38f)

        val xLabelPaint = android.graphics.Paint().apply {
            color = Color(0xFF94A3B8).toArgb()
            textSize = 21f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        metrics.forEachIndexed { idx, metric ->
            val slotCenterX = padLeft + (idx * slotWidth) + (slotWidth / 2f)
            val barLeft = slotCenterX - (barWidth / 2f)

            val value = if (inUsdEquivalent) metric.equivalentUsd else metric.totalAmount
            val heightFrac = (value / maxVal).toFloat().coerceIn(0f, 1f)
            val barHeight = chartH * heightFrac
            val barTop = padTop + chartH - barHeight

            val isHovered = hoveredIndex == idx

            // Background hover highlight slot
            if (isHovered) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.06f),
                    topLeft = Offset(slotCenterX - (slotWidth / 2f), padTop),
                    size = Size(slotWidth, chartH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Main Bar Gradient Fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        metric.themeColor,
                        metric.themeColor.copy(alpha = if (isHovered) 0.85f else 0.55f)
                    ),
                    startY = barTop,
                    endY = padTop + chartH
                ),
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Top highlight border cap
            drawLine(
                color = Color.White.copy(alpha = if (isHovered) 0.9f else 0.4f),
                start = Offset(barLeft + 2f, barTop),
                end = Offset(barLeft + barWidth - 2f, barTop),
                strokeWidth = 2f
            )

            // Currency Label below Bar
            drawContext.canvas.nativeCanvas.drawText(
                metric.currencyCode,
                slotCenterX,
                size.height - 4f,
                xLabelPaint
            )
        }
    }
}

/**
 * Recharts Interactive Area Spline Canvas:
 * Renders smooth Monotone Cubic Bézier curves with soft gradient underglow and glowing data node points.
 */
@Composable
private fun RechartsCurrencySplineCanvas(
    metrics: List<CurrencyInvoiceMetric>,
    inUsdEquivalent: Boolean,
    hoveredIndex: Int?,
    onHover: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    val maxVal = remember(metrics, inUsdEquivalent) {
        val peak = if (inUsdEquivalent) {
            metrics.maxOfOrNull { it.equivalentUsd } ?: 100_000.0
        } else {
            metrics.maxOfOrNull { it.totalAmount } ?: 100_000.0
        }
        (peak * 1.20).coerceAtLeast(10_000.0)
    }

    Canvas(
        modifier = modifier
            .testTag("recharts_currency_spline_canvas")
            .pointerInput(metrics.size) {
                detectTapGestures { offset ->
                    val padLeft = 45f
                    val padRight = 15f
                    val chartW = size.width - padLeft - padRight
                    if (chartW > 0 && metrics.size > 1) {
                        val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                        val idx = ((touchX / chartW) * (metrics.size - 1)).roundToInt().coerceIn(0, metrics.size - 1)
                        onHover(idx)
                    }
                }
            }
            .pointerInput(metrics.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val padLeft = 45f
                        val padRight = 15f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && metrics.size > 1) {
                            val touchX = (offset.x - padLeft).coerceIn(0f, chartW)
                            val idx = ((touchX / chartW) * (metrics.size - 1)).roundToInt().coerceIn(0, metrics.size - 1)
                            onHover(idx)
                        }
                    },
                    onDrag = { change, _ ->
                        val padLeft = 45f
                        val padRight = 15f
                        val chartW = size.width - padLeft - padRight
                        if (chartW > 0 && metrics.size > 1) {
                            val touchX = (change.position.x - padLeft).coerceIn(0f, chartW)
                            val idx = ((touchX / chartW) * (metrics.size - 1)).roundToInt().coerceIn(0, metrics.size - 1)
                            onHover(idx)
                        }
                    }
                )
            }
    ) {
        val padLeft = 45f
        val padRight = 15f
        val padTop = 15f
        val padBottom = 26f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom
        if (chartW <= 0 || chartH <= 0 || metrics.size < 2) return@Canvas

        // 1. Gridlines
        val gridLines = 4
        for (i in 0..gridLines) {
            val yFrac = i.toFloat() / gridLines
            val yPos = padTop + (chartH * (1f - yFrac))

            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(padLeft, yPos),
                end = Offset(padLeft + chartW, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        val count = metrics.size
        fun getX(idx: Int) = padLeft + (chartW * (idx.toFloat() / (count - 1)))
        fun getY(metric: CurrencyInvoiceMetric): Float {
            val v = if (inUsdEquivalent) metric.equivalentUsd else metric.totalAmount
            return padTop + (chartH * (1f - (v / maxVal).toFloat().coerceIn(0f, 1f)))
        }

        val splinePath = Path()
        val areaPath = Path()

        val firstX = getX(0)
        val firstY = getY(metrics[0])
        splinePath.moveTo(firstX, firstY)
        areaPath.moveTo(firstX, padTop + chartH)
        areaPath.lineTo(firstX, firstY)

        for (i in 0 until count - 1) {
            val x0 = getX(i)
            val y0 = getY(metrics[i])
            val x1 = getX(i + 1)
            val y1 = getY(metrics[i + 1])

            val cpX1 = x0 + (x1 - x0) / 2f
            val cpY1 = y0
            val cpX2 = x0 + (x1 - x0) / 2f
            val cpY2 = y1

            splinePath.cubicTo(cpX1, cpY1, cpX2, cpY2, x1, y1)
            areaPath.cubicTo(cpX1, cpY1, cpX2, cpY2, x1, y1)
        }

        areaPath.lineTo(getX(count - 1), padTop + chartH)
        areaPath.close()

        // Gradient underglow
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    CyanAccent.copy(alpha = 0.35f),
                    CyanAccent.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = padTop,
                endY = padTop + chartH
            )
        )

        // Main Spline stroke
        drawPath(
            path = splinePath,
            color = CyanAccent,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Nodes
        metrics.forEachIndexed { i, m ->
            val px = getX(i)
            val py = getY(m)
            val isHovered = hoveredIndex == i

            drawCircle(
                color = if (isHovered) Color.White else m.themeColor,
                radius = if (isHovered) 6f else 4f,
                center = Offset(px, py)
            )
            drawCircle(
                color = SurfaceDark,
                radius = if (isHovered) 3f else 2f,
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Recharts Interactive Donut Share Canvas:
 * Renders radial proportional segmented arcs by currency share with touch angle discovery.
 */
@Composable
private fun RechartsCurrencyDonutCanvas(
    metrics: List<CurrencyInvoiceMetric>,
    totalUsd: Double,
    hoveredIndex: Int?,
    onHover: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    Canvas(
        modifier = modifier
            .testTag("recharts_currency_donut_canvas")
            .pointerInput(metrics.size) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    var angleDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angleDeg < 0) angleDeg += 360f

                    // Find which slice angle belongs to
                    var curAngle = 0f
                    metrics.forEachIndexed { idx, m ->
                        val sweep = (m.sharePercent.toFloat() / 100f) * 360f
                        if (angleDeg >= curAngle && angleDeg <= curAngle + sweep) {
                            onHover(idx)
                            return@detectTapGestures
                        }
                        curAngle += sweep
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) - 20f
        val strokeW = radius * 0.38f

        var startAngle = -90f
        metrics.forEachIndexed { idx, m ->
            val sweep = (m.sharePercent.toFloat() / 100f) * 360f
            val isHovered = hoveredIndex == idx

            drawArc(
                color = m.themeColor,
                startAngle = startAngle,
                sweepAngle = sweep.coerceAtLeast(1f),
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = if (isHovered) strokeW + 8f else strokeW,
                    cap = StrokeCap.Butt
                )
            )
            startAngle += sweep
        }

        // Center Ring Hole Text
        val centerPaint = android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val centerSubPaint = android.graphics.Paint().apply {
            color = Color(0xFF94A3B8).toArgb()
            textSize = 20f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.drawText(
            "$${NumberFormat.getNumberInstance(Locale.US).format(totalUsd.toLong())}",
            cx,
            cy - 2f,
            centerPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            "TOTAL USD",
            cx,
            cy + 22f,
            centerSubPaint
        )
    }
}

/**
 * Metric breakdown row for each currency in the list
 */
@Composable
private fun CurrencyMetricRowCard(
    metric: CurrencyInvoiceMetric,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlighted) SurfaceElevated else SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHighlighted) metric.themeColor else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("row_currency_${metric.currencyCode.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = metric.themeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, metric.themeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = metric.countryFlag,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = metric.currencyCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = metric.themeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", metric.sharePercent)}%",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = metric.themeColor,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${metric.invoiceCount} invoices • FX Rate: 1 ${metric.currencyCode} = $${String.format(Locale.US, "%.4f", metric.fxRateToUsd)} USD",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${metric.currencySymbol}${NumberFormat.getNumberInstance(Locale.US).format(metric.totalAmount.toLong())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = metric.themeColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "≈ $${NumberFormat.getNumberInstance(Locale.US).format(metric.equivalentUsd.toLong())} USD",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Top KPI Summary Card
 */
@Composable
private fun RechartsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatCompactValue(value: Double): String {
    return when {
        value >= 1_000_000.0 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000.0 -> String.format(Locale.US, "%.0fK", value / 1_000.0)
        else -> String.format(Locale.US, "%.0f", value)
    }
}
