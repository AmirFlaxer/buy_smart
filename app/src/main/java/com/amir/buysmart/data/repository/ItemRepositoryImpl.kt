package com.amir.buysmart.data.repository

import com.amir.buysmart.data.local.ItemDao
import com.amir.buysmart.data.local.ItemHistoryDao
import com.amir.buysmart.data.local.entity.ItemHistoryEntity
import com.amir.buysmart.data.local.entity.ShoppingItemEntity
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.domain.model.DeleteReason
import com.amir.buysmart.domain.model.Deleter
import com.amir.buysmart.domain.model.ItemHistory
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.util.ItemMerge
import com.amir.buysmart.domain.util.UnitType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val itemDao: ItemDao,
    private val historyDao: ItemHistoryDao,
    private val auth: FirebaseAuth
) : ItemRepository {

    // שם המוחק - כמו displayName ב-HomeViewModel
    private fun currentDeleter(): Deleter {
        val user = auth.currentUser
        return Deleter(
            uid = user?.uid ?: "",
            name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
        )
    }

    override fun getItemsForList(listId: String): Flow<List<ShoppingItem>> {
        return firestoreService.observeAllItems(listId)
            .map { snapshot -> snapshot.items.filter { !it.isDeleted } }
            .onEach { items ->
                // החלפה מלאה של פריטי הרשימה - כך פריטים שנמחקו ע"י משתמש אחר
                // לא נשארים במטמון ולא מופיעים בהשלמה האוטומטית (Room מקבל פעילים בלבד)
                itemDao.replaceItemsForList(listId, items.map { ShoppingItemEntity.fromDomain(it) })
            }
    }

    override suspend fun addItem(item: ShoppingItem) {
        firestoreService.addItem(item)
    }

    override suspend fun toggleBought(itemId: String, listId: String, isBought: Boolean) {
        firestoreService.toggleBought(itemId, listId, isBought)
    }

    override suspend fun deleteItem(itemId: String, listId: String) {
        firestoreService.softDeleteItem(itemId, listId, DeleteReason.MANUAL, currentDeleter())
        itemDao.deleteItem(itemId)
    }

    override suspend fun undeleteItem(item: ShoppingItem) {
        firestoreService.undeleteItems(item.listId, listOf(item.id), markAutoRestored = false)
    }

    override suspend fun searchItemNames(query: String, listId: String): List<String> =
        itemDao.searchItemNames(query, listId)

    override suspend fun getItemByName(name: String, listId: String): ShoppingItem? =
        itemDao.getItemByName(name, listId)?.toDomain()

    override suspend fun updateItem(item: ShoppingItem) {
        firestoreService.updateItem(item)
        itemDao.updateItem(ShoppingItemEntity.fromDomain(item))
    }

    override suspend fun saveHistory(name: String, location: ShoppingLocation, note: String, quantity: String) {
        val key = name.trim().lowercase()
        val existing = historyDao.getByName(key)
        historyDao.upsert(ItemHistoryEntity(
            name = key,
            location = location.name,
            note = note.ifBlank { existing?.note ?: "" },
            quantity = quantity.ifBlank { existing?.quantity ?: "" }
        ))
    }

    override suspend fun getHistory(name: String): ItemHistory? =
        historyDao.getByName(name.trim().lowercase())?.toDomain()

    override suspend fun finishShopping(listId: String, categoryKey: String) {
        val boughtItems = firestoreService.getBoughtItemsByCategoryKey(listId, categoryKey)
        // ONE_TIME נמחק; RECURRING עובר לאזור "לחידוש" — מחכה לאישור לפני שחוזר לרשימה
        val (oneTime, recurring) = boughtItems.partition { it.type == ItemType.ONE_TIME }
        firestoreService.finishShoppingBatch(
            listId,
            deleteIds = oneTime.map { it.id },
            refillIds = recurring.map { it.id },
            by = currentDeleter()
        )
        itemDao.deleteItemsByIds(oneTime.map { it.id })
        recurring.forEach { itemDao.setPendingRefillAndResetBought(it.id, true) }
    }

    override suspend fun approvePendingRefill(item: ShoppingItem) {
        firestoreService.setPendingRefill(item.id, item.listId, false)
        itemDao.setPendingRefillAndResetBought(item.id, false)
    }

    // ההפך מ-approvePendingRefill: פריט פעיל שהתחרטו עליו חוזר להמתין באזור "לחידוש"
    override suspend fun moveToPendingRefill(item: ShoppingItem) {
        firestoreService.setPendingRefill(item.id, item.listId, true)
        itemDao.setPendingRefillAndResetBought(item.id, true)
    }

    override suspend fun mergeDuplicates(group: List<ShoppingItem>, unitPreference: String) {
        if (group.size < 2) return
        val pref = if (unitPreference == "COUNT") UnitType.COUNT else UnitType.WEIGHT
        val result = ItemMerge.merge(group, pref)
        firestoreService.mergeItemsBatch(group.first().listId, result.survivor, result.deleteIds, currentDeleter())
        itemDao.deleteItemsByIds(result.deleteIds)
        itemDao.updateItem(ShoppingItemEntity.fromDomain(result.survivor))
    }
}
