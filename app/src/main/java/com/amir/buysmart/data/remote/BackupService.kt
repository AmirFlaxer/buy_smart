package com.amir.buysmart.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * מבצע את [ops] ב-batches, כשכל batch מוגבל גם במספר פעולות ([BackupService.MAX_BATCH_OPS])
 * וגם בגודל מוערך ([BackupService.MAX_BATCH_BYTES]) - פריטים נושאים תמונות base64, ו-Firestore
 * דוחה commit מעל 10MiB. [firstBatchExtra] נכנס ל-batch הראשון (גם כש-[ops] ריק).
 */
internal suspend fun <T> FirebaseFirestore.commitInChunks(
    ops: List<T>,
    sizeOf: (T) -> Int,
    apply: (WriteBatch, T) -> Unit,
    firstBatchExtra: ((WriteBatch) -> Unit)? = null
) {
    var batch = batch()
    var count = 0
    var bytes = 0
    var pendingExtra = firstBatchExtra
    suspend fun flush() {
        pendingExtra?.let { it(batch); count++; pendingExtra = null }
        if (count > 0) batch.commit().await()
        batch = batch()
        count = 0
        bytes = 0
    }
    for (op in ops) {
        val size = sizeOf(op)
        if (count > 0 && (count >= BackupService.MAX_BATCH_OPS || bytes + size > BackupService.MAX_BATCH_BYTES)) {
            flush()
        }
        apply(batch, op)
        count++
        bytes += size
        pendingExtra?.let { it(batch); count++; pendingExtra = null }
    }
    flush()
}

/** גודל מוער של מפת-שדות: סכום אורכי המחרוזות (התמונות הן ה-base64 הגדול) + תקורה. */
internal fun Map<String, Any>.estimatedBytes(): Int =
    values.sumOf { (it as? String)?.length ?: 8 } + 200

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

    /** כותב מסמכי פריטים חדשים לגיבוי (set), ב-batches לפי מספר פעולות וגודל. */
    suspend fun writeItems(listId: String, backupId: String, docs: Map<String, Map<String, Any>>) {
        firestore.commitInChunks(
            ops = docs.entries.toList(),
            sizeOf = { it.value.estimatedBytes() },
            apply = { batch, (id, data) ->
                batch.set(
                    backupItems(listId, backupId).document(id),
                    data + ("createdAt" to FieldValue.serverTimestamp())
                )
            }
        )
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
        firestore.commitInChunks(
            ops = items,
            sizeOf = { 200 },
            apply = { batch, doc -> batch.delete(doc.reference) }
        )
        backupsCollection(listId).document(backupId).delete().await()
    }

    companion object {
        const val MAX_BATCH_OPS = 450
        const val MAX_BATCH_BYTES = 8_000_000
    }
}
