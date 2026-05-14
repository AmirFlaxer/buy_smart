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
    val quantity: String = "",
    val note: String = "",
    val location: String,
    val type: String,
    val isBought: Boolean,
    val addedBy: String,
    val addedByName: String = "",
    val listId: String
) {
    fun toDomain() = ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        note = note,
        location = ShoppingLocation.valueOf(location),
        type = ItemType.valueOf(type),
        isBought = isBought,
        addedBy = addedBy,
        addedByName = addedByName,
        listId = listId
    )

    companion object {
        fun fromDomain(item: ShoppingItem) = ShoppingItemEntity(
            id = item.id,
            name = item.name,
            quantity = item.quantity,
            note = item.note,
            location = item.location.name,
            type = item.type.name,
            isBought = item.isBought,
            addedBy = item.addedBy,
            addedByName = item.addedByName,
            listId = item.listId
        )
    }
}
