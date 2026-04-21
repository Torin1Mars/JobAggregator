package com.example.jobaggregator.domain

import android.content.Context
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.jobaggregator.data.JobCard
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
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

