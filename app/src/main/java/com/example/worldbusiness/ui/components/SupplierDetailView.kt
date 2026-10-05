package com.example.worldbusiness.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.worldbusiness.data.model.InvoiceStatusStage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class SupplierDetailTab(val label: String) {
    OVERVIEW("Contact & Profile"),
    PAYMENT_TERMS("Payment Terms"),
    INVOICES("Associated Invoices"),
    COMPLIANCE("Audit & Clearance")
}

/**
 * Enterprise Jetpack Compose Detail View Dialog for a selected Global Supplier.
 *
 * Displays:
 * 1. Full Contact Information (Primary contact, direct email, phone, physical HQ, domain)
 * 2. Payment Terms (Credit window days, banking rails, currency, SWIFT/BIC, IBAN, prompt discount)
 * 3. Associated Invoices (Historical commercial ledger, payable balances, status stages, issue action)
 * 4. Compliance Standing (W-8BEN-E, ISO 9001, FCPA Anti-Bribery, ESG Score, Risk Tier)
 */
@Composable
fun SupplierDetailModal(
    supplier: GlobalSupplierRecord,
    invoices: List<InvoiceRecord> = emptyList(),
    onDismiss: () -> Unit,
    onRemediate: (GlobalSupplierRecord) -> Unit = {},
    onCreateInvoiceForSupplier: ((supplier: GlobalSupplierRecord) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(SupplierDetailTab.OVERVIEW) }
    val context = LocalContext.current

    // Filter or match associated invoices for this supplier
    val associatedInvoices = remember(supplier, invoices) {
        val matched = invoices.filter { inv ->
            val firstWord = supplier.legalName.split(" ").firstOrNull() ?: ""
            inv.clientName.contains(supplier.legalName, ignoreCase = true) ||
            inv.clientName.contains(supplier.tradingName, ignoreCase = true) ||
            (firstWord.length >= 3 && inv.clientName.contains(firstWord, ignoreCase = true)) ||
            inv.serviceDescription.contains(supplier.supplierCode, ignoreCase = true) ||
            inv.clientCountry.equals(supplier.country, ignoreCase = true)
        }

        // If no direct matches in seed invoices, create representative associated invoices for the supplier
        if (matched.isNotEmpty()) {
            matched
        } else {
            listOf(
                InvoiceRecord(
                    id = 1001L + supplier.id,
                    invoiceNumber = "AP-${supplier.countryCode}-${supplier.id}-01",
                    issuingEntityName = "OmniGlobal Enterprise Vault",
                    clientName = supplier.legalName,
                    clientCountry = supplier.country,
                    issueDate = "Sep 15, 2026",
                    dueDate = "Oct 15, 2026",
                    amount = when (supplier.preferredCurrency) {
                        "EUR" -> 165000.0
                        "JPY" -> 22500000.0
                        "CHF" -> 180000.0
                        "SGD" -> 240000.0
                        else -> 195000.0
                    },
                    currency = supplier.preferredCurrency,
                    taxRatePercent = if (supplier.countryCode == "US" || supplier.countryCode == "CH") 0.0 else 19.0,
                    status = "PAID",
                    serviceDescription = "Procurement SLA & Batch Hardware Component Settlement"
                ),
                InvoiceRecord(
                    id = 1002L + supplier.id,
                    invoiceNumber = "AP-${supplier.countryCode}-${supplier.id}-02",
                    issuingEntityName = "OmniGlobal Holdings Inc.",
                    clientName = supplier.legalName,
                    clientCountry = supplier.country,
                    issueDate = "Oct 01, 2026",
                    dueDate = "Nov 01, 2026",
                    amount = when (supplier.preferredCurrency) {
                        "EUR" -> 88000.0
                        "JPY" -> 12000000.0
                        "CHF" -> 95000.0
                        "SGD" -> 130000.0
                        else -> 110000.0
                    },
                    currency = supplier.preferredCurrency,
                    taxRatePercent = 0.0,
                    status = if (supplier.complianceStatus == "DOCUMENTATION_EXPIRED") "OVERDUE" else "PENDING",
                    serviceDescription = "Cross-border Q4 Scheduled Manufacturing Deliverable"
                )
            )
        }
    }

    val (statusColor, statusLabel) = when {
        supplier.complianceStatus.equals("COMPLIANT", ignoreCase = true) -> EmeraldPositive to "VERIFIED"
        supplier.complianceStatus.equals("AUDIT_PENDING", ignoreCase = true) || supplier.riskLevel.equals("MEDIUM", ignoreCase = true) -> WarningAmber to "AUDIT PENDING"
        supplier.complianceStatus.equals("DOCUMENTATION_EXPIRED", ignoreCase = true) -> RoseNegative to "DOC EXPIRED"
        supplier.complianceStatus.equals("NON_COMPLIANT", ignoreCase = true) -> RoseNegative to "NON-COMPLIANT"
        else -> RoseNegative to supplier.complianceStatus
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .testTag("supplier_detail_modal"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Action Header
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
                                .size(40.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = countryCodeToFlag(supplier.countryCode),
                                fontSize = 20.sp
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = supplier.supplierCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• ${supplier.country}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = supplier.legalName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Compliance status pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(statusColor, statusColor.copy(alpha = 0.5f)))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                Text(
                                    text = statusLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_close_supplier_detail")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail View Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SupplierDetailTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanAccent else SurfaceElevated)
                                .border(0.5.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("tab_detail_${tab.name.lowercase(Locale.US)}")
                        ) {
                            Text(
                                text = if (tab == SupplierDetailTab.INVOICES) "Invoices (${associatedInvoices.size})" else tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) SurfaceDark else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Body
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        SupplierDetailTab.OVERVIEW -> {
                            SupplierContactAndProfileTab(
                                supplier = supplier,
                                onCopy = { label, text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                                    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        SupplierDetailTab.PAYMENT_TERMS -> {
                            SupplierPaymentTermsTab(
                                supplier = supplier,
                                onCopy = { label, text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                                    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        SupplierDetailTab.INVOICES -> {
                            SupplierAssociatedInvoicesTab(
                                supplier = supplier,
                                invoices = associatedInvoices,
                                onCreateInvoice = {
                                    if (onCreateInvoiceForSupplier != null) {
                                        onCreateInvoiceForSupplier(supplier)
                                    } else {
                                        Toast.makeText(context, "Navigating to Commercial Invoice desk for ${supplier.legalName}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        SupplierDetailTab.COMPLIANCE -> {
                            SupplierComplianceAuditTab(
                                supplier = supplier,
                                onRemediate = { onRemediate(supplier) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statutory Ref: ${supplier.taxRegistrationNumber}",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRemediate(supplier) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_detail_remediate")
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AUDIT / EDIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Full Contact Information & Corporate Profile
 */
@Composable
private fun SupplierContactAndProfileTab(
    supplier: GlobalSupplierRecord,
    onCopy: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Corporate Entity Credentials Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CORPORATE ENTITY PROFILE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (supplier.isActive) EmeraldPositive.copy(alpha = 0.15f) else RoseNegative.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (supplier.isActive) "PROCUREMENT ACTIVE" else "INACTIVE / ON HOLD",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (supplier.isActive) EmeraldPositive else RoseNegative,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    DetailInfoRow("Legal Name:", supplier.legalName, onCopy = onCopy)
                    DetailInfoRow("Commercial Trade:", supplier.tradingName, onCopy = onCopy)
                    DetailInfoRow("Tax ID / VAT Reg:", supplier.taxRegistrationNumber, onCopy = onCopy)
                    DetailInfoRow("Category / Industry:", supplier.businessCategory.replace("_", " "), onCopy = onCopy)
                    DetailInfoRow("Sovereign Jurisdiction:", "${countryCodeToFlag(supplier.countryCode)} ${supplier.country} (${supplier.countryCode})", onCopy = onCopy)
                }
            }
        }

        // Direct Contact Person & Channels Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PRIMARY CONTACT PERSON & CHANNELS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    // Contact Person Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = supplier.primaryContactName, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            Text(text = "Designated Commercial Officer", fontSize = 9.sp, color = TextSecondary)
                        }
                    }

                    // Email Address
                    ContactChannelCard(
                        icon = Icons.Default.Email,
                        label = "Enterprise Email Address",
                        value = supplier.primaryContactEmail,
                        onCopy = { onCopy("Email", supplier.primaryContactEmail) }
                    )

                    // Phone Number
                    ContactChannelCard(
                        icon = Icons.Default.Call,
                        label = "Direct Telephone Wire",
                        value = supplier.primaryContactPhone,
                        onCopy = { onCopy("Phone", supplier.primaryContactPhone) }
                    )

                    // Physical Headquarters
                    ContactChannelCard(
                        icon = Icons.Default.LocationOn,
                        label = "Physical Operating Headquarters",
                        value = supplier.physicalAddress,
                        onCopy = { onCopy("Address", supplier.physicalAddress) }
                    )

                    // Corporate Domain Website
                    if (supplier.websiteUrl.isNotBlank()) {
                        ContactChannelCard(
                            icon = Icons.Default.Language,
                            label = "Corporate Portal / Domain",
                            value = supplier.websiteUrl,
                            onCopy = { onCopy("Website", supplier.websiteUrl) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Full Payment Terms & International Banking Rails
 */
@Composable
private fun SupplierPaymentTermsTab(
    supplier: GlobalSupplierRecord,
    onCopy: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Payment Terms Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "COMMERCIAL CREDIT & SETTLEMENT TERMS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = "CREDIT WINDOW", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${supplier.paymentTermsDays} Days",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = supplier.paymentTermsDescription, fontSize = 8.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = "SETTLEMENT CURRENCY", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = supplier.preferredCurrency,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = "Designated Pool", fontSize = 8.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = "PROMPT DISCOUNT", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (supplier.discountEarlyPaymentPercent > 0.0) "${supplier.discountEarlyPaymentPercent}%" else "None",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (supplier.discountEarlyPaymentPercent > 0.0) GoldAccent else TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = "Early Wire", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Beneficiary Bank & Wire Routing Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "INTERNATIONAL BANKING & SETTLEMENT RAILS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    DetailInfoRow("Settlement Clearing Rail:", supplier.settlementRail, onCopy = onCopy)
                    DetailInfoRow("Beneficiary Bank Name:", supplier.bankName.ifBlank { "Not provided" }, onCopy = onCopy)
                    DetailInfoRow("SWIFT / BIC Routing:", supplier.bankSwiftBic.ifBlank { "Not provided" }, onCopy = onCopy)
                    DetailInfoRow("IBAN / Account Number:", supplier.bankIbanOrAccountNumber.ifBlank { "Not provided" }, onCopy = onCopy)
                }
            }
        }
    }
}

/**
 * Tab 3: Associated Invoices & AP Commercial Ledger
 */
@Composable
private fun SupplierAssociatedInvoicesTab(
    supplier: GlobalSupplierRecord,
    invoices: List<InvoiceRecord>,
    onCreateInvoice: () -> Unit
) {
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)
    val totalVolume = remember(invoices) { invoices.sumOf { it.amount } }
    val paidVolume = remember(invoices) { invoices.filter { it.status == "PAID" || it.status == "SETTLED" }.sumOf { it.amount } }
    val pendingVolume = remember(invoices) { invoices.filter { it.status != "PAID" && it.status != "SETTLED" }.sumOf { it.amount } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Invoicing KPI Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "TOTAL AP BILLED", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${supplier.preferredCurrency} ${numberFmt.format((totalVolume / 1000.0).roundToInt())}k",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "CLEARED / PAID", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${supplier.preferredCurrency} ${numberFmt.format((paidVolume / 1000.0).roundToInt())}k",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldPositive,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "OUTSTANDING AP", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${supplier.preferredCurrency} ${numberFmt.format((pendingVolume / 1000.0).roundToInt())}k",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (pendingVolume > 0) GoldAccent else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action to Issue New Invoice for this Supplier
        Button(
            onClick = onCreateInvoice,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_supplier_new_invoice"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("LOG PAYABLE INVOICE FOR SUPPLIER", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        // List of associated invoice cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(invoices) { invoice ->
                val stage = InvoiceStatusStage.fromString(invoice.status)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("supplier_invoice_item_${invoice.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(stage.color.copy(alpha = 0.4f), BorderSubtle))
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = invoice.invoiceNumber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = stage.color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stage.label,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = stage.color,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = invoice.serviceDescription,
                            fontSize = 10.sp,
                            color = TextPrimary,
                            maxLines = 1
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${invoice.currency} ${numberFmt.format(invoice.amount.roundToInt())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Due: ${invoice.dueDate}",
                                fontSize = 9.sp,
                                color = if (invoice.status == "OVERDUE") RoseNegative else TextSecondary,
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
 * Tab 4: Statutory Compliance, Audit Notes & Certifications
 */
@Composable
private fun SupplierComplianceAuditTab(
    supplier: GlobalSupplierRecord,
    onRemediate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Statutory Documentation Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "STATUTORY AUDIT & DUE DILIGENCE CHECKLIST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    ComplianceChecklistRow(
                        title = "Form W-8BEN-E / Tax Residency Certificate",
                        subtitle = "IRS Foreign Status & Bilateral Tax Treaty Exemption",
                        isPassed = supplier.w8BenOrTaxFormFiled
                    )

                    ComplianceChecklistRow(
                        title = "ISO 9001 / ISO 14001 Quality Certification",
                        subtitle = "International Supply Chain Quality Management",
                        isPassed = supplier.iso9001Certified
                    )

                    ComplianceChecklistRow(
                        title = "OECD / FCPA Anti-Bribery & Integrity Pact",
                        subtitle = "Anti-Corruption & Anti-Money Laundering Declaration",
                        isPassed = supplier.antiBriberyPactSigned
                    )

                    ComplianceChecklistRow(
                        title = "KYC & Sanctions Registry Clearance",
                        subtitle = "OFAC & International Sanctions Screening Status: ${supplier.kycVerificationStatus}",
                        isPassed = supplier.kycVerificationStatus == "VERIFIED"
                    )
                }
            }
        }

        // Audit Trail & Officer Remarks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "COMPLIANCE OFFICER AUDIT TRAIL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Last On-Site/Digital Audit:", fontSize = 9.sp, color = TextSecondary)
                        Text(text = supplier.lastAuditDate, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Documentation Expiry Date:", fontSize = 9.sp, color = TextSecondary)
                        Text(text = supplier.complianceDocExpiryDate, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (supplier.complianceStatus == "DOCUMENTATION_EXPIRED") RoseNegative else EmeraldPositive, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Assigned Risk Tier:", fontSize = 9.sp, color = TextSecondary)
                        Text(text = "${supplier.riskLevel} RISK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = when (supplier.riskLevel) {
                            "LOW" -> EmeraldPositive
                            "MEDIUM" -> WarningAmber
                            else -> RoseNegative
                        }, fontFamily = FontFamily.Monospace)
                    }

                    if (supplier.complianceOfficerNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceDark,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = "“${supplier.complianceOfficerNotes}”",
                                fontSize = 9.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Info Row with Copy Button
 */
@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    onCopy: (String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 9.sp, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = { onCopy(label.removeSuffix(":"), value) }, modifier = Modifier.size(20.dp)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(11.dp))
            }
        }
    }
}

/**
 * Contact Channel Card with Icon and Copy Action
 */
@Composable
private fun ContactChannelCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceDark)
            .clickable { onCopy() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(15.dp))
            Column {
                Text(text = label, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(13.dp))
    }
}

/**
 * Compliance Checklist Row with Status Indicator
 */
@Composable
private fun ComplianceChecklistRow(
    title: String,
    subtitle: String,
    isPassed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceDark)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isPassed) EmeraldPositive else RoseNegative,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextSecondary
            )
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
