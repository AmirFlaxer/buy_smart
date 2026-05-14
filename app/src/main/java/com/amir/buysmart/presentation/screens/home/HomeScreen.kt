package com.amir.buysmart.presentation.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.presentation.components.LocationSection
import com.amir.buysmart.presentation.components.VoiceInputButton

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
                title = { AppBarTitle() },
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
                        priority = state.quickAddPriority,
                        suggestions = state.quickAddSuggestions,
                        presetNotes = state.quickAddPresetNotes,
                        onNameChange = viewModel::onQuickAddNameChange,
                        onLocationChange = viewModel::onQuickAddLocationChange,
                        onPriorityChange = viewModel::onQuickAddPriorityChange,
                        onSuggestionSelected = viewModel::onQuickAddSuggestionSelected,
                        onPresetNoteToggle = viewModel::onQuickAddPresetNoteToggle,
                        onAdd = viewModel::quickAdd
                    )
                    HorizontalDivider()

                    if (state.pendingRefillItems.isEmpty() && state.itemsByLocation.isEmpty()) {
                        EmptyItemsView()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // סקשן "לחידוש" — פריטים שנקנו ומחכים לאישור
                            if (state.pendingRefillItems.isNotEmpty()) {
                                item {
                                    PendingRefillSection(
                                        items = state.pendingRefillItems,
                                        onApprove = viewModel::approvePendingRefill,
                                        onDelete = viewModel::deleteItem
                                    )
                                }
                            }

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
            onPriorityChange = viewModel::onEditPriorityChange,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::dismissEdit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PendingRefillSection(
    items: List<ShoppingItem>,
    onApprove: (ShoppingItem) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "🔄 לחידוש — נקנו בקנייה הקודמת",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${items.size} פריטים",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.name + if (item.quantity.isNotBlank()) " × ${item.quantity}" else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (item.note.isNotBlank()) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onApprove(item) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("הוסף שוב", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "מחק",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
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
    onPriorityChange: (ItemPriority) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedNotes = item.note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toSet()
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

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    label = { Text("שם המוצר") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                VoiceInputButton(
                    onResult = onNameChange,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

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
                        FilterChip(
                            selected = preset in selectedNotes,
                            onClick = { onPresetToggle(preset) },
                            label = { Text(preset) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = freeNoteInput,
                onValueChange = { input ->
                    freeNoteInput = input
                    val selectedPresets = selectedNotes.filter { it in presetNotes }
                    val parts = (selectedPresets + listOf(input.trim())).filter { it.isNotBlank() }
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

            // דחיפות
            Text("דחיפות", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ItemPriority.entries.forEachIndexed { index, priority ->
                    SegmentedButton(
                        selected = item.priority == priority,
                        onClick = { onPriorityChange(priority) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemPriority.entries.size)
                    ) { Text("${priority.emoji} ${priority.displayName}") }
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
    priority: ItemPriority,
    suggestions: List<String>,
    presetNotes: List<String>,
    onNameChange: (String) -> Unit,
    onLocationChange: (ShoppingLocation) -> Unit,
    onPriorityChange: (ItemPriority) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onPresetNoteToggle: (String) -> Unit,
    onAdd: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedNoteParts = note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            VoiceInputButton(
                onResult = { onNameChange(it) },
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = { onAdd(); dropdownExpanded = false },
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("הוסף")
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
        // דחיפות
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ItemPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { onPriorityChange(p) },
                    label = { Text("${p.emoji} ${p.displayName}", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        // הערות מהירות
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
private fun AppBarTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // לוגו — ריבוע מעוגל עם gradient + עגלה + ניצוץ
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF26A69A), Color(0xFF004D40)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            // ניצוץ זהב בפינה
            Text(
                text = "✦",
                color = Color(0xFFFFD740),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-1).dp)
            )
        }
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = "רשימת קניות",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
            Text(
                text = "AF Apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyItemsView() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("הרשימה ריקה", style = MaterialTheme.typography.headlineSmall)
        Text("הוסף פריט בשורה למעלה", style = MaterialTheme.typography.bodyMedium)
    }
}
