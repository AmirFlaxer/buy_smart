package com.amir.buysmart.data.repository

import com.amir.buysmart.data.remote.BackupService
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.data.remote.toFirestoreMap
import com.amir.buysmart.domain.model.ItemsSnapshot
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.protection.ProtectionPolicy
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
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

    fun observeAllItems(listId: String): Flow<ItemsSnapshot> = firestoreService.observeAllItems(listId)

    suspend fun autoRestore(listId: String, itemIds: List<String>) {
        firestoreService.undeleteItems(listId, itemIds, markAutoRestored = true)
    }

    /** מספר מסמכי הפריטים בשרת (כולל מחוקים), לזיהוי מחיקה מהשרת. */
    suspend fun serverItemDocCount(listId: String): Int =
        itemsCollection(listId).get(Source.SERVER).await().size()

    suspend fun listExistsAndIsMember(listId: String, uid: String): Boolean {
        val doc = listDoc(listId).get(Source.SERVER).await()
        if (!doc.exists()) return false
        return (doc.get("members") as? List<*>)?.contains(uid) == true
    }

    /** הגיבוי בעל ה-id הגבוה ביותר שיש לו כותרת (id הוא yyyy-MM-dd, לכן סדר מילוני = כרונולוגי). */
    suspend fun latestBackup(listId: String): BackupInfo? {
        val header = backupService.listHeaders(listId).maxByOrNull { it.id } ?: return null
        return BackupInfo(
            id = header.id,
            createdAtMillis = header.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            itemCount = header.getLong("itemCount")?.toInt() ?: 0,
            customLocations = header.customLocationsList()
        )
    }

    suspend fun restoreFromBackup(listId: String, backup: BackupInfo) {
        val docs = backupService.readItems(listId, backup.id)
        docs.chunked(BackupService.MAX_BATCH_OPS).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc ->
                val item = doc.toItem(listId) ?: return@forEach
                // set מחליף את המסמך כולו: שדות המחיקה נעלמים, createdAt לא נכתב
                batch.set(
                    itemsCollection(listId).document(doc.id),
                    item.toFirestoreMap() + ("autoRestoredAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        }
        if (backup.customLocations.isNotEmpty()) {
            listDoc(listId).update(
                "customLocations", FieldValue.arrayUnion(*backup.customLocations.toTypedArray())
            ).await()
        }
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
        backupService.writeItems(listId, backupId, missing)

        val customLocations = listDoc(listId).get(Source.SERVER).await().customLocationsList()
        return try {
            backupService.writeHeader(listId, backupId, uid, active.size, customLocations)
            true
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) false else throw e
        }
    }

    suspend fun purge(listId: String, nowMillis: Long) {
        val items = itemsCollection(listId).get().await().documents.mapNotNull { it.toItem(listId) }
        ProtectionPolicy.itemsToPurge(items, nowMillis)
            .chunked(BackupService.MAX_BATCH_OPS)
            .forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(itemsCollection(listId).document(it)) }
                batch.commit().await()
            }

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
}
