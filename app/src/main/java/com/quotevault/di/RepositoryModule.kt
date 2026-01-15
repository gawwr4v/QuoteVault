package com.quotevault.di

import com.quotevault.data.repository.AuthRepositoryImpl
import com.quotevault.data.repository.QuoteRepositoryImpl
import com.quotevault.data.repository.SettingsRepositoryImpl
import com.quotevault.domain.repository.AuthRepository
import com.quotevault.domain.repository.QuoteRepository
import com.quotevault.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        quoteRepositoryImpl: QuoteRepositoryImpl
    ): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
