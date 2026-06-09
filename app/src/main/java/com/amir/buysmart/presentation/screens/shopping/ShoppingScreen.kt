package com.amir.buysmart.presentation.screens.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.presentation.components.ItemImage
import com.amir.buysmart.presentation.components.LocationChipRow
import com.amir.buysmart.presentation.theme.priorityTint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingScreen(
    listId: String,
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listId) { viewModel.init(listId) }
    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛒 יוצא לקניות") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                val boughtCount = state.items.count { it.isBought }
                Button(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    enabled = boughtCount > 0
                ) {
                    Text(
                        if (boughtCount > 0) "סיימתי ב${state.selectedKey.displayName} ($boughtCount פריטים)"
                        else "סמן פריטים שנקנו",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            LocationChipRow(
                selected = state.selectedKey,
                customLocations = state.customLocations,
                onSelect = viewModel::selectLocationKey,
                onAddCustom = {},
                onDeleteCustom = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                showAddButton = false,
                showDeleteOnCustom = false
            )

            HorizontalDivider()

            // Progress bar
            if (state.items.isNotEmpty()) {
                val boughtCount = state.items.count { it.isBought }
                val progress = boughtCount.toFloat() / state.items.size
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            state.selectedKey.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$boughtCount / ${state.items.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider()
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("אין פריטים ב${state.selectedKey.displayName}")
                        Text("בחר מקום אחר", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ShoppingItemCard(
                            item = item,
                            onToggle = { viewModel.toggleBought(item) }
                        )
                    }
                }
            }
        }
        }
    }

    if (showFinishDialog) {
        val boughtCount = state.items.count { it.isBought }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("סיום קניות ב${state.selectedKey.displayName}") },
            text = {
                Text("נקנו $boughtCount פריטים — הם יאופסו לרשימה לקנייה הבאה.\nלסיים?")
            },
            confirmButton = {
                Button(onClick = { showFinishDialog = false; viewModel.finishShopping() }) {
                    Text("כן, סיימתי")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("המשך קניות") }
            }
        )
    }

    // אחרי סיום קנייה בקטגוריה מותאמת — להציע מחיקה
    state.justFinishedCustom?.let { name ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteFinishedCustom,
            title = { Text("למחוק את \"$name\"?") },
            text = {
                Text("סיימת לקנות ב-$name.\nאם זו הייתה קנייה חד-פעמית — אפשר למחוק את הקטגוריה.")
            },
            confirmButton = {
                Button(onClick = viewModel::confirmDeleteFinishedCustom) { Text("מחק קטגוריה") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteFinishedCustom) { Text("השאר") }
            }
        )
    }
}

@Composable
private fun ShoppingItemCard(item: ShoppingItem, onToggle: () -> Unit) {
    val containerColor = when {
        item.isBought -> MaterialTheme.colorScheme.surfaceVariant
        else -> priorityTint(item.priority) ?: MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isBought,
                onCheckedChange = { onToggle() }
            )
            if (item.imageUrl.isNotBlank()) {
                ItemImage(
                    data = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(end = 8.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.isBought) TextDecoration.LineThrough else null,
                        color = if (item.isBought)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.quantity.isNotBlank()) {
                        Text(
                            text = " × ${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.isBought)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.addedByName.isNotBlank()) {
                    Text(
                        text = item.addedByName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
