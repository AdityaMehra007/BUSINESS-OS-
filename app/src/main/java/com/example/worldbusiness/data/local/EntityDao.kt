package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.worldbusiness.data.model.EntityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityDao {
    @Query("SELECT * FROM entities ORDER BY id ASC")
    fun getAllEntities(): Flow<List<EntityRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(entity: EntityRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EntityRecord>)

    @Update
    suspend fun updateEntity(entity: EntityRecord)

    @Delete
    suspend fun deleteEntity(entity: EntityRecord)

    @Query("SELECT COUNT(*) FROM entities")
    suspend fun getCount(): Int
}
