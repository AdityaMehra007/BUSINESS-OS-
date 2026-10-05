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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.worldbusiness.data.model.CrossBorderInvoiceItem
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FormattedCrossBorderInvoice
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.repository.FormattedInvoiceEngine
import java.util.Locale

/**
 * Full-featured Formatted Cross-Border Invoice Viewer Modal.
 * Generates and presents structured international commercial invoices using data from Room
 * with automatic regional tax calculation, export, and paper mode preview.
 */
@Composable
fun FormattedCrossBorderInvoiceModal(
    invoice: InvoiceRecord,
    entities: List<EntityRecord>,
    fxBalances: List<FxBalanceRecord>,
    customLineItems: List<CrossBorderInvoiceItem>? = null,
    onDismiss: () -> Unit,
    onUpdateStatus: (Long, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isPaperMode by remember { mutableStateOf(false) }

    val doc = remember(invoice, entities, fxBalances, customLineItems) {
        FormattedInvoiceEngine.buildFormattedInvoice(invoice, entities, fxBalances, customLineItems)
    }

    val paperBackground = Color(0xFFF9FAFB)
    val paperCardBg = Color(0xFFFFFFFF)
    val paperBorder = Color(0xFFD1D5DB)
    val paperTextPrimary = Color(0xFF111827)
    val paperTextSecondary = Color(0xFF4B5563)
    val paperAccent = Color(0xFF1E40AF) // Navy Blue

    val activeBg = if (isPaperMode) paperBackground else SurfaceDark
    val activeCardBg = if (isPaperMode) paperCardBg else SurfaceElevated
    val activeBorder = if (isPaperMode) paperBorder else BorderSubtle
    val activeTextPrimary = if (isPaperMode) paperTextPrimary else TextPrimary
    val activeTextSecondary = if (isPaperMode) paperTextSecondary else TextSecondary
    val activeAccent = if (isPaperMode) paperAccent else CyanAccent

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("formatted_invoice_modal"),
        containerColor = activeBg,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Export Formatted Text
                Button(
                    onClick = {
                        val textDoc = FormattedInvoiceEngine.generatePlainTextDocument(doc)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Commercial Invoice ${doc.invoiceNumber}", textDoc)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Invoice ${doc.invoiceNumber} copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccent, contentColor = if (isPaperMode) Color.White else SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_export_formatted_invoice")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY INVOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = activeAccent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        dismissButton = {
            // Paper Mode / Dark Mode Toggle
            OutlinedButton(
                onClick = { isPaperMode = !isPaperMode },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = activeAccent),
                modifier = Modifier.testTag("btn_toggle_invoice_paper_mode")
            ) {
                Icon(
                    imageVector = if (isPaperMode) Icons.Default.Nightlight else Icons.Default.Print,
                    contentDescription = "Mode",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPaperMode) "DARK OS MODE" else "PRINT PAPER VIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Commercial Invoice",
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "INTERNATIONAL COMMERCIAL INVOICE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = activeTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Generated from Room Database • Bilateral Statutory Tax Rules",
                            fontSize = 9.sp,
                            color = activeTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = activeTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("formatted_invoice_document"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Document Paper Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = activeCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(activeAccent.copy(alpha = 0.5f), activeBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Document Meta Header: Title & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "COMMERCIAL INVOICE",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = activeTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "ISO 20022 Cross-Border Settlement",
                                    fontSize = 9.sp,
                                    color = activeTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "INVOICE NO: ${doc.invoiceNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val statusColor = when (doc.status) {
                                    "PAID" -> EmeraldPositive
                                    "OVERDUE" -> RoseNegative
                                    "IN_CLEARING" -> CyanAccent
                                    else -> GoldAccent
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = doc.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = statusColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "Issued: ${doc.issueDate}",
                                    fontSize = 9.sp,
                                    color = activeTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Due: ${doc.dueDate}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doc.status == "OVERDUE") RoseNegative else activeTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        HorizontalDivider(color = activeBorder.copy(alpha = 0.5f))

                        // Bilateral Parties Strip: Sourced from Room Database
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Issuer / Seller Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaperMode) Color(0xFFF3F4F6) else SurfaceDark,
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(activeBorder, activeBorder.copy(alpha = 0.4f)))),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "ISSUED BY (EXPORTER):",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = doc.issuingEntity.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTextPrimary
                                    )
                                    Text(
                                        text = doc.issuingEntity.jurisdiction,
                                        fontSize = 9.sp,
                                        color = activeTextSecondary
                                    )
                                    Text(
                                        text = "Tax ID: ${doc.issuingEntity.taxId}",
                                        fontSize = 9.sp,
                                        color = activeTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Officer: ${doc.issuingEntity.localDirector}",
                                        fontSize = 8.sp,
                                        color = activeTextSecondary
                                    )
                                }
                            }

                            // Client / Buyer Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaperMode) Color(0xFFF3F4F6) else SurfaceDark,
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(activeBorder, activeBorder.copy(alpha = 0.4f)))),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "BILLED TO (CLIENT):",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = doc.clientName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTextPrimary
                                    )
                                    Text(
                                        text = doc.clientAddress,
                                        fontSize = 9.sp,
                                        color = activeTextSecondary,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "Jurisdiction: ${doc.clientCountry}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = activeTextPrimary
                                    )
                                    Text(
                                        text = "VAT: ${doc.clientVatId}",
                                        fontSize = 9.sp,
                                        color = activeAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Line Items Table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_line_items_table"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "COMMERCIAL SERVICES & COMMODITY SCHEDULE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
                                fontFamily = FontFamily.Monospace
                            )

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isPaperMode) Color(0xFFE5E7EB) else SurfaceDark, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "DESCRIPTION / SAC", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = activeTextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                                Text(text = "QTY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = activeTextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                                Text(text = "RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = activeTextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                Text(text = "AMOUNT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = activeTextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                            }

                            // Table Rows
                            doc.lineItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(text = item.description, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeTextPrimary)
                                        Text(text = "SAC ${item.hsnSacCode} • Cross-Border Tech", fontSize = 8.sp, color = activeTextSecondary, fontFamily = FontFamily.Monospace)
                                    }
                                    Text(text = String.format(Locale.US, "%.0f", item.quantity), fontSize = 10.sp, color = activeTextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                                    Text(text = "${doc.currencySymbol}${Formatters.formatCurrency(item.unitPrice)}", fontSize = 10.sp, color = activeTextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                    Text(text = "${doc.currencySymbol}${Formatters.formatCurrency(item.totalAmount)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeTextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = activeBorder.copy(alpha = 0.3f))
                            }
                        }

                        // Automatic Regional Tax Calculation Breakdown Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("regional_tax_breakdown_card"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isPaperMode) Color(0xFFEFF6FF) else SurfaceDark),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GoldAccent.copy(alpha = 0.5f), activeBorder)))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AUTOMATIC REGIONAL TAX AUDIT & BREAKDOWN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = doc.clientCountry,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = activeAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Subtotal (Net of Tax):", fontSize = 10.sp, color = activeTextSecondary, fontFamily = FontFamily.Monospace)
                                    Text(text = "${doc.currencySymbol}${Formatters.formatCurrency(doc.subtotal)} ${doc.currency}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeTextPrimary, fontFamily = FontFamily.Monospace)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (doc.taxCalculation.isReverseCharge) "Regional VAT/GST (0% Reverse Charge):" else "Regional VAT/GST (${String.format(Locale.US, "%.1f", doc.taxCalculation.vatRatePercent)}%):",
                                        fontSize = 10.sp,
                                        color = activeTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "+${doc.currencySymbol}${Formatters.formatCurrency(doc.taxCalculation.vatAmount)} ${doc.currency}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (doc.taxCalculation.vatAmount == 0.0) EmeraldPositive else GoldAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (doc.taxCalculation.whtAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Withholding Tax Deduction (${String.format(Locale.US, "%.1f", doc.taxCalculation.whtRatePercent)}%):", fontSize = 10.sp, color = RoseNegative, fontFamily = FontFamily.Monospace)
                                        Text(text = "-${doc.currencySymbol}${Formatters.formatCurrency(doc.taxCalculation.whtAmount)} ${doc.currency}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseNegative, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                HorizontalDivider(color = activeBorder.copy(alpha = 0.4f))

                                // Net Receivable
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "TOTAL COMMERCIAL INVOICE PAYABLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = activeAccent, fontFamily = FontFamily.Monospace)
                                        Text(text = "≈ $${Formatters.formatCurrency(doc.taxCalculation.equivalentUsd)} USD Equivalent", fontSize = 8.sp, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                                    }
                                    Text(
                                        text = "${doc.currencySymbol}${Formatters.formatCurrency(doc.taxCalculation.netReceivable)} ${doc.currency}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = activeTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Statutory Compliance Note
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isPaperMode) Color.White else SurfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "STATUTORY TREATY NOTE: ${doc.taxCalculation.complianceNote}",
                                        fontSize = 8.sp,
                                        color = activeTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }

                        // Cross-Border Banking & Settlement Rails from Room
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPaperMode) Color(0xFFF3F4F6) else SurfaceDark,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(activeBorder, activeBorder.copy(alpha = 0.4f)))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Bank", tint = activeAccent, modifier = Modifier.size(13.dp))
                                    Text(text = "SETTLEMENT BANKING INSTRUCTIONS (ROOM VAULT)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = activeAccent, fontFamily = FontFamily.Monospace)
                                }
                                Text(text = "Institution: ${doc.bankInstitution}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = activeTextPrimary, fontFamily = FontFamily.Monospace)
                                Text(text = "SWIFT / BIC: ${doc.swiftBic} • Rail: ${doc.clearingRail}", fontSize = 9.sp, color = activeTextSecondary, fontFamily = FontFamily.Monospace)
                                Text(text = "IBAN / Wire Acct: ${doc.ibanOrAccount}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = activeAccent, fontFamily = FontFamily.Monospace)
                                Text(text = "Payment Ref: ${doc.invoiceNumber}", fontSize = 8.sp, color = activeTextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Customs Certification
                        Text(
                            text = "CUSTOMS & EXPORT DECLARATION: ${doc.customsCertification}",
                            fontSize = 8.sp,
                            color = activeTextSecondary,
                            lineHeight = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    )
}
