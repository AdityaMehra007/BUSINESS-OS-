package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.worldbusiness.data.model.ConversionQuote
import com.example.worldbusiness.data.model.ConversionUiState
import java.text.NumberFormat
import java.util.Locale

data class CurrencyMetadata(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

val WORLD_CURRENCIES = listOf(
    CurrencyMetadata("USD", "US Dollar", "$", "🇺🇸"),
    CurrencyMetadata("EUR", "Euro", "€", "🇪🇺"),
    CurrencyMetadata("GBP", "British Pound", "£", "🇬🇧"),
    CurrencyMetadata("SGD", "Singapore Dollar", "S$", "🇸🇬"),
    CurrencyMetadata("CHF", "Swiss Franc", "CHF", "🇨🇭"),
    CurrencyMetadata("JPY", "Japanese Yen", "¥", "🇯🇵"),
    CurrencyMetadata("BRL", "Brazilian Real", "R$", "🇧🇷"),
    CurrencyMetadata("AUD", "Australian Dollar", "A$", "🇦🇺"),
    CurrencyMetadata("CAD", "Canadian Dollar", "C$", "🇨🇦"),
    CurrencyMetadata("CNY", "Chinese Yuan", "¥", "🇨🇳"),
    CurrencyMetadata("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
    CurrencyMetadata("INR", "Indian Rupee", "₹", "🇮🇳"),
    CurrencyMetadata("KRW", "South Korean Won", "₩", "🇰🇷"),
    CurrencyMetadata("MXN", "Mexican Peso", "Mex$", "🇲🇽"),
    CurrencyMetadata("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
    CurrencyMetadata("SEK", "Swedish Krona", "kr", "🇸🇪"),
    CurrencyMetadata("NOK", "Norwegian Krone", "kr", "🇳🇴"),
    CurrencyMetadata("ZAR", "South African Rand", "R", "🇿🇦")
)

/**
 * Currency Conversion Calculator Modal.
 * Allows users to input an amount and select two currencies to see the real-time conversion value,
 * fetching rates from a public API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConversionCalculatorModal(
    amount: String,
    fromCurrency: String,
    toCurrency: String,
    conversionState: ConversionUiState,
    onAmountChange: (String) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onSwapCurrencies: () -> Unit,
    onRefreshRates: () -> Unit,
    onDismiss: () -> Unit,
    onExecuteSwap: ((from: String, to: String, amount: Double, rate: Double) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderSubtle)
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("currency_conversion_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modal Top Header Bar
            CalculatorModalHeader(
                onRefresh = onRefreshRates,
                onClose = onDismiss,
                isLoading = conversionState is ConversionUiState.Loading
            )

            // Amount Input Card with Presets
            AmountInputSection(
                amount = amount,
                fromCurrency = fromCurrency,
                onAmountChange = onAmountChange
            )

            // Currency Pair Selection with Swap Action
            CurrencyPairSelectorSection(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                onFromSelect = onFromCurrencyChange,
                onToSelect = onToCurrencyChange,
                onSwap = onSwapCurrencies
            )

            // Real-time Conversion Result Hero Card
            when (conversionState) {
                is ConversionUiState.Loading -> {
                    ConversionLoadingCard(fromCurrency, toCurrency)
                }
                is ConversionUiState.Success -> {
                    ConversionResultCard(
                        quote = conversionState.quote,
                        onExecuteSwap = onExecuteSwap
                    )
                }
                is ConversionUiState.Error -> {
                    ConversionErrorCard(
                        message = conversionState.message,
                        onRetry = onRefreshRates
                    )
                }
                is ConversionUiState.Idle -> {
                    // Ready state
                }
            }
        }
    }
}

@Composable
private fun CalculatorModalHeader(
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean
) {
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
                    .background(CyanAccent.copy(alpha = 0.15f))
                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "Currency Calculator",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "REAL-TIME FX CALCULATOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Live Public Exchange Rate Engine",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live rate refresh button
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .testTag("btn_refresh_rates")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyanAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Live Rates",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Close Modal Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .testTag("btn_close_converter_modal")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Calculator",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AmountInputSection(
    amount: String,
    fromCurrency: String,
    onAmountChange: (String) -> Unit
) {
    val fromSymbol = WORLD_CURRENCIES.find { it.code == fromCurrency }?.symbol ?: "$"
    val presetAmounts = listOf("1,000", "5,000", "10,000", "50,000", "100,000", "1,000,000")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "INPUT AMOUNT TO CONVERT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                // Allow digits and optional single decimal point
                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                    onAmountChange(input)
                }
            },
            leadingIcon = {
                Text(
                    text = fromSymbol,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    modifier = Modifier.padding(start = 12.dp)
                )
            },
            trailingIcon = {
                Text(
                    text = fromCurrency,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Formatters.getCurrencyColor(fromCurrency),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            placeholder = { Text("10000", color = TextMuted, fontSize = 16.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_converter_amount"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetAmounts.forEach { preset ->
                val rawPreset = preset.replace(",", "")
                val isSelected = amount == rawPreset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CyanAccent else SurfaceDark)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onAmountChange(rawPreset) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("preset_amt_$rawPreset")
                ) {
                    Text(
                        text = "$fromSymbol$preset",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyPairSelectorSection(
    fromCurrency: String,
    toCurrency: String,
    onFromSelect: (String) -> Unit,
    onToSelect: (String) -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "From" Currency Picker
        Box(modifier = Modifier.weight(1f)) {
            CurrencyDropdownPicker(
                label = "SOURCE (FROM)",
                selectedCurrency = fromCurrency,
                onSelect = onFromSelect,
                testTagPrefix = "from"
            )
        }

        // Swap Currencies Button
        IconButton(
            onClick = onSwap,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), CircleShape)
                .testTag("btn_swap_currencies")
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap From and To Currencies",
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        // "To" Currency Picker
        Box(modifier = Modifier.weight(1f)) {
            CurrencyDropdownPicker(
                label = "TARGET (TO)",
                selectedCurrency = toCurrency,
                onSelect = onToSelect,
                testTagPrefix = "to"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdownPicker(
    label: String,
    selectedCurrency: String,
    onSelect: (String) -> Unit,
    testTagPrefix: String
) {
    var expanded by remember { mutableStateOf(false) }
    val meta = WORLD_CURRENCIES.find { it.code == selectedCurrency } ?: CurrencyMetadata(selectedCurrency, selectedCurrency, "", "🌐")

    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("dropdown_${testTagPrefix}_currency"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meta.flag,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = meta.code,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Formatters.getCurrencyColor(meta.code),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = meta.name,
                            fontSize = 9.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(SurfaceDark)
                    .height(280.dp)
            ) {
                WORLD_CURRENCIES.forEach { curr ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = curr.flag, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${curr.code} • ${curr.symbol}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = curr.name,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(curr.code)
                            expanded = false
                        },
                        trailingIcon = {
                            if (curr.code == selectedCurrency) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversionResultCard(
    quote: ConversionQuote,
    onExecuteSwap: ((from: String, to: String, amount: Double, rate: Double) -> Unit)?
) {
    val targetMeta = WORLD_CURRENCIES.find { it.code == quote.toCurrency }
    val formattedConverted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 4
    }.format(quote.convertedAmount)

    val formattedInput = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(quote.amount)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("conversion_result_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Provider & Live Status Bar
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
                        .background(if (quote.isLiveApi) EmeraldPositive.copy(alpha = alphaPulse) else GoldAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (quote.isLiveApi) "LIVE PUBLIC API QUOTE" else "TREASURY BENCHMARK QUOTE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (quote.isLiveApi) EmeraldPositive else GoldAccent,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = quote.provider,
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        // Primary Converted Amount Hero Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "$formattedInput ${quote.fromCurrency} =",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${targetMeta?.symbol ?: ""} $formattedConverted",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("converted_result_value")
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Formatters.getCurrencyColor(quote.toCurrency).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quote.toCurrency,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Formatters.getCurrencyColor(quote.toCurrency),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Exchange Rate Details & Inverses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "EXCHANGE RATE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "1 ${quote.fromCurrency} = ${String.format(Locale.US, "%.5f", quote.exchangeRate)} ${quote.toCurrency}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "INVERSE RATE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "1 ${quote.toCurrency} = ${String.format(Locale.US, "%.5f", quote.inverseRate)} ${quote.fromCurrency}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Spread & Timestamp Meta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Interbank Institutional Zero-Markup Rate",
                fontSize = 9.sp,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = quote.timestamp,
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        // Optional Quick Action: Execute FX Swap using this live quote
        if (onExecuteSwap != null && quote.amount > 0.0) {
            Button(
                onClick = {
                    onExecuteSwap(quote.fromCurrency, quote.toCurrency, quote.amount, quote.exchangeRate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_execute_swap_from_calculator"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Execute FX Swap in Treasury (${quote.fromCurrency} ➔ ${quote.toCurrency})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConversionLoadingCard(from: String, to: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Fetching real-time exchange rates from public API...",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Connecting to interbank feed ($from ➔ $to)",
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ConversionErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, RoseNegative.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rate Calculation Error",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = RoseNegative
        )
        Text(
            text = message,
            fontSize = 11.sp,
            color = TextSecondary
        )
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retry Fetch")
        }
    }
}
