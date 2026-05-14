package com.amir.buysmart.domain.repository

import com.amir.buysmart.domain.model.ItemHistory
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun getItemsForList(listId: String): Flow<List<ShoppingItem>>
    fun getItemsByLocation(listId: String, location: ShoppingLocation): Flow<List<ShoppingItem>>
    suspend fun addItem(item: ShoppingItem)
    suspend fun toggleBought(itemId: String, listId: String, isBought: Boolean)
    suspend fun deleteItem(itemId: String, listId: String)
    suspend fun finishShopping(listId: String, location: ShoppingLocation)
    suspend fun searchItemNames(query: String, listId: String): List<String>
    suspend fun updateItem(item: ShoppingItem)
    suspend fun getItemByName(name: String, listId: String): ShoppingItem?
    suspend fun saveHistory(name: String, location: ShoppingLocation, note: String, quantity: String)
    suspend fun getHistory(name: String): ItemHistory?
}
