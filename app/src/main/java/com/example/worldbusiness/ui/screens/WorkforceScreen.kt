package com.example.worldbusiness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.local.RoomPayrollCurrencyAggregation
import com.example.worldbusiness.data.local.RoomPayrollEmploymentTypeAggregation
import com.example.worldbusiness.data.local.RoomPayrollJurisdictionAggregation
import com.example.worldbusiness.data.model.BulkPayrollDisbursementReceipt
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalPayrollSummary
import com.example.worldbusiness.data.model.PayrollCurrencyDisbursementBatch
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.ui.components.Formatters
import com.example.worldbusiness.ui.components.PayrollSchedulingDisbursementWidget
import com.example.worldbusiness.ui.components.PayrollTaxSummaryInterface
import com.example.worldbusiness.ui.components.ProjectedPayrollSummaryView

enum class WorkforceViewMode(val label: String) {
    PROJECTED_EXPENSES("Projected Payroll"),
    PAYROLL_SUMMARY("Tax Withholdings"),
    DISBURSEMENT("Bulk Disbursement"),
    ROSTER("Staff Directory")
}

@Composable
fun WorkforceScreen(
    teamMembers: List<TeamMemberRecord>,
    monthlyPayrollUsd: Double,
    payrollSummary: GlobalPayrollSummary = GlobalPayrollSummary(0, 0.0, 0.0, 0.0, 0.0, 0, emptyList(), emptyList()),
    balances: List<FxBalanceRecord> = emptyList(),
    roomCurrencies: List<RoomPayrollCurrencyAggregation> = emptyList(),
    roomJurisdictions: List<RoomPayrollJurisdictionAggregation> = emptyList(),
    roomEmploymentTypes: List<RoomPayrollEmploymentTypeAggregation> = emptyList(),
    thresholdAlerts: List<RegulatoryThresholdAlert> = emptyList(),
    onAcknowledgeThresholdAlert: (String) -> Unit = {},
    onFileThresholdReport: (String) -> Unit = {},
    onRunPayroll: () -> Unit,
    onExecuteBulkDisbursement: ((
        scheduledDate: String,
        fundingCurrency: String,
        batches: List<PayrollCurrencyDisbursementBatch>,
        selectedMemberIds: Set<Long>,
        onSuccess: (BulkPayrollDisbursementReceipt) -> Unit
    ) -> Unit)? = null,
    onFetchLiveRate: (suspend (fromCurrency: String, toCurrency: String) -> Double)? = null,
    onAddMember: (
        name: String,
        role: String,
        country: String,
        code: String,
        type: String,
        salary: Double,
        currency: String,
        taxJurisdiction: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(WorkforceViewMode.PROJECTED_EXPENSES) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredMembers = when (selectedFilter) {
        "SUBSIDIARY" -> teamMembers.filter { it.employmentType.contains("Subsidiary", ignoreCase = true) }
        "EOR" -> teamMembers.filter { it.employmentType.contains("EOR", ignoreCase = true) }
        "CONTRACTOR" -> teamMembers.filter { it.employmentType.contains("Contractor", ignoreCase = true) }
        else -> teamMembers
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("workforce_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DISTRIBUTED WORKFORCE & PAYROLL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${teamMembers.size} Multinational Hires",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewMode = WorkforceViewMode.DISBURSEMENT },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    modifier = Modifier.testTag("btn_open_disbursement")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Bulk Disbursement",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DISBURSE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_onboard_hire")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Onboard Hire",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ONBOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Workforce View Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkforceViewMode.values().forEach { mode ->
                val isSelected = viewMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent else SurfaceDark)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { viewMode = mode }
                        .padding(vertical = 9.dp)
                        .testTag("workforce_tab_${mode.name.lowercase(Locale.US)}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (viewMode) {
            WorkforceViewMode.PROJECTED_EXPENSES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ProjectedPayrollSummaryView(
                            summary = payrollSummary,
                            balances = balances,
                            teamMembers = teamMembers,
                            roomCurrencies = roomCurrencies,
                            roomJurisdictions = roomJurisdictions,
                            roomEmploymentTypes = roomEmploymentTypes,
                            thresholdAlerts = thresholdAlerts,
                            onAcknowledgeThresholdAlert = onAcknowledgeThresholdAlert,
                            onFileThresholdReport = onFileThresholdReport,
                            onRunPayroll = onRunPayroll,
                            onNavigateToDisbursement = { viewMode = WorkforceViewMode.DISBURSEMENT }
                        )
                    }
                }
            }

            WorkforceViewMode.PAYROLL_SUMMARY -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PayrollTaxSummaryInterface(
                            summary = payrollSummary,
                            onRunPayroll = onRunPayroll
                        )
                    }
                }
            }

            WorkforceViewMode.DISBURSEMENT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PayrollSchedulingDisbursementWidget(
                            teamMembers = teamMembers,
                            balances = balances,
                            onExecuteDisbursement = { schedDate, fundCurr, batches, memberIds, onSuccess ->
                                if (onExecuteBulkDisbursement != null) {
                                    onExecuteBulkDisbursement(schedDate, fundCurr, batches, memberIds, onSuccess)
                                } else {
                                    onRunPayroll()
                                }
                            },
                            onFetchLiveRate = onFetchLiveRate ?: { _, _ -> 1.0 }
                        )
                    }
                }
            }

            WorkforceViewMode.ROSTER -> {
                // Payroll Run Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("payroll_dispatch_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "MONTHLY MULTI-CURRENCY PAYROLL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = Formatters.formatUsd(monthlyPayrollUsd),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = onRunPayroll,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SurfaceDark),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_run_global_payroll")
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = "Run Payroll", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "EXECUTE RUN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Batch automated dispatch across BACS, FAST, SIC, PIX & ACH banking rails.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "SUBSIDIARY", "EOR", "CONTRACTOR").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanAccent else SurfaceElevated)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("filter_team_$filter")
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        TeamMemberCard(member = member)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        OnboardHireDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, role, country, code, type, salary, curr, tax ->
                onAddMember(name, role, country, code, type, salary, curr, tax)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TeamMemberCard(member: TeamMemberRecord) {
    val isPaidThisCycle = member.status == "PAID_THIS_CYCLE"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
            .testTag("team_card_${member.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = member.countryCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = member.country,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPaidThisCycle) EmeraldPositive.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPaidThisCycle) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = "Status",
                        tint = if (isPaidThisCycle) EmeraldPositive else CyanAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isPaidThisCycle) "PAID THIS CYCLE" else member.employmentType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaidThisCycle) EmeraldPositive else CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = member.fullName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = member.role,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "MONTHLY COMPENSATION", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = Formatters.formatCurrency(member.monthlyCompensation, member.currency),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "TAX WITHHOLDING", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = member.taxJurisdiction,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardHireDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        role: String,
        country: String,
        code: String,
        type: String,
        salary: Double,
        currency: String,
        taxJurisdiction: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Germany") }
    var code by remember { mutableStateOf("DE") }
    var type by remember { mutableStateOf("Direct Subsidiary") }
    var salaryStr by remember { mutableStateOf("14000") }
    var currency by remember { mutableStateOf("EUR") }
    var tax by remember { mutableStateOf("Finanzamt Lohnsteuer (German Tax)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "ONBOARD MULTINATIONAL HIRE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Candidate Full Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Henrik Richter") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Executive / Staff Role", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Director of European Solutions") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("ISO", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Contract Type (Direct, EOR, B2B)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = salaryStr,
                        onValueChange = { salaryStr = it },
                        label = { Text("Monthly Pay", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = tax,
                    onValueChange = { tax = it },
                    label = { Text("Tax Authority / Withholding", fontSize = 11.sp) },
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
            Button(
                onClick = {
                    if (name.isNotBlank() && role.isNotBlank()) {
                        val sal = salaryStr.toDoubleOrNull() ?: 10000.0
                        onConfirm(name, role, country, code, type, sal, currency, tax)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("ENROLL IN GLOBAL PAYROLL", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
