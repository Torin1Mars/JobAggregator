package com.example.jobaggregator.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.jobaggregator.data.DatabaseJobCard
import kotlinx.coroutines.flow.Flow

@Dao
interface JobsDbDao {
    //Getting
    @Query("SELECT * FROM JobsDB")
    fun get_all_Jobs (): Flow<List<DatabaseJobCard>>

    @Query("SELECT * FROM JobsDB")
    suspend fun get_all_JobsList (): List<DatabaseJobCard>

    @Query("SELECT * FROM JobsDB WHERE job_jobIdOnWebsite IN (:jobIds)")
    suspend fun getJobsByIds(jobIds: List<String>): List<DatabaseJobCard>

    //Adding
    @Insert
    fun addOneJobCard(newJobCard: DatabaseJobCard): Unit

    @Insert
    fun addJobCardList(newJobCard: MutableList<DatabaseJobCard>): Unit

    //Updating

    //Deleting
    @Query("DELETE FROM JobsDB")
    fun deleteDb()

    //Additional
    @Query("SELECT COUNT(*) FROM JobsDB")
    suspend fun getVacanciesCount(): Int

    @Query("SELECT COUNT(*) FROM JobsDB")
    fun getDbCountFlow(): Flow<Int>
}
