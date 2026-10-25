package com.amir.buysmart.data.remote

import android.util.Log
import com.amir.buysmart.domain.model.DeleteReason
import com.amir.buysmart.domain.model.Deleter
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ItemsSnapshot
import com.amir.buysmart.domain.model.JoinRequest
import com.amir.buysmart.domain.model.JoinResult
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.model.ShoppingLocation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** מפת-השדות של פריט ב-Firestore. בלי שדות מחיקה/שחזור (deletedAt וכו') - אלה מנוהלים בנפרד. */
internal fun ShoppingItem.toFirestoreMap(): Map<String, Any> = mapOf(
    "name" to name,
    "quantity" to quantity,
    "note" to note,
    "location" to location.name,
    "customLocation" to customLocation,
    "type" to type.name,
    "isBought" to isBought,
    "addedBy" to addedBy,
    "addedByName" to addedByName,
    "priority" to priority.name,
    "pendingRefill" to pendingRefill,
    "imageUrl" to imageUrl,
    "purchaseCount" to purchaseCount
)

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun itemsCollection(listId: String) =
        firestore.collection("lists").document(listId).collection("items")

    private fun listsCollection() = firestore.collection("lists")

    private fun joinRequestsCollection(listId: String) =
        firestore.collection("lists").document(listId).collection("joinRequests")

    private fun inviteCodesCollection() = firestore.collection("inviteCodes")

    // קודים שכבר ווידאנו/יצרנו להם מיפוי ב-session הזה — למניעת כתיבות חוזרות ב-backfill.
    private val backfilledCodes = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /** ממיר מסמך Firestore ל-ShoppingItem. מחזיר null אם המיפוי נכשל. */
    internal fun DocumentSnapshot.toShoppingItem(listId: String): ShoppingItem? {
        return try {
            ShoppingItem(
                id = id,
                name = getString("name") ?: "",
                quantity = getString("quantity") ?: "",
                note = getString("note") ?: "",
                location = try {
                    ShoppingLocation.valueOf(getString("location") ?: "SUPERMARKET")
                } catch (e: Exception) {
                    ShoppingLocation.OTHER
                },
                customLocation = getString("customLocation") ?: "",
                type = try {
                    ItemType.valueOf(getString("type") ?: "ONE_TIME")
                } catch (e: Exception) {
                    ItemType.ONE_TIME
                },
                isBought = getBoolean("isBought") ?: false,
                addedBy = getString("addedBy") ?: "",
                addedByName = getString("addedByName") ?: "",
                listId = listId,
                priority = try {
                    ItemPriority.valueOf(getString("priority") ?: "NORMAL")
                } catch (e: Exception) {
                    ItemPriority.NORMAL
                },
                pendingRefill = getBoolean("pendingRefill") ?: false,
                imageUrl = getString("imageUrl") ?: "",
                deletedAtMillis = getTimestamp("deletedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?.toDate()?.time,
                deleteReason = getString("deleteReason") ?: "",
                deletedByName = getString("deletedByName") ?: "",
                autoRestoredAtMillis = getTimestamp("autoRestoredAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?.toDate()?.time,
                purchaseCount = getLong("purchaseCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.w(TAG, "כשל במיפוי פריט ${id}", e)
            null
        }
    }

    private fun DocumentSnapshot.toShoppingList(): ShoppingList? {
        return try {
            ShoppingList(
                id = id,
                name = getString("name") ?: "",
                ownerId = getString("ownerId") ?: "",
                members = (get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                inviteCode = getString("inviteCode") ?: "",
                customLocations = (get("customLocations") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) {
            Log.w(TAG, "כשל במיפוי רשימה ${id}", e)
            null
        }
    }

    /** כל מסמכי הפריטים ברשימה, כולל מחוקים (deletedAt) - הסינון באחריות הקורא. */
    fun observeAllItems(listId: String): Flow<ItemsSnapshot> = callbackFlow {
        val listener = itemsCollection(listId)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeAllItems נכשל עבור $listId", error)
                    close(error); return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.mapNotNull { it.toShoppingItem(listId) }
                trySend(ItemsSnapshot(items, snapshot.metadata.isFromCache))
            }
        awaitClose { listener.remove() }
    }

    suspend fun addItem(item: ShoppingItem) {
        val data = item.toFirestoreMap()
        if (item.id.isBlank()) {
            itemsCollection(item.listId).add(data).await()
        } else {
            itemsCollection(item.listId).document(item.id).set(data).await()
        }
    }

    suspend fun updateItem(item: ShoppingItem) {
        val data = mapOf(
            "name" to item.name,
            "quantity" to item.quantity,
            "note" to item.note,
            "location" to item.location.name,
            "customLocation" to item.customLocation,
            "type" to item.type.name,
            "priority" to item.priority.name,
            "imageUrl" to item.imageUrl
        )
        itemsCollection(item.listId).document(item.id).update(data).await()
    }

    suspend fun toggleBought(itemId: String, listId: String, isBought: Boolean) {
        itemsCollection(listId).document(itemId).update("isBought", isBought).await()
    }

    suspend fun setPendingRefill(itemId: String, listId: String, pendingRefill: Boolean) {
        itemsCollection(listId).document(itemId)
            .update(mapOf("pendingRefill" to pendingRefill, "isBought" to false))
            .await()
    }

    /** מחיקה רכה: המסמך נשאר, ומסומן כמחוק (מי, מתי ולמה). */
    suspend fun softDeleteItem(itemId: String, listId: String, reason: String, by: Deleter) {
        itemsCollection(listId).document(itemId).update(softDeleteFields(reason, by)).await()
    }

    private fun softDeleteFields(reason: String, by: Deleter): Map<String, Any> = mapOf(
        "deletedAt" to FieldValue.serverTimestamp(),
        "deleteReason" to reason,
        "deletedBy" to by.uid,
        "deletedByName" to by.name
    )

    /** מחזיר פריטים מחוקים לרשימה (ב-batch). markAutoRestored מסמן שהשחזור בוצע אוטומטית. */
    suspend fun undeleteItems(listId: String, itemIds: List<String>, markAutoRestored: Boolean) {
        if (itemIds.isEmpty()) return
        val batch = firestore.batch()
        itemIds.forEach {
            val fields = mutableMapOf<String, Any>(
                "deletedAt" to FieldValue.delete(),
                "deleteReason" to FieldValue.delete(),
                "deletedBy" to FieldValue.delete(),
                "deletedByName" to FieldValue.delete()
            )
            if (markAutoRestored) fields["autoRestoredAt"] = FieldValue.serverTimestamp()
            batch.update(itemsCollection(listId).document(it), fields)
        }
        batch.commit().await()
    }

    /**
     * סיום קנייה בקטגוריה ב-batch אטומי אחד: מחיקה רכה של החד-פעמיים והעברת החוזרים
     * ל"לחידוש". כשל רשת באמצע לא משאיר את הקטגוריה חצי-מטופלת.
     */
    suspend fun finishShoppingBatch(
        listId: String,
        deleteIds: List<String>,
        refillIds: List<String>,
        by: Deleter
    ) {
        if (deleteIds.isEmpty() && refillIds.isEmpty()) return
        val batch = firestore.batch()
        deleteIds.forEach {
            batch.update(itemsCollection(listId).document(it), softDeleteFields(DeleteReason.FINISH, by))
        }
        refillIds.forEach {
            batch.update(
                itemsCollection(listId).document(it),
                mapOf(
                    "pendingRefill" to true,
                    "isBought" to false,
                    "purchaseCount" to FieldValue.increment(1)
                )
            )
        }
        batch.commit().await()
    }

    /**
     * ממזג כפילויות ב-batch אטומי: מעדכן את הפריט השורד ומוחק (רך) את המיותרים.
     * כשל רשת באמצע לא משאיר מצב חצי-ממוזג.
     */
    suspend fun mergeItemsBatch(
        listId: String,
        survivor: ShoppingItem,
        deleteIds: List<String>,
        by: Deleter
    ) {
        if (deleteIds.isEmpty()) return
        val batch = firestore.batch()
        batch.update(
            itemsCollection(listId).document(survivor.id),
            mapOf(
                "name" to survivor.name,
                "quantity" to survivor.quantity,
                "note" to survivor.note,
                "priority" to survivor.priority.name,
                "type" to survivor.type.name,
                "imageUrl" to survivor.imageUrl
            )
        )
        deleteIds.forEach {
            batch.update(itemsCollection(listId).document(it), softDeleteFields(DeleteReason.MERGE, by))
        }
        batch.commit().await()
    }

    suspend fun getBoughtItemsByCategoryKey(listId: String, categoryKey: String): List<ShoppingItem> {
        val isCustom = categoryKey.startsWith("CUSTOM:")
        val customName = if (isCustom) categoryKey.removePrefix("CUSTOM:") else ""
        val query = if (isCustom) {
            itemsCollection(listId)
                .whereEqualTo("customLocation", customName)
                .whereEqualTo("isBought", true)
        } else {
            // בלי whereEqualTo על customLocation: מסמכים ישנים (לפני פיצ'ר הקטגוריות
            // המותאמות) חסרים את השדה, ו-Firestore לא מחזיר אותם על שוויון ל-"".
            // הסינון נעשה בזיכרון אחרי הפענוח (toShoppingItem משלים "" כברירת מחדל)
            itemsCollection(listId)
                .whereEqualTo("location", categoryKey)
                .whereEqualTo("isBought", true)
        }
        return query.get().await().documents
            .mapNotNull { it.toShoppingItem(listId) }
            .filter { !it.isDeleted }
            .filter { isCustom || it.customLocation.isEmpty() }
    }

    fun getUserLists(userId: String): Flow<List<ShoppingList>> = callbackFlow {
        val listener = listsCollection()
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getUserLists נכשל עבור $userId", error)
                    close(error); return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull { it.toShoppingList() } ?: emptyList()
                // backfill עצלן: מבטיח שלכל רשימה קיימת יש מיפוי inviteCodes (לרשימות שנוצרו
                // לפני מעבר ל-lookup). fire-and-forget — לא חוסם את הזרימה.
                lists.forEach { ensureInviteCodeMapping(it.id, it.inviteCode, it.name) }
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createList(list: ShoppingList): ShoppingList {
        val code = generateUniqueInviteCode()
        val data = mapOf(
            "name" to list.name,
            "ownerId" to list.ownerId,
            "members" to list.members,
            "inviteCode" to code,
            "customLocations" to list.customLocations
        )
        val ref = listsCollection().add(data).await()
        // מיפוי הקוד → הרשימה (lookup ישיר בהצטרפות, ללא סריקת כל הרשימות).
        inviteCodesCollection().document(code)
            .set(mapOf("listId" to ref.id, "name" to list.name))
            .await()
        backfilledCodes.add(code)
        return list.copy(id = ref.id, inviteCode = code)
    }

    /** מגריל קוד הזמנה בן 6 ספרות שאין לו עדיין מיפוי, כדי למנוע התנגשות. */
    private suspend fun generateUniqueInviteCode(): String {
        repeat(10) {
            val code = (100000..999999).random().toString()
            val exists = inviteCodesCollection().document(code).get().await().exists()
            if (!exists) return code
        }
        return (100000..999999).random().toString()
    }

    /** יוצר מיפוי inviteCodes/{code} → listId אם עדיין אין. נקרא ברקע (backfill). */
    private fun ensureInviteCodeMapping(listId: String, code: String, name: String) {
        if (code.isBlank() || !backfilledCodes.add(code)) return
        val doc = inviteCodesCollection().document(code)
        doc.get().addOnSuccessListener { snap ->
            if (snap?.exists() != true) {
                doc.set(mapOf("listId" to listId, "name" to name))
            }
        }
    }

    suspend fun addCustomLocation(listId: String, name: String) {
        listsCollection().document(listId)
            .update("customLocations", FieldValue.arrayUnion(name))
            .await()
    }

    suspend fun removeCustomLocation(listId: String, name: String) {
        listsCollection().document(listId)
            .update("customLocations", FieldValue.arrayRemove(name))
            .await()
    }

    suspend fun leaveList(listId: String, userId: String) {
        listsCollection().document(listId)
            .update("members", FieldValue.arrayRemove(userId))
            .await()
    }

    /** שולח בקשת הצטרפות לפי קוד. אם כבר חבר — מחזיר AlreadyMember למעבר ישיר. */
    suspend fun requestToJoin(inviteCode: String, userId: String, userName: String): JoinResult {
        // תרגום הקוד ל-listId דרך lookup ישיר (ללא סריקת כל הרשימות).
        val mapping = inviteCodesCollection().document(inviteCode).get().await()
        val listId = mapping.getString("listId")?.takeIf { it.isNotBlank() } ?: return JoinResult.NotFound
        val listName = mapping.getString("name") ?: ""
        // בדיקת "כבר חבר": חבר יכול לקרוא את מסמך הרשימה; מי שאינו חבר יקבל
        // permission-denied — ואז נתייחס אליו כלא-חבר וניצור בקשת הצטרפות.
        val existingList = try {
            listsCollection().document(listId).get().await().toShoppingList()
        } catch (e: Exception) {
            null
        }
        if (existingList != null && userId in existingList.members) {
            return JoinResult.AlreadyMember(existingList)
        }
        joinRequestsCollection(listId).document(userId)
            .set(mapOf("uid" to userId, "name" to userName))
            .await()
        return JoinResult.Requested(listId, listName)
    }

    /** בקשות הצטרפות ממתינות לרשימה (לצד המאשר). */
    fun observeJoinRequests(listId: String): Flow<List<JoinRequest>> = callbackFlow {
        val listener = joinRequestsCollection(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeJoinRequests נכשל עבור $listId", error)
                    close(error); return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { d ->
                    val uid = d.getString("uid") ?: return@mapNotNull null
                    JoinRequest(uid = uid, name = d.getString("name") ?: "")
                } ?: emptyList()
                trySend(requests.sortedBy { it.name })
            }
        awaitClose { listener.remove() }
    }

    /** מאשר בקשה: מוסיף ל-members ומוחק את מסמך הבקשה. */
    suspend fun approveJoinRequest(listId: String, uid: String) {
        listsCollection().document(listId)
            .update("members", FieldValue.arrayUnion(uid))
            .await()
        joinRequestsCollection(listId).document(uid).delete().await()
    }

    /** דוחה/מבטל בקשה: מוחק את מסמך הבקשה בלבד. */
    suspend fun rejectJoinRequest(listId: String, uid: String) {
        joinRequestsCollection(listId).document(uid).delete().await()
    }

    /** true כל עוד קיימת בקשה ממתינה שלי לרשימה (לצד המבקש — לזיהוי דחייה). */
    fun observeMyJoinRequest(listId: String, uid: String): Flow<Boolean> = callbackFlow {
        val listener = joinRequestsCollection(listId).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeMyJoinRequest נכשל עבור $listId/$uid", error)
                    close(error); return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    companion object {
        private const val TAG = "FirestoreService"
    }
}
