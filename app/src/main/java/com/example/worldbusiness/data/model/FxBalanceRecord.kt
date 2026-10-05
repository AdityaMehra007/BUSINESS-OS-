package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fx_balances")
data class FxBalanceRecord(
    @PrimaryKey
    val currencyCode: String, // "USD", "EUR", "GBP", "SGD", "JPY", "CHF", "AUD", "CAD"
    val currencyName: String,
    val symbol: String,
    val balance: Double,
    val rateToUsd: Double,
    val dailyChangePercent: Double
)
