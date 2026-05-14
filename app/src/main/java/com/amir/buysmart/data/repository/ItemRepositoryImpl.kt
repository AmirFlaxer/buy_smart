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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val itemDao: ItemDao,
    private val historyDao: ItemHistoryDao
) : ItemRepository {

    override fun getItemsForList(listId: String): Flow<List<ShoppingItem>> {
        return firestoreService.getItemsForList(listId).onEach { items ->
            items.forEach { itemDao.insertItem(ShoppingItemEntity.fromDomain(it)) }
        }
    }

    override fun getItemsByLocation(listId: String, location: ShoppingLocation): Flow<List<ShoppingItem>> {
        return firestoreService.getItemsForList(listId).map { items ->
            items.filter { it.location == location }
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
        historyDao.upsert(ItemHistoryEntity(
            name = name.trim().lowercase(),
            location = location.name,
            note = note,
            quantity = quantity
        ))
    }

    override suspend fun getHistory(name: String): ItemHistory? =
        historyDao.getByName(name.trim().lowercase())?.toDomain()

    override suspend fun finishShopping(listId: String, location: ShoppingLocation) {
        val boughtItems = firestoreService.getBoughtItemsByLocation(listId, location)
        boughtItems.forEach { item ->
            if (item.type == ItemType.ONE_TIME) {
                firestoreService.deleteItem(item.id, listId)
                itemDao.deleteItem(item.id)
            } else {
                firestoreService.toggleBought(item.id, listId, false)
                itemDao.updateBought(item.id, false)
            }
        }
    }
}
