package com.example.jobaggregator.hiltProviders

import android.content.Context
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.ViewModels.RabotaUaParserViewModel
import com.example.jobaggregator.domain.JobsDbDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltRabotaUaParserViewModelProvider {
    @Singleton
    @Provides
    fun provideRabotaUaParserViewModel(@ApplicationContext appContext: Context, applicationDao: JobsDbDao ): RabotaUaParserViewModel {
        return RabotaUaParserViewModel(appContext, applicationDao)
    }
}