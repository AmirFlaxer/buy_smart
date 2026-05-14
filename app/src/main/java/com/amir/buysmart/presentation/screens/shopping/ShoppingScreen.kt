package com.amir.buysmart.presentation.screens.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation

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
                        Icon(Icons.Default.ArrowBack, "חזור")
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
                        if (boughtCount > 0) "סיימתי ב${state.selectedLocation.displayName} ($boughtCount פריטים)"
                        else "סמן פריטים שנקנו",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShoppingLocation.entries.forEach { location ->
                    FilterChip(
                        selected = state.selectedLocation == location,
                        onClick = { viewModel.selectLocation(location) },
                        label = { Text("${location.emoji} ${location.displayName}") }
                    )
                }
            }

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
                            "${state.selectedLocation.displayName}",
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
                        Text("אין פריטים ב${state.selectedLocation.displayName}")
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

    if (showFinishDialog) {
        val boughtCount = state.items.count { it.isBought }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("סיום קניות ב${state.selectedLocation.displayName}") },
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
}

@Composable
private fun ShoppingItemCard(item: ShoppingItem, onToggle: () -> Unit) {
    val containerColor = when {
        item.isBought -> MaterialTheme.colorScheme.surfaceVariant
        item.priority == ItemPriority.URGENT -> Color(0xFFFFCDD2)
        item.priority == ItemPriority.NOT_URGENT -> Color(0xFFFFF9C4)
        else -> MaterialTheme.colorScheme.surface
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
            Spacer(Modifier.width(8.dp))
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
