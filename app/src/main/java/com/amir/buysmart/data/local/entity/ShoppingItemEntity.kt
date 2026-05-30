package com.amir.buysmart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val quantity: String = "",
    val note: String = "",
    val location: String,
    val customLocation: String = "",
    val type: String,
    val isBought: Boolean,
    val addedBy: String,
    val addedByName: String = "",
    val listId: String,
    val priority: String = "NORMAL",
    val pendingRefill: Boolean = false,
    val imageUrl: String = ""
) {
    fun toDomain() = ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        note = note,
        location = try { ShoppingLocation.valueOf(location) } catch (e: Exception) { ShoppingLocation.OTHER },
        customLocation = customLocation,
        type = ItemType.valueOf(type),
        isBought = isBought,
        addedBy = addedBy,
        addedByName = addedByName,
        listId = listId,
        priority = try { ItemPriority.valueOf(priority) } catch (e: Exception) { ItemPriority.NORMAL },
        pendingRefill = pendingRefill,
        imageUrl = imageUrl
    )

    companion object {
        fun fromDomain(item: ShoppingItem) = ShoppingItemEntity(
            id = item.id,
            name = item.name,
            quantity = item.quantity,
            note = item.note,
            location = item.location.name,
            customLocation = item.customLocation,
            type = item.type.name,
            isBought = item.isBought,
            addedBy = item.addedBy,
            addedByName = item.addedByName,
            listId = item.listId,
            priority = item.priority.name,
            pendingRefill = item.pendingRefill,
            imageUrl = item.imageUrl
        )
    }
}
