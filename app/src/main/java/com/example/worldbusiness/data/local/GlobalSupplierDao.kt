package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing the directory of global corporate suppliers.
 */
@Dao
interface GlobalSupplierDao {

    @Query("SELECT * FROM global_suppliers ORDER BY legalName ASC")
    fun getAllSuppliers(): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE isActive = 1 ORDER BY legalName ASC")
    fun getActiveSuppliers(): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE complianceStatus = :status ORDER BY legalName ASC")
    fun getSuppliersByCompliance(status: String): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE countryCode = :countryCode ORDER BY legalName ASC")
    fun getSuppliersByCountry(countryCode: String): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE businessCategory = :category ORDER BY legalName ASC")
    fun getSuppliersByCategory(category: String): Flow<List<GlobalSupplierRecord>>

    @Query("""
        SELECT * FROM global_suppliers 
        WHERE legalName LIKE '%' || :query || '%' 
           OR tradingName LIKE '%' || :query || '%' 
           OR supplierCode LIKE '%' || :query || '%' 
           OR taxRegistrationNumber LIKE '%' || :query || '%'
           OR country LIKE '%' || :query || '%'
           OR primaryContactName LIKE '%' || :query || '%'
        ORDER BY legalName ASC
    """)
    fun searchSuppliers(query: String): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE riskLevel = 'HIGH' OR complianceStatus = 'NON_COMPLIANT' ORDER BY legalName ASC")
    fun getHighRiskSuppliers(): Flow<List<GlobalSupplierRecord>>

    @Query("SELECT * FROM global_suppliers WHERE id = :id LIMIT 1")
    fun getSupplierById(id: Long): Flow<GlobalSupplierRecord?>

    @Query("SELECT * FROM global_suppliers WHERE supplierCode = :supplierCode LIMIT 1")
    fun getSupplierByCode(supplierCode: String): Flow<GlobalSupplierRecord?>

    @Query("SELECT * FROM global_suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierByIdSync(id: Long): GlobalSupplierRecord?

    @Query("SELECT COUNT(*) FROM global_suppliers")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM global_suppliers WHERE complianceStatus = 'COMPLIANT'")
    suspend fun getCompliantCount(): Int

    @Query("SELECT COUNT(*) FROM global_suppliers WHERE complianceStatus = 'AUDIT_PENDING'")
    suspend fun getPendingAuditCount(): Int

    @Query("SELECT COUNT(*) FROM global_suppliers WHERE complianceStatus = 'DOCUMENTATION_EXPIRED' OR complianceStatus = 'NON_COMPLIANT'")
    suspend fun getNonCompliantOrExpiredCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: GlobalSupplierRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<GlobalSupplierRecord>)

    @Update
    suspend fun updateSupplier(supplier: GlobalSupplierRecord)

    @Delete
    suspend fun deleteSupplier(supplier: GlobalSupplierRecord)

    @Query("DELETE FROM global_suppliers WHERE id = :id")
    suspend fun deleteSupplierById(id: Long): Int

    @Query("UPDATE global_suppliers SET complianceStatus = :newStatus, complianceOfficerNotes = :notes WHERE id = :id")
    suspend fun updateComplianceStatus(id: Long, newStatus: String, notes: String)

    @Query("UPDATE global_suppliers SET paymentTermsDays = :termsDays, paymentTermsDescription = :description, preferredCurrency = :currency WHERE id = :id")
    suspend fun updatePaymentTerms(id: Long, termsDays: Int, description: String, currency: String)

    @Query("UPDATE global_suppliers SET w8BenOrTaxFormFiled = :w8Ben, iso9001Certified = :iso, complianceDocExpiryDate = :expiryDate WHERE id = :id")
    suspend fun updateDocumentationStatus(id: Long, w8Ben: Boolean, iso: Boolean, expiryDate: String)

    @Query("UPDATE global_suppliers SET isActive = :isActive WHERE id = :id")
    suspend fun setSupplierActiveStatus(id: Long, isActive: Boolean)
}
