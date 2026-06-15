package com.example.jobaggregator.hiltProviders

import android.content.Context
import com.example.jobaggregator.Parsers.RabotaUaParser
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltMainViewModelProviders {
    @Singleton
    @Provides
    fun provideWorkUaParser(@ApplicationContext appContext: Context): WorkUaParser {
        return WorkUaParser(appContext)
    }

    @Singleton
    @Provides
    fun provideRabotaUaParser(@ApplicationContext appContext: Context, rabotaUaViewModel: WebViewProducerViewModel): RabotaUaParser {
        return RabotaUaParser(appContext, rabotaUaViewModel)
    }
}