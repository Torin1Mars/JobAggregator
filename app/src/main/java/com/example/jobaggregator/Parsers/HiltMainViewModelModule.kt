package com.example.jobaggregator.Parsers

import android.content.Context
import com.example.jobaggregator.domain.JobsDatabase
import com.example.jobaggregator.domain.JobsDbDao
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
}