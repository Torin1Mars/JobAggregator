package com.example.jobaggregator.hilt

import android.content.Context
import com.example.jobaggregator.ksoup.WorkUaParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltMainViewModelModule {
    @Singleton
    @Provides
    fun provideWorkUaParser(@ApplicationContext appContext:Context): WorkUaParser {
        return WorkUaParser (appContext)
    }

}