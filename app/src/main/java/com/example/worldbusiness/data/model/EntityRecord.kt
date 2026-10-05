package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entities")
data class EntityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val jurisdiction: String,
    val countryCode: String,
    val entityType: String,
    val taxId: String,
    val status: String, // "ACTIVE", "GOOD_STANDING", "FILING_DUE"
    val baseCurrency: String,
    val operatingCapital: Double,
    val annualFilingDeadline: String,
    val localDirector: String,
    val complianceScore: Int // 0 - 100
)
