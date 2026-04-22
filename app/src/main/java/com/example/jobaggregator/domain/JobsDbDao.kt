package com.example.jobaggregator.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.flow.Flow

@Dao
interface JobsDbDao {
    //Getting
    @Query("SELECT * FROM JobsDB")
    fun get_all_Jobs (): Flow<List<DatabaseJobCard>>

    //Adding
    @Insert
    fun addOneJobCard(newJobCard: DatabaseJobCard): Unit

    @Insert
    fun addJobCardList(newJobCard: MutableList<DatabaseJobCard>): Unit

    //Updating

    //Deleting
    @Query("DELETE FROM JobsDB")
    fun deleteDb()
}
