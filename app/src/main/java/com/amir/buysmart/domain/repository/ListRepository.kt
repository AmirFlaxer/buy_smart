package com.amir.buysmart.domain.repository

import com.amir.buysmart.domain.model.JoinRequest
import com.amir.buysmart.domain.model.JoinResult
import com.amir.buysmart.domain.model.ShoppingList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun getUserLists(userId: String): Flow<List<ShoppingList>>
    suspend fun createList(list: ShoppingList): ShoppingList
    suspend fun leaveList(userId: String, listId: String)
    suspend fun getActiveListId(userId: String): String?
    suspend fun setActiveList(userId: String, listId: String)
    suspend fun addCustomLocation(listId: String, name: String)
    suspend fun removeCustomLocation(listId: String, name: String)

    // ──── הצטרפות עם אישור ────
    /** שולח בקשת הצטרפות לפי קוד (או מחזיר AlreadyMember אם כבר חבר). */
    suspend fun requestToJoin(inviteCode: String, userId: String, userName: String): JoinResult
    /** בקשות ההצטרפות הממתינות לרשימה (לצד המאשר). */
    fun observeJoinRequests(listId: String): Flow<List<JoinRequest>>
    /** מאשר בקשה: מוסיף את המשתמש ל-members ומוחק את הבקשה. */
    suspend fun approveJoinRequest(listId: String, uid: String)
    /** דוחה/מבטל בקשה: מוחק אותה. */
    suspend fun rejectJoinRequest(listId: String, uid: String)
    /** האם קיימת לי בקשה ממתינה לרשימה (לצד המבקש — לזיהוי דחייה). */
    fun observeMyJoinRequest(listId: String, uid: String): Flow<Boolean>

    // ──── שמירת בקשה ממתינה מקומית (DataStore) ────
    /** מחזיר (listId, listName) של בקשה ממתינה שמורה, או null. */
    suspend fun getPendingJoin(): Pair<String, String>?
    suspend fun setPendingJoin(listId: String, listName: String)
    suspend fun clearPendingJoin()

    // ──── העדפת יחידה למיזוג כפילויות (DataStore) ────
    /** "WEIGHT" (ברירת מחדל) או "COUNT". */
    fun getMergeUnitPreference(): kotlinx.coroutines.flow.Flow<String>
    suspend fun setMergeUnitPreference(value: String)
}
