package com.example.worldbusiness.data.repository

import android.util.Log
import com.example.worldbusiness.data.model.ConversionQuote
import com.example.worldbusiness.data.model.CurrencySyncStatus
import com.example.worldbusiness.data.model.ExchangeRateApiResponse
import com.example.worldbusiness.data.model.LiveCurrencyFeed
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String = "USD"
    ): ExchangeRateApiResponse
}

/**
 * Real-Time Currency Conversion Service.
 * Fetches live financial foreign exchange rates from global financial APIs
 * and provides automatic currency conversions for cross-border invoicing and treasury operations.
 */
class RealTimeCurrencyService(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "RealTimeCurrencyService"
        private const val BASE_URL = "https://open.er-api.com/"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    private val _currencyFeed = MutableStateFlow(
        LiveCurrencyFeed(
            baseCurrency = "USD",
            ratesToUsd = LiveCurrencyFeed.defaultRatesToUsd,
            lastUpdatedUtc = "Initialized Baseline",
            providerName = "ExchangeRate-API Global Feed",
            status = CurrencySyncStatus.OFFLINE_FALLBACK
        )
    )
    val currencyFeed: StateFlow<LiveCurrencyFeed> = _currencyFeed.asStateFlow()

    init {
        // Automatically fetch live rates upon initialization
        fetchLiveRates()
    }

    fun refresh() {
        fetchLiveRates()
    }

    private fun fetchLiveRates() {
        scope.launch {
            _currencyFeed.value = _currencyFeed.value.copy(status = CurrencySyncStatus.SYNCING)
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getLatestRates("USD")
                }

                if (response.result.equals("success", ignoreCase = true) && response.rates != null) {
                    val rawRates = response.rates
                    val calculatedRatesToUsd = mutableMapOf<String, Double>()
                    calculatedRatesToUsd["USD"] = 1.0

                    // Convert raw base-USD rates (1 USD = X Currency) into rateToUsd (1 Currency = Y USD)
                    rawRates.forEach { (code, rateFromUsd) ->
                        val upper = code.uppercase(Locale.US)
                        if (rateFromUsd > 0.0) {
                            calculatedRatesToUsd[upper] = if (upper == "USD") 1.0 else 1.0 / rateFromUsd
                        }
                    }

                    // Preserve key sovereign defaults if missing from payload
                    LiveCurrencyFeed.defaultRatesToUsd.forEach { (code, defaultVal) ->
                        if (!calculatedRatesToUsd.containsKey(code)) {
                            calculatedRatesToUsd[code] = defaultVal
                        }
                    }

                    val timeStr = response.timeLastUpdateUtc ?: SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).format(Date())

                    _currencyFeed.value = LiveCurrencyFeed(
                        baseCurrency = "USD",
                        ratesToUsd = calculatedRatesToUsd,
                        lastUpdatedUtc = timeStr,
                        providerName = response.provider ?: "ExchangeRate-API",
                        status = CurrencySyncStatus.LIVE
                    )
                    Log.d(TAG, "Successfully loaded real-time FX rates. EUR: ${calculatedRatesToUsd["EUR"]}, GBP: ${calculatedRatesToUsd["GBP"]}")
                } else {
                    useFallback("API returned non-success response")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch live FX rates from network, using resilient fallback: ${e.message}")
                useFallback("Network offline or timeout: ${e.message}")
            }
        }
    }

    private fun useFallback(reason: String) {
        val currentRates = _currencyFeed.value.ratesToUsd
        _currencyFeed.value = _currencyFeed.value.copy(
            ratesToUsd = if (currentRates.isNotEmpty()) currentRates else LiveCurrencyFeed.defaultRatesToUsd,
            status = CurrencySyncStatus.CACHED,
            lastUpdatedUtc = "Cached • $reason"
        )
    }

    /**
     * Converts an amount from one currency to another in real-time.
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String = "USD"): Double {
        return _currencyFeed.value.convert(amount, fromCurrency, toCurrency)
    }

    /**
     * Gets the current live exchange rate of a currency expressed in USD.
     */
    fun getRateToUsd(currencyCode: String): Double {
        return _currencyFeed.value.getRateToUsd(currencyCode)
    }

    /**
     * Computes the cross exchange rate between any two currencies (1 fromCurrency = X toCurrency).
     */
    fun getCrossRate(fromCurrency: String, toCurrency: String): Double {
        return _currencyFeed.value.getCrossRate(fromCurrency, toCurrency)
    }

    /**
     * Asynchronously fetches a high-precision live conversion quote from the financial API
     * with graceful fallback to cached sovereign matrix.
     */
    suspend fun fetchQuote(fromCurrency: String, toCurrency: String, amount: Double): ConversionQuote {
        val from = fromCurrency.uppercase(Locale.US)
        val to = toCurrency.uppercase(Locale.US)
        val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss 'UTC'", Locale.US)
        val nowStr = timeFormat.format(Date())

        if (from == to) {
            return ConversionQuote(
                amount = amount,
                fromCurrency = from,
                toCurrency = to,
                convertedAmount = amount,
                exchangeRate = 1.0,
                inverseRate = 1.0,
                isLiveApi = true,
                provider = "Exact Sovereign Match (1:1)",
                timestamp = nowStr
            )
        }

        try {
            val response = withContext(Dispatchers.IO) {
                api.getLatestRates(from)
            }
            val rate = response.rates?.get(to)
            if (rate != null && rate > 0.0) {
                val converted = amount * rate
                val timestampStr = response.timeLastUpdateUtc ?: nowStr
                return ConversionQuote(
                    amount = amount,
                    fromCurrency = from,
                    toCurrency = to,
                    convertedAmount = converted,
                    exchangeRate = rate,
                    inverseRate = 1.0 / rate,
                    isLiveApi = true,
                    provider = "Open Exchange Rates API (Real-Time • High Precision)",
                    timestamp = timestampStr
                )
            }
        } catch (_: Exception) {
            // Graceful fallback to cached state below
        }

        // Fallback to feed cross-rate
        val crossRate = _currencyFeed.value.getCrossRate(from, to)
        val converted = amount * crossRate
        return ConversionQuote(
            amount = amount,
            fromCurrency = from,
            toCurrency = to,
            convertedAmount = converted,
            exchangeRate = crossRate,
            inverseRate = if (crossRate > 0.0) 1.0 / crossRate else 0.0,
            isLiveApi = false,
            provider = "Sovereign Cached Matrix (Room / Offline)",
            timestamp = nowStr
        )
    }
}
