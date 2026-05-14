package com.amir.buysmart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amir.buysmart.data.local.entity.ItemHistoryEntity

@Dao
interface ItemHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ItemHistoryEntity)

    @Query("SELECT * FROM item_history WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ItemHistoryEntity?
}
