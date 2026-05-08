package com.amir.buysmart.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.presentation.components.LocationSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddItem: (String) -> Unit,
    onGoShopping: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inviteInput by remember { mutableStateOf("") }
    var createInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.activeList?.name ?: "BuySmart") },
                actions = {
                    state.activeList?.let { list ->
                        IconButton(onClick = { /* share invite code */ }) {
                            Icon(Icons.Default.Share, contentDescription = "שתף קוד")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.activeList?.let {
                    SmallFloatingActionButton(onClick = { onGoShopping(it.id) }) {
                        Icon(Icons.Default.ShoppingCart, "יוצא לקניות")
                    }
                }
                FloatingActionButton(
                    onClick = { state.activeList?.let { onAddItem(it.id) } ?: viewModel.showCreateDialog() }
                ) {
                    Icon(Icons.Default.Add, "הוסף פריט")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.activeList == null -> EmptyListsView(
                    onCreateList = viewModel::showCreateDialog,
                    onJoinList = viewModel::showJoinDialog
                )

                state.itemsByLocation.isEmpty() -> EmptyItemsView(
                    inviteCode = state.activeList?.inviteCode ?: "",
                    onAddItem = { state.activeList?.let { onAddItem(it.id) } }
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.itemsByLocation.entries.toList()) { (location, items) ->
                        LocationSection(location = location, items = items)
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideCreateDialog,
            title = { Text("רשימה חדשה") },
            text = {
                OutlinedTextField(
                    value = createInput,
                    onValueChange = { createInput = it },
                    label = { Text("שם הרשימה") }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createList(createInput); createInput = "" }) {
                    Text("צור")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideCreateDialog) { Text("ביטול") }
            }
        )
    }

    if (state.showJoinDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideJoinDialog,
            title = { Text("הצטרף לרשימה") },
            text = {
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it },
                    label = { Text("קוד הזמנה (6 ספרות)") }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.joinList(inviteInput); inviteInput = "" }) {
                    Text("הצטרף")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideJoinDialog) { Text("ביטול") }
            }
        )
    }
}

@Composable
private fun EmptyListsView(onCreateList: () -> Unit, onJoinList: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("אין לך רשימות עדיין", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateList, Modifier.fillMaxWidth()) { Text("צור רשימה חדשה") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onJoinList, Modifier.fillMaxWidth()) { Text("הצטרף לרשימה קיימת") }
    }
}

@Composable
private fun EmptyItemsView(inviteCode: String, onAddItem: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("הרשימה ריקה", style = MaterialTheme.typography.headlineSmall)
        if (inviteCode.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("קוד הזמנה: $inviteCode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddItem) { Text("הוסף פריט ראשון") }
    }
}
