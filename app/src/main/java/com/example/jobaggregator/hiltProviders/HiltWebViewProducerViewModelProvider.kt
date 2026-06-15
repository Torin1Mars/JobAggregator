package com.example.jobaggregator.hiltProviders

import android.content.Context
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import com.example.jobaggregator.domain.JobsDbDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltWebViewProducerViewModelProvider {
    @Singleton
    @Provides
    fun provideRabotaUaParserViewModel(@ApplicationContext appContext: Context, applicationDao: JobsDbDao ): WebViewProducerViewModel {
        return WebViewProducerViewModel(appContext, applicationDao)
    }
}