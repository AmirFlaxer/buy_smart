package com.amir.buysmart.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.amir.buysmart.data.remote.FirestoreService
import com.amir.buysmart.domain.model.JoinRequest
import com.amir.buysmart.domain.model.JoinResult
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ListRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val dataStore: DataStore<Preferences>
) : ListRepository {

    private val activeListKey = stringPreferencesKey("active_list_id")
    private val pendingJoinIdKey = stringPreferencesKey("pending_join_list_id")
    private val pendingJoinNameKey = stringPreferencesKey("pending_join_list_name")
    private val mergeUnitKey = stringPreferencesKey("merge_unit_preference")

    override fun getUserLists(userId: String): Flow<List<ShoppingList>> =
        firestoreService.getUserLists(userId)

    override suspend fun createList(list: ShoppingList): ShoppingList =
        firestoreService.createList(list)

    override suspend fun leaveList(userId: String, listId: String) {
        firestoreService.leaveList(listId, userId)
        dataStore.edit { it.remove(activeListKey) }
    }

    override suspend fun getActiveListId(userId: String): String? =
        dataStore.data.first()[activeListKey]

    override suspend fun setActiveList(userId: String, listId: String) {
        dataStore.edit { it[activeListKey] = listId }
    }

    override suspend fun addCustomLocation(listId: String, name: String) {
        firestoreService.addCustomLocation(listId, name)
    }

    override suspend fun removeCustomLocation(listId: String, name: String) {
        firestoreService.removeCustomLocation(listId, name)
    }

    // ──── הצטרפות עם אישור ────

    override suspend fun requestToJoin(inviteCode: String, userId: String, userName: String): JoinResult =
        firestoreService.requestToJoin(inviteCode, userId, userName)

    override fun observeJoinRequests(listId: String): Flow<List<JoinRequest>> =
        firestoreService.observeJoinRequests(listId)

    override suspend fun approveJoinRequest(listId: String, uid: String) =
        firestoreService.approveJoinRequest(listId, uid)

    override suspend fun rejectJoinRequest(listId: String, uid: String) =
        firestoreService.rejectJoinRequest(listId, uid)

    override fun observeMyJoinRequest(listId: String, uid: String): Flow<Boolean> =
        firestoreService.observeMyJoinRequest(listId, uid)

    // ──── שמירת בקשה ממתינה מקומית ────

    override suspend fun getPendingJoin(): Pair<String, String>? {
        val prefs = dataStore.data.first()
        val id = prefs[pendingJoinIdKey] ?: return null
        return id to (prefs[pendingJoinNameKey] ?: "")
    }

    override suspend fun setPendingJoin(listId: String, listName: String) {
        dataStore.edit {
            it[pendingJoinIdKey] = listId
            it[pendingJoinNameKey] = listName
        }
    }

    override suspend fun clearPendingJoin() {
        dataStore.edit {
            it.remove(pendingJoinIdKey)
            it.remove(pendingJoinNameKey)
        }
    }

    override fun getMergeUnitPreference(): Flow<String> =
        dataStore.data.map { it[mergeUnitKey] ?: "WEIGHT" }

    override suspend fun setMergeUnitPreference(value: String) {
        dataStore.edit { it[mergeUnitKey] = value }
    }
}
