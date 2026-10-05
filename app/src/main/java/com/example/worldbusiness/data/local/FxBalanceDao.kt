package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.worldbusiness.data.model.FxBalanceRecord
import kotlinx.coroutines.flow.Flow

data class RoomTreasuryAggregation(
    val totalVaultsCount: Int,
    val totalConsolidatedUsd: Double,
    val highestBalanceUsd: Double,
    val lowestBalanceUsd: Double
)

@Dao
interface FxBalanceDao {
    @Query("SELECT * FROM fx_balances ORDER BY balance * rateToUsd DESC")
    fun getAllBalances(): Flow<List<FxBalanceRecord>>

    @Query("SELECT COUNT(*) as totalVaultsCount, COALESCE(SUM(balance * rateToUsd), 0.0) as totalConsolidatedUsd, COALESCE(MAX(balance * rateToUsd), 0.0) as highestBalanceUsd, COALESCE(MIN(balance * rateToUsd), 0.0) as lowestBalanceUsd FROM fx_balances")
    fun getConsolidatedTreasuryTotals(): Flow<RoomTreasuryAggregation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: FxBalanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<FxBalanceRecord>)

    @Update
    suspend fun updateBalance(balance: FxBalanceRecord)

    @Query("SELECT * FROM fx_balances WHERE currencyCode = :code LIMIT 1")
    suspend fun getByCode(code: String): FxBalanceRecord?

    @Query("SELECT COUNT(*) FROM fx_balances")
    suspend fun getCount(): Int
}
