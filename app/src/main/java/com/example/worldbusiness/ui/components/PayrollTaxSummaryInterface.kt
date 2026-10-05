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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.worldbusiness.data.model.EmployeePayrollDetail
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.JurisdictionSummary
import java.util.Locale

/**
 * Multi-Jurisdiction Payroll & Tax Withholding Summary Interface
 * Calculates statutory tax withholdings for diverse multinational jurisdictions
 * and displays gross, itemized deductions, and net pay per employee in their local currency.
 */
@Composable
fun PayrollTaxSummaryInterface(
    summary: GlobalPayrollSummary,
    onRunPayroll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedJurisdiction by remember { mutableStateOf("ALL") }
    var selectedEmploymentType by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEmployeeForDetail by remember { mutableStateOf<EmployeePayrollDetail?>(null) }

    val filteredEmployees = remember(summary, selectedJurisdiction, selectedEmploymentType, searchQuery) {
        summary.employeeDetails.filter { emp ->
            val matchesJurisdiction = selectedJurisdiction == "ALL" || emp.countryCode.equals(selectedJurisdiction, ignoreCase = true)
            val matchesType = selectedEmploymentType == "ALL" || when (selectedEmploymentType) {
                "SUBSIDIARY" -> emp.employmentType.contains("Subsidiary", ignoreCase = true)
                "EOR" -> emp.employmentType.contains("EOR", ignoreCase = true)
                "CONTRACTOR" -> emp.employmentType.contains("Contractor", ignoreCase = true)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                emp.fullName.contains(searchQuery, ignoreCase = true) ||
                emp.role.contains(searchQuery, ignoreCase = true) ||
                emp.country.contains(searchQuery, ignoreCase = true) ||
                emp.taxJurisdiction.contains(searchQuery, ignoreCase = true)

            matchesJurisdiction && matchesType && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payroll_summary_interface"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive KPI Cards (Gross, Withholdings, Net Pay, Effective Rate)
        PayrollSummaryKpiGrid(
            summary = summary,
            onRunPayroll = onRunPayroll
        )

        // Jurisdiction Breakdown Horizontal Carousel
        JurisdictionCarousel(
            jurisdictions = summary.jurisdictions,
            selectedJurisdiction = selectedJurisdiction,
            onSelectJurisdiction = { selectedJurisdiction = it }
        )

        // Filter and Search Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search employee, role, or country...", fontSize = 11.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("payroll_search_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true
            )
        }

        // Employment Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All Types (${summary.totalEmployees})",
                "SUBSIDIARY" to "Direct Subsidiary",
                "EOR" to "Full-Time (EOR)",
                "CONTRACTOR" to "B2B Contractor (0% WHT)"
            ).forEach { (typeKey, label) ->
                val isSelected = selectedEmploymentType == typeKey
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { selectedEmploymentType = typeKey }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("filter_payroll_$typeKey")
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section Title with Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EMPLOYEE NET PAY & TAX WITHHOLDING SCHEDULE (${filteredEmployees.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "LOCAL CURRENCY SETTLEMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        // Employee Cards List
        if (filteredEmployees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No employees match the current filters.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredEmployees.forEach { employee ->
                    EmployeePayrollCard(
                        detail = employee,
                        onClick = { selectedEmployeeForDetail = employee }
                    )
                }
            }
        }
    }

    // Detailed Pay Slip / Tax Audit Dialog
    selectedEmployeeForDetail?.let { emp ->
        PaySlipDetailDialog(
            detail = emp,
            onDismiss = { selectedEmployeeForDetail = null }
        )
    }
}

/**
 * Top KPI Grid for Payroll Summary
 */
@Composable
private fun PayrollSummaryKpiGrid(
    summary: GlobalPayrollSummary,
    onRunPayroll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payroll_kpi_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(GoldAccent.copy(alpha = 0.45f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Execution Action
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
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Payroll",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GLOBAL PAYROLL RUN SUMMARY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${summary.totalEmployees} Employees across ${summary.jurisdictionCount} Jurisdictions",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onRunPayroll,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_execute_payroll_run")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Execute Run",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DISBURSE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // 4-Column Stat Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gross Payroll
                SummaryMetricTile(
                    title = "TOTAL GROSS",
                    value = "$${Formatters.formatCompactNumber(summary.totalGrossUsd)}",
                    subtitle = "Monthly Commitment",
                    tint = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Tax Withheld
                SummaryMetricTile(
                    title = "TAX WITHHELD",
                    value = "$${Formatters.formatCompactNumber(summary.totalTaxWithheldUsd)}",
                    subtitle = "${String.format(Locale.US, "%.1f", summary.overallEffectiveTaxRate)}% Effective",
                    tint = RoseNegative,
                    modifier = Modifier.weight(1f)
                )

                // Disbursed Net Pay
                SummaryMetricTile(
                    title = "NET DISBURSED",
                    value = "$${Formatters.formatCompactNumber(summary.totalNetPayUsd)}",
                    subtitle = "Worker Local Payouts",
                    tint = EmeraldPositive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Metric Tile
 */
@Composable
private fun SummaryMetricTile(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f))
        )),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                fontSize = 9.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Horizontal Carousel of Jurisdiction Summaries
 */
@Composable
private fun JurisdictionCarousel(
    jurisdictions: List<JurisdictionSummary>,
    selectedJurisdiction: String,
    onSelectJurisdiction: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "TAX JURISDICTIONS & REGIMES (${jurisdictions.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "ALL" Option Chip
            val isAllSelected = selectedJurisdiction == "ALL"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAllSelected) CyanAccent.copy(alpha = 0.15f) else SurfaceDark,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(if (isAllSelected) CyanAccent else BorderSubtle, BorderSubtle)
                    )
                ),
                modifier = Modifier
                    .clickable { onSelectJurisdiction("ALL") }
                    .testTag("jurisdiction_chip_all")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "🌐 Global",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllSelected) CyanAccent else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "All Regions",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Individual Jurisdictions
            jurisdictions.forEach { jur ->
                val isSelected = selectedJurisdiction == jur.countryCode
                val flag = Formatters.getCurrencyFlag(jur.currency)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CyanAccent.copy(alpha = 0.15f) else SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(if (isSelected) CyanAccent else BorderSubtle, BorderSubtle)
                        )
                    ),
                    modifier = Modifier
                        .clickable { onSelectJurisdiction(jur.countryCode) }
                        .testTag("jurisdiction_chip_${jur.countryCode.lowercase(Locale.US)}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = flag, fontSize = 13.sp)
                            Text(
                                text = "${jur.countryCode} • ${jur.currency}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyanAccent else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Net: ${jur.currencySymbol}${Formatters.formatCompactNumber(jur.totalNetPayLocal)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Withheld: ${String.format(Locale.US, "%.1f", jur.averageEffectiveTaxRate)}%",
                                fontSize = 9.sp,
                                color = RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${jur.employeeCount} staff)",
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
}

/**
 * Individual Employee Payroll Card
 * Displays gross pay, itemized statutory tax withholdings, and Net Pay in Local Currency
 */
@Composable
private fun EmployeePayrollCard(
    detail: EmployeePayrollDetail,
    onClick: () -> Unit
) {
    val isPaid = detail.paymentStatus == "PAID_THIS_CYCLE"
    val flag = Formatters.getCurrencyFlag(detail.currency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("employee_payroll_card_${detail.memberId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.4f))
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Name, Role, Flag, Employment Type
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
                            .size(32.dp)
                            .background(SurfaceElevated, CircleShape)
                            .border(1.dp, BorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = flag, fontSize = 16.sp)
                    }

                    Column {
                        Text(
                            text = detail.fullName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${detail.role} • ${detail.country}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isPaid) EmeraldPositive.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            0.5.dp,
                            if (isPaid) EmeraldPositive.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "PENDING DISPATCH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) EmeraldPositive else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Financial Summary Block: Gross -> Tax Withholding -> Net Pay (in Local Currency)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariantDark.copy(alpha = 0.6f),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                    listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.2f))
                )),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gross Pay
                    Column {
                        Text(
                            text = "GROSS PAY",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${detail.currencySymbol}${Formatters.formatCurrency(detail.grossPayLocal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = detail.currency,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Minus Icon / Divider
                    Text(
                        text = "-",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = RoseNegative
                    )

                    // Tax & Social Withholding
                    Column {
                        Text(
                            text = "TAX WITHHELD",
                            fontSize = 9.sp,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "-${detail.currencySymbol}${Formatters.formatCurrency(detail.totalTaxWithheldLocal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", detail.effectiveTaxRatePercent)}% Withholding",
                            fontSize = 9.sp,
                            color = RoseNegative.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Equals Icon / Divider
                    Text(
                        text = "=",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPositive
                    )

                    // Net Pay in Local Currency
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NET DISBURSED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${detail.currencySymbol}${Formatters.formatCurrency(detail.netPayLocal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "≈ $${Formatters.formatCurrency(detail.netPayUsd)} USD",
                            fontSize = 9.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Tax Jurisdiction & Banking Rail Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Jurisdiction",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = detail.taxJurisdiction,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View Pay Slip",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Pay Slip",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pay Slip & Tax Breakdown Detail Dialog
 * Itemizes every tax withholding deduction and shows net pay calculation
 */
@Composable
private fun PaySlipDetailDialog(
    detail: EmployeePayrollDetail,
    onDismiss: () -> Unit
) {
    val flag = Formatters.getCurrencyFlag(detail.currency)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = flag, fontSize = 20.sp)
                    Column {
                        Text(
                            text = detail.fullName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${detail.role} • ${detail.country}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = detail.employmentType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Regulatory Authority Banner
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TAX AUTHORITY & JURISDICTION:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = detail.taxAuthority,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = detail.statutoryNotes,
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Gross Pay Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gross Compensation:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${detail.currencySymbol}${Formatters.formatCurrency(detail.grossPayLocal)} ${detail.currency}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Itemized Deductions
                Text(
                    text = "ITEMIZED STATUTORY DEDUCTIONS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseNegative,
                    fontFamily = FontFamily.Monospace
                )

                if (detail.deductions.isEmpty()) {
                    Text(
                        text = "• No statutory withholdings at source (B2B Contractor reverse-charge schedule).",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        detail.deductions.forEach { deduction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = deduction.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (deduction.description.isNotEmpty()) {
                                        Text(
                                            text = deduction.description,
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "-${detail.currencySymbol}${Formatters.formatCurrency(deduction.amountLocal)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseNegative,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", deduction.ratePercent)}%",
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Total Deductions Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Statutory Withholding:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "-${detail.currencySymbol}${Formatters.formatCurrency(detail.totalTaxWithheldLocal)} (${String.format(Locale.US, "%.1f", detail.effectiveTaxRatePercent)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Prominent Net Disbursed Pay Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPositive.copy(alpha = 0.12f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                        listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.4f))
                    )),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NET PAY PER EMPLOYEE (LOCAL CURRENCY):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${detail.currencySymbol}${Formatters.formatCurrency(detail.netPayLocal)} ${detail.currency}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "≈ $${Formatters.formatCurrency(detail.netPayUsd)} USD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Banking Rail Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Disbursement Banking Rail:",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = detail.clearingRail,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark
    )
}
