package com.amir.buysmart.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ListRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val dataStore: DataStore<Preferences>
) : ListRepository {

    private val activeListKey = stringPreferencesKey("active_list_id")

    override fun getUserLists(userId: String): Flow<List<ShoppingList>> =
        firestoreService.getUserLists(userId)

    override suspend fun createList(list: ShoppingList): ShoppingList =
        firestoreService.createList(list)

    override suspend fun joinListByCode(inviteCode: String, userId: String): ShoppingList? =
        firestoreService.joinListByCode(inviteCode, userId)

    override suspend fun getActiveListId(userId: String): String? =
        dataStore.data.first()[activeListKey]

    override suspend fun setActiveList(userId: String, listId: String) {
        dataStore.edit { it[activeListKey] = listId }
    }
}
