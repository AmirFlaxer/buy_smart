package com.amir.buysmart.presentation.screens.additem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    listId: String,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הוסף פריט") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "חזור")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("שם המוצר") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("מקום קנייה", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(ShoppingLocation.entries) { location ->
                    FilterChip(
                        selected = state.location == location,
                        onClick = { viewModel.onLocationChange(location) },
                        label = { Text("${location.emoji} ${location.displayName}") }
                    )
                }
            }

            Text("סוג", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.save(listId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.name.isNotBlank() && !state.isSaving
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("הוסף לרשימה", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
