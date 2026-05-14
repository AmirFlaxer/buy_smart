package com.amir.buysmart.presentation.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.presentation.components.LocationSection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddItem: (String) -> Unit,
    onGoShopping: (String) -> Unit,
    inviteCodeFromLink: String? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(inviteCodeFromLink) {
        if (!inviteCodeFromLink.isNullOrBlank()) {
            viewModel.joinList(inviteCodeFromLink)
        }
    }

    BackHandler {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "לחץ שוב לצאת", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) { kotlinx.coroutines.delay(2000); backPressedOnce = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.activeList?.name ?: "BuySmart") },
                actions = {
                    state.activeList?.let { list ->
                        BadgedBox(
                            badge = { if (state.totalItems > 0) Badge { Text(state.totalItems.toString()) } },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(onClick = { onGoShopping(list.id) }) {
                                Icon(Icons.Default.ShoppingCart, "יוצא לקניות")
                            }
                        }
                        if (list.inviteCode.isNotEmpty()) {
                            IconButton(onClick = {
                                val deepLink = "buysmart://join/${list.inviteCode}"
                                val shareText = "הצטרף לרשימת הקניות שלנו ב-BuySmart!\n" +
                                    "פתח: $deepLink\n" +
                                    "או: פתח BuySmart ← הצטרף לרשימה ← קוד: ${list.inviteCode}"
                                context.startActivity(
                                    Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }, "שתף רשימה")
                                )
                            }) {
                                Icon(Icons.Default.Share, "שתף רשימה")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            state.activeList?.let {
                FloatingActionButton(onClick = { onAddItem(it.id) }) {
                    Icon(Icons.Default.Add, "הוסף פריט")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                // אינדיקציה ליצירת רשימה
                state.isCreatingList -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("יוצר רשימה...", style = MaterialTheme.typography.bodyLarge)
                }

                state.activeList == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                else -> Column(Modifier.fillMaxSize()) {
                    QuickAddBar(
                        name = state.quickAddName,
                        note = state.quickAddNote,
                        location = state.quickAddLocation,
                        suggestions = state.quickAddSuggestions,
                        presetNotes = state.quickAddPresetNotes,
                        onNameChange = viewModel::onQuickAddNameChange,
                        onLocationChange = viewModel::onQuickAddLocationChange,
                        onSuggestionSelected = viewModel::onQuickAddSuggestionSelected,
                        onPresetNoteToggle = viewModel::onQuickAddPresetNoteToggle,
                        onAdd = viewModel::quickAdd
                    )
                    HorizontalDivider()

                    if (state.itemsByLocation.isEmpty()) {
                        EmptyItemsView(
                            inviteCode = state.activeList?.inviteCode ?: "",
                            onShare = { code ->
                                val deepLink = "buysmart://join/$code"
                                val shareText = "הצטרף לרשימת הקניות שלנו ב-BuySmart!\n" +
                                    "פתח: $deepLink\n" +
                                    "או: פתח BuySmart ← הצטרף לרשימה ← קוד: $code"
                                context.startActivity(
                                    Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }, "שתף רשימה")
                                )
                            }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.itemsByLocation.entries.toList()) { (location, items) ->
                                LocationSection(
                                    location = location,
                                    items = items,
                                    onDeleteItem = viewModel::deleteItem,
                                    onEditItem = viewModel::startEditItem
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // דיאלוג כפילות
    state.quickAddDuplicate?.let { existing ->
        AlertDialog(
            onDismissRequest = viewModel::dismissQuickAddDuplicate,
            title = { Text("${existing.name} כבר ברשימה") },
            text = {
                val qtyText = if (existing.quantity.isNotBlank()) " (${existing.quantity})" else ""
                Text("${existing.name}$qtyText כבר קיים ב${existing.location.displayName}.\nמה לעשות?")
            },
            confirmButton = {
                Button(onClick = viewModel::increaseQuantityFromDuplicate) {
                    Text("הגדל כמות")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::dismissQuickAddDuplicate) { Text("ביטול") }
                    OutlinedButton(onClick = viewModel::addDespiteQuickAddDuplicate) { Text("הוסף בכל זאת") }
                }
            }
        )
    }

    // BottomSheet לעריכת פריט
    state.editingItem?.let { item ->
        EditItemBottomSheet(
            item = item,
            presetNotes = state.editPresetNotes,
            onNameChange = viewModel::onEditNameChange,
            onQuantityChange = viewModel::onEditQuantityChange,
            onNoteChange = viewModel::onEditNoteChange,
            onPresetToggle = viewModel::onEditNotePresetToggle,
            onLocationChange = viewModel::onEditLocationChange,
            onTypeChange = viewModel::onEditTypeChange,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::dismissEdit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditItemBottomSheet(
    item: ShoppingItem,
    presetNotes: List<String>,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPresetToggle: (String) -> Unit,
    onLocationChange: (ShoppingLocation) -> Unit,
    onTypeChange: (ItemType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedNotes = item.note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    // הפרד בין הערות שהן presets לבין מלל חופשי
    val freeText = selectedNotes.filter { it !in presetNotes }.joinToString(", ")
    var freeNoteInput by remember(item.id) { mutableStateOf(freeText) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("עריכת פריט", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                label = { Text("שם המוצר") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = item.quantity,
                onValueChange = onQuantityChange,
                label = { Text("כמות") },
                placeholder = { Text("לדוגמה: 2, 500 גרם") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // הערות מובנות
            if (presetNotes.isNotEmpty()) {
                Text("הערות מהירות", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetNotes.forEach { preset ->
                        val isSelected = preset in selectedNotes
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPresetToggle(preset) },
                            label = { Text(preset) }
                        )
                    }
                }
            }

            // מלל חופשי להערה
            OutlinedTextField(
                value = freeNoteInput,
                onValueChange = { input ->
                    freeNoteInput = input
                    // שמור presets שנבחרו + מלל חופשי
                    val selectedPresets = selectedNotes.filter { it in presetNotes }
                    val parts = (selectedPresets + listOf(input.trim()))
                        .filter { it.isNotBlank() }
                    onNoteChange(parts.joinToString(", "))
                },
                label = { Text("הערה חופשית") },
                placeholder = { Text("לדוגמה: מותג ספציפי, הגדלה...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // מקום קנייה
            Text("מקום קנייה", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShoppingLocation.entries.forEach { loc ->
                    FilterChip(
                        selected = item.location == loc,
                        onClick = { onLocationChange(loc) },
                        label = { Text("${loc.emoji} ${loc.displayName}") }
                    )
                }
            }

            // סוג
            Text("סוג", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ItemType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = item.type == type,
                        onClick = { onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemType.entries.size)
                    ) { Text(type.displayName) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = item.name.isNotBlank()
                ) { Text("שמור") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickAddBar(
    name: String,
    note: String,
    location: ShoppingLocation,
    suggestions: List<String>,
    presetNotes: List<String>,
    onNameChange: (String) -> Unit,
    onLocationChange: (ShoppingLocation) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onPresetNoteToggle: (String) -> Unit,
    onAdd: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedNoteParts = note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded && suggestions.isNotEmpty(),
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it); dropdownExpanded = true },
                    placeholder = { Text("הוסף פריט...") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    supportingText = if (note.isNotBlank()) {{ Text(note, maxLines = 1) }} else null
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded && suggestions.isNotEmpty(),
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = { onSuggestionSelected(suggestion); dropdownExpanded = false }
                        )
                    }
                }
            }
            FilledIconButton(
                onClick = { onAdd(); dropdownExpanded = false },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Add, "הוסף")
            }
        }
        Spacer(Modifier.height(6.dp))
        // קטגוריה
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShoppingLocation.entries.forEach { loc ->
                FilterChip(
                    selected = location == loc,
                    onClick = { onLocationChange(loc) },
                    label = { Text("${loc.emoji} ${loc.displayName}") }
                )
            }
        }
        // הערות מהירות (מופיע רק כשיש presets לפריט שהוקלד)
        if (presetNotes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "הערות מהירות",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presetNotes.forEach { preset ->
                    FilterChip(
                        selected = preset in selectedNoteParts,
                        onClick = { onPresetNoteToggle(preset) },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyItemsView(inviteCode: String, onShare: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("הרשימה ריקה", style = MaterialTheme.typography.headlineSmall)
        Text("הוסף פריט בשורה למעלה", style = MaterialTheme.typography.bodyMedium)
        if (inviteCode.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { onShare(inviteCode) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("שתף רשימה עם בני הבית")
            }
        }
    }
}
