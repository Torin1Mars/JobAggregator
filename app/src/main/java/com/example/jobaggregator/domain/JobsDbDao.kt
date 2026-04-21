package com.example.jobaggregator.domain

import androidx.room.Dao
import androidx.room.Query
import com.example.jobaggregator.Db.DatabaseJobCard
import kotlinx.coroutines.flow.Flow

@Dao
interface JobsDao {

    @Query("SELECT * FROM JobsDb")
    suspend fun get_all_Jobs (): Flow<DatabaseJobCard>



}