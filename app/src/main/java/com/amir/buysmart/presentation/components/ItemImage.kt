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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
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
        // decorFitsSystemWindows = false + systemBarsPadding: במכשירים שמדווחים את פסי המערכת לדיאלוג
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // סרגל הכפתורים בראש המסך: בתחתית חלק מהמכשירים (למשל סמסונג עם 3 כפתורי ניווט)
            // החלון נמתח מתחת לפס הניווט בלי לדווח את גובהו, והלחיצה נקלטת כ"בית"/"חזור"
            if (onReplace != null || onDelete != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onReplace != null) {
                        FilledTonalButton(onClick = onReplace, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("החלף", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (onDelete != null) {
                        Button(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("מחק", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            // התמונה ממלאת את כל מה שמתחת לסרגל — הכפתורים לא מסתירים אותה
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ItemImageContent(
                    data = data,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
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
