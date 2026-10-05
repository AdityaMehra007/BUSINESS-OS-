package com.example.worldbusiness.data.model

data class CurrencyAccountDetail(
    val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val nativeBalance: Double,
    val rateToUsd: Double,
    val dailyChangePercent: Double,
    val bankInstitution: String,
    val accountMasked: String,
    val swiftBic: String,
    val routingCode: String,
    val liquidityTier: String,
    val yieldApy: Double,
    val convertedBalanceInBase: Double,
    val conversionRateToBase: Double,
    val portfolioSharePercent: Double
)

data class TreasuryConversionSummary(
    val baseCurrency: String,
    val totalConsolidatedBalance: Double,
    val accountsCount: Int,
    val net24hImpactAmount: Double,
    val net24hImpactPercent: Double,
    val weightedYieldApy: Double,
    val accounts: List<CurrencyAccountDetail>
)

data class CrossRateQuote(
    val basePair: String,
    val rate: Double,
    val bid: Double,
    val ask: Double,
    val spreadPercent: Double,
    val dailyChangePercent: Double
)

data class HistoricalBalancePoint(
    val dayIndex: Int,
    val dateLabel: String,
    val fullDate: String,
    val balance: Double,
    val dailyChange: Double,
    val dailyChangePercent: Double,
    val isPeak: Boolean = false,
    val isTrough: Boolean = false,
    val eventNote: String? = null
)

data class TreasuryTrendHistory(
    val currencyCode: String,
    val currencySymbol: String,
    val baseCurrency: String,
    val timeRangeDays: Int,
    val points: List<HistoricalBalancePoint>,
    val startBalance: Double,
    val currentBalance: Double,
    val netChangeAmount: Double,
    val netChangePercent: Double,
    val minBalance: Double,
    val maxBalance: Double,
    val averageBalance: Double,
    val minDate: String,
    val maxDate: String,
    val volatilityPercent: Double
)
