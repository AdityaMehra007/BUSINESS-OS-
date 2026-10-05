package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val role: String,
    val country: String,
    val countryCode: String,
    val employmentType: String, // "Full-Time (EOR)", "Direct Subsidiary", "B2B Contractor"
    val monthlyCompensation: Double,
    val currency: String,
    val status: String, // "ACTIVE", "PAID_THIS_CYCLE", "ONBOARDING"
    val taxJurisdiction: String,
    val lastPaidDate: String
)
