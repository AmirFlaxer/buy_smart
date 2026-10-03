package com.amir.buysmart.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * מציג תמונת פריט. תומך בשני מקורות:
 *  - base64 (תמונות חדשות שנשמרות ב-Firestore)
 *  - URL מסוג http/https (תמונות ישנות מ-Firebase Storage, לתאימות לאחור)
 *
 * מחרוזת ריקה לא מציגה דבר.
 * expandable = true: לחיצה פותחת את התמונה במסך מלא, לחיצה נוספת (או "חזור") סוגרת.
 * onReplace / onDelete: אם הועברו — במסך המלא מוצגים כפתורי "החלף תמונה" / "מחק תמונה".
 */
@Composable
fun ItemImage(
    data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    expandable: Boolean = false,
    onReplace: ((Uri) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    if (data.isBlank()) return

    var showFullScreen by rememberSaveable { mutableStateOf(false) }
    val clickModifier = if (expandable) Modifier.clickable { showFullScreen = true } else Modifier

    // הבוחר יושב מחוץ לדיאלוג — התוצאה מגיעה גם אחרי שהמסך המלא נסגר
    val pickImage = onReplace?.let { replace ->
        rememberImagePicker { uri ->
            showFullScreen = false
            replace(uri)
        }
    }

    ItemImageContent(data, contentDescription, modifier.then(clickModifier), contentScale)

    if (showFullScreen) {
        FullScreenImageDialog(
            data = data,
            contentDescription = contentDescription,
            onDismiss = { showFullScreen = false },
            onReplace = pickImage,
            onDelete = onDelete?.let { delete -> { showFullScreen = false; delete() } }
        )
    }
}

@Composable
private fun FullScreenImageDialog(
    data: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
    onReplace: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            ItemImageContent(
                data = data,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (onReplace != null || onDelete != null) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        // מעל פס הניווט/המחוות של המערכת — אחרת לחיצה נקלטת כ"בית"/"חזור"
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    if (onReplace != null) {
                        FilledTonalButton(onClick = onReplace) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("החלף תמונה")
                        }
                    }
                    if (onDelete != null) {
                        Button(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("מחק תמונה")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("למחוק את התמונה?") },
            text = { Text("הפריט יישאר ברשימה, רק בלי תמונה.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("מחק", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("ביטול") }
            }
        )
    }
}

@Composable
private fun ItemImageContent(
    data: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
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
