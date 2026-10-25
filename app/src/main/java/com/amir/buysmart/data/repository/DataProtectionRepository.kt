package com.amir.buysmart.data.repository

import com.amir.buysmart.data.remote.BackupService
import android.util.Log
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.data.remote.commitInChunks
import com.amir.buysmart.data.remote.estimatedBytes
import com.amir.buysmart.data.remote.toFirestoreMap
import com.amir.buysmart.domain.model.ItemsSnapshot
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.protection.ProtectionPolicy
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class BackupInfo(
    val id: String,
    val createdAtMillis: Long,
    val itemCount: Int,
    val customLocations: List<String>
)

/**
 * הגנה על נתוני הרשימה: גיבויים, שחזור מגיבוי וניקוי. מחלקה קונקרטית בלי ממשק (הכרעה).
 * חריגות מועברות לקורא, חוץ ממקרה PERMISSION_DENIED המתועד ב-createBackupIfMissing.
 */
@Singleton
class DataProtectionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firestoreService: FirestoreService,
    private val backupService: BackupService
) {
    private fun listDoc(listId: String) = firestore.collection("lists").document(listId)

    private fun itemsCollection(listId: String) = listDoc(listId).collection("items")

    private fun DocumentSnapshot.toItem(listId: String): ShoppingItem? =
        with(firestoreService) { toShoppingItem(listId) }

    private fun DocumentSnapshot.customLocationsList(): List<String> =
        (get("customLocations") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    fun observeAllItems(listId: String, includeMetadataChanges: Boolean = false): Flow<ItemsSnapshot> =
        firestoreService.observeAllItems(listId, includeMetadataChanges)

    suspend fun autoRestore(listId: String, itemIds: List<String>) {
        firestoreService.undeleteItems(listId, itemIds, markAutoRestored = true)
    }

    /** שחזור ידני של פריטים שנמחקו, ממסך "נמחקו לאחרונה". */
    suspend fun undelete(listId: String, itemIds: List<String>) {
        firestoreService.undeleteItems(listId, itemIds, markAutoRestored = false)
    }

    /** מספר מסמכי הפריטים בשרת (כולל מחוקים), לזיהוי מחיקה מהשרת. */
    suspend fun serverItemDocCount(listId: String): Int =
        itemsCollection(listId).get(Source.SERVER).await().size()

    suspend fun listExistsAndIsMember(listId: String, uid: String): Boolean {
        // הכללים מתירים קריאה רק לחבר: רשימה שנמחקה או אי-חברות מגיעות כ-PERMISSION_DENIED
        val doc = try {
            listDoc(listId).get(Source.SERVER).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return false
            throw e
        }
        if (!doc.exists()) return false
        return (doc.get("members") as? List<*>)?.contains(uid) == true
    }

    /** הגיבוי האחרון לפי createdAt (זמן-שרת). כותרת בלי createdAt מדולגת. */
    suspend fun latestBackup(listId: String): BackupInfo? {
        val (header, createdAt) = backupService.listHeaders(listId)
            .mapNotNull { h -> h.getTimestamp("createdAt")?.toDate()?.time?.let { h to it } }
            .maxByOrNull { it.second } ?: return null
        return BackupInfo(
            id = header.id,
            createdAtMillis = createdAt,
            itemCount = header.getLong("itemCount")?.toInt() ?: 0,
            customLocations = header.customLocationsList()
        )
    }

    suspend fun restoreFromBackup(listId: String, backup: BackupInfo) {
        val items = backupService.readItems(listId, backup.id).mapNotNull { doc ->
            doc.toItem(listId) ?: run {
                Log.w(TAG, "פריט בגיבוי ${backup.id} לא נמפה, מדלג: ${doc.id}")
                null
            }
        }
        // customLocations ב-batch הראשון, כדי ששחזור חלקי ישחזר לפחות את הקטגוריות
        val locationsExtra: ((WriteBatch) -> Unit)? = if (backup.customLocations.isEmpty()) null else { b ->
            b.update(listDoc(listId), "customLocations", FieldValue.arrayUnion(*backup.customLocations.toTypedArray()))
        }
        firestore.commitInChunks(
            ops = items,
            sizeOf = { it.toFirestoreMap().estimatedBytes() },
            apply = { batch, item ->
                // set מחליף את המסמך כולו: שדות המחיקה נעלמים, createdAt לא נכתב
                batch.set(
                    itemsCollection(listId).document(item.id),
                    item.toFirestoreMap() + ("autoRestoredAt" to FieldValue.serverTimestamp())
                )
            },
            firstBatchExtra = locationsExtra
        )
    }

    /**
     * יוצר גיבוי אם חסר. מחזיר true רק אם הכותרת נכתבה על ידינו. כתיבה נדחית של
     * הכותרת (PERMISSION_DENIED) פירושה שמכשיר אחר הקדים אותנו - מחזיר false.
     */
    suspend fun createBackupIfMissing(listId: String, backupId: String, uid: String): Boolean {
        if (backupService.headerExists(listId, backupId)) return false

        val active = itemsCollection(listId).get(Source.SERVER).await().documents
            .mapNotNull { it.toItem(listId) }
            .filter { !it.isDeleted }
        if (active.isEmpty()) return false

        // create שנדחה מפיל batch שלם, לכן כותבים רק מסמכים שעדיין לא קיימים
        val existing = backupService.existingItemIds(listId, backupId)
        val missing = active.filter { it.id !in existing }
            .associate { it.id to it.toFirestoreMap() }
        val customLocations = listDoc(listId).get(Source.SERVER).await().customLocationsList()
        return try {
            backupService.writeItems(listId, backupId, missing)
            backupService.writeHeader(listId, backupId, uid, active.size, customLocations)
            true
        } catch (e: FirebaseFirestoreException) {
            // מסמך גיבוי הוא create-only: דחייה פירושה שמכשיר אחר כתב אותו בינתיים - לא שגיאה
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            Log.w(TAG, "גיבוי $backupId של $listId נכתב במקביל ממכשיר אחר", e)
            false
        }
    }

    suspend fun purge(listId: String, nowMillis: Long) {
        val items = itemsCollection(listId).get(Source.SERVER).await().documents.mapNotNull { it.toItem(listId) }
        firestore.commitInChunks(
            ops = ProtectionPolicy.itemsToPurge(items, nowMillis),
            sizeOf = { 200 },
            apply = { batch, id -> batch.delete(itemsCollection(listId).document(id)) }
        )

        val backups = backupService.listHeaders(listId)
            .mapNotNull { h -> h.getTimestamp("createdAt")?.toDate()?.time?.let { h.id to it } }
            .toMap()
        ProtectionPolicy.backupsToPurge(backups, nowMillis).forEach {
            backupService.deleteBackup(listId, it)
        }
    }

    suspend fun userListIds(uid: String): List<String> =
        firestore.collection("lists").whereArrayContains("members", uid).get().await()
            .documents.map { it.id }

    private companion object {
        const val TAG = "DataProtectionRepo"
    }
}
