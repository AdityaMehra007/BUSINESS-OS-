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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import com.example.worldbusiness.data.model.RegulatoryThresholdAlert
import com.example.worldbusiness.data.model.RegulatoryThresholdStats
import com.example.worldbusiness.data.model.ThresholdAlertStatus
import com.example.worldbusiness.data.model.ThresholdCategory
import java.util.Locale

/**
 * High-priority Alert Ribbon alerting users when payroll expenses or cross-border
 * wire transfers approach statutory local reporting thresholds.
 */
@Composable
fun RegulatoryThresholdAlertBanner(
    alerts: List<RegulatoryThresholdAlert>,
    onOpenModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAlerts = alerts.filter { it.status == ThresholdAlertStatus.APPROACHING_THRESHOLD || it.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED }
    if (activeAlerts.isEmpty()) return

    val hasExceeded = activeAlerts.any { it.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED }
    val bannerColor = if (hasExceeded) RoseNegative else GoldAccent
    val payrollCount = activeAlerts.count { it.category == ThresholdCategory.PAYROLL_EXPENSES }
    val transferCount = activeAlerts.count { it.category == ThresholdCategory.CROSS_BORDER_TRANSFER }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bannerColor.copy(alpha = 0.12f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(bannerColor.copy(alpha = 0.7f), BorderSubtle))
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenModal)
            .testTag("regulatory_compliance_notification_system")
            .testTag("threshold_alert_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(bannerColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasExceeded) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = "Threshold Warning",
                        tint = bannerColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (hasExceeded) "STATUTORY THRESHOLD EXCEEDED" else "APPROACHING REGULATORY THRESHOLDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = bannerColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(bannerColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${activeAlerts.size} ALERTS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = bannerColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "$payrollCount payroll obligation${if (payrollCount > 1) "s" else ""} • $transferCount cross-border wire${if (transferCount > 1) "s" else ""} near statutory filing limits",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Review ➔",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = bannerColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Dedicated Card displaying a single Regulatory Threshold Alert with statutory citations,
 * current vs limit utilization progress bar, and filing actions.
 */
@Composable
fun RegulatoryThresholdAlertCard(
    alert: RegulatoryThresholdAlert,
    onAcknowledge: (String) -> Unit,
    onFileReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExceeded = alert.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED
    val isHandled = alert.status == ThresholdAlertStatus.ACKNOWLEDGED || alert.status == ThresholdAlertStatus.REPORT_FILED
    val accentColor = when {
        isHandled -> EmeraldPositive
        isExceeded -> RoseNegative
        else -> GoldAccent
    }

    val categoryIcon = if (alert.category == ThresholdCategory.PAYROLL_EXPENSES) Icons.Default.Payments else Icons.Default.SyncAlt

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("threshold_alert_card")
            .testTag(if (alert.category == ThresholdCategory.PAYROLL_EXPENSES) "payroll_threshold_alert" else "crossborder_threshold_alert"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.6f), BorderSubtle))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Badges & Jurisdictions
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
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.jurisdictionCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = alert.category.label,
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (alert.category == ThresholdCategory.PAYROLL_EXPENSES) "PAYROLL" else "CROSS-BORDER WIRE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = alert.status.label.uppercase(Locale.US),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Title & Regulatory Body
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${alert.regulatoryBody} • ${alert.statutoryReference}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Progress Bar & Financial Utilization
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUTORY LIMIT UTILIZATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", alert.utilizationPercent)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .testTag("threshold_progress_bar")
                ) {
                    val progressFraction = (alert.utilizationPercent / 100.0).toFloat().coerceIn(0.01f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current: ${Formatters.formatCurrency(alert.currentAmount)} ${alert.currency} (~$${Formatters.formatCompactNumber(alert.currentAmountUsd)})",
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Limit: ${Formatters.formatCurrency(alert.thresholdLimit)} ${alert.currency}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Description Callout
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceElevated.copy(alpha = 0.5f),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = alert.description,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Filing",
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Filing Required: ${alert.requiredFilingForm}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Statutory Deadline: ${alert.reportingDeadline}",
                        fontSize = 9.sp,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Remediation Recommendation
            Text(
                text = "ACTION: ${alert.remediationAction}",
                fontSize = 9.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            // Action Buttons
            if (!isHandled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAcknowledge(alert.id) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_acknowledge_threshold_alert")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "Acknowledge",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACKNOWLEDGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { onFileReport(alert.id) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_file_regulatory_report")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "File Report",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SUBMIT FILING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = "Handled",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Regulatory filing recorded on immutable cryptographic audit log.",
                        fontSize = 10.sp,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Full Notification Center Dialog for Regulatory Reporting Thresholds
 */
@Composable
fun RegulatoryThresholdModal(
    alerts: List<RegulatoryThresholdAlert>,
    stats: RegulatoryThresholdStats,
    onDismiss: () -> Unit,
    onAcknowledge: (String) -> Unit,
    onFileReport: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            "PAYROLL" -> alerts.filter { it.category == ThresholdCategory.PAYROLL_EXPENSES }
            "TRANSFERS" -> alerts.filter { it.category == ThresholdCategory.CROSS_BORDER_TRANSFER }
            "EXCEEDED" -> alerts.filter { it.status == ThresholdAlertStatus.THRESHOLD_EXCEEDED }
            "APPROACHING" -> alerts.filter { it.status == ThresholdAlertStatus.APPROACHING_THRESHOLD }
            else -> alerts
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("regulatory_threshold_modal"),
        containerColor = SurfaceDark,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CLOSE", color = CyanAccent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
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
                        contentDescription = "Regulatory Compliance",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "REGULATORY THRESHOLD NOTIFICATIONS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${alerts.size} Active statutory thresholds monitored in real-time",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Metrics Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModalMetricPill("TOTAL", "${stats.totalAlertsCount}", CyanAccent, Modifier.weight(1f))
                    ModalMetricPill("EXCEEDED", "${stats.exceededCount}", RoseNegative, Modifier.weight(1f))
                    ModalMetricPill("NEAR LIMIT", "${stats.approachingCount}", GoldAccent, Modifier.weight(1f))
                    ModalMetricPill("FILED", "${stats.acknowledgedCount}", EmeraldPositive, Modifier.weight(1f))
                }

                // Filter Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All (${alerts.size})",
                        "PAYROLL" to "Payroll (${stats.payrollAlertsCount})",
                        "TRANSFERS" to "Wires (${stats.transferAlertsCount})",
                        "EXCEEDED" to "Exceeded (${stats.exceededCount})"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("filter_threshold_$key")
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

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                // Alert Cards List
                if (filteredAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alerts in this category.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAlerts, key = { it.id }) { item ->
                            RegulatoryThresholdAlertCard(
                                alert = item,
                                onAcknowledge = onAcknowledge,
                                onFileReport = onFileReport
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ModalMetricPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderSubtle, BorderSubtle.copy(alpha = 0.3f)))),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
