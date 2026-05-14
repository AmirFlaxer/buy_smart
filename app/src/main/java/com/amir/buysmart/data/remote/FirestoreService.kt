package com.amir.buysmart.data.remote

import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.model.ShoppingLocation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun itemsCollection(listId: String) =
        firestore.collection("lists").document(listId).collection("items")

    private fun listsCollection() = firestore.collection("lists")

    fun getItemsForList(listId: String): Flow<List<ShoppingItem>> = callbackFlow {
        val listener = itemsCollection(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ShoppingItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            quantity = doc.getString("quantity") ?: "",
                            note = doc.getString("note") ?: "",
                            location = ShoppingLocation.valueOf(doc.getString("location") ?: "SUPERMARKET"),
                            type = ItemType.valueOf(doc.getString("type") ?: "ONE_TIME"),
                            isBought = doc.getBoolean("isBought") ?: false,
                            addedBy = doc.getString("addedBy") ?: "",
                            addedByName = doc.getString("addedByName") ?: "",
                            listId = listId
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addItem(item: ShoppingItem) {
        val data = mapOf(
            "name" to item.name,
            "quantity" to item.quantity,
            "note" to item.note,
            "location" to item.location.name,
            "type" to item.type.name,
            "isBought" to item.isBought,
            "addedBy" to item.addedBy,
            "addedByName" to item.addedByName
        )
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
            "type" to item.type.name
        )
        itemsCollection(item.listId).document(item.id).update(data).await()
    }

    suspend fun toggleBought(itemId: String, listId: String, isBought: Boolean) {
        itemsCollection(listId).document(itemId).update("isBought", isBought).await()
    }

    suspend fun deleteItem(itemId: String, listId: String) {
        itemsCollection(listId).document(itemId).delete().await()
    }

    suspend fun getBoughtItemsByLocation(listId: String, location: ShoppingLocation): List<ShoppingItem> {
        return itemsCollection(listId)
            .whereEqualTo("location", location.name)
            .whereEqualTo("isBought", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                try {
                    ShoppingItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        note = doc.getString("note") ?: "",
                        location = location,
                        type = ItemType.valueOf(doc.getString("type") ?: "ONE_TIME"),
                        isBought = true,
                        addedBy = doc.getString("addedBy") ?: "",
                        addedByName = doc.getString("addedByName") ?: "",
                        listId = listId
                    )
                } catch (e: Exception) { null }
            }
    }

    fun getUserLists(userId: String): Flow<List<ShoppingList>> = callbackFlow {
        val listener = listsCollection()
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val lists = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ShoppingList(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            ownerId = doc.getString("ownerId") ?: "",
                            members = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            inviteCode = doc.getString("inviteCode") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createList(list: ShoppingList): ShoppingList {
        val data = mapOf(
            "name" to list.name,
            "ownerId" to list.ownerId,
            "members" to list.members,
            "inviteCode" to list.inviteCode
        )
        val ref = listsCollection().add(data).await()
        return list.copy(id = ref.id)
    }

    suspend fun joinListByCode(inviteCode: String, userId: String): ShoppingList? {
        val query = listsCollection()
            .whereEqualTo("inviteCode", inviteCode)
            .get()
            .await()
        val doc = query.documents.firstOrNull() ?: return null
        listsCollection().document(doc.id)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            .await()
        return ShoppingList(
            id = doc.id,
            name = doc.getString("name") ?: "",
            ownerId = doc.getString("ownerId") ?: "",
            members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()?.plus(userId) ?: listOf(userId),
            inviteCode = inviteCode
        )
    }

    suspend fun saveUserFcmToken(userId: String, token: String) {
        firestore.collection("users").document(userId)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }
}
