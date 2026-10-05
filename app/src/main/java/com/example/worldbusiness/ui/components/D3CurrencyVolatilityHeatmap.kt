package com.example.worldbusiness.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.ui.theme.WarningAmber
import com.example.worldbusiness.data.model.HedgingStrategyType
import com.example.worldbusiness.data.model.HedgingUrgency
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Historical Time Horizon for Volatility Calculation
 */
enum class VolatilityTimeHorizon(val code: String, val label: String, val days: Int) {
    ONE_WEEK("1W", "7-Day Realized", 7),
    ONE_MONTH("1M", "30-Day Realized", 30),
    THREE_MONTHS("3M", "90-Day Standard", 90),
    ONE_YEAR("1Y", "365-Day Macro", 365)
}

/**
 * Presentation View for Volatility Heatmap
 */
enum class HeatmapLayoutMode(val label: String) {
    CROSS_MATRIX("Cross-Currency Matrix"),
    RANKED_PAIRS("Hedging Candidates")
}

/**
 * Rendering Engine (Interactive Canvas Vector Matrix or D3.js SVG WebView)
 */
enum class D3HeatmapEngine(val label: String) {
    CANVAS_MATRIX("Canvas Vector Matrix"),
    D3_SVG_WEB("D3.js SVG Visualizer")
}

/**
 * Model representing exchange rate volatility & hedging recommendations for a currency pair
 */
data class CurrencyPairVolatility(
    val base: String,
    val quote: String,
    val pairSymbol: String,
    val vol1W: Double,
    val vol1M: Double,
    val vol3M: Double,
    val vol1Y: Double,
    val impliedVol: Double,
    val dailyPipsRange: Double,
    val urgency: HedgingUrgency,
    val recommendedStrategy: HedgingStrategyType,
    val strategyReasoning: String,
    val var95PerMillion: Double
) {
    fun getVolForHorizon(horizon: VolatilityTimeHorizon): Double {
        return when (horizon) {
            VolatilityTimeHorizon.ONE_WEEK -> vol1W
            VolatilityTimeHorizon.ONE_MONTH -> vol1M
            VolatilityTimeHorizon.THREE_MONTHS -> vol3M
            VolatilityTimeHorizon.ONE_YEAR -> vol1Y
        }
    }
}

/**
 * Enterprise D3-Based Currency Volatility Heatmap Widget.
 *
 * Visualizes historical exchange rate volatility across major currency pairs for informed hedging.
 * Provides color-coded risk gradients, Value-at-Risk (VaR) estimation, and derivative strategy recommendations.
 */
@Composable
fun D3CurrencyVolatilityHeatmap(
    onConfigureHedge: ((pair: String, strategy: HedgingStrategyType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedHorizon by remember { mutableStateOf(VolatilityTimeHorizon.THREE_MONTHS) }
    var layoutMode by remember { mutableStateOf(HeatmapLayoutMode.CROSS_MATRIX) }
    var engineMode by remember { mutableStateOf(D3HeatmapEngine.CANVAS_MATRIX) }
    var selectedPair by remember { mutableStateOf<CurrencyPairVolatility?>(null) }

    val majorCurrencies = remember { listOf("USD", "EUR", "GBP", "JPY", "CHF", "AUD", "CAD", "SGD") }
    val volatilityData = remember { D3VolatilityDataSet.ALL_PAIRS }

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("d3_volatility_heatmap_widget"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Widget Header & Controls
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
                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "D3 VOLATILITY HEATMAP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HEDGING ADVISORY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Historical Exchange Rate Variance & Value-at-Risk Engine",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            // Engine Mode Switcher
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .clickable {
                        engineMode = if (engineMode == D3HeatmapEngine.CANVAS_MATRIX) {
                            D3HeatmapEngine.D3_SVG_WEB
                        } else {
                            D3HeatmapEngine.CANVAS_MATRIX
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("btn_toggle_d3_engine")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                    Text(
                        text = if (engineMode == D3HeatmapEngine.CANVAS_MATRIX) "SVG D3" else "CANVAS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Toolbar: Horizon Selector + Layout Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horizon Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                VolatilityTimeHorizon.values().forEach { horizon ->
                    val isSelected = selectedHorizon == horizon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanAccent else SurfaceElevated)
                            .border(0.5.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { selectedHorizon = horizon }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("horizon_selector_${horizon.code.lowercase(Locale.US)}")
                    ) {
                        Text(
                            text = "${horizon.code} (${horizon.days}d)",
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) SurfaceDark else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Layout Mode Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeatmapLayoutMode.values().forEach { mode ->
                    val isSel = layoutMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) SurfaceElevated else SurfaceDark)
                            .border(0.5.dp, if (isSel) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { layoutMode = mode }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (mode == HeatmapLayoutMode.CROSS_MATRIX) "Grid" else "Ranked",
                            fontSize = 9.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) CyanAccent else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Volatility Color Scale Legend Bar
        VolatilityColorScaleLegend()

        // Heatmap Visualization Container
        if (layoutMode == HeatmapLayoutMode.CROSS_MATRIX) {
            if (engineMode == D3HeatmapEngine.CANVAS_MATRIX) {
                D3CanvasMatrixHeatmap(
                    currencies = majorCurrencies,
                    volatilityMap = volatilityData,
                    horizon = selectedHorizon,
                    selectedPair = selectedPair,
                    onSelectPair = { selectedPair = it }
                )
            } else {
                D3SvgWebViewHeatmap(
                    currencies = majorCurrencies,
                    volatilityMap = volatilityData,
                    horizon = selectedHorizon
                )
            }
        } else {
            RankedVolatilityPairsList(
                pairs = volatilityData,
                horizon = selectedHorizon,
                selectedPair = selectedPair,
                onSelectPair = { selectedPair = it }
            )
        }

        // Selected Currency Pair Hedging Insight Drawer
        selectedPair?.let { pair ->
            PairHedgingStrategyDossierCard(
                pair = pair,
                horizon = selectedHorizon,
                onClose = { selectedPair = null },
                onExecute = {
                    onConfigureHedge?.invoke(pair.pairSymbol, pair.recommendedStrategy)
                }
            )
        }
    }
}

/**
 * Color Scale Gradient Bar explaining the D3 heatmap thresholds
 */
@Composable
private fun VolatilityColorScaleLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "VOLATILITY SPECTRUM:", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "<6% Safe", fontSize = 8.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                Text(text = "6-10% Moderate", fontSize = 8.sp, color = GoldAccent, fontFamily = FontFamily.Monospace)
                Text(text = "10-14% Elevated", fontSize = 8.sp, color = WarningAmber, fontFamily = FontFamily.Monospace)
                Text(text = ">14% Critical", fontSize = 8.sp, color = RoseNegative, fontFamily = FontFamily.Monospace)
            }
        }

        // Multi-stop gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            EmeraldPositive,
                            CyanAccent,
                            GoldAccent,
                            WarningAmber,
                            RoseNegative
                        )
                    )
                )
        )
    }
}

/**
 * Interactive Native Canvas Matrix Heatmap for 60 FPS performance and touch navigation
 */
@Composable
private fun D3CanvasMatrixHeatmap(
    currencies: List<String>,
    volatilityMap: List<CurrencyPairVolatility>,
    horizon: VolatilityTimeHorizon,
    selectedPair: CurrencyPairVolatility?,
    onSelectPair: (CurrencyPairVolatility) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .testTag("canvas_volatility_heatmap"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(BorderSubtle, CyanAccent.copy(alpha = 0.2f)))
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(currencies, horizon) {
                    detectTapGestures { offset ->
                        val count = currencies.size
                        val labelPadding = 28f
                        val cellW = (size.width - labelPadding) / count
                        val cellH = (size.height - labelPadding) / count

                        val col = ((offset.x - labelPadding) / cellW).toInt()
                        val row = ((offset.y - labelPadding) / cellH).toInt()

                        if (col in 0 until count && row in 0 until count && col != row) {
                            val base = currencies[row]
                            val quote = currencies[col]
                            val matched = volatilityMap.firstOrNull {
                                (it.base == base && it.quote == quote) || (it.base == quote && it.quote == base)
                            }
                            if (matched != null) {
                                onSelectPair(matched)
                            }
                        }
                    }
                }
        ) {
            val count = currencies.size
            val labelPadding = 28f
            val cellW = (size.width - labelPadding) / count
            val cellH = (size.height - labelPadding) / count

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 22f
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(120, 144, 156)
                textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            // Draw Column Headers
            for (c in 0 until count) {
                val cx = labelPadding + c * cellW + cellW / 2
                drawContext.canvas.nativeCanvas.drawText(
                    currencies[c],
                    cx,
                    labelPadding - 8f,
                    labelPaint
                )
            }

            // Draw Row Headers
            for (r in 0 until count) {
                val cy = labelPadding + r * cellH + cellH / 2 + 7f
                drawContext.canvas.nativeCanvas.drawText(
                    currencies[r],
                    labelPadding / 2,
                    cy,
                    labelPaint
                )
            }

            // Draw Heatmap Cells
            for (r in 0 until count) {
                for (c in 0 until count) {
                    val x = labelPadding + c * cellW + 1.5f
                    val y = labelPadding + r * cellH + 1.5f
                    val w = cellW - 3f
                    val h = cellH - 3f

                    if (r == c) {
                        // Diagonal: Same currency (0.0% variance)
                        drawRoundRect(
                            color = Color(0xFF131B2A),
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "—",
                            x + w / 2,
                            y + h / 2 + 7f,
                            textPaint.apply { color = android.graphics.Color.DKGRAY }
                        )
                    } else {
                        val base = currencies[r]
                        val quote = currencies[c]
                        val pairRecord = volatilityMap.firstOrNull {
                            (it.base == base && it.quote == quote) || (it.base == quote && it.quote == base)
                        }

                        val vol = pairRecord?.getVolForHorizon(horizon) ?: 8.5
                        val cellColor = getVolatilityColor(vol)
                        val isPairSelected = selectedPair != null && (
                            (selectedPair.base == base && selectedPair.quote == quote) ||
                            (selectedPair.base == quote && selectedPair.quote == base)
                        )

                        // Render cell background
                        drawRoundRect(
                            color = cellColor.copy(alpha = if (isPairSelected) 0.95f else 0.75f),
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Selection border or critical pulsing
                        if (isPairSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, y),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(4f, 4f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                        } else if (vol >= 14.0) {
                            drawRoundRect(
                                color = RoseNegative.copy(alpha = 0.4f + pulseProgress * 0.6f),
                                topLeft = Offset(x, y),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(4f, 4f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                            )
                        }

                        // Volatility percentage text
                        val volText = String.format(Locale.US, "%.1f", vol)
                        drawContext.canvas.nativeCanvas.drawText(
                            volText,
                            x + w / 2,
                            y + h / 2 + 7f,
                            textPaint.apply {
                                color = if (vol >= 10.0) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Embedded D3.js SVG Web Visualizer for advanced SVG interactive heatmaps.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun D3SvgWebViewHeatmap(
    currencies: List<String>,
    volatilityMap: List<CurrencyPairVolatility>,
    horizon: VolatilityTimeHorizon
) {
    val htmlContent = remember(currencies, volatilityMap, horizon) {
        buildD3HeatmapHtml(currencies, volatilityMap, horizon)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .testTag("d3_svg_webview_heatmap"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, CyanAccent.copy(alpha = 0.2f))))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(0xFF070B12.toInt())
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://d3js.org", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://d3js.org", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Ranked Pairs List with Volatility KPI Badges
 */
@Composable
private fun RankedVolatilityPairsList(
    pairs: List<CurrencyPairVolatility>,
    horizon: VolatilityTimeHorizon,
    selectedPair: CurrencyPairVolatility?,
    onSelectPair: (CurrencyPairVolatility) -> Unit
) {
    val sortedPairs = remember(pairs, horizon) {
        pairs.sortedByDescending { it.getVolForHorizon(horizon) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sortedPairs.forEach { pair ->
            val vol = pair.getVolForHorizon(horizon)
            val color = getVolatilityColor(vol)
            val isSelected = selectedPair?.pairSymbol == pair.pairSymbol

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SurfaceElevated else SurfaceDark)
                    .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onSelectPair(pair) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                    Text(
                        text = pair.pairSymbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = pair.recommendedStrategy.label,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", vol)}% Vol",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VaR 95%: $${(pair.var95PerMillion / 1000).roundToInt()}k",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Detailed Hedging Strategy Dossier Card for the selected currency pair
 */
@Composable
private fun PairHedgingStrategyDossierCard(
    pair: CurrencyPairVolatility,
    horizon: VolatilityTimeHorizon,
    onClose: () -> Unit,
    onExecute: () -> Unit
) {
    val vol = pair.getVolForHorizon(horizon)
    val color = getVolatilityColor(vol)
    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pair_hedging_dossier_${pair.pairSymbol.replace('/', '_')}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), BorderSubtle)))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${pair.pairSymbol} HEDGING DOSSIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            // Key Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "${horizon.code} HISTORICAL VOL",
                    value = "${String.format(Locale.US, "%.1f", vol)}%",
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "IMPLIED VOL (IV)",
                    value = "${String.format(Locale.US, "%.1f", pair.impliedVol)}%",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "95% VaR (PER $1M)",
                    value = "$${numberFmt.format(pair.var95PerMillion.roundToInt())}",
                    color = if (pair.var95PerMillion > 25000) RoseNegative else GoldAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Strategy Recommendation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECOMMENDED DERIVATIVE STRATEGY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${pair.urgency.label.uppercase(Locale.US)} URGENCY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = pair.recommendedStrategy.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = pair.strategyReasoning,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 12.sp
                    )
                }
            }

            // Quick Execution Action
            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_execute_pair_hedge")
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EXECUTE HEDGE FOR ${pair.pairSymbol}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = SurfaceDark,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, fontFamily = FontFamily.Monospace)
        }
    }
}

/**
 * Maps volatility percentage to color thresholds
 */
private fun getVolatilityColor(volatilityPercent: Double): Color {
    return when {
        volatilityPercent < 6.0 -> EmeraldPositive   // Low risk (< 6%)
        volatilityPercent < 10.0 -> GoldAccent       // Moderate (6 - 10%)
        volatilityPercent < 14.0 -> WarningAmber     // Elevated (10 - 14%)
        else -> RoseNegative                         // Critical (> 14%)
    }
}

/**
 * D3.js SVG Heatmap Template Builder
 */
private fun buildD3HeatmapHtml(
    currencies: List<String>,
    volatilityMap: List<CurrencyPairVolatility>,
    horizon: VolatilityTimeHorizon
): String {
    val matrixJson = StringBuilder("[")
    currencies.forEachIndexed { r, base ->
        currencies.forEachIndexed { c, quote ->
            val vol = if (base == quote) 0.0 else {
                volatilityMap.firstOrNull {
                    (it.base == base && it.quote == quote) || (it.base == quote && it.quote == base)
                }?.getVolForHorizon(horizon) ?: 8.5
            }
            matrixJson.append("{\"r\":$r,\"c\":$c,\"base\":\"$base\",\"quote\":\"$quote\",\"vol\":$vol},")
        }
    }
    if (matrixJson.endsWith(",")) matrixJson.setLength(matrixJson.length - 1)
    matrixJson.append("]")

    val currenciesJson = currencies.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
          <script src="https://d3js.org/d3.v7.min.js"></script>
          <style>
            body {
              margin: 0;
              padding: 6px;
              background-color: #070B12;
              color: #E2E8F0;
              font-family: monospace;
              overflow: hidden;
            }
            svg {
              width: 100%;
              height: 100%;
            }
            .cell {
              rx: 4px;
              ry: 4px;
              stroke: #1E293B;
              stroke-width: 1px;
              cursor: pointer;
              transition: transform 0.15s ease;
            }
            .cell:hover {
              stroke: #38BDF8;
              stroke-width: 2px;
            }
            .label {
              font-size: 10px;
              font-weight: bold;
              fill: #94A3B8;
            }
            .val-text {
              font-size: 9px;
              font-weight: 800;
              text-anchor: middle;
              dominant-baseline: central;
              pointer-events: none;
            }
            #tooltip {
              position: absolute;
              display: none;
              background: #0F172A;
              border: 1px solid #38BDF8;
              padding: 4px 8px;
              border-radius: 4px;
              font-size: 9px;
              color: #F8FAFC;
              pointer-events: none;
            }
          </style>
        </head>
        <body>
          <div id="tooltip"></div>
          <svg id="heatmap"></svg>
          <script>
            const currencies = $currenciesJson;
            const data = $matrixJson;
            const n = currencies.length;

            const svg = d3.select("#heatmap");
            const width = window.innerWidth || 340;
            const height = window.innerHeight || 270;
            const margin = { top: 25, right: 10, bottom: 10, left: 32 };
            const innerW = width - margin.left - margin.right;
            const innerH = height - margin.top - margin.bottom;

            const cellW = innerW / n;
            const cellH = innerH / n;

            const colorScale = d3.scaleSequential()
              .domain([0, 18])
              .interpolator(d3.interpolateTurbo);

            const g = svg.append("g")
              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // Column Labels
            g.selectAll(".col-label")
              .data(currencies)
              .enter()
              .append("text")
              .attr("class", "label")
              .attr("x", (d, i) => i * cellW + cellW / 2)
              .attr("y", -8)
              .attr("text-anchor", "middle")
              .text(d => d);

            // Row Labels
            g.selectAll(".row-label")
              .data(currencies)
              .enter()
              .append("text")
              .attr("class", "label")
              .attr("x", -8)
              .attr("y", (d, i) => i * cellH + cellH / 2 + 4)
              .attr("text-anchor", "end")
              .text(d => d);

            const tooltip = d3.select("#tooltip");

            // Heatmap Cells
            g.selectAll(".cell")
              .data(data)
              .enter()
              .append("rect")
              .attr("class", "cell")
              .attr("x", d => d.c * cellW + 1)
              .attr("y", d => d.r * cellH + 1)
              .attr("width", cellW - 2)
              .attr("height", cellH - 2)
              .attr("fill", d => {
                if (d.r === d.c) return "#131B2A";
                if (d.vol < 6.0) return "#10B981";
                if (d.vol < 10.0) return "#F59E0B";
                if (d.vol < 14.0) return "#F97316";
                return "#F43F5E";
              })
              .on("mouseover", function(event, d) {
                if (d.r !== d.c) {
                  tooltip.style("display", "block")
                    .html(d.base + "/" + d.quote + ": " + d.vol.toFixed(1) + "% Vol")
                    .style("left", (event.pageX + 10) + "px")
                    .style("top", (event.pageY - 20) + "px");
                }
              })
              .on("mouseout", function() {
                tooltip.style("display", "none");
              });

            // Text Labels
            g.selectAll(".val-text")
              .data(data)
              .enter()
              .append("text")
              .attr("class", "val-text")
              .attr("x", d => d.c * cellW + cellW / 2)
              .attr("y", d => d.r * cellH + cellH / 2)
              .attr("fill", d => (d.r === d.c || d.vol >= 10.0) ? "#FFFFFF" : "#0F172A")
              .text(d => d.r === d.c ? "—" : d.vol.toFixed(1));
          </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Historical Data Set for Global Currency Volatility & Hedging Strategies
 */
object D3VolatilityDataSet {
    val ALL_PAIRS = listOf(
        CurrencyPairVolatility(
            base = "EUR",
            quote = "USD",
            pairSymbol = "EUR/USD",
            vol1W = 7.2,
            vol1M = 8.1,
            vol3M = 8.9,
            vol1Y = 7.8,
            impliedVol = 9.4,
            dailyPipsRange = 64.0,
            urgency = HedgingUrgency.MODERATE,
            recommendedStrategy = HedgingStrategyType.FORWARD_CONTRACT,
            strategyReasoning = "Balanced ECB/Fed rate differential. Plain vanilla forward locks in predictable Euro operating cash flow.",
            var95PerMillion = 18400.0
        ),
        CurrencyPairVolatility(
            base = "GBP",
            quote = "USD",
            pairSymbol = "GBP/USD",
            vol1W = 10.4,
            vol1M = 11.0,
            vol3M = 11.2,
            vol1Y = 10.5,
            impliedVol = 12.1,
            dailyPipsRange = 88.0,
            urgency = HedgingUrgency.HIGH,
            recommendedStrategy = HedgingStrategyType.ZERO_COST_COLLAR,
            strategyReasoning = "Elevated Sterling variance driven by BoE inflation trajectory. Zero-cost collar sets firm floor with upside participation.",
            var95PerMillion = 23200.0
        ),
        CurrencyPairVolatility(
            base = "USD",
            quote = "JPY",
            pairSymbol = "USD/JPY",
            vol1W = 14.8,
            vol1M = 14.2,
            vol3M = 13.8,
            vol1Y = 12.4,
            impliedVol = 15.6,
            dailyPipsRange = 132.0,
            urgency = HedgingUrgency.CRITICAL,
            recommendedStrategy = HedgingStrategyType.LAYERED_TRANCHES,
            strategyReasoning = "High BoJ yield-curve policy pivot risk and FX intervention threat. Layered dynamic tranches provide asymmetric downside shield.",
            var95PerMillion = 28600.0
        ),
        CurrencyPairVolatility(
            base = "USD",
            quote = "CHF",
            pairSymbol = "USD/CHF",
            vol1W = 6.9,
            vol1M = 7.4,
            vol3M = 7.8,
            vol1Y = 7.1,
            impliedVol = 8.2,
            dailyPipsRange = 52.0,
            urgency = HedgingUrgency.LOW,
            recommendedStrategy = HedgingStrategyType.FORWARD_CONTRACT,
            strategyReasoning = "Swiss Franc safe-haven stability. Standard forward netting sufficiently absorbs low baseline variance.",
            var95PerMillion = 16100.0
        ),
        CurrencyPairVolatility(
            base = "AUD",
            quote = "USD",
            pairSymbol = "AUD/USD",
            vol1W = 11.8,
            vol1M = 11.1,
            vol3M = 10.6,
            vol1Y = 11.4,
            impliedVol = 11.9,
            dailyPipsRange = 76.0,
            urgency = HedgingUrgency.HIGH,
            recommendedStrategy = HedgingStrategyType.ZERO_COST_COLLAR,
            strategyReasoning = "Commodity export sensitivity and APAC regional trade exposure. Zero-cost collar manages commodity cycle swings.",
            var95PerMillion = 21900.0
        ),
        CurrencyPairVolatility(
            base = "USD",
            quote = "CAD",
            pairSymbol = "USD/CAD",
            vol1W = 6.8,
            vol1M = 7.1,
            vol3M = 7.4,
            vol1Y = 6.9,
            impliedVol = 7.9,
            dailyPipsRange = 58.0,
            urgency = HedgingUrgency.LOW,
            recommendedStrategy = HedgingStrategyType.FORWARD_CONTRACT,
            strategyReasoning = "USMCA trade integration and close central bank policy alignment. Discretionary forward netting recommended.",
            var95PerMillion = 15300.0
        ),
        CurrencyPairVolatility(
            base = "USD",
            quote = "SGD",
            pairSymbol = "USD/SGD",
            vol1W = 4.8,
            vol1M = 5.0,
            vol3M = 5.2,
            vol1Y = 4.9,
            impliedVol = 5.8,
            dailyPipsRange = 36.0,
            urgency = HedgingUrgency.LOW,
            recommendedStrategy = HedgingStrategyType.NATURAL_MATCH,
            strategyReasoning = "MAS S-NEER exchange rate band management. Ultra-low volatility allows natural balance sheet pooling.",
            var95PerMillion = 10800.0
        ),
        CurrencyPairVolatility(
            base = "EUR",
            quote = "GBP",
            pairSymbol = "EUR/GBP",
            vol1W = 6.2,
            vol1M = 6.5,
            vol3M = 6.8,
            vol1Y = 6.4,
            impliedVol = 7.3,
            dailyPipsRange = 44.0,
            urgency = HedgingUrgency.LOW,
            recommendedStrategy = HedgingStrategyType.FORWARD_CONTRACT,
            strategyReasoning = "Channel cross-border trade corridor. Stable corridor suitable for 3-month forward locking.",
            var95PerMillion = 14100.0
        ),
        CurrencyPairVolatility(
            base = "EUR",
            quote = "JPY",
            pairSymbol = "EUR/JPY",
            vol1W = 13.9,
            vol1M = 13.2,
            vol3M = 12.8,
            vol1Y = 11.9,
            impliedVol = 14.1,
            dailyPipsRange = 118.0,
            urgency = HedgingUrgency.HIGH,
            recommendedStrategy = HedgingStrategyType.ZERO_COST_COLLAR,
            strategyReasoning = "ECB/BoJ cross-spread sensitivity with heightened gap risk. Collar protects cross-border AP liabilities.",
            var95PerMillion = 26500.0
        ),
        CurrencyPairVolatility(
            base = "GBP",
            quote = "JPY",
            pairSymbol = "GBP/JPY",
            vol1W = 15.6,
            vol1M = 15.1,
            vol3M = 14.6,
            vol1Y = 13.8,
            impliedVol = 16.4,
            dailyPipsRange = 154.0,
            urgency = HedgingUrgency.CRITICAL,
            recommendedStrategy = HedgingStrategyType.SYNTHETIC_SWAP,
            strategyReasoning = "Widest historical daily range and carry unwind risk. Synthetic cross-currency swaps recommended to insulate balance sheet.",
            var95PerMillion = 30200.0
        )
    )
}
