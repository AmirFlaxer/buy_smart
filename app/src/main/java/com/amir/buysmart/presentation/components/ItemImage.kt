package com.amir.buysmart.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * מציג תמונת פריט. תומך בשני מקורות:
 *  - base64 (תמונות חדשות שנשמרות ב-Firestore)
 *  - URL מסוג http/https (תמונות ישנות מ-Firebase Storage, לתאימות לאחור)
 *
 * מחרוזת ריקה לא מציגה דבר.
 */
@Composable
fun ItemImage(
    data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (data.isBlank()) return

    if (data.startsWith("http", ignoreCase = true)) {
        AsyncImage(
            model = data,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val bitmap: ImageBitmap? = remember(data) { decodeBase64(data) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

private fun decodeBase64(data: String): ImageBitmap? {
    return try {
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
