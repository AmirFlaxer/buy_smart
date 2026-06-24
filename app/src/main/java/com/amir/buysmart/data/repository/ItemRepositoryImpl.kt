package com.amir.buysmart.data.repository

import com.amir.buysmart.data.local.ItemDao
import com.amir.buysmart.data.local.ItemHistoryDao
import com.amir.buysmart.data.local.entity.ItemHistoryEntity
import com.amir.buysmart.data.local.entity.ShoppingItemEntity
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.domain.model.ItemHistory
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.util.ItemMerge
import com.amir.buysmart.domain.util.UnitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val itemDao: ItemDao,
    private val historyDao: ItemHistoryDao
) : ItemRepository {

    override fun getItemsForList(listId: String): Flow<List<ShoppingItem>> {
        return firestoreService.getItemsForList(listId).onEach { items ->
            // החלפה מלאה של פריטי הרשימה — כך פריטים שנמחקו ע"י משתמש אחר
            // לא נשארים במטמון ולא מופיעים בהשלמה האוטומטית
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
        firestoreService.deleteItem(itemId, listId)
        itemDao.deleteItem(itemId)
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
            refillIds = recurring.map { it.id }
        )
        itemDao.deleteItemsByIds(oneTime.map { it.id })
        recurring.forEach { itemDao.setPendingRefillAndResetBought(it.id, true) }
    }

    override suspend fun approvePendingRefill(item: ShoppingItem) {
        firestoreService.setPendingRefill(item.id, item.listId, false)
        itemDao.setPendingRefillAndResetBought(item.id, false)
    }

    override suspend fun mergeDuplicates(group: List<ShoppingItem>, unitPreference: String) {
        if (group.size < 2) return
        val pref = if (unitPreference == "COUNT") UnitType.COUNT else UnitType.WEIGHT
        val result = ItemMerge.merge(group, pref)
        firestoreService.mergeItemsBatch(group.first().listId, result.survivor, result.deleteIds)
        itemDao.deleteItemsByIds(result.deleteIds)
        itemDao.updateItem(ShoppingItemEntity.fromDomain(result.survivor))
    }
}
