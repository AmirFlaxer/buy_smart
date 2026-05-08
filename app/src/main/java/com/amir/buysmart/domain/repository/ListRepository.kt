package com.amir.buysmart.domain.repository

import com.amir.buysmart.domain.model.ShoppingList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun getUserLists(userId: String): Flow<List<ShoppingList>>
    suspend fun createList(list: ShoppingList): ShoppingList
    suspend fun joinListByCode(inviteCode: String, userId: String): ShoppingList?
    suspend fun getActiveListId(userId: String): String?
    suspend fun setActiveList(userId: String, listId: String)
}
