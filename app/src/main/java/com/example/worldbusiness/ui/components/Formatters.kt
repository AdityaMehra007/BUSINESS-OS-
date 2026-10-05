package com.example.worldbusiness.ui.components

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.CyanAccent
import java.text.NumberFormat
import java.util.Locale

object Formatters {
    fun formatCurrency(amount: Double, currency: String): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val symbol = when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "SGD" -> "S$"
            "CHF" -> "CHF "
            "JPY" -> "¥"
            "BRL" -> "R$"
            else -> "$currency "
        }
        return "$symbol${numberFormat.format(amount)}"
    }

    fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return numberFormat.format(amount)
    }

    fun formatCompactNumber(amount: Double): String {
        val absVal = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000.0 -> "$sign${String.format(Locale.US, "%.2fB", absVal / 1_000_000_000.0)}"
            absVal >= 1_000_000.0 -> "$sign${String.format(Locale.US, "%.2fM", absVal / 1_000_000.0)}"
            absVal >= 1_000.0 -> "$sign${String.format(Locale.US, "%.1fK", absVal / 1_000.0)}"
            else -> "$sign${String.format(Locale.US, "%.2f", absVal)}"
        }
    }

    fun formatUsd(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return "$${numberFormat.format(amount)}"
    }

    fun getStatusColor(status: String): Color {
        return when (status.uppercase(Locale.US)) {
            "PAID", "ACTIVE", "CLEARED", "GOOD_STANDING", "PAID_THIS_CYCLE" -> EmeraldPositive
            "PENDING", "IN_TRANSIT", "FILING_DUE", "PORT_INSPECTION" -> GoldAccent
            "OVERDUE", "DOCUMENTATION_REQ", "FAILED" -> RoseNegative
            else -> CyanAccent
        }
    }

    fun formatRate(rate: Double): String {
        return if (rate < 0.01) {
            String.format(Locale.US, "%.5f", rate)
        } else if (rate < 1.0) {
            String.format(Locale.US, "%.4f", rate)
        } else if (rate < 100.0) {
            String.format(Locale.US, "%.4f", rate)
        } else {
            String.format(Locale.US, "%.2f", rate)
        }
    }

    fun getCurrencyColor(currency: String): Color {
        return when (currency.uppercase(Locale.US)) {
            "USD" -> Color(0xFF10B981) // Emerald Green
            "EUR" -> Color(0xFF3B82F6) // Deep Blue
            "GBP" -> Color(0xFFA855F7) // Royal Purple
            "SGD" -> Color(0xFFF97316) // Singapore Amber Orange
            "CHF" -> Color(0xFFEF4444) // Swiss Red
            "JPY" -> Color(0xFFEC4899) // Cherry Pink
            "BRL" -> Color(0xFF06B6D4) // Tropical Cyan
            "AUD" -> Color(0xFFEAB308) // Gold
            "CAD" -> Color(0xFFF43F5E) // Maple Crimson
            else -> CyanAccent
        }
    }

    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase(Locale.US)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "SGD" -> "S$"
            "CHF" -> "CHF "
            "JPY" -> "¥"
            "BRL" -> "R$"
            "AUD" -> "A$"
            "CAD" -> "C$"
            else -> "$currency "
        }
    }

    fun getCurrencyFlag(currency: String): String {
        return when (currency.uppercase(Locale.US)) {
            "USD" -> "🇺🇸"
            "EUR" -> "🇪🇺"
            "GBP" -> "🇬🇧"
            "SGD" -> "🇸🇬"
            "CHF" -> "🇨🇭"
            "JPY" -> "🇯🇵"
            "BRL" -> "🇧🇷"
            "AUD" -> "🇦🇺"
            "CAD" -> "🇨🇦"
            else -> "🌐"
        }
    }
}
