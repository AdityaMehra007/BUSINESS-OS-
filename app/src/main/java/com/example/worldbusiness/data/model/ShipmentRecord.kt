package com.example.worldbusiness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipments")
data class ShipmentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackingCode: String,
    val origin: String,
    val destination: String,
    val carrier: String,
    val incoterm: String, // "DDP", "FOB", "CIF", "EXW"
    val cargoDescription: String,
    val cargoValue: Double,
    val currency: String,
    val customsStatus: String, // "CLEARED", "IN_TRANSIT", "PORT_INSPECTION", "DOCUMENTATION_REQ"
    val estimatedArrival: String
)
