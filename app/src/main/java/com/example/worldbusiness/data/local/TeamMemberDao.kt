package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.worldbusiness.data.model.TeamMemberRecord
import kotlinx.coroutines.flow.Flow

data class RoomPayrollCurrencyAggregation(
    val currency: String,
    val memberCount: Int,
    val totalLocalGross: Double
)

data class RoomPayrollJurisdictionAggregation(
    val taxJurisdiction: String,
    val country: String,
    val countryCode: String,
    val memberCount: Int,
    val totalLocalGross: Double,
    val currency: String
)

data class RoomPayrollEmploymentTypeAggregation(
    val employmentType: String,
    val memberCount: Int,
    val totalLocalGross: Double
)

data class RoomPayrollTotalsAggregation(
    val totalHeadcount: Int,
    val totalCurrenciesCount: Int
)

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members ORDER BY id ASC")
    fun getAllTeamMembers(): Flow<List<TeamMemberRecord>>

    @Query("SELECT currency, COUNT(*) as memberCount, COALESCE(SUM(monthlyCompensation), 0.0) as totalLocalGross FROM team_members GROUP BY currency ORDER BY memberCount DESC")
    fun getPayrollCurrencyAggregations(): Flow<List<RoomPayrollCurrencyAggregation>>

    @Query("SELECT taxJurisdiction, country, countryCode, COUNT(*) as memberCount, COALESCE(SUM(monthlyCompensation), 0.0) as totalLocalGross, currency FROM team_members GROUP BY taxJurisdiction, country, countryCode, currency ORDER BY memberCount DESC")
    fun getPayrollJurisdictionAggregations(): Flow<List<RoomPayrollJurisdictionAggregation>>

    @Query("SELECT employmentType, COUNT(*) as memberCount, COALESCE(SUM(monthlyCompensation), 0.0) as totalLocalGross FROM team_members GROUP BY employmentType ORDER BY memberCount DESC")
    fun getPayrollEmploymentTypeAggregations(): Flow<List<RoomPayrollEmploymentTypeAggregation>>

    @Query("SELECT COUNT(*) as totalHeadcount, COUNT(DISTINCT currency) as totalCurrenciesCount FROM team_members")
    fun getPayrollTotalsAggregation(): Flow<RoomPayrollTotalsAggregation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<TeamMemberRecord>)

    @Update
    suspend fun updateMember(member: TeamMemberRecord)

    @Delete
    suspend fun deleteMember(member: TeamMemberRecord)

    @Query("UPDATE team_members SET status = 'PAID_THIS_CYCLE', lastPaidDate = :paidDate")
    suspend fun markAllAsPaid(paidDate: String)

    @Query("UPDATE team_members SET status = 'PAID_THIS_CYCLE', lastPaidDate = :paidDate WHERE id IN (:memberIds)")
    suspend fun markMembersAsPaid(memberIds: List<Long>, paidDate: String)

    @Query("SELECT COUNT(*) FROM team_members")
    suspend fun getCount(): Int
}
