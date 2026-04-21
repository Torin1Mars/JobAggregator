package com.example.jobaggregator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "JobsDB")
data class DatabaseJobCard(

    @PrimaryKey(autoGenerate = true)
    val idInDb : Int = 0,

    val publicationDate: String,
    val jobCard: JobCard
)