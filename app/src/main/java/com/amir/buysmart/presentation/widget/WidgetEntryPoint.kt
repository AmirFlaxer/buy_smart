package com.amir.buysmart.presentation.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.amir.buysmart.data.local.ItemDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun itemDao(): ItemDao
    fun dataStore(): DataStore<Preferences>
}
