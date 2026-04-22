package com.example.jobaggregator.Parsers

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltMainViewModelModule {
    @Singleton
    @Provides
    fun provideWorkUaParser(@ApplicationContext appContext: Context): WorkUaParser {
        return WorkUaParser (appContext)
    }

    @Singleton
    @Provides
    fun provideRabotaUaParser(@ApplicationContext appContext: Context): RabotaUaParser{
        return RabotaUaParser(appContext)
    }
}
