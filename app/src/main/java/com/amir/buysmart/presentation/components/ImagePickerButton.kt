package com.amir.buysmart.presentation.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * כפתור הוספת תמונה לפריט. בלחיצה — בוחר בין גלריה למצלמה.
 * מחזיר Uri של התמונה שנבחרה דרך onImagePicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerButton(
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "הוסף תמונה"
) {
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { onImagePicked(it) }
        }
        pendingCameraUri = null
    }

    OutlinedButton(
        onClick = { showOptions = true },
        modifier = modifier
    ) {
        Icon(Icons.Default.AddAPhoto, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("בחר מקור תמונה") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("גלריה") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable {
                            showOptions = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    ListItem(
                        headlineContent = { Text("מצלמה") },
                        leadingContent = { Icon(Icons.Default.PhotoCamera, null) },
                        modifier = Modifier.clickable {
                            showOptions = false
                            val uri = createCameraUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOptions = false }) { Text("ביטול") }
            }
        )
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
