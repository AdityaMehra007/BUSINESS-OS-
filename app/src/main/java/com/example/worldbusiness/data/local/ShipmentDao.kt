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

    @Query("SELECT COUNT(*) FROM shipments")
    suspend fun getCount(): Int
}
