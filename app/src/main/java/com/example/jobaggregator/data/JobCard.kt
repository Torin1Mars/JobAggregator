package com.example.jobaggregator.data

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import java.time.LocalDate


data class JobCard(
    val publicationDate: LocalDate,
    val jobTitle: String,
    val jobDescription: String?,
    val jobLocation: String?,
    val jobCompany: String?,
    val jobSalary: String?,
    val jobUrl : Url
)
