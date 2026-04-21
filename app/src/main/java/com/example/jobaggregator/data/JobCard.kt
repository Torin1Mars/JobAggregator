package com.example.jobaggregator.data

import kotlinx.serialization.Serializable

@Serializable
data class JobCard(
    val jobIdOnWebsite: String,
    val publicationDate: String,
    val jobTitle: String,
    val jobDescription: String?,
    val jobLocation: String?,
    val jobCompany: String?,
    val jobSalary: String?,
    val jobUrl : String
)
