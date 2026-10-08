package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.worldbusiness.data.model.ShipmentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ShipmentDao {
    @Query("SELECT * FROM shipments ORDER BY id DESC")
    fun getAllShipments(): Flow<List<ShipmentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ShipmentRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shipments: List<ShipmentRecord>)

    @Update
    suspend fun updateShipment(shipment: ShipmentRecord)

    @Delete
    suspend fun deleteShipment(shipment: ShipmentRecord)

    @Query("SELECT * FROM shipments WHERE id = :id")
    suspend fun getShipmentById(id: Long): ShipmentRecord?

    @Query("UPDATE shipments SET customsStatus = :newStatus, estimatedArrival = :newEta WHERE id = :id")
    suspend fun updateStatusAndEta(id: Long, newStatus: String, newEta: String)

    @Query("UPDATE shipments SET customsStatus = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String)

    @Query("SELECT COUNT(*) FROM shipments")
    suspend fun getCount(): Int
}
