package com.example.jobaggregator.domain

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard
import com.google.gson.Gson


@Database(entities = [DatabaseJobCard::class], version = 1, exportSchema = false)
@TypeConverters(JobCardDBConverter::class)
abstract class JobsDatabase: RoomDatabase(){

    abstract fun jobsDatabaseDAO(): JobsDbDao

    companion object{
        @Volatile
        private var INSTANCE:  JobsDatabase? = null

        fun getDatabase(context: Context):  JobsDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        JobsDatabase::class.java,
                        "JobsDB"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

class JobCardDBConverter(){

    @TypeConverter
    fun convertJobCardFromStr(formatedJobCard: String?): JobCard {
        return Gson().fromJson(formatedJobCard, JobCard::class.java )
    }

    @TypeConverter
    fun convertJobCardToStr(rawJobCard: JobCard): String? {
        return Gson().toJson(rawJobCard)
    }
}
