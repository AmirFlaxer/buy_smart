package com.amir.buysmart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val type: String,
    val isBought: Boolean,
    val addedBy: String,
    val listId: String
) {
    fun toDomain() = ShoppingItem(
        id = id,
        name = name,
        location = ShoppingLocation.valueOf(location),
        type = ItemType.valueOf(type),
        isBought = isBought,
        addedBy = addedBy,
        listId = listId
    )

    companion object {
        fun fromDomain(item: ShoppingItem) = ShoppingItemEntity(
            id = item.id,
            name = item.name,
            location = item.location.name,
            type = item.type.name,
            isBought = item.isBought,
            addedBy = item.addedBy,
            listId = item.listId
        )
    }
}
