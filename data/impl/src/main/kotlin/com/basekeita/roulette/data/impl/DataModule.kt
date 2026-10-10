package com.basekeita.roulette.data.impl

import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.data.impl.repository.ItemsRepositoryImpl
import com.basekeita.roulette.data.impl.repository.SavedListsRepositoryImpl
import com.basekeita.roulette.data.impl.repository.UserSettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    internal abstract fun bindsItemsRepository(itemsRepository: ItemsRepositoryImpl): ItemsRepository

    @Binds
    internal abstract fun bindsSavedListsRepository(savedListsRepository: SavedListsRepositoryImpl): SavedListsRepository

    @Binds
    internal abstract fun bindsUserSettingsRepository(userSettingsRepository: UserSettingsRepositoryImpl): UserSettingsRepository

    @Binds
    internal abstract fun bindsDefaultItemLabels(defaultItemLabels: ResourceDefaultItemLabels): DefaultItemLabels
}
