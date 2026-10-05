package com.example.worldbusiness.data.model

import android.graphics.Bitmap

data class InvoiceLineItem(
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double
)

data class ScannedInvoiceResult(
    val vendorName: String,
    val vendorTaxId: String = "",
    val clientName: String,
    val clientCountry: String = "United States",
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val currency: String, // "EUR", "USD", "GBP", "CHF", "SGD"
    val currencySymbol: String = "$",
    val subtotal: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val description: String,
    val bankDetails: String = "",
    val confidenceScore: Int = 95, // 0 - 100%
    val rawOcrText: String = "",
    val capturedBitmap: Bitmap? = null
)

data class SamplePaperInvoice(
    val id: String,
    val title: String,
    val countryFlag: String,
    val currency: String,
    val rawText: String,
    val expectedResult: ScannedInvoiceResult
)
