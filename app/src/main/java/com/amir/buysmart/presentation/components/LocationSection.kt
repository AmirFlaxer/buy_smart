package com.amir.buysmart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.presentation.theme.priorityTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSection(
    key: LocationKey,
    items: List<ShoppingItem>,
    onDeleteItem: (ShoppingItem) -> Unit,
    onEditItem: (ShoppingItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${key.emoji} ${key.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${items.size} פריטים",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                SwipeableItemRow(
                    item = item,
                    onDelete = { onDeleteItem(item) },
                    onEdit = { onEditItem(item) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableItemRow(
    item: ShoppingItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val align = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterStart
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F), RoundedCornerShape(6.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = align
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        ItemRow(item = item, onDelete = onDelete, onEdit = onEdit)
    }
}

@Composable
fun ItemRow(
    item: ShoppingItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = priorityTint(item.priority) ?: MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.imageUrl.isNotBlank()) {
            ItemImage(
                data = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .padding(end = 6.dp),
                contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "• ${item.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.quantity.isNotBlank()) {
                    Text(
                        text = " × ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
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
        TextButton(
            onClick = onEdit,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("שינוי", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "מחק",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
