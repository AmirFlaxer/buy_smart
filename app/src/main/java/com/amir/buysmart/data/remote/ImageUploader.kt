package com.amir.buysmart.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * מקודד תמונת פריט ל-base64 לשמירה ישירות ב-Firestore (ללא Firebase Storage).
 * התמונה מוקטנת ל-MAX_DIMENSION ונדחסת ל-JPEG כדי להישאר מתחת למגבלת מסמך Firestore (1MB).
 */
@Singleton
class ImageUploader @Inject constructor() {

    /** מקודד את התמונה ל-base64 (ללא קידומת data:). מחזיר null במקרה של כשל. */
    suspend fun encodeItemImage(context: Context, uri: Uri): String? {
        return try {
            val bytes = compressImage(context, uri) ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "קידוד תמונה נכשל", e)
            null
        }
    }

    /** טוען את התמונה מוקטנת ודחוסה ל-bytes של JPEG. */
    private fun compressImage(context: Context, uri: Uri): ByteArray? {
        // שלב 1: קריאת מימדים בלבד (ללא טעינת הביטמאפ לזיכרון)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        // שלב 2: חישוב inSampleSize לחיסכון בזיכרון בטעינה
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        // שלב 3: התאמה מדויקת ל-MAX_DIMENSION
        val scaled = scaleDown(bitmap, MAX_DIMENSION)
        if (scaled != bitmap) bitmap.recycle()

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim && h / 2 >= maxDim) {
            w /= 2; h /= 2; sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    companion object {
        private const val TAG = "ImageUploader"
        // 720px מספיק לתמונת פריט קטנה ושומר על הקידוד קטן ב-Firestore
        private const val MAX_DIMENSION = 720
        private const val JPEG_QUALITY = 75
    }
}
