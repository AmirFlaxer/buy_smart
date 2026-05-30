package com.amir.buysmart.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUploader @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * מעלה תמונה ל-Firebase Storage תחת items/{listId}/{uuid}.jpg
     * ומחזיר URL לקריאה. במקרה של כשל — מחזיר null.
     */
    suspend fun uploadItemImage(context: Context, listId: String, uri: Uri): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("items/$listId/$fileName")
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }
}
