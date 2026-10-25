package com.amir.buysmart.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.util.RefillOrder

/** אזור "לחידוש" במסך הראשי: עד 8 תגיות לפי תדירות קנייה, וקישור למסך המלא. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RefillTags(
    items: List<ShoppingItem>,
    onApprove: (ShoppingItem) -> Unit,
    onShowAll: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "🔄 לחידוש · ${items.size}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RefillOrder.topTags(items).forEach { item ->
                AssistChip(
                    onClick = { onApprove(item) },
                    label = { Text(item.name, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        TextButton(onClick = onShowAll) { Text("הצג הכל (${items.size}) ›") }
    }
}

/** "לחידוש" המלא במסך מלא (Dialog), עם התוכן הקיים של [PendingRefillSection]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillFullScreen(
    count: Int,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("🔄 לחידוש ($count)") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) { content() }
        }
    }
}
