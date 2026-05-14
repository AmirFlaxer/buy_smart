package com.amir.buysmart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amir.buysmart.data.local.entity.ItemHistoryEntity
import com.amir.buysmart.data.local.entity.ShoppingItemEntity

@Database(
    entities = [ShoppingItemEntity::class, ItemHistoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun itemHistoryDao(): ItemHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN quantity TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN addedByName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS item_history " +
                    "(name TEXT NOT NULL PRIMARY KEY, location TEXT NOT NULL, " +
                    "note TEXT NOT NULL, quantity TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN pendingRefill INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
