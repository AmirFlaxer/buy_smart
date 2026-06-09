package com.amir.buysmart.presentation.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.amir.buysmart.presentation.components.ImagePickerButton
import com.amir.buysmart.presentation.components.ItemImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.JoinRequest
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.presentation.components.AddCustomLocationDialog
import com.amir.buysmart.presentation.components.LocationChipRow
import com.amir.buysmart.presentation.components.LocationSection
import com.amir.buysmart.presentation.components.VoiceInputButton
import com.amir.buysmart.presentation.theme.BrandLogoEnd
import com.amir.buysmart.presentation.theme.BrandLogoStart
import com.amir.buysmart.presentation.theme.BrandSpark

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
    val clipboard = LocalClipboardManager.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var overflowExpanded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showAddCustomLocationDialog by remember { mutableStateOf(false) }
    var showAddCustomFromEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.recentlyDeleted) {
        val deleted = state.recentlyDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "${deleted.name} נמחק",
            actionLabel = "בטל",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.clearRecentlyDeleted()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        viewModel.clearError()
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Default.MoreVert, "עוד")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            if (list.inviteCode.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("קוד רשימה: ${list.inviteCode}") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                    onClick = {
                                        clipboard.setText(AnnotatedString(list.inviteCode))
                                        Toast.makeText(context, "הקוד הועתק", Toast.LENGTH_SHORT).show()
                                        overflowExpanded = false
                                    }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text("הצטרף לרשימה") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                                onClick = {
                                    overflowExpanded = false
                                    showJoinDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("עזוב רשימה") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                                onClick = {
                                    overflowExpanded = false
                                    showLeaveDialog = true
                                }
                            )
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
        Box(
            Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
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

                else -> Column(Modifier.fillMaxSize().widthIn(max = 720.dp)) {
                    QuickAddBar(
                        name = state.quickAddName,
                        note = state.quickAddNote,
                        selectedKey = state.quickAddSelectedKey,
                        customLocations = state.customLocations,
                        priority = state.quickAddPriority,
                        suggestions = state.quickAddSuggestions,
                        presetNotes = state.quickAddPresetNotes,
                        onNameChange = viewModel::onQuickAddNameChange,
                        onLocationKeyChange = viewModel::onQuickAddLocationKeyChange,
                        onPriorityChange = viewModel::onQuickAddPriorityChange,
                        onSuggestionSelected = viewModel::onQuickAddSuggestionSelected,
                        onPresetNoteToggle = viewModel::onQuickAddPresetNoteToggle,
                        onAdd = viewModel::quickAdd,
                        onAddCustomLocation = { showAddCustomLocationDialog = true },
                        onDeleteCustomLocation = viewModel::removeCustomLocation
                    )
                    HorizontalDivider()

                    state.pendingJoin?.let { pending ->
                        PendingJoinBanner(
                            listName = pending.listName,
                            onCancel = viewModel::cancelJoinRequest
                        )
                    }

                    if (state.joinRequests.isNotEmpty()) {
                        JoinRequestsSection(
                            requests = state.joinRequests,
                            onApprove = viewModel::approveRequest,
                            onReject = viewModel::rejectRequest
                        )
                    }

                    if (state.pendingRefillItems.isEmpty() && state.itemsByCategory.isEmpty()) {
                        EmptyItemsView()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // מוצרים לקנייה — מקובצים לפי קטגוריה
                            items(state.itemsByCategory.entries.toList()) { (key, items) ->
                                LocationSection(
                                    key = key,
                                    items = items,
                                    onDeleteItem = viewModel::deleteItemWithUndo,
                                    onEditItem = viewModel::startEditItem
                                )
                            }

                            // סקשן "לחידוש" — פריטים שנקנו ומחכים לאישור (בסוף, אחרי הרשימה לקנייה)
                            if (state.pendingRefillItems.isNotEmpty()) {
                                item {
                                    PendingRefillSection(
                                        items = state.pendingRefillItems,
                                        onApprove = viewModel::approvePendingRefill,
                                        onDelete = viewModel::deleteItem
                                    )
                                }
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

    // דיאלוג עזיבת רשימה
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("עזוב רשימה") },
            text = { Text("האם לעזוב את הרשימה?\nהפריטים יישארו לחברים האחרים, אך לא יהיו זמינים לך יותר.") },
            confirmButton = {
                Button(onClick = {
                    showLeaveDialog = false
                    viewModel.leaveList()
                }) { Text("עזוב") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("ביטול") }
            }
        )
    }

    // דיאלוג הצטרפות לרשימה בעזרת קוד
    if (showJoinDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("הצטרף לרשימה") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("הזן את קוד ההזמנה שקיבלת מחבר ברשימה.")
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.filter { c -> !c.isWhitespace() } },
                        label = { Text("קוד הזמנה") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showJoinDialog = false
                        viewModel.joinList(codeInput)
                    },
                    enabled = codeInput.isNotBlank()
                ) { Text("הצטרף") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("ביטול") }
            }
        )
    }

    // BottomSheet לעריכת פריט
    state.editingItem?.let { item ->
        EditItemBottomSheet(
            item = item,
            customLocations = state.customLocations,
            presetNotes = state.editPresetNotes,
            pendingImageUri = state.editPendingImageUri,
            isUploadingImage = state.editIsUploadingImage,
            onNameChange = viewModel::onEditNameChange,
            onQuantityChange = viewModel::onEditQuantityChange,
            onNoteChange = viewModel::onEditNoteChange,
            onPresetToggle = viewModel::onEditNotePresetToggle,
            onLocationKeyChange = viewModel::onEditLocationKeyChange,
            onTypeChange = viewModel::onEditTypeChange,
            onPriorityChange = viewModel::onEditPriorityChange,
            onImagePicked = { uri -> viewModel.onEditImagePicked(context, uri) },
            onImageRemoved = viewModel::onEditImageRemoved,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::dismissEdit,
            onAddCustomLocation = { showAddCustomFromEditDialog = true },
            onDeleteCustomLocation = viewModel::removeCustomLocation
        )
    }

    // דיאלוג הוספת קטגוריה מותאמת (מ-QuickAdd)
    if (showAddCustomLocationDialog) {
        AddCustomLocationDialog(
            onDismiss = { showAddCustomLocationDialog = false },
            onConfirm = { name ->
                viewModel.addCustomLocation(name)
                viewModel.onQuickAddLocationKeyChange(LocationKey.Custom(name))
                showAddCustomLocationDialog = false
            }
        )
    }

    // דיאלוג הוספת קטגוריה מותאמת (מ-Edit Bottom Sheet)
    if (showAddCustomFromEditDialog) {
        AddCustomLocationDialog(
            onDismiss = { showAddCustomFromEditDialog = false },
            onConfirm = { name ->
                viewModel.addCustomLocation(name)
                viewModel.onEditLocationKeyChange(LocationKey.Custom(name))
                showAddCustomFromEditDialog = false
            }
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
                        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(48.dp)) {
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
    customLocations: List<String>,
    presetNotes: List<String>,
    pendingImageUri: Uri?,
    isUploadingImage: Boolean,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPresetToggle: (String) -> Unit,
    onLocationKeyChange: (LocationKey) -> Unit,
    onTypeChange: (ItemType) -> Unit,
    onPriorityChange: (ItemPriority) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onImageRemoved: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onAddCustomLocation: () -> Unit,
    onDeleteCustomLocation: (String) -> Unit
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
            val currentKey = if (item.customLocation.isNotBlank())
                LocationKey.Custom(item.customLocation)
                else LocationKey.BuiltIn(item.location)
            LocationChipRow(
                selected = currentKey,
                customLocations = customLocations,
                onSelect = onLocationKeyChange,
                onAddCustom = onAddCustomLocation,
                onDeleteCustom = onDeleteCustomLocation
            )

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

            // תמונה
            Text("תמונה (אופציונלי)", style = MaterialTheme.typography.titleSmall)
            val hasImage = pendingImageUri != null || item.imageUrl.isNotBlank()
            if (hasImage) {
                Box(Modifier.fillMaxWidth()) {
                    val imageModifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                    if (pendingImageUri != null) {
                        // תצוגה מקדימה של תמונה שזה עתה נבחרה (Uri מקומי)
                        AsyncImage(
                            model = pendingImageUri,
                            contentDescription = "תמונת המוצר",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ItemImage(
                            data = item.imageUrl,
                            contentDescription = "תמונת המוצר",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isUploadingImage) {
                        Box(
                            Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    IconButton(
                        onClick = onImageRemoved,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close, "הסר תמונה", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                ImagePickerButton(
                    onImagePicked = onImagePicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = item.name.isNotBlank() && !isUploadingImage
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
    selectedKey: LocationKey,
    customLocations: List<String>,
    priority: ItemPriority,
    suggestions: List<String>,
    presetNotes: List<String>,
    onNameChange: (String) -> Unit,
    onLocationKeyChange: (LocationKey) -> Unit,
    onPriorityChange: (ItemPriority) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onPresetNoteToggle: (String) -> Unit,
    onAdd: () -> Unit,
    onAddCustomLocation: () -> Unit,
    onDeleteCustomLocation: (String) -> Unit
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
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
        // קטגוריה — מובנות + מותאמות + "+"
        LocationChipRow(
            selected = selectedKey,
            customLocations = customLocations,
            onSelect = onLocationKeyChange,
            onAddCustom = onAddCustomLocation,
            onDeleteCustom = onDeleteCustomLocation
        )
        // דחיפות + הערות מהירות - מוצגות רק כשמתחילים להקליד, לצמצום עומס ויזואלי
        if (name.isNotBlank()) {
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
                        colors = listOf(BrandLogoStart, BrandLogoEnd),
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
                color = BrandSpark,
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
private fun PendingJoinBanner(listName: String, onCancel: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                text = "ממתין לאישור הצטרפות ל\"$listName\"",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text("בטל") }
        }
    }
}

@Composable
private fun JoinRequestsSection(
    requests: List<JoinRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "בקשות הצטרפות (${requests.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            requests.forEach { req ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = req.name.ifBlank { "משתמש" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onApprove(req.uid) }) {
                        Icon(Icons.Default.Check, "אשר", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onReject(req.uid) }) {
                        Icon(Icons.Default.Close, "דחה", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "הרשימה ריקה",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "הוסף את הפריט הראשון בשורה שלמעלה ↑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
