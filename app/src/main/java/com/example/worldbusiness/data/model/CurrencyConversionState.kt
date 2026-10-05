package com.example.worldbusiness.data.model

data class ConversionQuote(
    val amount: Double,
    val fromCurrency: String,
    val toCurrency: String,
    val convertedAmount: Double,
    val exchangeRate: Double,
    val inverseRate: Double,
    val isLiveApi: Boolean,
    val provider: String,
    val timestamp: String,
    val feePercent: Double = 0.0 // 0% interbank rate
)

sealed class ConversionUiState {
    object Idle : ConversionUiState()
    object Loading : ConversionUiState()
    data class Success(val quote: ConversionQuote) : ConversionUiState()
    data class Error(val message: String, val fallbackQuote: ConversionQuote? = null) : ConversionUiState()
}
