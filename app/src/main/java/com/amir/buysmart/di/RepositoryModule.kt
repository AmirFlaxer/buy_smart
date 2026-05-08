package com.amir.buysmart.di

import com.amir.buysmart.data.repository.ItemRepositoryImpl
import com.amir.buysmart.data.repository.ListRepositoryImpl
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.repository.ListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository

    @Binds @Singleton
    abstract fun bindListRepository(impl: ListRepositoryImpl): ListRepository
}
