package com.example.jobaggregator.domain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseProviders {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JobsDatabase{
        return JobsDatabase.getDatabase(context)
    }

    @Provides
    fun provideDatabaseDAO(db:JobsDatabase): JobsDbDao{
        return db.jobsDatabaseDAO()
    }
}

