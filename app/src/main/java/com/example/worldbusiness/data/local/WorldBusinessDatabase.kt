package com.example.worldbusiness.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.GlobalSupplierRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.model.TeamMemberRecord
import com.example.worldbusiness.data.model.TreasuryTransactionRecord

@Database(
    entities = [
        EntityRecord::class,
        InvoiceRecord::class,
        TeamMemberRecord::class,
        ShipmentRecord::class,
        FxBalanceRecord::class,
        TreasuryTransactionRecord::class,
        AuditLogRecord::class,
        GlobalSupplierRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class WorldBusinessDatabase : RoomDatabase() {
    abstract fun entityDao(): EntityDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun shipmentDao(): ShipmentDao
    abstract fun fxBalanceDao(): FxBalanceDao
    abstract fun treasuryTransactionDao(): TreasuryTransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun globalSupplierDao(): GlobalSupplierDao

    companion object {
        @Volatile
        private var INSTANCE: WorldBusinessDatabase? = null

        fun getDatabase(context: Context): WorldBusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorldBusinessDatabase::class.java,
                    "world_business_os_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
