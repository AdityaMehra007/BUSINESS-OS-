package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val issuingEntityName: String,
    val clientName: String,
    val clientCountry: String,
    val issueDate: String,
    val dueDate: String,
    val amount: Double,
    val currency: String,
    val taxRatePercent: Double,
    val status: String, // "PAID", "PENDING", "OVERDUE", "IN_CLEARING"
    val serviceDescription: String,
    val isSynced: Boolean = true,
    val syncStatus: String = "SYNCED", // "SYNCED", "PENDING_SYNC", "SYNC_CONFLICT", "LOCAL_DRAFT"
    val lastSyncedAt: String = "Oct 04, 2026 • 12:00 UTC",
    val offlineCreated: Boolean = false,
    val localVersion: Long = 1L,
    val syncError: String? = null
)
