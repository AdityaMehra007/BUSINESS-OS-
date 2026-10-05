package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.worldbusiness.data.model.TreasuryTransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TreasuryTransactionDao {
    @Query("SELECT * FROM treasury_transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<TreasuryTransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TreasuryTransactionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TreasuryTransactionRecord>)

    @Query("SELECT COUNT(*) FROM treasury_transactions")
    suspend fun getCount(): Int
}
