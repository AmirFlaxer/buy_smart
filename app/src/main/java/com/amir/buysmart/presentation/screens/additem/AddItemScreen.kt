package com.amir.buysmart.presentation.screens.additem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.presentation.components.AddCustomLocationDialog
import com.amir.buysmart.presentation.components.ImagePickerButton
import com.amir.buysmart.presentation.components.ItemImage
import com.amir.buysmart.presentation.components.LocationChipRow
import com.amir.buysmart.presentation.components.VoiceInputButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
    listId: String,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listId) { viewModel.setListId(listId) }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    if (showAddCustomDialog) {
        AddCustomLocationDialog(
            onDismiss = { showAddCustomDialog = false },
            onConfirm = { name ->
                viewModel.addCustomLocation(name)
                viewModel.onLocationKeyChange(LocationKey.Custom(name))
                showAddCustomDialog = false
            }
        )
    }

    // דיאלוג כפילות
    state.duplicateItem?.let { existing ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateDialog,
            title = { Text("${existing.name} כבר ברשימה") },
            text = {
                val qtyText = if (existing.quantity.isNotBlank()) " (${existing.quantity})" else ""
                Text("${existing.name}$qtyText כבר קיים ב${existing.location.displayName}.\nמה לעשות?")
            },
            confirmButton = {
                Button(onClick = viewModel::increaseQuantityOfDuplicate) { Text("הגדל כמות") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::dismissDuplicateDialog) { Text("ביטול") }
                    OutlinedButton(onClick = { viewModel.saveAsDuplicate(listId) }) { Text("הוסף בכל זאת") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הוסף פריט") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור") }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // שם פריט עם autocomplete + מיקרופון
            var dropdownExpanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded && state.suggestions.isNotEmpty(),
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onNameChange(it); dropdownExpanded = true },
                        label = { Text("שם המוצר") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded && state.suggestions.isNotEmpty(),
                        onDismissRequest = { viewModel.clearSuggestions(); dropdownExpanded = false }
                    ) {
                        state.suggestions.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { viewModel.onSuggestionSelected(s); dropdownExpanded = false }
                            )
                        }
                    }
                }
                VoiceInputButton(
                    onResult = { viewModel.onNameChange(it) },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // כמות
            OutlinedTextField(
                value = state.quantity,
                onValueChange = viewModel::onQuantityChange,
                label = { Text("כמות (אופציונלי)") },
                placeholder = { Text("לדוגמה: 2, 500 גרם, ליטר") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // הערות מובנות (לפי שם הפריט)
            if (state.presetNotes.isNotEmpty()) {
                Text("הערות מהירות", style = MaterialTheme.typography.titleSmall)
                val selectedParts = state.note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.presetNotes.forEach { preset ->
                        FilterChip(
                            selected = preset in selectedParts,
                            onClick = { viewModel.onPresetNoteToggle(preset) },
                            label = { Text(preset) }
                        )
                    }
                }
            }

            // הערה חופשית
            val presets = state.presetNotes.toSet()
            val selectedParts = state.note.split(", ").map { it.trim() }.filter { it.isNotBlank() }
            val freeText = selectedParts.filter { it !in presets }.joinToString(", ")
            var freeNoteInput by remember { mutableStateOf(freeText) }
            OutlinedTextField(
                value = freeNoteInput,
                onValueChange = { input ->
                    freeNoteInput = input
                    val selectedPresets = selectedParts.filter { it in presets }
                    val parts = (selectedPresets + listOf(input.trim())).filter { it.isNotBlank() }
                    viewModel.onNoteChange(parts.joinToString(", "))
                },
                label = { Text("הערה חופשית (אופציונלי)") },
                placeholder = { Text("לדוגמה: של יטבתה, ב-500 גרם") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // מקום קנייה
            Text("מקום קנייה", style = MaterialTheme.typography.titleSmall)
            LocationChipRow(
                selected = state.selectedKey,
                customLocations = state.customLocations,
                onSelect = viewModel::onLocationKeyChange,
                onAddCustom = { showAddCustomDialog = true },
                onDeleteCustom = viewModel::removeCustomLocation
            )

            // דחיפות
            Text("דחיפות", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ItemPriority.entries.forEachIndexed { index, priority ->
                    SegmentedButton(
                        selected = state.priority == priority,
                        onClick = { viewModel.onPriorityChange(priority) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemPriority.entries.size)
                    ) { Text("${priority.emoji} ${priority.displayName}") }
                }
            }

            // סוג פריט
            Text("סוג", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ItemType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemType.entries.size)
                    ) { Text(type.displayName) }
                }
            }

            // תמונה
            Text("תמונת המוצר (אופציונלי)", style = MaterialTheme.typography.titleSmall)
            val hasImage = state.pendingImageUri != null || state.imageUrl.isNotBlank()
            if (hasImage) {
                Box(Modifier.fillMaxWidth()) {
                    val imageModifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                    if (state.pendingImageUri != null) {
                        AsyncImage(
                            model = state.pendingImageUri,
                            contentDescription = "תמונת המוצר",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ItemImage(
                            data = state.imageUrl,
                            contentDescription = "תמונת המוצר",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (state.isUploadingImage) {
                        Box(
                            Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    IconButton(
                        onClick = viewModel::removeImage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "הסר תמונה",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                ImagePickerButton(
                    onImagePicked = { uri -> viewModel.onImagePicked(context, uri) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(listId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.name.isNotBlank() && !state.isSaving && !state.isUploadingImage
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("הוסף לרשימה", style = MaterialTheme.typography.titleMedium)
            }
        }
        }
    }
}
