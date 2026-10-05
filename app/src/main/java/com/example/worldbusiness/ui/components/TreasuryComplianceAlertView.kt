package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import com.example.worldbusiness.data.model.ComplianceAlert
import com.example.worldbusiness.data.model.ComplianceAlertStatus
import com.example.worldbusiness.data.model.ComplianceOverviewStats
import com.example.worldbusiness.data.model.ComplianceSeverity
import java.util.Locale

/**
 * Real-Time Compliance Alert System View
 * Monitors treasury transactions and flags potential regulatory violations or anti-money laundering (AML) risks.
 */
@Composable
fun TreasuryComplianceAlertView(
    alerts: List<ComplianceAlert>,
    stats: ComplianceOverviewStats,
    onRemediateAlert: (alertId: String, newStatus: ComplianceAlertStatus) -> Unit,
    onSimulateHighRiskTx: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedJurisdiction by remember { mutableStateOf("ALL") }
    var selectedSeverity by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAlertForRemediation by remember { mutableStateOf<ComplianceAlert?>(null) }

    val filteredAlerts = remember(alerts, selectedJurisdiction, selectedSeverity, searchQuery) {
        alerts.filter { alert ->
            val matchesJur = selectedJurisdiction == "ALL" || alert.jurisdictionCode.equals(selectedJurisdiction, ignoreCase = true)
            val matchesSev = selectedSeverity == "ALL" || when (selectedSeverity) {
                "CRITICAL" -> alert.severity == ComplianceSeverity.CRITICAL
                "HIGH" -> alert.severity == ComplianceSeverity.HIGH
                "MEDIUM" -> alert.severity == ComplianceSeverity.MEDIUM
                "RESOLVED" -> alert.status == ComplianceAlertStatus.RESOLVED || alert.status == ComplianceAlertStatus.SAR_REPORTED || alert.status == ComplianceAlertStatus.WHITELISTED
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                alert.title.contains(searchQuery, ignoreCase = true) ||
                alert.counterparty.contains(searchQuery, ignoreCase = true) ||
                alert.transactionReference.contains(searchQuery, ignoreCase = true) ||
                alert.statutoryRuleCode.contains(searchQuery, ignoreCase = true) ||
                alert.regulatoryBody.contains(searchQuery, ignoreCase = true)

            matchesJur && matchesSev && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("treasury_compliance_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Compliance Cockpit Banner
        ComplianceCockpitHeader(
            stats = stats,
            onSimulateHighRisk = onSimulateHighRiskTx
        )

        // Search & Filter Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search rule code, counterparty, or reference...", fontSize = 11.sp, color = TextMuted) },
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
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("compliance_search_input"),
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

        // Jurisdiction Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All Jurisdictions (${alerts.size})",
                "US" to "🇺🇸 US (FinCEN/OFAC)",
                "CH" to "🇨🇭 Switzerland (FINMA)",
                "SG" to "🇸🇬 Singapore (MAS)",
                "EU" to "🇪🇺 European Union (AMLD6)",
                "BR" to "🇧🇷 Brazil (BACEN/COAF)"
            ).forEach { (jurKey, label) ->
                val isSelected = selectedJurisdiction == jurKey
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { selectedJurisdiction = jurKey }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("filter_jur_$jurKey")
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

        // Severity Sub-Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All Alerts",
                "CRITICAL" to "Critical AML",
                "HIGH" to "High Risk",
                "MEDIUM" to "Moderate",
                "RESOLVED" to "Resolved"
            ).forEach { (sevKey, label) ->
                val isSelected = selectedSeverity == sevKey
                val badgeColor = when (sevKey) {
                    "CRITICAL" -> RoseNegative
                    "HIGH" -> GoldAccent
                    "MEDIUM" -> CyanAccent
                    "RESOLVED" -> EmeraldPositive
                    else -> TextSecondary
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) badgeColor.copy(alpha = 0.2f) else SurfaceDark, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) badgeColor else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { selectedSeverity = sevKey }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) badgeColor else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Alerts List Section Header
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
                    imageVector = Icons.Default.Gavel,
                    contentDescription = "Regulatory",
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "REGULATORY AUDIT TRAIL (${filteredAlerts.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "REAL-TIME MONITORING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        // Alerts List
        if (filteredAlerts.isEmpty()) {
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
                    text = "No compliance flags match the selected criteria.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredAlerts.forEach { alert ->
                    ComplianceAlertCard(
                        alert = alert,
                        onClick = { selectedAlertForRemediation = alert }
                    )
                }
            }
        }
    }

    // Interactive Alert Remediation Dialog
    selectedAlertForRemediation?.let { alert ->
        ComplianceRemediationDialog(
            alert = alert,
            onDismiss = { selectedAlertForRemediation = null },
            onUpdateStatus = { newStatus ->
                onRemediateAlert(alert.id, newStatus)
                selectedAlertForRemediation = null
            }
        )
    }
}

/**
 * Top Executive Compliance Cockpit Header
 */
@Composable
private fun ComplianceCockpitHeader(
    stats: ComplianceOverviewStats,
    onSimulateHighRisk: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compliance_cockpit_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.45f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Real-Time Scanning Action
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Compliance",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "REAL-TIME COMPLIANCE & AML MONITOR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(EmeraldPositive.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                        Text(
                            text = "${stats.totalTransactionsAudited} Treasury Transactions Scanned • ${stats.monitoredJurisdictionsCount} Jurisdictions Protected",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onSimulateHighRisk,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNegative, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_simulate_risk_alert")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Simulate",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TEST INJECT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // 4-Column Stat Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compliance Health Score
                ComplianceKpiTile(
                    title = "HEALTH SCORE",
                    value = "${stats.complianceHealthScore}/100",
                    subtitle = if (stats.complianceHealthScore >= 90) "Investment Grade" else "Action Required",
                    tint = if (stats.complianceHealthScore >= 80) EmeraldPositive else RoseNegative,
                    modifier = Modifier.weight(1f)
                )

                // Critical AML Flags
                ComplianceKpiTile(
                    title = "CRITICAL FLAGS",
                    value = "${stats.criticalFlagsCount} Critical",
                    subtitle = "${stats.activeFlagsCount} Total Active",
                    tint = if (stats.criticalFlagsCount > 0) RoseNegative else EmeraldPositive,
                    modifier = Modifier.weight(1f)
                )

                // High Risk Volume
                ComplianceKpiTile(
                    title = "FLAGGED VOLUME",
                    value = "$${Formatters.formatCompactNumber(stats.totalHighRiskVolumeUsd)}",
                    subtitle = "Escrow / Wire Hold",
                    tint = if (stats.totalHighRiskVolumeUsd > 0) GoldAccent else TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Remediated / Closed
                ComplianceKpiTile(
                    title = "REMEDIATED",
                    value = "${stats.resolvedCount} Audited",
                    subtitle = "${stats.underReviewCount} In Review",
                    tint = EmeraldPositive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Metric Tile for Compliance Cockpit
 */
@Composable
private fun ComplianceKpiTile(
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Compliance Alert Card
 */
@Composable
private fun ComplianceAlertCard(
    alert: ComplianceAlert,
    onClick: () -> Unit
) {
    val severityColor = when (alert.severity) {
        ComplianceSeverity.CRITICAL -> RoseNegative
        ComplianceSeverity.HIGH -> GoldAccent
        ComplianceSeverity.MEDIUM -> CyanAccent
        ComplianceSeverity.LOW, ComplianceSeverity.CLEARED -> EmeraldPositive
    }

    val isResolved = alert.status == ComplianceAlertStatus.RESOLVED ||
        alert.status == ComplianceAlertStatus.WHITELISTED ||
        alert.status == ComplianceAlertStatus.SAR_REPORTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("compliance_alert_card_${alert.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(if (isResolved) BorderSubtle else severityColor.copy(alpha = 0.45f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Severity Badge, Rule Code, Jurisdiction & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Severity Badge
                    Box(
                        modifier = Modifier
                            .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, severityColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.severity.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Rule Code
                    Box(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.statutoryRuleCode,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isResolved) EmeraldPositive.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            0.5.dp,
                            if (isResolved) EmeraldPositive.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.status.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isResolved) EmeraldPositive else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = alert.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            // Transaction & Counterparty Details Surface
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
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
                            text = "COUNTERPARTY / REF",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = alert.counterparty,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = alert.transactionReference,
                            fontSize = 9.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TRANSACTION VALUE",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${alert.currency} ${Formatters.formatCurrency(alert.amount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "≈ $${Formatters.formatCurrency(alert.amountUsd)} USD",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Footer: Regulatory Body & Remediation Recommendation
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
                        imageVector = Icons.Default.Policy,
                        contentDescription = "Agency",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = alert.regulatoryBody,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Inspect & Remediate",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "Inspect",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compliance Remediation Dialog
 * Enables compliance officers to review the statutory rule and execute resolution actions.
 */
@Composable
private fun ComplianceRemediationDialog(
    alert: ComplianceAlert,
    onDismiss: () -> Unit,
    onUpdateStatus: (ComplianceAlertStatus) -> Unit
) {
    val severityColor = when (alert.severity) {
        ComplianceSeverity.CRITICAL -> RoseNegative
        ComplianceSeverity.HIGH -> GoldAccent
        ComplianceSeverity.MEDIUM -> CyanAccent
        ComplianceSeverity.LOW, ComplianceSeverity.CLEARED -> EmeraldPositive
    }

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
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Legal",
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "REGULATORY AUDIT AUDIT TRAIL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = alert.statutoryRuleCode,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(SurfaceElevated, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.jurisdictionName,
                        fontSize = 9.sp,
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = alert.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Text(
                    text = alert.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = BorderSubtle)

                // Recommended Remediation Step
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                        listOf(severityColor.copy(alpha = 0.4f), BorderSubtle)
                    )),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "RECOMMENDED REMEDIATION ACTION:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = alert.remediationRecommendation,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "Mandatory Filing: ${alert.requiredFiling}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Remediation Action Buttons
                Text(
                    text = "EXECUTE COMPLIANCE DECISION:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Option 1: File SAR / Regulatory Report
                    Button(
                        onClick = { onUpdateStatus(ComplianceAlertStatus.SAR_REPORTED) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseNegative.copy(alpha = 0.2f), contentColor = RoseNegative),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(RoseNegative, BorderSubtle))),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "SAR", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FILE SUSPICIOUS ACTIVITY REPORT (SAR)", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Option 2: Place Under Review
                    Button(
                        onClick = { onUpdateStatus(ComplianceAlertStatus.UNDER_REVIEW) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f), contentColor = GoldAccent),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldAccent, BorderSubtle))),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FindInPage, contentDescription = "Review", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PLACE UNDER AUDIT REVIEW / REQUEST EDD", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Option 3: Resolve & Clear
                    Button(
                        onClick = { onUpdateStatus(ComplianceAlertStatus.RESOLVED) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Resolve", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESOLVE & MARK AUDIT COMPLIANT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
