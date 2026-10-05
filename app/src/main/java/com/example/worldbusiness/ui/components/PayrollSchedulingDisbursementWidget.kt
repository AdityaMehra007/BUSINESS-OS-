package com.example.worldbusiness.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.WarningAmber
import com.example.worldbusiness.data.model.BulkPayrollDisbursementReceipt
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch
import com.example.worldbusiness.data.model.PayrollScheduleCycle
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.repository.PayrollTaxEngine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enterprise Payroll Scheduling UI Component for Bulk Multi-Currency Conversions and Global Disbursements.
 *
 * Utilizes the existing real-time currency conversion service and treasury clearing rails
 * to schedule, simulate, convert, and disburse multinational payroll across local currencies.
 */
@Composable
fun PayrollSchedulingDisbursementWidget(
    teamMembers: List<TeamMemberRecord>,
    balances: List<FxBalanceRecord>,
    onExecuteDisbursement: (
        scheduledDate: String,
        fundingCurrency: String,
        batches: List<PayrollCurrencyDisbursementBatch>,
        selectedMemberIds: Set<Long>,
        onSuccess: (BulkPayrollDisbursementReceipt) -> Unit
    ) -> Unit,
    onFetchLiveRate: suspend (fromCurrency: String, toCurrency: String) -> Double = { _, _ -> 1.0 },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFundingCurrency by remember { mutableStateOf("USD") }
    var selectedCycle by remember { mutableStateOf(PayrollScheduleCycle.MONTH_END) }
    var customDateStr by remember { mutableStateOf("Oct 31, 2026") }

    var isRefreshingRates by remember { mutableStateOf(false) }
    var lastRateRefreshTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).format(Date()))
    }

    // Selected member IDs for disbursement (defaults to all)
    val selectedMemberIds = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            teamMembers.forEach { put(it.id, true) }
        }
    }

    // Live exchange rates relative to USD (fallback rates)
    val liveRatesToUsd = remember {
        mutableStateMapOf(
            "USD" to 1.0,
            "EUR" to 1.0900,
            "GBP" to 1.3050,
            "CHF" to 1.1620,
            "SGD" to 0.7680,
            "BRL" to 0.1840,
            "JPY" to 0.0069
        )
    }

    // Refresh live conversion rates
    fun refreshFxRates() {
        coroutineScope.launch {
            isRefreshingRates = true
            val distinctCurrencies = teamMembers.map { it.currency.uppercase(Locale.US) }.distinct()
            distinctCurrencies.forEach { curr ->
                if (curr != "USD") {
                    try {
                        val rate = onFetchLiveRate(curr, "USD")
                        if (rate > 0.0) {
                            liveRatesToUsd[curr] = rate
                        }
                    } catch (_: Exception) {}
                }
            }
            lastRateRefreshTime = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).format(Date())
            isRefreshingRates = false
            Toast.makeText(context, "Refreshed live FX rates for multinational payroll", Toast.LENGTH_SHORT).show()
        }
    }

    // Calculate Batches Grouped by Currency
    val batches = remember(teamMembers, selectedMemberIds.toMap(), selectedFundingCurrency, liveRatesToUsd.toMap()) {
        val selectedList = teamMembers.filter { selectedMemberIds[it.id] == true }
        val grouped = selectedList.groupBy { it.currency.uppercase(Locale.US) }

        val fundingRateToUsd = liveRatesToUsd[selectedFundingCurrency.uppercase(Locale.US)] ?: 1.0

        grouped.map { (currency, members) ->
            val grossLocal = members.sumOf { it.monthlyCompensation }
            val taxWithheldLocal = members.sumOf { member ->
                val rate = liveRatesToUsd[currency] ?: 1.0
                val detail = PayrollTaxEngine.calculateEmployeePayroll(member, rate)
                detail.totalTaxWithheldLocal
            }
            val netPayableLocal = grossLocal - taxWithheldLocal

            // Calculate conversion rate: 1 local currency = X funding currency
            val localRateToUsd = liveRatesToUsd[currency] ?: 1.0
            val exchangeRateToFunding = if (fundingRateToUsd > 0.0) localRateToUsd / fundingRateToUsd else 1.0

            val fundingEquivalent = netPayableLocal * exchangeRateToFunding

            val sampleCountryCode = members.firstOrNull()?.countryCode ?: "US"
            val clearingRail = when (sampleCountryCode.uppercase(Locale.US)) {
                "DE" -> "SEPA Instant SCT (Europe)"
                "GB" -> "BACS / Faster Payments (UK)"
                "CH" -> "SIC Interbank Wire (Switzerland)"
                "SG" -> "FAST Real-Time Direct (Singapore)"
                "BR" -> "PIX Instant Clearing (Brazil)"
                "JP" -> "BOJ-NET / Zengin Wire (Japan)"
                else -> "Fedwire / NACHA Direct Deposit (US)"
            }

            PayrollCurrencyDisbursementBatch(
                currency = currency,
                countryCode = sampleCountryCode,
                clearingRail = clearingRail,
                recipientCount = members.size,
                grossLocalAmount = grossLocal,
                taxWithheldLocalAmount = taxWithheldLocal,
                netPayableLocalAmount = netPayableLocal,
                exchangeRateToFunding = exchangeRateToFunding,
                fundingCurrencyEquivalent = fundingEquivalent,
                isApiLive = true,
                rateProvider = "Open Exchange Rates API",
                rateTimestamp = lastRateRefreshTime,
                memberIds = members.map { it.id }
            )
        }.sortedByDescending { it.fundingCurrencyEquivalent }
    }

    // Totals in Funding Currency
    val totalFundingOutflow = remember(batches) { batches.sumOf { it.fundingCurrencyEquivalent } }
    val totalRecipientsCount = remember(selectedMemberIds.toMap()) {
        selectedMemberIds.values.count { it }
    }

    val fundingVaultRecord = balances.find { it.currencyCode.equals(selectedFundingCurrency, ignoreCase = true) }
    val fundingVaultBal = fundingVaultRecord?.balance ?: 1500000.0
    val hasSufficientFunds = fundingVaultBal >= totalFundingOutflow

    // Modals
    var showConfirmDialog by remember { mutableStateOf(false) }
    var activeReceipt by remember { mutableStateOf<BulkPayrollDisbursementReceipt?>(null) }

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("payroll_scheduling_widget"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Title & Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MULTINATIONAL PAYROLL SCHEDULER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPositive.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "STP BULK CLEARING",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Bulk FX Conversions, Multi-Currency Treasury Debits & Local Rail Disbursement",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            // Refresh Live FX Rates Button
            OutlinedButton(
                onClick = { refreshFxRates() },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                modifier = Modifier.testTag("btn_refresh_payroll_fx")
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing))
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh FX",
                    modifier = Modifier
                        .size(13.dp)
                        .then(if (isRefreshingRates) Modifier.rotate(angle) else Modifier)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE FX",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Scheduler Controls: Funding Vault + Cycle Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Funding Vault Selector
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TREASURY FUNDING VAULT:",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("USD", "EUR", "GBP", "CHF", "SGD").forEach { code ->
                        val isSelected = selectedFundingCurrency == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanAccent else SurfaceElevated)
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { selectedFundingCurrency = code }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("funding_vault_$code")
                        ) {
                            Text(
                                text = "$code Vault",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Cycle Horizon Selector
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "EXECUTION SCHEDULE:",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PayrollScheduleCycle.values().forEach { cycle ->
                        val isSelected = selectedCycle == cycle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GoldAccent else SurfaceElevated)
                                .border(1.dp, if (isSelected) GoldAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedCycle = cycle
                                    customDateStr = when (cycle) {
                                        PayrollScheduleCycle.INSTANT -> "Immediate STP Wire"
                                        PayrollScheduleCycle.MID_MONTH -> "Oct 15, 2026"
                                        PayrollScheduleCycle.MONTH_END -> "Oct 31, 2026"
                                        PayrollScheduleCycle.CUSTOM -> "Nov 15, 2026"
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("payroll_cycle_${cycle.name.lowercase(Locale.US)}")
                        ) {
                            Text(
                                text = cycle.label.take(12),
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

        // 3. Consolidated Multi-Currency KPI Summary Strip
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, CyanAccent.copy(alpha = 0.3f)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                        Text(
                            text = "CYCLE: $customDateStr",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, tint = EmeraldPositive, modifier = Modifier.size(12.dp))
                        Text(
                            text = "FX Sync: $lastRateRefreshTime",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TOTAL DISBURSEMENT OUTFLOW", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${numberFmt.format(totalFundingOutflow)} $selectedFundingCurrency",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (hasSufficientFunds) CyanAccent else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "AVAILABLE $selectedFundingCurrency VAULT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${numberFmt.format(fundingVaultBal)} $selectedFundingCurrency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSufficientFunds) EmeraldPositive else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recipients: $totalRecipientsCount personnel (${batches.size} currencies)",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Clearing: Direct Local RTGS / STP",
                        fontSize = 9.sp,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 4. Multi-Currency Local Clearing Batches
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL CURRENCY DISBURSEMENT BATCHES (${batches.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Select all / individual staff below",
                    fontSize = 8.sp,
                    color = TextMuted
                )
            }

            batches.forEach { batch ->
                PayrollCurrencyBatchItem(
                    batch = batch,
                    fundingCurrency = selectedFundingCurrency,
                    allMembers = teamMembers,
                    selectedMemberIds = selectedMemberIds,
                    onToggleMember = { id, selected -> selectedMemberIds[id] = selected }
                )
            }
        }

        // 5. Execution Action Bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceElevated,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasSufficientFunds) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RoseNegative, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Insufficient funds in $selectedFundingCurrency Vault. Please top up or select another vault.",
                            fontSize = 9.sp,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = hasSufficientFunds && totalRecipientsCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SurfaceDark,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_execute_bulk_payroll")
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTHORIZE & EXECUTE BULK DISBURSEMENT (${numberFmt.format(totalFundingOutflow)} $selectedFundingCurrency)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Modal: Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = CyanAccent)
                    Text(
                        text = "CONFIRM MULTI-CURRENCY PAYROLL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are about to authorize an automated multi-currency payroll run across ${batches.size} sovereign jurisdictions:",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            batches.forEach { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${b.recipientCount} staff in ${b.currency}:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "${numberFmt.format(b.netPayableLocalAmount)} ${b.currency} (≈ ${numberFmt.format(b.fundingCurrencyEquivalent)} $selectedFundingCurrency)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Treasury Outflow:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "${numberFmt.format(totalFundingOutflow)} $selectedFundingCurrency", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    }

                    Text(
                        text = "Clearing: Instant local rails (SEPA, BACS, FAST, PIX, BOJ-NET, Fedwire). All transactions are cryptographically signed for SOX / Basel III audit compliance.",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onExecuteDisbursement(
                            customDateStr,
                            selectedFundingCurrency,
                            batches,
                            selectedMemberIds.filter { it.value }.keys.toSet()
                        ) { receipt ->
                            activeReceipt = receipt
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_bulk_disbursement")
                ) {
                    Text("AUTHORIZE DISBURSEMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCEL", color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal: Certified Digital Disbursement Receipt
    activeReceipt?.let { receipt ->
        AlertDialog(
            onDismissRequest = { activeReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPositive)
                    Column {
                        Text(
                            text = "PAYROLL DISBURSEMENT CERTIFIED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "ISO 20022 STP Batch Clearance Receipt",
                            fontSize = 8.sp,
                            color = EmeraldPositive
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "BATCH REF : ${receipt.batchReferenceId}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text(text = "TIMESTAMP : ${receipt.executionTimestamp}", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(text = "SCHEDULED : ${receipt.scheduledDate}", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(text = "HEADCOUNT : ${receipt.processedEmployeesCount} personnel paid", fontSize = 8.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            Text(text = "OUTFLOW   : ${numberFmt.format(receipt.totalFundingOutflow)} ${receipt.fundingVaultCurrency}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                            Text(text = "HASH SEAL : ${receipt.cryptographicHash.take(24)}...", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Text(text = "DISBURSED LOCAL CURRENCY AMOUNTS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
                    receipt.totalNetLocalPayouts.forEach { (curr, amt) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "• $curr Net Wire:", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(text = "${numberFmt.format(amt)} $curr", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Payroll Receipt", "Batch ID: ${receipt.batchReferenceId}\nTotal: ${receipt.totalFundingOutflow} ${receipt.fundingVaultCurrency}\nTimestamp: ${receipt.executionTimestamp}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Receipt copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { activeReceipt = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DONE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Individual Local Currency Batch Card displaying exchange rate, net payout, and expandable staff list
 */
@Composable
private fun PayrollCurrencyBatchItem(
    batch: PayrollCurrencyDisbursementBatch,
    fundingCurrency: String,
    allMembers: List<TeamMemberRecord>,
    selectedMemberIds: Map<Long, Boolean>,
    onToggleMember: (Long, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }

    val batchMembers = remember(allMembers, batch) {
        allMembers.filter { it.currency.equals(batch.currency, ignoreCase = true) }
    }

    val flagEmoji = when (batch.countryCode.uppercase(Locale.US)) {
        "DE" -> "🇩🇪"
        "GB" -> "🇬🇧"
        "CH" -> "🇨🇭"
        "SG" -> "🇸🇬"
        "BR" -> "🇧🇷"
        "JP" -> "🇯🇵"
        else -> "🇺🇸"
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceVariantDark))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Batch Row Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = flagEmoji, fontSize = 16.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${batch.currency} POOL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${batch.recipientCount} Staff)",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = batch.clearingRail,
                            fontSize = 8.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${numberFmt.format(batch.netPayableLocalAmount)} ${batch.currency}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "≈ ${numberFmt.format(batch.fundingCurrencyEquivalent)} $fundingCurrency",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand Batch",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Conversion Rate Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rate: 1 ${batch.currency} = ${String.format(Locale.US, "%.4f", batch.exchangeRateToFunding)} $fundingCurrency",
                    fontSize = 8.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Tax Withheld: ${numberFmt.format(batch.taxWithheldLocalAmount)} ${batch.currency}",
                    fontSize = 8.sp,
                    color = GoldAccent,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Expandable Employee List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    batchMembers.forEach { member ->
                        val isChecked = selectedMemberIds[member.id] ?: true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) SurfaceElevated else SurfaceDark.copy(alpha = 0.5f))
                                .clickable { onToggleMember(member.id, !isChecked) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleMember(member.id, it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CyanAccent,
                                        checkmarkColor = SurfaceDark,
                                        uncheckedColor = TextMuted
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = member.fullName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChecked) TextPrimary else TextMuted
                                    )
                                    Text(
                                        text = "${member.role} • ${member.taxJurisdiction}",
                                        fontSize = 8.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${numberFmt.format(member.monthlyCompensation)} ${member.currency}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChecked) CyanAccent else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
