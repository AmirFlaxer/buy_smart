package com.amir.buysmart.data.local

import androidx.room.*
import com.amir.buysmart.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY location, name")
    fun getItemsForList(listId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND location = :location ORDER BY isBought, name")
    fun getItemsByLocation(listId: String, location: String): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isBought = :isBought WHERE id = :id")
    suspend fun updateBought(id: String, isBought: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND location = :location AND isBought = 1")
    suspend fun getBoughtItemsByLocation(listId: String, location: String): List<ShoppingItemEntity>

    @Query("DELETE FROM shopping_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<String>)

    @Query("UPDATE shopping_items SET isBought = 0 WHERE id IN (:ids)")
    suspend fun resetBoughtForIds(ids: List<String>)
}
