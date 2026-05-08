package com.amir.buysmart.domain.repository

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
}
