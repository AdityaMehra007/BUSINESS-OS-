package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.JurisdictionRegulatoryOverview
import com.example.worldbusiness.data.model.RegulatoryTaxDeadlineItem
import com.example.worldbusiness.data.model.TaxRequirementType
import com.example.worldbusiness.data.repository.CorporateTaxComplianceEngine
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Executive Dashboard Widget displaying upcoming tax filing deadlines and regulatory
 * reporting requirements for all jurisdictions where the corporation operates.
 */
@Composable
fun JurisdictionTaxDeadlinesWidget(
    entities: List<EntityRecord>,
    modifier: Modifier = Modifier,
    onNavigateToEntities: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var itemForDetailModal by remember { mutableStateOf<RegulatoryTaxDeadlineItem?>(null) }
    var itemForFilingReceipt by remember { mutableStateOf<RegulatoryTaxDeadlineItem?>(null) }

    // Map of locally filed or extended items
    var filedItemIds by remember { mutableStateOf(setOf<String>()) }
    var extendedItemIds by remember { mutableStateOf(setOf<String>()) }

    val rawDeadlines = remember(entities) {
        CorporateTaxComplianceEngine.generateDeadlines(entities)
    }

    val deadlines = remember(rawDeadlines, filedItemIds, extendedItemIds) {
        rawDeadlines.map { item ->
            when {
                filedItemIds.contains(item.id) -> item.copy(
                    complianceStatus = "FILED_CONFIRMED",
                    daysRemaining = 0,
                    electronicFilingReceipt = "GATEWAY-REC-${item.statutoryFormCode}-2026-X8849"
                )
                extendedItemIds.contains(item.id) -> item.copy(
                    complianceStatus = "EXTENSION_GRANTED",
                    daysRemaining = item.daysRemaining + 30,
                    dueDateString = "${item.dueDateString} (+30D Ext)"
                )
                else -> item
            }
        }.sortedBy { if (it.complianceStatus == "FILED_CONFIRMED") 9999 else it.daysRemaining }
    }

    val overview = remember(deadlines, entities) {
        CorporateTaxComplianceEngine.computeOverview(deadlines, entities)
    }

    val filteredDeadlines = when (selectedFilter) {
        "IMMINENT" -> deadlines.filter { it.daysRemaining in 1..30 && it.complianceStatus != "FILED_CONFIRMED" }
        "CIT" -> deadlines.filter { it.statutoryCategory == TaxRequirementType.CORPORATE_INCOME_TAX }
        "VAT" -> deadlines.filter { it.statutoryCategory == TaxRequirementType.VAT_GST_SALES }
        "REG" -> deadlines.filter { it.statutoryCategory == TaxRequirementType.ANNUAL_CORPORATE_RETURN || it.statutoryCategory == TaxRequirementType.BENEFICIAL_OWNERSHIP_AML }
        "US" -> deadlines.filter { it.countryCode == "US" }
        "CH" -> deadlines.filter { it.countryCode == "CH" }
        "SG" -> deadlines.filter { it.countryCode == "SG" }
        "GB" -> deadlines.filter { it.countryCode == "GB" }
        "DE" -> deadlines.filter { it.countryCode == "DE" }
        "BR" -> deadlines.filter { it.countryCode == "BR" }
        else -> deadlines
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("jurisdiction_tax_deadlines_widget"),
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
            // Widget Title Header
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
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "Tax Deadlines",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "TAX FILING & REGULATORY REPORTING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Statutory calendar across ${overview.jurisdictionsCoveredCount} operating jurisdictions",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (overview.urgentActionCount > 0) RoseNegative.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (overview.urgentActionCount > 0) listOf(RoseNegative, RoseNegative.copy(alpha = 0.4f))
                            else listOf(GoldAccent, GoldAccent.copy(alpha = 0.4f))
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
                                .background(if (overview.urgentActionCount > 0) RoseNegative else GoldAccent, CircleShape)
                        )
                        Text(
                            text = "${overview.upcomingIn30DaysCount} IMMINENT (<30D)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overview.urgentActionCount > 0) RoseNegative else GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // 4-Pillar Executive Summary Metric Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "DUE IN < 15 DAYS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${overview.urgentActionCount} Returns",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (overview.urgentActionCount > 0) RoseNegative else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "EST. TAX DUE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(overview.totalEstimatedTaxDueUsd.roundToInt())}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PENALTY RISK", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(overview.totalStatutoryPenaltyAtRiskUsd.roundToInt())}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "GROUP COMPLIANCE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${overview.averageJurisdictionComplianceScore}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ALL" to "All Filings (${deadlines.size})",
                    "IMMINENT" to "Imminent <30D (${overview.upcomingIn30DaysCount})",
                    "CIT" to "Corporate CIT",
                    "VAT" to "VAT / GST",
                    "REG" to "Registry / BOI",
                    "US" to "🇺🇸 US",
                    "CH" to "🇨🇭 CH",
                    "SG" to "🇸🇬 SG",
                    "GB" to "🇬🇧 UK",
                    "DE" to "🇩🇪 DE",
                    "BR" to "🇧🇷 BR"
                ).forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("filter_tax_$key")
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

            // Deadline Cards List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredDeadlines.isNotEmpty()) {
                    filteredDeadlines.forEach { item ->
                        StatutoryDeadlineRowCard(
                            item = item,
                            onFileClick = {
                                filedItemIds = filedItemIds + item.id
                                itemForFilingReceipt = item
                            },
                            onExtendClick = {
                                extendedItemIds = extendedItemIds + item.id
                            },
                            onViewDetail = {
                                itemForDetailModal = item
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No statutory filing obligations match the active filter.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Detail Modal for Filing Specifications & Schedules
    itemForDetailModal?.let { item ->
        StatutoryDetailModal(
            item = item,
            onDismiss = { itemForDetailModal = null },
            onFileNow = {
                filedItemIds = filedItemIds + item.id
                itemForFilingReceipt = item
                itemForDetailModal = null
            }
        )
    }

    // Filing Receipt Modal
    itemForFilingReceipt?.let { item ->
        ElectronicFilingReceiptDialog(
            item = item,
            onDismiss = { itemForFilingReceipt = null }
        )
    }
}

/**
 * Individual Card representing an upcoming statutory filing requirement.
 */
@Composable
private fun StatutoryDeadlineRowCard(
    item: RegulatoryTaxDeadlineItem,
    onFileClick: () -> Unit,
    onExtendClick: () -> Unit,
    onViewDetail: () -> Unit
) {
    val isFiled = item.complianceStatus == "FILED_CONFIRMED"
    val isExtended = item.complianceStatus == "EXTENSION_GRANTED"

    val countdownColor = when {
        isFiled -> EmeraldPositive
        item.daysRemaining <= 15 -> RoseNegative
        item.daysRemaining <= 30 -> GoldAccent
        else -> CyanAccent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .border(
                1.dp,
                if (isFiled) EmeraldPositive.copy(alpha = 0.5f)
                else if (item.daysRemaining <= 15) RoseNegative.copy(alpha = 0.5f)
                else BorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
            .testTag("tax_deadline_card_${item.id.lowercase(Locale.US)}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Flag, Jurisdiction, Form Code, Countdown Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = item.countryFlag, fontSize = 14.sp)
                    Text(
                        text = item.jurisdictionName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(SurfaceDark, RoundedCornerShape(4.dp))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.statutoryFormCode,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = countdownColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(countdownColor, countdownColor.copy(alpha = 0.4f))))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isFiled) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Filed", tint = EmeraldPositive, modifier = Modifier.size(10.dp))
                            Text(text = "FILED & VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        } else if (isExtended) {
                            Icon(imageVector = Icons.Default.Update, contentDescription = "Extended", tint = CyanAccent, modifier = Modifier.size(10.dp))
                            Text(text = "${item.daysRemaining}D (+30D EXT)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        } else {
                            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = "Countdown", tint = countdownColor, modifier = Modifier.size(10.dp))
                            Text(text = "${item.daysRemaining} DAYS REMAINING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = countdownColor, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Filing Title & Entity Info
            Column {
                Text(
                    text = item.filingTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${item.entityName} • Authority: ${item.statutoryAuthority}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Financial & Authority Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "DUE DATE", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = item.dueDateString, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                }

                if (item.estimatedTaxLiabilityUsd != null && item.estimatedTaxLiabilityUsd > 0.0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "EST. TAX DUE", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "$${NumberFormat.getNumberInstance(Locale.US).format(item.estimatedTaxLiabilityUsd.roundToInt())} USD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "PENALTY EXPOSURE", fontSize = 7.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(item.potentialPenaltyRiskUsd.roundToInt())} USD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseNegative,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Signatory & Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Signatory: ${item.localResidentSignatory} • ${item.submissionMethod}",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isFiled) {
                    Button(
                        onClick = onFileClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("btn_file_return_${item.id.lowercase(Locale.US)}")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "File", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "FILE RETURN", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    if (!isExtended) {
                        OutlinedButton(
                            onClick = onExtendClick,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_extend_tax_${item.id.lowercase(Locale.US)}")
                        ) {
                            Icon(imageVector = Icons.Default.Update, contentDescription = "Extend", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "+30D EXT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onViewDetail,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Specs", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "SPECS", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Detailed Modal displaying statutory filing specs, required schedules, and penal codes.
 */
@Composable
private fun StatutoryDetailModal(
    item: RegulatoryTaxDeadlineItem,
    onDismiss: () -> Unit,
    onFileNow: () -> Unit
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
                    Text(text = item.countryFlag, fontSize = 18.sp)
                    Column {
                        Text(
                            text = item.statutoryFormCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = item.jurisdictionName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
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
                    text = item.filingTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = item.statutoryNotes,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                // Required Schedules Box
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
                        Text(
                            text = "MANDATORY STATUTORY SCHEDULES:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        item.requiredSchedules.forEach { schedule ->
                            Text(
                                text = "✓ $schedule",
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Penal Code Citation
                if (item.penalCodeReference.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoseNegative.copy(alpha = 0.1f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(RoseNegative.copy(alpha = 0.5f), BorderSubtle))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "STATUTORY PENAL CODE REFERENCE:", fontSize = 7.sp, color = RoseNegative, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(text = item.penalCodeReference, fontSize = 9.sp, color = TextPrimary)
                        }
                    }
                }

                // Signatory and Submission API
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Gateway: ${item.submissionMethod}", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = "Signatory: ${item.localResidentSignatory}", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {
            if (item.complianceStatus != "FILED_CONFIRMED") {
                Button(
                    onClick = onFileNow,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
                ) {
                    Text(text = "FILE RETURN NOW", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark)
                ) {
                    Text(text = "OK (ALREADY FILED)", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CLOSE", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Electronic Filing Receipt Dialog with simulated cryptographic proof of submission.
 */
@Composable
private fun ElectronicFilingReceiptDialog(
    item: RegulatoryTaxDeadlineItem,
    onDismiss: () -> Unit
) {
    val receiptId = "GATEWAY-REC-${item.statutoryFormCode}-2026-X8849"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = EmeraldPositive)
                Text(
                    text = "STATUTORY FILING TRANSMITTED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "The corporate tax return has been successfully transmitted and accepted by the sovereign statutory authority gateway.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldPositive, BorderSubtle))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "ELECTRONIC CONFIRMATION RECEIPT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        Text(text = "Receipt ID: $receiptId", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        Text(text = "Authority: ${item.statutoryAuthority}", fontSize = 9.sp, color = TextSecondary)
                        Text(text = "Filing Entity: ${item.entityName}", fontSize = 9.sp, color = TextSecondary)
                        Text(text = "Transmission Timestamp: 2026-10-01 14:02:18 GMT", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(text = "Status: ACCEPTED_AUDITED (No Action Required)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPositive, contentColor = SurfaceDark)
            ) {
                Text(text = "DISMISS RECEIPT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}
