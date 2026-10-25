package com.amir.buysmart.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * גישה גולמית ל-Firestore בלבד עבור גיבויי רשימה: lists/{id}/backups/{bid} (כותרת)
 * ו-lists/{id}/backups/{bid}/items (העתקי הפריטים). ההחלטות באחריות DataProtectionRepository.
 */
@Singleton
class BackupService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun backupsCollection(listId: String) =
        firestore.collection("lists").document(listId).collection("backups")

    private fun backupItems(listId: String, backupId: String) =
        backupsCollection(listId).document(backupId).collection("items")

    /** האם כותרת הגיבוי קיימת בשרת. */
    suspend fun headerExists(listId: String, backupId: String): Boolean =
        backupsCollection(listId).document(backupId).get(Source.SERVER).await().exists()

    /** כל כותרות הגיבויים של הרשימה (מסמכים שקיימים בפועל, לא רק תת-אוסף). */
    suspend fun listHeaders(listId: String): List<DocumentSnapshot> =
        backupsCollection(listId).get(Source.SERVER).await().documents

    suspend fun existingItemIds(listId: String, backupId: String): Set<String> =
        backupItems(listId, backupId).get(Source.SERVER).await().documents.map { it.id }.toSet()

    suspend fun readItems(listId: String, backupId: String): List<DocumentSnapshot> =
        backupItems(listId, backupId).get(Source.SERVER).await().documents

    /** כותב מסמכי פריטים חדשים לגיבוי (set), ב-batches של עד MAX_BATCH_OPS. */
    suspend fun writeItems(listId: String, backupId: String, docs: Map<String, Map<String, Any>>) {
        docs.entries.chunked(MAX_BATCH_OPS).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, data) ->
                batch.set(
                    backupItems(listId, backupId).document(id),
                    data + ("createdAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        }
    }

    suspend fun writeHeader(
        listId: String,
        backupId: String,
        uid: String,
        itemCount: Int,
        customLocations: List<String>
    ) {
        backupsCollection(listId).document(backupId).set(
            mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to uid,
                "itemCount" to itemCount,
                "customLocations" to customLocations
            )
        ).await()
    }

    /** מוחק גיבוי: קודם הפריטים, אחר כך הכותרת (כדי שלא יישארו פריטים יתומים בלי כותרת). */
    suspend fun deleteBackup(listId: String, backupId: String) {
        val items = backupItems(listId, backupId).get(Source.SERVER).await().documents
        items.chunked(MAX_BATCH_OPS).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        backupsCollection(listId).document(backupId).delete().await()
    }

    companion object {
        const val MAX_BATCH_OPS = 450
    }
}
