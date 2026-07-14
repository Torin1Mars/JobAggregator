package com.example.jobaggregator.hiltProviders

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.jobaggregator.Parsers.WebViewPool
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.ViewModels.RabotaUaParserVm
import com.example.jobaggregator.ViewModels.WorkUaParserVm
import com.example.jobaggregator.domain.JobsDbDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProviders {
    /*@RequiresApi(Build.VERSION_CODES.O)
    @Singleton
    @Provides
    fun provideMainViewModel(@ApplicationContext appContext: Context,
                             workUaParserVm: WorkUaParserVm,
                             rabotaUaParserVm: RabotaUaParserVm,
                             vacanciesDb: JobsDbDao ): MainViewModel {
        return MainViewModel(context = appContext,  jobsDatabase = vacanciesDb)
    }*/

    @Singleton
    @Provides
    fun provideWorkUaParserVm(@ApplicationContext appContext: Context, workUaParser: WorkUaParser ): WorkUaParserVm{
        return WorkUaParserVm(appContext, workUaParser)
    }

    @Singleton
    @Provides
    fun provideWorkUaParser(@ApplicationContext appContext: Context): WorkUaParser {
        return WorkUaParser(appContext)
    }

    @Singleton
    @Provides
    fun provideRabotaUaParserVm(@ApplicationContext appContext: Context): RabotaUaParserVm{
        return RabotaUaParserVm(appContext)
    }

}
