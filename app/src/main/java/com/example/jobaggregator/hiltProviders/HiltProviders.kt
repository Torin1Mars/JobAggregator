package com.example.jobaggregator.hiltProviders

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.jobaggregator.Parsers.WebViewPool
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.domain.JobsDbDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProviders {
    @RequiresApi(Build.VERSION_CODES.O)
    @Singleton
    @Provides
    fun provideMainViewModel(@ApplicationContext appContext: Context,vacanciesDb: JobsDbDao ): MainViewModel {
        return MainViewModel(context = appContext, vacanciesDb)
    }

    @Singleton
    @Provides
    fun provideWorkUaParser(@ApplicationContext appContext: Context): WorkUaParser {
        return WorkUaParser(appContext)
    }

    @Singleton
    @Provides
    //For RabotaUa parser Logic
    fun provideWebViewPool(@ApplicationContext appContext: Context): WebViewPool {
        return WebViewPool(appContext)
    }
}
