package com.amir.buysmart.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingLocation

/**
 * שורת צ'יפים אחידה לקטגוריות: מובנות + מותאמות + כפתור "+" + מחיקה לצ'יפ מותאם.
 *
 * @param selected הקטגוריה הנבחרת כרגע (null אם אין)
 * @param customLocations רשימת שמות קטגוריות מותאמות אישית
 * @param onSelect נבחרה קטגוריה
 * @param onAddCustom המשתמש לחץ "+" — הצג דיאלוג להוספה
 * @param onDeleteCustom המשתמש לחץ "×" על צ'יפ מותאם
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationChipRow(
    selected: LocationKey?,
    customLocations: List<String>,
    onSelect: (LocationKey) -> Unit,
    onAddCustom: () -> Unit,
    onDeleteCustom: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = true,
    showDeleteOnCustom: Boolean = true
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // מובנים
        ShoppingLocation.entries.forEach { loc ->
            val isSelected = (selected as? LocationKey.BuiltIn)?.location == loc
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(LocationKey.BuiltIn(loc)) },
                label = { Text("${loc.emoji} ${loc.displayName}") }
            )
        }
        // מותאמים אישית — עם או בלי כפתור מחיקה
        customLocations.forEach { name ->
            val isSelected = (selected as? LocationKey.Custom)?.name == name
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(LocationKey.Custom(name)) },
                label = { Text("🏬 $name") },
                trailingIcon = if (showDeleteOnCustom) {
                    {
                        IconButton(
                            onClick = { onDeleteCustom(name) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "מחק קטגוריה",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null
            )
        }
        // הוסף קטגוריה חדשה
        if (showAddButton) {
            AssistChip(
                onClick = onAddCustom,
                label = { Text("הוסף") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}

/**
 * דיאלוג להוספת קטגוריה מותאמת אישית.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוסף קטגוריה") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("שם הקטגוריה") },
                placeholder = { Text("לדוגמה: מקס סטוק, פארם, איקאה") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()); },
                enabled = name.trim().isNotBlank()
            ) { Text("הוסף") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
