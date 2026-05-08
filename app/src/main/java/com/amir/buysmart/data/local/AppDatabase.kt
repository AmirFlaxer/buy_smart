package com.amir.buysmart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amir.buysmart.data.local.entity.ShoppingItemEntity

@Database(entities = [ShoppingItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
