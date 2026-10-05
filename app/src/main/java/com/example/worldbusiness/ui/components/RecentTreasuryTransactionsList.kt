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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionFilterType(val label: String) {
    ALL("All Flows"),
    CREDITS("Credits (+)"),
    DEBITS("Debits (-)")
}

/**
 * Reusable Compose list component for recent treasury transactions that displays:
 * - date (timestamp and settlement date)
 * - recipient (institution, vendor, subsidiary, or clearing account)
 * - amount (formatted localized amount)
 * - currency (with stylized currency pill badge)
 * - icons indicating credit or debit status (vibrant emerald downward arrow for credit inflow,
 *   crisp rose upward arrow for debit outflow, with accessibility semantics and status badges)
 */
@Composable
fun RecentTreasuryTransactionsList(
    transactions: List<TreasuryTransactionRecord>,
    modifier: Modifier = Modifier,
    title: String = "RECENT TREASURY TRANSACTIONS",
    maxItems: Int? = null,
    showHeader: Boolean = true,
    showFilters: Boolean = true,
    showSearch: Boolean = true,
    showSummaryStats: Boolean = true,
    onRecordTransactionClick: (() -> Unit)? = null,
    onViewAllClick: (() -> Unit)? = null
) {
    var filterType by remember { mutableStateOf(TransactionFilterType.ALL) }
    var selectedCurrency by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTransactionForAudit by remember { mutableStateOf<TreasuryTransactionRecord?>(null) }

    // Currencies present in current transactions
    val availableCurrencies = remember(transactions) {
        listOf("ALL") + transactions.map { it.currency }.distinct().sorted()
    }

    // Filtered transaction list
    val filteredTransactions by remember(transactions, filterType, selectedCurrency, searchQuery, maxItems) {
        derivedStateOf {
            val list = transactions.filter { tx ->
                val matchesType = when (filterType) {
                    TransactionFilterType.ALL -> true
                    TransactionFilterType.CREDITS -> tx.isCredit
                    TransactionFilterType.DEBITS -> !tx.isCredit
                }
                val matchesCurrency = selectedCurrency == "ALL" || tx.currency.equals(selectedCurrency, ignoreCase = true)
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim().lowercase(Locale.US)
                    tx.recipient.lowercase(Locale.US).contains(q) ||
                            tx.senderOrCounterparty.lowercase(Locale.US).contains(q) ||
                            tx.referenceCode.lowercase(Locale.US).contains(q) ||
                            tx.note.lowercase(Locale.US).contains(q) ||
                            tx.currency.lowercase(Locale.US).contains(q)
                }
                matchesType && matchesCurrency && matchesSearch
            }
            if (maxItems != null && maxItems > 0) list.take(maxItems) else list
        }
    }

    // Calculated summary stats
    val totalCredits = remember(transactions) {
        transactions.filter { it.isCredit }.sumOf { it.amount }
    }
    val totalDebits = remember(transactions) {
        transactions.filter { !it.isCredit }.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_treasury_transactions_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Transactions Ledger",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredTransactions.size} ENTRIES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRecordTransactionClick != null) {
                        Button(
                            onClick = onRecordTransactionClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier.testTag("btn_new_treasury_tx")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Transaction",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Entry",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (onViewAllClick != null && maxItems != null && transactions.size > maxItems) {
                        TextButton(
                            onClick = onViewAllClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_view_all_transactions")
                        ) {
                            Text(
                                text = "View All (${transactions.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }

        // Summary Stats Bar (Credits Inflows vs Debits Outflows)
        if (showSummaryStats && transactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inflows Metric
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(EmeraldPositive.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Total Credits Inflow",
                            tint = EmeraldPositive,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TOTAL CREDITS (+)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "+$%,.0f", totalCredits),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(BorderSubtle)
                )

                // Outflows Metric
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(RoseNegative.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Total Debits Outflow",
                            tint = RoseNegative,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TOTAL DEBITS (-)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "-$%,.0f", totalDebits),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(BorderSubtle)
                )

                // Net Cashflow
                val netFlow = totalCredits - totalDebits
                val netColor = if (netFlow >= 0) EmeraldPositive else RoseNegative
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET DISBURSEMENT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%s$%,.0f", if (netFlow >= 0) "+$" else "-$", Math.abs(netFlow)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Search Bar
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("treasury_transactions_search_input"),
                placeholder = {
                    Text(
                        text = "Search recipient, counterparty, SWIFT/SEPA ref...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Filter Chips (Credits / Debits / All & Currency)
        if (showFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter by Credit/Debit type
                TransactionFilterType.values().forEach { type ->
                    val isSelected = filterType == type
                    val chipColor = when (type) {
                        TransactionFilterType.ALL -> CyanAccent
                        TransactionFilterType.CREDITS -> EmeraldPositive
                        TransactionFilterType.DEBITS -> RoseNegative
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = type },
                        label = {
                            Text(
                                text = type.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                            selectedLabelColor = chipColor,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) chipColor else BorderSubtle
                        ),
                        modifier = Modifier.testTag("filter_chip_${type.name.lowercase(Locale.US)}")
                    )
                }

                // Currency selector chips
                if (availableCurrencies.size > 2) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(BorderSubtle)
                    )

                    availableCurrencies.forEach { curr ->
                        val isCurrSelected = selectedCurrency == curr
                        FilterChip(
                            selected = isCurrSelected,
                            onClick = { selectedCurrency = curr },
                            label = {
                                Text(
                                    text = curr,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                selectedLabelColor = GoldAccent,
                                containerColor = SurfaceDark,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isCurrSelected,
                                borderColor = if (isCurrSelected) GoldAccent else BorderSubtle
                            ),
                            modifier = Modifier.testTag("filter_curr_$curr")
                        )
                    }
                }
            }
        }

        // Transaction Rows
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No Transactions Found",
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Treasury Transactions Match Current Filter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Adjust filter criteria or record a new transaction",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredTransactions.forEach { transaction ->
                    TreasuryTransactionItemCard(
                        transaction = transaction,
                        onClick = { selectedTransactionForAudit = transaction }
                    )
                }
            }
        }
    }

    // Detail Audit Dialog
    selectedTransactionForAudit?.let { tx ->
        TreasuryTransactionAuditDialog(
            transaction = tx,
            onDismiss = { selectedTransactionForAudit = null }
        )
    }
}

/**
 * Individual Transaction Item Row displaying:
 * - date
 * - recipient
 * - amount
 * - currency
 * - icons indicating credit or debit status (downward emerald for credit, upward rose for debit)
 */
@Composable
fun TreasuryTransactionItemCard(
    transaction: TreasuryTransactionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.isCredit
    val statusColor = if (isCredit) EmeraldPositive else RoseNegative
    val statusContainerColor = if (isCredit) EmeraldPositive.copy(alpha = 0.12f) else RoseNegative.copy(alpha = 0.12f)
    val statusBorderColor = if (isCredit) EmeraldPositive.copy(alpha = 0.35f) else RoseNegative.copy(alpha = 0.35f)
    val formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(transaction.amount)
    val amountPrefix = if (isCredit) "+ " else "- "
    val statusDescription = if (isCredit) "Credit transaction inflow" else "Debit transaction outflow"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("tx_item_${transaction.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Credit/Debit Status Icon + Date + Recipient + Counterparty
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Status Icon (Credit vs Debit) with high-contrast indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(statusContainerColor)
                    .border(1.dp, statusBorderColor, CircleShape)
                    .semantics { contentDescription = statusDescription }
                    .testTag("tx_status_icon_${transaction.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isCredit) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Credit Status Icon",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Debit Status Icon",
                        tint = RoseNegative,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top line: Date and Reference
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.date,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = BorderSubtle
                    )
                    Text(
                        text = transaction.referenceCode,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Recipient (Title)
                Text(
                    text = transaction.recipient,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )

                // Counterparty / Subtitle
                if (transaction.senderOrCounterparty.isNotBlank()) {
                    Text(
                        text = "Counterparty: ${transaction.senderOrCounterparty}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Amount, Currency, and Credit/Debit Status Pill
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Amount with Sign and Currency
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$amountPrefix$formattedAmount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("tx_amount_${transaction.id}")
                )

                // Currency Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Formatters.getCurrencyColor(transaction.currency).copy(alpha = 0.2f))
                        .border(1.dp, Formatters.getCurrencyColor(transaction.currency).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                        .testTag("tx_currency_${transaction.id}")
                ) {
                    Text(
                        text = transaction.currency,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Formatters.getCurrencyColor(transaction.currency),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Status Badge: Credit (Inflow) / Debit (Outflow) + Settlement Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Category or Status
                Text(
                    text = transaction.category.replace("_", " "),
                    fontSize = 8.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusContainerColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isCredit) "CREDIT" else "DEBIT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Detailed Transaction Audit Modal Dialog
 */
@Composable
fun TreasuryTransactionAuditDialog(
    transaction: TreasuryTransactionRecord,
    onDismiss: () -> Unit
) {
    val isCredit = transaction.isCredit
    val statusColor = if (isCredit) EmeraldPositive else RoseNegative
    val statusContainerColor = if (isCredit) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (isCredit) "Credit" else "Debit",
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "TREASURY CLEARING AUDIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = transaction.referenceCode,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Amount Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusContainerColor)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCredit) "TRANSACTION CREDIT (INFLOW)" else "TRANSACTION DEBIT (OUTFLOW)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${if (isCredit) "+" else "-"} ${transaction.currency} ${String.format(Locale.US, "%,.2f", transaction.amount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                AuditField("Settlement Timestamp", transaction.date)
                AuditField("Primary Recipient", transaction.recipient)
                AuditField("Counterparty / Originator", transaction.senderOrCounterparty)
                AuditField("Currency & ISO Code", "${transaction.currency} (${Formatters.getCurrencyFlag(transaction.currency)})")
                AuditField("Classification Category", transaction.category.replace("_", " "))
                if (transaction.note.isNotBlank()) {
                    AuditField("Clearing Memo / Audit Note", transaction.note)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss Audit", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun AuditField(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(Locale.US),
            fontSize = 8.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/**
 * Dialog to dispatch and record a new multi-currency treasury transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTreasuryTransactionDialog(
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        recipient: String,
        counterparty: String,
        amount: Double,
        currency: String,
        isCredit: Boolean,
        category: String,
        note: String
    ) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(availableCurrencies.firstOrNull() ?: "USD") }
    var isCredit by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("COMMERCIAL_SETTLEMENT") }
    var note by remember { mutableStateOf("") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "COMMERCIAL_SETTLEMENT" to "Commercial Invoice Settlement",
        "GLOBAL_PAYROLL" to "Global Workforce Payroll",
        "SUPPLY_FREIGHT" to "Supply Chain & Customs Freight",
        "FX_INTERBANK" to "Interbank FX Clearing",
        "CAPITAL_INJECTION" to "Direct Capital Reserve Deposit"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "RECORD TREASURY TRANSACTION",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Credit vs Debit Selector Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isCredit) EmeraldPositive.copy(alpha = 0.2f) else RoseNegative.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = if (isCredit) "Credit Icon" else "Debit Icon",
                                tint = if (isCredit) EmeraldPositive else RoseNegative,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isCredit) "CREDIT (INFLOW)" else "DEBIT (OUTFLOW)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCredit) EmeraldPositive else RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isCredit) "Adds to sovereign vault balance" else "Deducts from vault balance",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isCredit,
                        onCheckedChange = { isCredit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPositive,
                            checkedTrackColor = EmeraldPositive.copy(alpha = 0.4f),
                            uncheckedThumbColor = RoseNegative,
                            uncheckedTrackColor = RoseNegative.copy(alpha = 0.4f)
                        )
                    )
                }

                // Recipient
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Institution / Payee") },
                    placeholder = { Text("e.g. JPMorgan Chase Treasury, Maersk Logistics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Counterparty / Originator
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text("Counterparty / Ordering Customer") },
                    placeholder = { Text("e.g. OmniGlobal Holdings, Client SE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Amount and Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        placeholder = { Text("50000.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            availableCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("$curr (${Formatters.getCurrencyFlag(curr)})", color = TextPrimary) },
                                    onClick = {
                                        selectedCurrency = curr
                                        currencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Note / Memo
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Audit Note / Memo") },
                    placeholder = { Text("e.g. Q3 Clearing Milestone settlement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            val isValid = recipient.isNotBlank() && amt > 0.0
            Button(
                onClick = {
                    onConfirm(
                        recipient,
                        if (counterparty.isBlank()) "OmniGlobal Treasury Rail" else counterparty,
                        amt,
                        selectedCurrency,
                        isCredit,
                        selectedCategory,
                        note
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Settlement", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    )
}
