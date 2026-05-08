package com.amir.buysmart.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation

@Composable
fun LocationSection(
    location: ShoppingLocation,
    items: List<ShoppingItem>,
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
                    "${location.emoji} ${location.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${items.count { !it.isBought }} פריטים",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                ItemRow(item = item, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun ItemRow(item: ShoppingItem, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = if (item.isBought) "✓ ${item.name}" else "• ${item.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isBought)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.onSurface
        )
        if (item.type.name == "RECURRING") {
            Spacer(Modifier.width(6.dp))
            Text("🔄", style = MaterialTheme.typography.bodySmall)
        }
    }
}
