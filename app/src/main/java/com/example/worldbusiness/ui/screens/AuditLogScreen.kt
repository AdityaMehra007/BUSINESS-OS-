package com.example.worldbusiness.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import com.example.worldbusiness.data.model.AuditActionType
import com.example.worldbusiness.data.model.AuditChainVerificationResult
import com.example.worldbusiness.data.model.AuditLogModule
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.AuditLogStatus
import com.example.worldbusiness.data.model.AuditLogSummaryStats
import com.example.worldbusiness.data.repository.AuditCryptographicEngine
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    auditLogs: List<AuditLogRecord>,
    summaryStats: AuditLogSummaryStats,
    onRecordAdminAction: (
        actionType: AuditActionType,
        module: AuditLogModule,
        description: String,
        amount: Double?,
        currency: String?,
        sourceJurisdiction: String,
        destinationJurisdiction: String?
    ) -> Unit,
    onVerifyChain: () -> AuditChainVerificationResult,
    onExportCsv: () -> String,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedModuleFilter by remember { mutableStateOf("ALL") }
    var showRecordActionDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<AuditChainVerificationResult?>(null) }
    var inspectedRecord by remember { mutableStateOf<AuditLogRecord?>(null) }
    var exportedCsvContent by remember { mutableStateOf<String?>(null) }

    val filteredLogs = remember(auditLogs, searchQuery, selectedModuleFilter) {
        auditLogs.filter { log ->
            val matchesModule = when (selectedModuleFilter) {
                "ALL" -> true
                else -> log.module == selectedModuleFilter
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.logId.contains(searchQuery, ignoreCase = true) ||
                log.description.contains(searchQuery, ignoreCase = true) ||
                log.actorUsername.contains(searchQuery, ignoreCase = true) ||
                log.sourceJurisdiction.contains(searchQuery, ignoreCase = true) ||
                (log.destinationJurisdiction?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesModule && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("audit_log_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Header with Cryptographic Verification Trigger
        item {
            AuditHeaderSection(
                summaryStats = summaryStats,
                onVerifyClick = {
                    verificationResult = onVerifyChain()
                    showVerificationDialog = true
                },
                onRecordClick = { showRecordActionDialog = true },
                onExportClick = { exportedCsvContent = onExportCsv() }
            )
        }

        // 4-Pillar Metric Strip
        item {
            AuditKpiStripCard(summaryStats = summaryStats)
        }

        // Search and Module Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_search_input"),
                    placeholder = {
                        Text(
                            text = "Search by Log ID, Actor, Jurisdiction, or Keyword...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Horizontal Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All Modules (${auditLogs.size})",
                        AuditLogModule.TREASURY.name to "Treasury & FX",
                        AuditLogModule.COMMERCIAL.name to "Invoicing & Settlements",
                        AuditLogModule.ENTITIES.name to "Corporate Entities",
                        AuditLogModule.WORKFORCE.name to "Workforce & Payroll",
                        AuditLogModule.LOGISTICS.name to "Logistics",
                        AuditLogModule.TAX_COMPLIANCE.name to "Tax Filings",
                        AuditLogModule.SYSTEM_ADMIN.name to "Admin & Security"
                    ).forEach { (key, label) ->
                        val isSelected = selectedModuleFilter == key
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedModuleFilter = key }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("filter_audit_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIT TRAIL LEDGER (${filteredLogs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "SHA-256 HASH-CHAINED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPositive,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Audit Items List
        if (filteredLogs.isNotEmpty()) {
            items(filteredLogs, key = { it.id }) { log ->
                AuditLogCard(
                    log = log,
                    onInspectClick = { inspectedRecord = log }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = TextMuted, modifier = Modifier.size(32.dp))
                        Text(
                            text = "No Audit Records Found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "No audit entries match the current search or module filter.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Record Admin Action Dialog
    if (showRecordActionDialog) {
        RecordAdminActionDialog(
            onDismiss = { showRecordActionDialog = false },
            onConfirm = { actionType, module, desc, amount, curr, src, dest ->
                onRecordAdminAction(actionType, module, desc, amount, curr, src, dest)
                showRecordActionDialog = false
            }
        )
    }

    // Cryptographic Chain Verification Modal
    if (showVerificationDialog && verificationResult != null) {
        CryptographicVerificationDialog(
            result = verificationResult!!,
            onDismiss = { showVerificationDialog = false }
        )
    }

    // Forensic Record Detail Modal
    inspectedRecord?.let { log ->
        ForensicInspectionDialog(
            log = log,
            onDismiss = { inspectedRecord = null }
        )
    }

    // Exported CSV Modal
    exportedCsvContent?.let { csv ->
        ExportedReportDialog(
            csv = csv,
            onDismiss = { exportedCsvContent = null }
        )
    }
}

/**
 * Top Header with Real-time Integrity indicator and Action Buttons.
 */
@Composable
private fun AuditHeaderSection(
    summaryStats: AuditLogSummaryStats,
    onVerifyClick: () -> Unit,
    onRecordClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.Security,
                            contentDescription = "Audit Engine",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CENTRALIZED AUDIT TRAIL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "SOX 404 & MiFID II Tamper-Evident Ledger",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (summaryStats.chainIntegrityValid) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (summaryStats.chainIntegrityValid) listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.4f))
                            else listOf(RoseNegative, RoseNegative.copy(alpha = 0.4f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (summaryStats.chainIntegrityValid) EmeraldPositive else RoseNegative, CircleShape)
                        )
                        Text(
                            text = if (summaryStats.chainIntegrityValid) "IMMUTABLE CERTIFIED" else "INTEGRITY ALERT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summaryStats.chainIntegrityValid) EmeraldPositive else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onVerifyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_verify_audit_chain")
                ) {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = "Verify", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "VERIFY CHAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onRecordClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_record_admin_action")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Record", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "LOG ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                OutlinedButton(
                    onClick = onExportClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.testTag("btn_export_audit_csv")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * 4-Pillar KPI Strip Card
 */
@Composable
private fun AuditKpiStripCard(summaryStats: AuditLogSummaryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "TOTAL AUDITED", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "${summaryStats.totalLogsCount} Events",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "CROSS-BORDER", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "${summaryStats.crossBorderTxCount} Flows",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "AUDITED VOLUME", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "$${NumberFormat.getNumberInstance(Locale.US).format((summaryStats.totalVolumeAuditedUsd / 1000000.0).roundToInt())}M USD",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = GoldAccent,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "IMMUTABILITY", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(
                text = "${summaryStats.verifiedImmutablePercent.roundToInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Individual Audit Log Card with cryptographic hash details.
 */
@Composable
private fun AuditLogCard(
    log: AuditLogRecord,
    onInspectClick: () -> Unit
) {
    val moduleColor = when (log.module) {
        AuditLogModule.TREASURY.name -> CyanAccent
        AuditLogModule.COMMERCIAL.name -> EmeraldPositive
        AuditLogModule.ENTITIES.name -> GoldAccent
        AuditLogModule.WORKFORCE.name -> Color(0xFFB388FF)
        AuditLogModule.LOGISTICS.name -> Color(0xFFFF80AB)
        AuditLogModule.TAX_COMPLIANCE.name -> Color(0xFF80D8FF)
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("audit_card_${log.logId.lowercase(Locale.US)}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Log ID, Module Pill, Timestamp, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(4.dp))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.logId,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(moduleColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.module.take(4).uppercase(Locale.US),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = moduleColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = log.timestampFormatted,
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldPositive.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = EmeraldPositive, modifier = Modifier.size(10.dp))
                        Text(text = "VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Description
            Text(
                text = log.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 16.sp
            )

            // Actor & Route
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "ACTOR / ROLE", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${log.actorUsername} (${log.actorRole})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (log.destinationJurisdiction != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "CROSS-BORDER FLOW", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${log.sourceJurisdiction.take(16)} ➔ ${log.destinationJurisdiction.take(16)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Financial & Cryptographic Hash Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (log.financialAmount != null && log.financialAmount > 0.0) {
                    Text(
                        text = "Amount: ${NumberFormat.getNumberInstance(Locale.US).format(log.financialAmount.roundToInt())} ${log.currency ?: "USD"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Standard: ${log.complianceStandard}",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onInspectClick() }
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Hash", tint = CyanAccent, modifier = Modifier.size(12.dp))
                    Text(
                        text = "SHA: ${log.cryptographicHash.take(8)}...${log.cryptographicHash.takeLast(6)}",
                        fontSize = 8.sp,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Record Administrative Action Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordAdminActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        actionType: AuditActionType,
        module: AuditLogModule,
        description: String,
        amount: Double?,
        currency: String?,
        sourceJurisdiction: String,
        destinationJurisdiction: String?
    ) -> Unit
) {
    var selectedModule by remember { mutableStateOf(AuditLogModule.SYSTEM_ADMIN) }
    var selectedAction by remember { mutableStateOf(AuditActionType.MANUAL_COMPLIANCE_OVERRIDE) }
    var description by remember { mutableStateOf("") }
    var sourceJurisdiction by remember { mutableStateOf("Global Sovereign Enterprise Hub") }
    var destinationJurisdiction by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }

    var moduleDropdownExpanded by remember { mutableStateOf(false) }
    var actionDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Record", tint = CyanAccent)
                    Text(
                        text = "LOG ADMINISTRATIVE ACTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Records an immutable administrative action into the enterprise compliance chain.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                // Module Selector
                ExposedDropdownMenuBox(
                    expanded = moduleDropdownExpanded,
                    onExpandedChange = { moduleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModule.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Module", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = moduleDropdownExpanded,
                        onDismissRequest = { moduleDropdownExpanded = false }
                    ) {
                        AuditLogModule.values().forEach { mod ->
                            DropdownMenuItem(
                                text = { Text(mod.label, fontSize = 11.sp) },
                                onClick = {
                                    selectedModule = mod
                                    moduleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Action Type Selector
                ExposedDropdownMenuBox(
                    expanded = actionDropdownExpanded,
                    onExpandedChange = { actionDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAction.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Type", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = actionDropdownExpanded,
                        onDismissRequest = { actionDropdownExpanded = false }
                    ) {
                        AuditActionType.values().forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.label, fontSize = 11.sp) },
                                onClick = {
                                    selectedAction = act
                                    actionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Audit Justification / Description", fontSize = 10.sp) },
                    placeholder = { Text("e.g. Quarterly SOX 404 security key verification", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_admin_audit_desc")
                )

                // Source Jurisdiction
                OutlinedTextField(
                    value = sourceJurisdiction,
                    onValueChange = { sourceJurisdiction = it },
                    label = { Text("Operating Jurisdiction", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    val dest = if (destinationJurisdiction.isNotBlank()) destinationJurisdiction else null
                    val finalDesc = if (description.isNotBlank()) description else selectedAction.label
                    onConfirm(selectedAction, selectedModule, finalDesc, amount, currency, sourceJurisdiction, dest)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_log_action")
            ) {
                Text(text = "COMMIT TO AUDIT LOG", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Cryptographic Chain Verification Modal
 */
@Composable
private fun CryptographicVerificationDialog(
    result: AuditChainVerificationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Status",
                    tint = if (result.isValid) EmeraldPositive else RoseNegative
                )
                Text(
                    text = if (result.isValid) "CHAIN INTEGRITY VERIFIED" else "INTEGRITY COMPROMISED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isValid) EmeraldPositive else RoseNegative,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (result.isValid) {
                        "All ${result.totalRecordsVerified} audit records have been cryptographically re-hashed and mathematically verified against their parent SHA-256 blocks. Zero tampering or sequence corruption detected."
                    } else {
                        "Discrepancy detected at log ID ${result.compromisedRecordId}. The SHA-256 block digest failed to reconcile with parent hash."
                    },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "CRYPTOGRAPHIC PROOF METRICS", fontSize = 8.sp, color = CyanAccent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = "Total Verified: ${result.totalRecordsVerified} Blocks", fontSize = 10.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        Text(text = "Root Hash: ${result.rootHash.take(16)}...", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "Latest Tip: ${result.latestHash.take(16)}...", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "Timestamp: ${result.verificationTimestamp}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text(text = "DISMISS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Forensic Detail Inspection Dialog
 */
@Composable
private fun ForensicInspectionDialog(
    log: AuditLogRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Forensics", tint = CyanAccent)
                    Text(
                        text = "FORENSIC BLOCK INSPECTOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ForensicRow("Log ID", log.logId)
                ForensicRow("Timestamp", log.timestampFormatted)
                ForensicRow("Actor", "${log.actorUsername} (${log.actorRole})")
                ForensicRow("Actor IP", log.actorIpAddress)
                ForensicRow("Action Type", log.actionType)
                ForensicRow("Module", log.module)
                ForensicRow("Jurisdiction", log.sourceJurisdiction)
                if (log.destinationJurisdiction != null) {
                    ForensicRow("Destination", log.destinationJurisdiction)
                }
                if (log.financialAmount != null) {
                    ForensicRow("Amount", "${log.financialAmount} ${log.currency ?: "USD"}")
                }
                ForensicRow("Compliance", log.complianceStandard)

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                Text(text = "PREVIOUS BLOCK HASH (PARENT):", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(text = log.previousHash, fontSize = 8.sp, color = GoldAccent, fontFamily = FontFamily.Monospace)

                Text(text = "CURRENT BLOCK HASH (SHA-256):", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(text = log.cryptographicHash, fontSize = 8.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text(text = "DONE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Exported Report Dialog
 */
@Composable
private fun ExportedReportDialog(
    csv: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Export", tint = CyanAccent)
                Text(text = "AUDIT TRAIL EXPORT READY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Formatted CSV data with full SHA-256 cryptographic hashes for regulatory submission (SOX 404 / MiFID II / FinCEN):", fontSize = 11.sp, color = TextSecondary)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp).horizontalScroll(rememberScrollState())) {
                        Text(text = csv, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)) {
                Text(text = "CLOSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun ForensicRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}
