package com.example.worldbusiness.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @Json(name = "result") val result: String? = null,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "base_code") val baseCode: String? = null,
    @Json(name = "time_last_update_utc") val timeLastUpdateUtc: String? = null,
    @Json(name = "rates") val rates: Map<String, Double>? = null
)

enum class CurrencySyncStatus(
    val label: String,
    val color: Color,
    val isLive: Boolean
) {
    SYNCING("Fetching Live FX API...", CyanAccent, true),
    LIVE("Live Financial Feed", EmeraldPositive, true),
    CACHED("Cached Rates", GoldAccent, false),
    OFFLINE_FALLBACK("Sovereign Baseline", TextMuted, false)
}

data class LiveCurrencyFeed(
    val baseCurrency: String = "USD",
    val ratesToUsd: Map<String, Double> = defaultRatesToUsd,
    val lastUpdatedUtc: String = "Just now",
    val providerName: String = "ExchangeRate-API / Financial Feed",
    val status: CurrencySyncStatus = CurrencySyncStatus.LIVE,
    val rateSpreadPercent: Double = 0.05
) {
    companion object {
        val defaultRatesToUsd = mapOf(
            "USD" to 1.0,
            "EUR" to 1.0925,
            "GBP" to 1.3040,
            "SGD" to 0.7710,
            "CHF" to 1.1730,
            "JPY" to 0.0068,
            "BRL" to 0.1840,
            "AUD" to 0.6580,
            "CAD" to 0.7320,
            "CNY" to 0.1410,
            "INR" to 0.0120,
            "AED" to 0.2723,
            "HKD" to 0.1285,
            "MXN" to 0.0515,
            "SEK" to 0.0965
        )
    }

    fun getRateToUsd(currencyCode: String): Double {
        val upper = currencyCode.uppercase()
        return ratesToUsd[upper] ?: defaultRatesToUsd[upper] ?: 1.0
    }

    fun getCrossRate(fromCurrency: String, toCurrency: String): Double {
        val fromRate = getRateToUsd(fromCurrency)
        val toRate = getRateToUsd(toCurrency)
        return if (toRate > 0.0) fromRate / toRate else 1.0
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String = "USD"): Double {
        val crossRate = getCrossRate(fromCurrency, toCurrency)
        return amount * crossRate
    }
}
