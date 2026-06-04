package com.amir.buysmart.domain.model

/** בקשת הצטרפות ממתינה לרשימה — נשמרת ב-lists/{listId}/joinRequests/{uid}. */
data class JoinRequest(
    val uid: String = "",
    val name: String = ""
)

/** תוצאת ניסיון הצטרפות לפי קוד. */
sealed interface JoinResult {
    /** המשתמש כבר חבר — מחזיר את הרשימה למעבר ישיר. */
    data class AlreadyMember(val list: ShoppingList) : JoinResult
    /** נשלחה בקשה הממתינה לאישור חבר ברשימה. */
    data class Requested(val listId: String, val listName: String) : JoinResult
    /** לא נמצאה רשימה עם הקוד. */
    data object NotFound : JoinResult
}
