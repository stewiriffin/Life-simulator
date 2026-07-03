package com.maisha.game.di

import com.maisha.game.data.local.DatabaseHealth
import com.maisha.game.data.local.GameDatabaseAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindGameDatabaseAccess(health: DatabaseHealth): GameDatabaseAccess
}
