package com.example.worldbusiness.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.TextMuted

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val rateToUsd: Double
)

data class CrossBorderTaxRule(
    val countryCode: String,
    val countryName: String,
    val taxName: String, // "VAT", "MWST", "GST", "USt", "Sales Tax"
    val standardRatePercent: Double,
    val reverseChargeAllowed: Boolean,
    val defaultWhtPercent: Double,
    val complianceReference: String
)

data class CrossBorderTaxCalculation(
    val subtotal: Double,
    val currency: String,
    val vatRatePercent: Double,
    val vatAmount: Double,
    val isReverseCharge: Boolean,
    val whtRatePercent: Double,
    val whtAmount: Double,
    val grossTotal: Double, // subtotal + vat
    val netReceivable: Double, // subtotal + vat - wht
    val equivalentUsd: Double,
    val complianceNote: String
)

enum class InvoiceStatusStage(
    val key: String,
    val label: String,
    val color: Color,
    val description: String,
    val stepIndex: Int
) {
    DRAFT("DRAFT", "Draft", Color(0xFF94A3B8), "Commercial terms and VAT rules being configured as local draft", 0),
    SENT("SENT", "Sent", Color(0xFF38BDF8), "Statutory invoice sent to client, awaiting cross-border remittance", 1),
    PENDING("PENDING", "Pending", GoldAccent, "Statutory invoice issued to client, awaiting cross-border payment", 1),
    IN_CLEARING("IN_CLEARING", "In-Clearing", CyanAccent, "Payment initiated, clearing through international SWIFT/SEPA rails", 2),
    PAID("PAID", "Paid", EmeraldPositive, "Funds settled into sovereign multi-currency treasury vault", 3),
    SETTLED("SETTLED", "Paid", EmeraldPositive, "Funds settled into sovereign multi-currency treasury vault", 3),
    OVERDUE("OVERDUE", "Overdue", RoseNegative, "Exceeded statutory grace period, automated dunning active", 1),
    DISPUTED("DISPUTED", "Disputed", RoseNegative, "Invoice placed on hold pending contractual or tax reconciliation", 1);

    companion object {
        fun fromString(status: String?): InvoiceStatusStage {
            if (status.isNullOrBlank()) return PENDING
            val normalized = status.trim().uppercase()
            return when (normalized) {
                "DRAFT" -> DRAFT
                "SENT" -> SENT
                "PENDING" -> PENDING
                "IN_CLEARING", "CLEARING", "INCLEARING" -> IN_CLEARING
                "PAID", "SETTLED" -> PAID
                "OVERDUE" -> OVERDUE
                "DISPUTED" -> DISPUTED
                else -> values().find { it.key.equals(normalized, ignoreCase = true) } ?: PENDING
            }
        }
    }
}
