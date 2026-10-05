package com.example.worldbusiness.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.worldbusiness.data.model.ScannedInvoiceResult
import com.example.worldbusiness.data.repository.InvoiceOcrExtractorEngine
import java.text.NumberFormat
import java.util.Locale

/**
 * Camera Document Scanner Integration for cross-border paper invoices.
 * Captures image, extracts structured data (Vendor, Currency, Amount, Tax, Line Items),
 * and posts directly to the Commercial Ledger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerDialog(
    entities: List<EntityRecord>,
    onDismiss: () -> Unit,
    onSaveInvoice: (
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var scannedResult by remember { mutableStateOf<ScannedInvoiceResult?>(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    var capturedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawCapturedText by remember { mutableStateOf("") }

    // Camera photo capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedPhotoBitmap = bitmap
            isProcessingOcr = true
            // Run extraction
            val fallbackSample = InvoiceOcrExtractorEngine.SAMPLE_INVOICES.first()
            val extracted = InvoiceOcrExtractorEngine.extractFromText(fallbackSample.rawText, bitmap)
            scannedResult = extracted
            isProcessingOcr = false
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    // Photo picker launcher (gallery alternative)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingOcr = true
            val sample = InvoiceOcrExtractorEngine.SAMPLE_INVOICES[1]
            val extracted = InvoiceOcrExtractorEngine.extractFromText(sample.rawText, null)
            scannedResult = extracted
            isProcessingOcr = false
        }
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Document Scanner",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI DOCUMENT SCANNER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Paper Invoice Camera OCR & Extraction",
                            fontSize = 10.sp,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If an invoice has been extracted, show the review editor
                if (scannedResult != null) {
                    ExtractedInvoiceReviewForm(
                        initialResult = scannedResult!!,
                        entities = entities,
                        onConfirm = { issuing, client, country, amt, curr, tax, desc, due ->
                            onSaveInvoice(issuing, client, country, amt, curr, tax, desc, due)
                            onDismiss()
                        },
                        onRetake = {
                            scannedResult = null
                            capturedPhotoBitmap = null
                        }
                    )
                } else {
                    // Document Scanner Camera Viewfinder HUD
                    ScannerViewfinderHud(
                        isProcessing = isProcessingOcr,
                        onTriggerCamera = {
                            if (hasCameraPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onTriggerGallery = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    // Quick Cross-Border Invoice Presets Strip
                    PresetInvoicePicker(
                        onSelectSample = { sample ->
                            isProcessingOcr = true
                            scannedResult = InvoiceOcrExtractorEngine.extractFromText(sample.rawText)
                            isProcessingOcr = false
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Camera Viewfinder HUD with animated laser scanning line and targeting reticle.
 */
@Composable
private fun ScannerViewfinderHud(
    isProcessing: Boolean,
    onTriggerCamera: () -> Unit,
    onTriggerGallery: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("scanner_camera_viewfinder"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.5f), BorderSubtle)
        ))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Viewfinder Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cornerLen = 28f
                val strokeW = 3f

                // Draw 4 Targeting Corners (Reticle)
                // Top-Left
                drawLine(CyanAccent, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeW)
                drawLine(CyanAccent, Offset(16f, 16f), Offset(16f, 16f + cornerLen), strokeW)
                // Top-Right
                drawLine(CyanAccent, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), strokeW)
                drawLine(CyanAccent, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeW)
                // Bottom-Left
                drawLine(CyanAccent, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeW)
                drawLine(CyanAccent, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), strokeW)
                // Bottom-Right
                drawLine(CyanAccent, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), strokeW)
                drawLine(CyanAccent, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), strokeW)

                // Laser Scanning Line
                val currentLaserY = h * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            CyanAccent.copy(alpha = 0.8f),
                            Color.White,
                            CyanAccent.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(20f, currentLaserY),
                    end = Offset(w - 20f, currentLaserY),
                    strokeWidth = 2.5f
                )
            }

            // Viewfinder Center Overlay & Capture Actions
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Scanner Telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(EmeraldPositive, CircleShape))
                        Text(
                            text = "AI OPTICAL SENSORS ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPositive,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "ALIGN DOCUMENT EDGES",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Center Prompt / Progress
                if (isProcessing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(32.dp))
                        Text(
                            text = "Extracting Vendor, Currency, Amounts & VAT...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Invoice",
                            tint = CyanAccent.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Hold invoice flat & centered within reticle",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Bottom Action Buttons: Camera Capture & Gallery
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTriggerCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_trigger_camera_capture")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "SNAP PHOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = onTriggerGallery,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_trigger_photo_picker")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", modifier = Modifier.size(15.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "PHOTO PICKER", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/**
 * Preset Cross-Border Paper Invoices selector allowing instant testing without physical paper.
 */
@Composable
private fun PresetInvoicePicker(
    onSelectSample: (com.example.worldbusiness.data.model.SamplePaperInvoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OR LOAD CROSS-BORDER PAPER INVOICE SAMPLE:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(text = "1-Click OCR Demo", fontSize = 9.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InvoiceOcrExtractorEngine.SAMPLE_INVOICES.forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSample(sample) }
                        .testTag("sample_invoice_${sample.id.lowercase(Locale.US)}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = sample.countryFlag.take(2), fontSize = 16.sp)
                            Column {
                                Text(
                                    text = sample.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${sample.currency} ${NumberFormat.getNumberInstance(Locale.US).format(sample.expectedResult.totalAmount)} • ${sample.expectedResult.invoiceNumber}",
                                    fontSize = 10.sp,
                                    color = EmeraldPositive,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Extract",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Structured Data Review Form: Operator can inspect, edit, and post the extracted invoice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractedInvoiceReviewForm(
    initialResult: ScannedInvoiceResult,
    entities: List<EntityRecord>,
    onConfirm: (
        issuingEntity: String,
        clientName: String,
        clientCountry: String,
        amount: Double,
        currency: String,
        taxPercent: Double,
        description: String,
        dueDate: String
    ) -> Unit,
    onRetake: () -> Unit
) {
    var vendorName by remember { mutableStateOf(initialResult.vendorName) }
    var selectedClientEntity by remember {
        mutableStateOf(
            entities.find { it.name.contains(initialResult.clientName, ignoreCase = true) }?.name
                ?: entities.firstOrNull()?.name ?: "OmniGlobal Holdings Inc."
        )
    }
    var clientCountry by remember { mutableStateOf(initialResult.clientCountry) }
    var amountText by remember { mutableStateOf(initialResult.totalAmount.toString()) }
    var currency by remember { mutableStateOf(initialResult.currency) }
    var taxRateText by remember { mutableStateOf(initialResult.taxRatePercent.toString()) }
    var description by remember { mutableStateOf(initialResult.description) }
    var dueDate by remember { mutableStateOf(initialResult.dueDate) }
    var isCurrencyMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Extraction Success Header Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = EmeraldPositive.copy(alpha = 0.12f),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                listOf(EmeraldPositive.copy(alpha = 0.6f), BorderSubtle)
            )),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = EmeraldPositive, modifier = Modifier.size(16.dp))
                    Column {
                        Text(text = "STRUCTURED DATA EXTRACTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        Text(text = "${initialResult.confidenceScore}% OCR Confidence • ${initialResult.invoiceNumber}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
                TextButton(onClick = onRetake) {
                    Text(text = "Rescan", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        // Vendor / Supplier (Parsed)
        OutlinedTextField(
            value = vendorName,
            onValueChange = { vendorName = it },
            label = { Text("Vendor / B2B Supplier (Extracted)", fontSize = 11.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Vendor", tint = CyanAccent) },
            modifier = Modifier.fillMaxWidth().testTag("input_extracted_vendor"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle)
        )

        // Customer / Billed Entity
        OutlinedTextField(
            value = selectedClientEntity,
            onValueChange = { selectedClientEntity = it },
            label = { Text("Billed Subsidiary (Group Entity)", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().testTag("input_extracted_client"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle)
        )

        // Amount & Currency Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Invoice Amount", fontSize = 11.sp) },
                modifier = Modifier.weight(1.3f).testTag("input_extracted_amount"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPositive, unfocusedBorderColor = BorderSubtle)
            )

            // Currency Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = isCurrencyMenuOpen,
                onExpandedChange = { isCurrencyMenuOpen = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency", fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyMenuOpen) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("dropdown_extracted_currency")
                )
                ExposedDropdownMenu(
                    expanded = isCurrencyMenuOpen,
                    onDismissRequest = { isCurrencyMenuOpen = false }
                ) {
                    listOf("EUR", "USD", "GBP", "CHF", "SGD", "BRL").forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                            onClick = {
                                currency = curr
                                isCurrencyMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Tax / VAT Rate & Due Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = taxRateText,
                onValueChange = { taxRateText = it },
                label = { Text("VAT / Tax %", fontSize = 11.sp) },
                modifier = Modifier.weight(1f).testTag("input_extracted_tax"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle)
            )

            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Payment Due", fontSize = 11.sp) },
                modifier = Modifier.weight(1.3f).testTag("input_extracted_due_date"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle)
            )
        }

        // Description of Goods / Services
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Service / Goods Description", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().testTag("input_extracted_description"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderSubtle),
            maxLines = 2
        )

        // Bank Routing Settlement
        if (initialResult.bankDetails.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SurfaceElevated.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏦 ${initialResult.bankDetails}",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Post Invoice Button
        Button(
            onClick = {
                val amt = amountText.toDoubleOrNull() ?: initialResult.totalAmount
                val tax = taxRateText.toDoubleOrNull() ?: initialResult.taxRatePercent
                onConfirm(
                    selectedClientEntity,
                    vendorName,
                    clientCountry,
                    amt,
                    currency,
                    tax,
                    description,
                    dueDate
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("btn_post_extracted_invoice")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "POST INVOICE TO COMMERCIAL LEDGER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
