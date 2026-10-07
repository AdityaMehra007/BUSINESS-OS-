package com.example.worldbusiness.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private fun createFirestore(context: Context): FirebaseFirestore {
    val appCtx = context.applicationContext
    if (FirebaseApp.getApps(appCtx).isEmpty()) {
        FirebaseApp.initializeApp(appCtx)
    }
    return FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
}

/**
 * Enterprise Cloud Firebase Firestore Synchronization & Authentication Repository.
 * Handles authenticated multi-device cloud replication for Treasury, Entities, and Invoicing.
 */
class FirebaseSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "FirebaseSyncRepository"
    }

    // Convenience secondary constructor resolving custom database ID from string resources
    constructor(context: Context) : this(
        createFirestore(context),
        FirebaseAuth.getInstance()
    )

    private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUserFlow: StateFlow<FirebaseUser?> = _currentUserFlow.asStateFlow()

    private val _syncStateFlow = MutableStateFlow("IDLE")
    val syncStateFlow: StateFlow<String> = _syncStateFlow.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserFlow.value = firebaseAuth.currentUser
        }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("Operation requires authenticated executive session")
    }

    /**
     * Creates or updates the executive user profile in Firestore
     */
    suspend fun saveUserProfile(displayName: String, email: String, baseCurrency: String = "USD") {
        val uid = requireUserId()
        val profileData = hashMapOf(
            "userId" to uid,
            "displayName" to displayName.ifBlank { "Executive User" },
            "email" to email,
            "organizationName" to "OmniGlobal Holdings Inc.",
            "baseCurrency" to baseCurrency.uppercase(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            profileData["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(profileData, SetOptions.merge()).await()
        Log.d(TAG, "User profile saved for $uid")
    }

    /**
     * Synchronizes a corporate entity record to Cloud Firestore
     */
    suspend fun syncEntity(entity: EntityRecord) {
        val uid = requireUserId()
        val docId = entity.id.toString()
        val data = hashMapOf(
            "id" to docId,
            "userId" to uid,
            "name" to entity.name,
            "jurisdiction" to entity.jurisdiction,
            "countryCode" to entity.countryCode.take(2).uppercase(),
            "entityType" to entity.entityType,
            "taxId" to entity.taxId,
            "status" to entity.status,
            "baseCurrency" to entity.baseCurrency.take(3).uppercase(),
            "operatingCapital" to entity.operatingCapital,
            "annualFilingDeadline" to entity.annualFilingDeadline,
            "localDirector" to entity.localDirector,
            "complianceScore" to entity.complianceScore,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection("users").document(uid).collection("entities").document(docId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data, SetOptions.merge()).await()
        Log.d(TAG, "Entity ${entity.name} synced to Firestore for user $uid")
    }

    /**
     * Synchronizes a cross-border invoice record to Cloud Firestore
     */
    suspend fun syncInvoice(invoice: InvoiceRecord) {
        val uid = requireUserId()
        val docId = if (invoice.id > 0) invoice.id.toString() else invoice.invoiceNumber
        val data = hashMapOf(
            "id" to docId,
            "userId" to uid,
            "invoiceNumber" to invoice.invoiceNumber,
            "issuingEntityName" to invoice.issuingEntityName,
            "clientName" to invoice.clientName,
            "clientCountry" to invoice.clientCountry,
            "issueDate" to invoice.issueDate,
            "dueDate" to invoice.dueDate,
            "amount" to invoice.amount,
            "currency" to invoice.currency.take(3).uppercase(),
            "taxRatePercent" to invoice.taxRatePercent,
            "status" to invoice.status,
            "serviceDescription" to invoice.serviceDescription,
            "isSynced" to true,
            "syncStatus" to "SYNCED_CLOUD",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection("users").document(uid).collection("invoices").document(docId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data, SetOptions.merge()).await()
        Log.d(TAG, "Invoice ${invoice.invoiceNumber} synced to Firestore for user $uid")
    }

    /**
     * Synchronizes a multi-currency treasury balance to Cloud Firestore
     */
    suspend fun syncFxBalance(balance: FxBalanceRecord) {
        val uid = requireUserId()
        val docId = balance.currencyCode.uppercase()
        val data = hashMapOf(
            "currencyCode" to docId,
            "userId" to uid,
            "currencyName" to balance.currencyName,
            "symbol" to balance.symbol,
            "balance" to balance.balance,
            "rateToUsd" to balance.rateToUsd,
            "dailyChangePercent" to balance.dailyChangePercent,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(uid)
            .collection("fx_balances").document(docId)
            .set(data, SetOptions.merge())
            .await()
        Log.d(TAG, "FX Balance $docId synced to Firestore for user $uid")
    }

    /**
     * Batch synchronization of all corporate entities, invoices, and FX balances
     */
    suspend fun syncAllData(
        entities: List<EntityRecord>,
        invoices: List<InvoiceRecord>,
        balances: List<FxBalanceRecord>
    ): Int {
        val uid = requireUserId()
        _syncStateFlow.value = "SYNCING"
        var syncedCount = 0

        try {
            entities.forEach { entity ->
                syncEntity(entity)
                syncedCount++
            }
            invoices.forEach { invoice ->
                syncInvoice(invoice)
                syncedCount++
            }
            balances.forEach { balance ->
                syncFxBalance(balance)
                syncedCount++
            }
            _syncStateFlow.value = "SYNCED"
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch Firestore synchronization", e)
            _syncStateFlow.value = "ERROR: ${e.message}"
            throw e
        }

        return syncedCount
    }

    /**
     * Observes cloud entities for the authenticated user in real-time
     */
    fun observeCloudEntities(): Flow<List<EntityRecord>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .collection("entities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing entities: ${error.message}")
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        EntityRecord(
                            id = doc.getString("id")?.toLongOrNull() ?: 0L,
                            name = doc.getString("name") ?: "",
                            jurisdiction = doc.getString("jurisdiction") ?: "",
                            countryCode = doc.getString("countryCode") ?: "",
                            entityType = doc.getString("entityType") ?: "",
                            taxId = doc.getString("taxId") ?: "",
                            status = doc.getString("status") ?: "GOOD_STANDING",
                            baseCurrency = doc.getString("baseCurrency") ?: "USD",
                            operatingCapital = doc.getDouble("operatingCapital") ?: 0.0,
                            annualFilingDeadline = doc.getString("annualFilingDeadline") ?: "",
                            localDirector = doc.getString("localDirector") ?: "",
                            complianceScore = doc.getLong("complianceScore")?.toInt() ?: 95
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun signOut() {
        auth.signOut()
    }
}
