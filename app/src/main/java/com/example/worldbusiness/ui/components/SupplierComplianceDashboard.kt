package com.example.worldbusiness.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.SupplierAuditExportEngine
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Summary Statistics for Supplier Compliance & Risk Evaluation.
 */
data class SupplierComplianceMetrics(
    val totalCount: Int,
    val verifiedCount: Int,
    val expiringOrPendingCount: Int,
    val nonCompliantCount: Int,
    val verifiedRatio: Float,
    val expiringRatio: Float,
    val nonCompliantRatio: Float,
    val overallHealthScore: Int,
    val w8BenFilingRate: Int,
    val isoCertRate: Int,
    val antiBriberyRate: Int,
    val averageEsgScore: Double
)

/**
 * Enterprise Jetpack Compose Dashboard Component for Supplier Compliance & Supply Chain Risk Visualization.
 *
 * Visualizes compliance status across three tiers:
 * - Green (EmeraldPositive): Verified & Fully Compliant
 * - Amber (WarningAmber / GoldAccent): Expiring Documentation / Audit Pending
 * - Red (RoseNegative): Non-Compliant / Expired Documentation / Critical Risk
 */
@Composable
fun SupplierComplianceDashboard(
    suppliers: List<GlobalSupplierRecord>,
    invoices: List<InvoiceRecord> = emptyList(),
    onAddSupplier: (GlobalSupplierRecord) -> Unit = {},
    onUpdateSupplierCompliance: (id: Long, newStatus: String, notes: String) -> Unit = { _, _, _ -> },
    onUpdateSupplierPaymentTerms: (id: Long, termsDays: Int, description: String, currency: String) -> Unit = { _, _, _, _ -> },
    onCreateInvoiceForSupplier: ((GlobalSupplierRecord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRiskFilter by remember { mutableStateOf("ALL") }
    var showAddSupplierModal by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }
    var selectedSupplierForRemediation by remember { mutableStateOf<GlobalSupplierRecord?>(null) }
    var selectedSupplierForDetail by remember { mutableStateOf<GlobalSupplierRecord?>(null) }

    // Compute Metrics & Ratios
    val metrics = remember(suppliers) {
        val total = suppliers.size.coerceAtLeast(1)
        val verified = suppliers.count { it.complianceStatus.equals("COMPLIANT", ignoreCase = true) }
        val expiringOrPending = suppliers.count {
            it.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) ||
            it.complianceStatus.equals("IN_REVIEW", ignoreCase = true) ||
            it.riskLevel.equals("MEDIUM", ignoreCase = true)
        }
        val nonCompliant = suppliers.count {
            it.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) ||
            it.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) ||
            it.complianceStatus.equals("BLACKLISTED", ignoreCase = true) ||
            it.riskLevel.equals("HIGH", ignoreCase = true)
        }

        val verifiedRatio = verified.toFloat() / total.toFloat()
        val expiringRatio = expiringOrPending.toFloat() / total.toFloat()
        val nonCompliantRatio = nonCompliant.toFloat() / total.toFloat()

        // Overall Health Score (weighted: 100 for verified, 50 for pending, 0 for non-compliant)
        val score = if (suppliers.isNotEmpty()) {
            ((verified * 100 + expiringOrPending * 50) / suppliers.size).coerceIn(0, 100)
        } else 100

        val w8BenRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.w8BenOrTaxFormFiled }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val isoRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.iso9001Certified }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val antiBriberyRate = if (suppliers.isNotEmpty()) {
            ((suppliers.count { it.antiBriberyPactSigned }.toDouble() / suppliers.size) * 100).roundToInt()
        } else 100

        val avgEsg = if (suppliers.isNotEmpty()) {
            suppliers.map { it.esgRatingScore }.average()
        } else 88.0

        SupplierComplianceMetrics(
            totalCount = suppliers.size,
            verifiedCount = verified,
            expiringOrPendingCount = expiringOrPending,
            nonCompliantCount = nonCompliant,
            verifiedRatio = verifiedRatio,
            expiringRatio = expiringRatio,
            nonCompliantRatio = nonCompliantRatio,
            overallHealthScore = score,
            w8BenFilingRate = w8BenRate,
            isoCertRate = isoRate,
            antiBriberyRate = antiBriberyRate,
            averageEsgScore = avgEsg
        )
    }

    // Filter suppliers
    val filteredSuppliers = remember(suppliers, searchQuery, selectedRiskFilter) {
        suppliers.filter { supplier ->
            val matchesSearch = searchQuery.isBlank() ||
                supplier.legalName.contains(searchQuery, ignoreCase = true) ||
                supplier.supplierCode.contains(searchQuery, ignoreCase = true) ||
                supplier.country.contains(searchQuery, ignoreCase = true) ||
                supplier.primaryContactName.contains(searchQuery, ignoreCase = true) ||
                supplier.businessCategory.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedRiskFilter) {
                "ALL" -> true
                "VERIFIED", "COMPLIANT" -> supplier.complianceStatus.equals("COMPLIANT", ignoreCase = true)
                "EXPIRING", "AUDIT_PENDING" -> supplier.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) || supplier.riskLevel.equals("MEDIUM", ignoreCase = true)
                "NON_COMPLIANT" -> supplier.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) || supplier.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) || supplier.riskLevel.equals("HIGH", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("supplier_compliance_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header & Enrollment Button
        SupplierComplianceHeader(
            onOpenEnrollment = { showAddSupplierModal = true },
            onOpenExport = { showExportModal = true }
        )

        // Color-Coded Multi-Segment Status Bar & Risk Proportions
        SupplierComplianceColorStatusBarCard(
            metrics = metrics,
            selectedFilter = selectedRiskFilter,
            onSelectFilter = { selectedRiskFilter = it }
        )

        // Statutory Documentation & ESG Radar Grid
        StatutoryDocumentationAuditGrid(metrics = metrics)

        // Filter & Search Toolbar
        SupplierFilterToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            selectedFilter = selectedRiskFilter,
            onSelectFilter = { selectedRiskFilter = it },
            filteredCount = filteredSuppliers.size,
            totalCount = suppliers.size
        )

        // Supplier Cards Roster
        if (filteredSuppliers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredSuppliers.forEach { supplier ->
                    SupplierComplianceItemCard(
                        supplier = supplier,
                        onClick = { selectedSupplierForDetail = supplier },
                        onRemediate = { selectedSupplierForRemediation = supplier }
                    )
                }
            }
        } else {
            EmptySuppliersCard(
                query = searchQuery,
                onReset = {
                    searchQuery = ""
                    selectedRiskFilter = "ALL"
                }
            )
        }
    }

    // Modal: Full Supplier Detail View (Contact Info, Payment Terms, Associated Invoices)
    selectedSupplierForDetail?.let { supplier ->
        SupplierDetailModal(
            supplier = supplier,
            invoices = invoices,
            onDismiss = { selectedSupplierForDetail = null },
            onRemediate = {
                selectedSupplierForDetail = null
                selectedSupplierForRemediation = it
            },
            onCreateInvoiceForSupplier = onCreateInvoiceForSupplier
        )
    }

    // Modal: Remediate Supplier Compliance Status & Audit Notes
    selectedSupplierForRemediation?.let { supplier ->
        SupplierRemediationModal(
            supplier = supplier,
            onDismiss = { selectedSupplierForRemediation = null },
            onSave = { newStatus, notes, termsDays, termsDesc, termsCurr ->
                onUpdateSupplierCompliance(supplier.id, newStatus, notes)
                onUpdateSupplierPaymentTerms(supplier.id, termsDays, termsDesc, termsCurr)
                selectedSupplierForRemediation = null
            }
        )
    }

    // Modal: Enroll New Global Supplier
    if (showAddSupplierModal) {
        AddSupplierEnrollmentModal(
            onDismiss = { showAddSupplierModal = false },
            onConfirm = { newSupplier ->
                onAddSupplier(newSupplier)
                showAddSupplierModal = false
            }
        )
    }

    // Modal: Export Supplier Directory Audit Report (CSV / Statutory Dossier)
    if (showExportModal) {
        SupplierExportModal(
            suppliers = suppliers,
            onDismiss = { showExportModal = false }
        )
    }
}

/**
 * Header Section with Title & Actions
 */
@Composable
private fun SupplierComplianceHeader(
    onOpenEnrollment: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Compliance",
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "SUPPLY CHAIN COMPLIANCE & RISK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Real-Time Supplier Verification, W-8BEN & ISO Audit Monitor",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onOpenExport,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                modifier = Modifier.testTag("btn_export_supplier_directory")
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXPORT AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onOpenEnrollment,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.testTag("btn_enroll_supplier")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ENROLL", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * Export Modal for CSV & Statutory Audit Dossier Generation
 */
@Composable
fun SupplierExportModal(
    suppliers: List<GlobalSupplierRecord>,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") } // "CSV" or "DOSSIER"
    val context = LocalContext.current

    val exportContent = remember(selectedFormat, suppliers) {
        if (selectedFormat == "CSV") {
            SupplierAuditExportEngine.generateCsv(suppliers)
        } else {
            SupplierAuditExportEngine.generateAuditDossierText(suppliers)
        }
    }

    val previewText = remember(exportContent) {
        exportContent.lines().take(18).joinToString("\n")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = CyanAccent)
                Column {
                    Text(
                        text = "EXPORT SUPPLIER DIRECTORY AUDIT DATA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Statutory Regulatory Reporting & Supply Chain Audit",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Format Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "CSV" to "CSV SPREADSHEET (.csv)",
                        "DOSSIER" to "STATUTORY DOSSIER (.pdf/.txt)"
                    ).forEach { (formatKey, label) ->
                        val isSelected = selectedFormat == formatKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanAccent else SurfaceElevated)
                                .clickable { selectedFormat = formatKey }
                                .padding(vertical = 7.dp)
                                .testTag("export_format_$formatKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Summary Stats Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Entities: ${suppliers.size}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text(text = "Format: $selectedFormat", fontSize = 9.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    Text(text = "Size: ${exportContent.length} bytes", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }

                // Code/Text Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceElevated))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewText + if (exportContent.lines().size > 18) "\n... [${exportContent.lines().size - 18} more lines]" else "",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Supplier Directory ($selectedFormat)", exportContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied $selectedFormat audit report to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_copy_export_data")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        SupplierAuditExportEngine.exportAndShareReport(
                            context = context,
                            content = exportContent,
                            subject = "World Business OS - Supplier Directory Audit Report ($selectedFormat)",
                            mimeType = if (selectedFormat == "CSV") "text/csv" else "text/plain"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_share_export_data")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE / EXPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Primary Color-Coded Multi-Segment Status Bar Component
 * Visualizes compliance across: Green (Verified), Amber (Expiring / Audit Pending), Red (Non-Compliant)
 */
@Composable
fun SupplierComplianceColorStatusBarCard(
    metrics: SupplierComplianceMetrics,
    selectedFilter: String = "ALL",
    onSelectFilter: (String) -> Unit = {}
) {
    val animatedVerifiedRatio by animateFloatAsState(
        targetValue = metrics.verifiedRatio,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "verifiedRatio"
    )
    val animatedExpiringRatio by animateFloatAsState(
        targetValue = metrics.expiringRatio,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "expiringRatio"
    )
    val animatedNonCompliantRatio by animateFloatAsState(
        targetValue = metrics.nonCompliantRatio,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "nonCompliantRatio"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier_compliance_status_bar_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.4f), BorderSubtle))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Health Score & Risk Tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SUPPLIER COMPLIANCE STATUS BAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Aggregated Enterprise Supply Chain Risk Index",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        metrics.overallHealthScore >= 80 -> EmeraldPositive.copy(alpha = 0.15f)
                        metrics.overallHealthScore >= 60 -> WarningAmber.copy(alpha = 0.15f)
                        else -> RoseNegative.copy(alpha = 0.15f)
                    },
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                when {
                                    metrics.overallHealthScore >= 80 -> EmeraldPositive
                                    metrics.overallHealthScore >= 60 -> WarningAmber
                                    else -> RoseNegative
                                },
                                BorderSubtle
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "HEALTH INDEX:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${metrics.overallHealthScore}/100",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                metrics.overallHealthScore >= 80 -> EmeraldPositive
                                metrics.overallHealthScore >= 60 -> WarningAmber
                                else -> RoseNegative
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // The Multi-Segment Color-Coded Status Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(9.dp))
                        .testTag("color_coded_status_bar")
                ) {
                    Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                        // 1. Green Segment: Verified & Compliant
                        if (animatedVerifiedRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(animatedVerifiedRatio)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.85f))
                                        )
                                    )
                                    .clickable { onSelectFilter("VERIFIED") }
                                    .testTag("status_bar_segment_green"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (animatedVerifiedRatio >= 0.18f) {
                                    Text(
                                        text = "${(animatedVerifiedRatio * 100).roundToInt()}%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SurfaceDark,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // 2. Amber Segment: Expiring Documentation / Audit Pending
                        if (animatedExpiringRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(animatedExpiringRatio)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(WarningAmber, GoldAccent)
                                        )
                                    )
                                    .clickable { onSelectFilter("EXPIRING") }
                                    .testTag("status_bar_segment_amber"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (animatedExpiringRatio >= 0.12f) {
                                    Text(
                                        text = "${(animatedExpiringRatio * 100).roundToInt()}%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SurfaceDark,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // 3. Red Segment: Non-Compliant / Expired Documentation
                        if (animatedNonCompliantRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(animatedNonCompliantRatio)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(RoseNegative, RoseNegative.copy(alpha = 0.85f))
                                        )
                                    )
                                    .clickable { onSelectFilter("NON_COMPLIANT") }
                                    .testTag("status_bar_segment_red"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (animatedNonCompliantRatio >= 0.10f) {
                                    Text(
                                        text = "${(animatedNonCompliantRatio * 100).roundToInt()}%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Legend & Filter Pills below the Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Green Indicator: Verified
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelectFilter(if (selectedFilter == "VERIFIED") "ALL" else "VERIFIED") }
                            .padding(4.dp)
                            .testTag("legend_verified")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EmeraldPositive, CircleShape)
                        )
                        Text(
                            text = "VERIFIED: ${metrics.verifiedCount} (${(metrics.verifiedRatio * 100).roundToInt()}%)",
                            fontSize = 9.sp,
                            fontWeight = if (selectedFilter == "VERIFIED") FontWeight.Black else FontWeight.Bold,
                            color = if (selectedFilter == "VERIFIED") EmeraldPositive else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Amber Indicator: Expiring / Audit Pending
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelectFilter(if (selectedFilter == "EXPIRING") "ALL" else "EXPIRING") }
                            .padding(4.dp)
                            .testTag("legend_expiring")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(WarningAmber, CircleShape)
                        )
                        Text(
                            text = "EXPIRING: ${metrics.expiringOrPendingCount} (${(metrics.expiringRatio * 100).roundToInt()}%)",
                            fontSize = 9.sp,
                            fontWeight = if (selectedFilter == "EXPIRING") FontWeight.Black else FontWeight.Bold,
                            color = if (selectedFilter == "EXPIRING") WarningAmber else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Red Indicator: Non-Compliant
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelectFilter(if (selectedFilter == "NON_COMPLIANT") "ALL" else "NON_COMPLIANT") }
                            .padding(4.dp)
                            .testTag("legend_non_compliant")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(RoseNegative, CircleShape)
                        )
                        Text(
                            text = "NON-COMPLIANT: ${metrics.nonCompliantCount} (${(metrics.nonCompliantRatio * 100).roundToInt()}%)",
                            fontSize = 9.sp,
                            fontWeight = if (selectedFilter == "NON_COMPLIANT") FontWeight.Black else FontWeight.Bold,
                            color = if (selectedFilter == "NON_COMPLIANT") RoseNegative else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4-Tile Documentation Audit & ESG Radar Grid
 */
@Composable
private fun StatutoryDocumentationAuditGrid(
    metrics: SupplierComplianceMetrics
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tile 1: W-8BEN-E Tax Residency
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceElevated))),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "W-8BEN-E TAX", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${metrics.w8BenFilingRate}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (metrics.w8BenFilingRate >= 80) EmeraldPositive else WarningAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "IRS Residency Filed", fontSize = 8.sp, color = TextSecondary)
            }
        }

        // Tile 2: ISO 9001 Quality Certified
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceElevated))),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "ISO 9001 / 14001", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${metrics.isoCertRate}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldPositive,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "Quality Certified", fontSize = 8.sp, color = TextSecondary)
            }
        }

        // Tile 3: Anti-Bribery Pacts
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceElevated))),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "ANTI-BRIBERY", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${metrics.antiBriberyRate}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (metrics.antiBriberyRate >= 80) EmeraldPositive else RoseNegative,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "FCPA / OECD Signed", fontSize = 8.sp, color = TextSecondary)
            }
        }

        // Tile 4: Average ESG Rating
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, SurfaceElevated))),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "AVG ESG SCORE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = String.format(Locale.US, "%.1f", metrics.averageEsgScore),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "Sustainability Index", fontSize = 8.sp, color = TextSecondary)
            }
        }
    }
}

/**
 * Filter Toolbar with Search and Risk Status Chips
 */
@Composable
private fun SupplierFilterToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    filteredCount: Int,
    totalCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supplier_search_input"),
            placeholder = {
                Text(text = "Search by supplier name, code, contact, country, or category...", fontSize = 11.sp, color = TextMuted)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(14.dp))
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "STATUS FILTER:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)

            listOf(
                Triple("ALL", "ALL ($totalCount)", SurfaceElevated),
                Triple("VERIFIED", "VERIFIED (GREEN)", EmeraldPositive),
                Triple("EXPIRING", "EXPIRING / IN REVIEW (AMBER)", WarningAmber),
                Triple("NON_COMPLIANT", "NON-COMPLIANT (RED)", RoseNegative)
            ).forEach { (filterKey, label, color) ->
                val isSelected = selectedFilter == filterKey
                Box(
                    modifier = Modifier
                        .background(if (isSelected) color else SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(0.5.dp, if (isSelected) color else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectFilter(filterKey) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("filter_supplier_$filterKey")
                ) {
                    Text(
                        text = label,
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

/**
 * Individual Supplier Compliance Card with Status Pill, Risk Tags & Remediation Button
 */
@Composable
private fun SupplierComplianceItemCard(
    supplier: GlobalSupplierRecord,
    onClick: () -> Unit = {},
    onRemediate: () -> Unit
) {
    val (statusColor, statusLabel) = when {
        supplier.complianceStatus.equals("COMPLIANT", ignoreCase = true) -> EmeraldPositive to "VERIFIED"
        supplier.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) || supplier.riskLevel.equals("MEDIUM", ignoreCase = true) -> WarningAmber to "AUDIT PENDING"
        supplier.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) -> RoseNegative to "DOC EXPIRED"
        supplier.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) -> RoseNegative to "NON-COMPLIANT"
        else -> RoseNegative to supplier.complianceStatus
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("supplier_card_${supplier.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(statusColor.copy(alpha = 0.5f), BorderSubtle)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Code, Country Flag, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = supplier.supplierCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${countryCodeToFlag(supplier.countryCode)} ${supplier.country}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Color-Coded Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(statusColor, statusColor.copy(alpha = 0.5f)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (statusColor == RoseNegative) statusColor.copy(alpha = pulseAlpha) else statusColor,
                                    CircleShape
                                )
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Legal Name & Category
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = supplier.legalName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "${supplier.businessCategory.replace("_", " ")} • Contact: ${supplier.primaryContactName} (${supplier.primaryContactEmail})",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Terms & Statutory Checklist Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Terms
                Column {
                    Text(text = "PAYMENT TERMS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${supplier.paymentTermsDescription} (${supplier.preferredCurrency})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // W-8BEN-E Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "W-8BEN TAX", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(
                            imageVector = if (supplier.w8BenOrTaxFormFiled) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (supplier.w8BenOrTaxFormFiled) EmeraldPositive else RoseNegative,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (supplier.w8BenOrTaxFormFiled) "Filed" else "Missing",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (supplier.w8BenOrTaxFormFiled) EmeraldPositive else RoseNegative,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // ISO 9001
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ISO 9001", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(
                            imageVector = if (supplier.iso9001Certified) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (supplier.iso9001Certified) EmeraldPositive else WarningAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (supplier.iso9001Certified) "Certified" else "None",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (supplier.iso9001Certified) EmeraldPositive else WarningAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // ESG Rating Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ESG SCORE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${supplier.esgRatingScore}/100",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (supplier.esgRatingScore >= 80) EmeraldPositive else WarningAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Officer Remarks / Expiration timeline
            if (supplier.complianceOfficerNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (statusColor == RoseNegative) Icons.Default.Warning else Icons.Default.Policy,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = supplier.complianceOfficerNotes,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            lineHeight = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Card Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Doc Expiry: ${supplier.complianceDocExpiryDate}",
                    fontSize = 8.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = onRemediate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (statusColor == RoseNegative) RoseNegative else SurfaceElevated,
                        contentColor = if (statusColor == RoseNegative) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier.testTag("btn_remediate_supplier_${supplier.id}")
                ) {
                    Text(
                        text = if (statusColor == RoseNegative) "RESOLVE RISK" else "AUDIT & REMEDIATE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Modal to Remediate Compliance Risk, Update Documentation Status & Notes
 */
@Composable
private fun SupplierRemediationModal(
    supplier: GlobalSupplierRecord,
    onDismiss: () -> Unit,
    onSave: (newStatus: String, notes: String, termsDays: Int, termsDesc: String, termsCurr: String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(supplier.complianceStatus) }
    var notesText by remember { mutableStateOf(supplier.complianceOfficerNotes) }
    var termsDaysText by remember { mutableStateOf(supplier.paymentTermsDays.toString()) }
    var termsDescText by remember { mutableStateOf(supplier.paymentTermsDescription) }
    var termsCurrency by remember { mutableStateOf(supplier.preferredCurrency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanAccent)
                Column {
                    Text(
                        text = "COMPLIANCE REMEDIATION DESK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = supplier.legalName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "1. TRANSITION COMPLIANCE STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("COMPLIANT", "VERIFIED (GREEN)", EmeraldPositive),
                        Triple("AUDIT_PENDING", "IN REVIEW (AMBER)", WarningAmber),
                        Triple("NON_COMPLIANT", "FLAGGED (RED)", RoseNegative)
                    ).forEach { (statusCode, label, color) ->
                        val isSelected = selectedStatus == statusCode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) color else SurfaceElevated)
                                .clickable { selectedStatus = statusCode }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusCode.replace("_", " "),
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Compliance Officer Audit Notes
                Text(text = "2. COMPLIANCE AUDIT OBSERVATIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Audit Observations / Statutory Directive", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_remediation_notes"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    minLines = 2,
                    maxLines = 3
                )

                // Payment Terms Adjustments
                Text(text = "3. ADJUST PAYMENT TERMS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = termsDaysText,
                        onValueChange = { termsDaysText = it },
                        label = { Text("Terms Days", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = termsDescText,
                        onValueChange = { termsDescText = it },
                        label = { Text("Term Code", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = termsCurrency,
                        onValueChange = { termsCurrency = it.uppercase(Locale.US) },
                        label = { Text("Currency", fontSize = 9.sp) },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = termsDaysText.toIntOrNull() ?: supplier.paymentTermsDays
                    onSave(selectedStatus, notesText, days, termsDescText, termsCurrency)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_remediation")
            ) {
                Text("SAVE REMEDIATION", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Modal to Enroll a New Global Supplier
 */
@Composable
private fun AddSupplierEnrollmentModal(
    onDismiss: () -> Unit,
    onConfirm: (GlobalSupplierRecord) -> Unit
) {
    var supplierCode by remember { mutableStateOf("SUP-${(1000..9999).random()}") }
    var legalName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Germany") }
    var countryCode by remember { mutableStateOf("DE") }
    var category by remember { mutableStateOf("MANUFACTURING") }
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var paymentDaysStr by remember { mutableStateOf("30") }
    var paymentCurrency by remember { mutableStateOf("EUR") }
    var w8BenFiled by remember { mutableStateOf(true) }
    var isoCertified by remember { mutableStateOf(true) }
    var esgScoreStr by remember { mutableStateOf("88") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyanAccent)
                Text(
                    text = "ENROLL GLOBAL SUPPLIER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = supplierCode,
                        onValueChange = { supplierCode = it },
                        label = { Text("Code", fontSize = 9.sp) },
                        modifier = Modifier.weight(0.9f).testTag("input_supplier_code"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = legalName,
                        onValueChange = { legalName = it },
                        label = { Text("Legal Entity Name", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.3f).testTag("input_supplier_name"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.2f).testTag("input_supplier_country"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.uppercase(Locale.US) },
                        label = { Text("Code (2-letter)", fontSize = 9.sp) },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Primary Contact Director", fontSize = 9.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_supplier_contact"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Email", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentCurrency,
                        onValueChange = { paymentCurrency = it.uppercase(Locale.US) },
                        label = { Text("Currency", fontSize = 9.sp) },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Checklist Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "IRS Form W-8BEN-E Filed", fontSize = 10.sp, color = TextPrimary)
                    Switch(
                        checked = w8BenFiled,
                        onCheckedChange = { w8BenFiled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ISO 9001 / 14001 Certified", fontSize = 10.sp, color = TextPrimary)
                    Switch(
                        checked = isoCertified,
                        onCheckedChange = { isoCertified = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (legalName.isNotBlank()) {
                        val newRecord = GlobalSupplierRecord(
                            supplierCode = supplierCode,
                            legalName = legalName,
                            tradingName = legalName,
                            country = country,
                            countryCode = countryCode,
                            taxRegistrationNumber = "VAT-${(10000..99999).random()}",
                            businessCategory = category,
                            primaryContactName = contactName.ifBlank { "Director" },
                            primaryContactEmail = contactEmail.ifBlank { "supply@${legalName.lowercase().replace(" ", "")}.com" },
                            primaryContactPhone = contactPhone.ifBlank { "+49 00 000" },
                            physicalAddress = "$country Industrial Quarter",
                            paymentTermsDays = paymentDaysStr.toIntOrNull() ?: 30,
                            paymentTermsDescription = "NET_${paymentDaysStr}",
                            preferredCurrency = paymentCurrency,
                            complianceStatus = if (w8BenFiled && isoCertified) "COMPLIANT" else "AUDIT_PENDING",
                            w8BenOrTaxFormFiled = w8BenFiled,
                            iso9001Certified = isoCertified,
                            esgRatingScore = esgScoreStr.toIntOrNull() ?: 85,
                            antiBriberyPactSigned = true,
                            kycVerificationStatus = "VERIFIED",
                            riskLevel = if (w8BenFiled && isoCertified) "LOW" else "MEDIUM",
                            complianceOfficerNotes = "Enrolled via World Business OS Compliance Desk"
                        )
                        onConfirm(newRecord)
                    }
                },
                enabled = legalName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_add_supplier")
            ) {
                Text("ENROLL SUPPLIER", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Empty Suppliers State
 */
@Composable
private fun EmptySuppliersCard(
    query: String,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
            Text(
                text = if (query.isNotEmpty()) "No global suppliers match \"$query\"" else "No global suppliers found.",
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            if (query.isNotEmpty()) {
                TextButton(onClick = onReset) {
                    Text("Clear Filter", color = CyanAccent, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Utility: Converts 2-letter country code to Unicode Flag emoji
 */
private fun countryCodeToFlag(countryCode: String): String {
    val upper = countryCode.uppercase(Locale.US)
    return when (upper) {
        "DE" -> "🇩🇪"
        "TW" -> "🇹🇼"
        "JP" -> "🇯🇵"
        "CH" -> "🇨🇭"
        "SG" -> "🇸🇬"
        "BR" -> "🇧🇷"
        "CN" -> "🇨🇳"
        "US" -> "🇺🇸"
        "GB" -> "🇬🇧"
        "FR" -> "🇫🇷"
        "NL" -> "🇳🇱"
        "AU" -> "🇦🇺"
        else -> "🌐"
    }
}
