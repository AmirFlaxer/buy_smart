package com.amir.buysmart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amir.buysmart.domain.model.ItemHistory
import com.amir.buysmart.domain.model.ShoppingLocation

@Entity(tableName = "item_history")
data class ItemHistoryEntity(
    @PrimaryKey val name: String,
    val location: String,
    val note: String,
    val quantity: String
) {
    fun toDomain() = ItemHistory(
        location = try { ShoppingLocation.valueOf(location) } catch (e: Exception) { ShoppingLocation.SUPERMARKET },
        note = note,
        quantity = quantity
    )
}
