package com.amir.buysmart.presentation.screens.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ShoppingLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    listId: String,
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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
                Button(
                    onClick = viewModel::finishShopping,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
                ) {
                    Text("סיימתי קניות ב-${state.selectedLocation.displayName}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShoppingLocation.entries) { location ->
                    FilterChip(
                        selected = state.selectedLocation == location,
                        onClick = { viewModel.selectLocation(location) },
                        label = { Text("${location.emoji} ${location.displayName}") }
                    )
                }
            }

            HorizontalDivider()

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין פריטים ב${state.selectedLocation.displayName}")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isBought)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isBought,
                                    onCheckedChange = { viewModel.toggleBought(item) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (item.isBought) TextDecoration.LineThrough else null,
                                    color = if (item.isBought)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (item.type.name == "RECURRING") {
                                    Spacer(Modifier.weight(1f))
                                    Text("🔄", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
