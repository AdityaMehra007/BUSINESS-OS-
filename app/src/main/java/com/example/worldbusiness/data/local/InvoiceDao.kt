package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.worldbusiness.data.model.InvoiceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceRecord>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceRecord)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceRecord)

    @Query("UPDATE invoices SET status = :newStatus WHERE id = :invoiceId")
    suspend fun updateStatus(invoiceId: Long, newStatus: String)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getCount(): Int

    @Query("SELECT * FROM invoices WHERE isSynced = 0 OR syncStatus = 'PENDING_SYNC' ORDER BY id ASC")
    suspend fun getPendingSyncInvoices(): List<InvoiceRecord>

    @Query("SELECT COUNT(*) FROM invoices WHERE isSynced = 0 OR syncStatus = 'PENDING_SYNC'")
    fun getPendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceRecord?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceRecord?

    @Query("UPDATE invoices SET isSynced = 1, syncStatus = 'SYNCED', lastSyncedAt = :timestamp, syncError = NULL WHERE id = :invoiceId")
    suspend fun markAsSynced(invoiceId: Long, timestamp: String)

    @Query("UPDATE invoices SET isSynced = 0, syncStatus = 'PENDING_SYNC' WHERE id = :invoiceId")
    suspend fun markPendingSync(invoiceId: Long)

    @Query("UPDATE invoices SET isSynced = 1, syncStatus = 'SYNCED', lastSyncedAt = :timestamp, syncError = NULL WHERE id IN (:invoiceIds)")
    suspend fun markMultipleAsSynced(invoiceIds: List<Long>, timestamp: String)

    @Query("UPDATE invoices SET syncStatus = :status, lastSyncedAt = :timestamp, syncError = :error WHERE id = :invoiceId")
    suspend fun updateSyncStatus(invoiceId: Long, status: String, timestamp: String, error: String? = null)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteById(invoiceId: Long)
}
