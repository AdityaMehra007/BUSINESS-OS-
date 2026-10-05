package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treasury_transactions")
data class TreasuryTransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val recipient: String,
    val senderOrCounterparty: String,
    val amount: Double,
    val currency: String,
    val isCredit: Boolean, // true = Credit / Inflow (+), false = Debit / Outflow (-)
    val category: String,  // "COMMERCIAL_SETTLEMENT", "GLOBAL_PAYROLL", "SUPPLY_FREIGHT", "FX_INTERBANK", "CAPITAL_INJECTION"
    val status: String,    // "SETTLED", "CLEARED", "PENDING"
    val referenceCode: String,
    val note: String = ""
)
