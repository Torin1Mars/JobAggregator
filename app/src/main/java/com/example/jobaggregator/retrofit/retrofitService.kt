package com.example.jobaggregator.retrofit


import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface retrofitService {

    @GET("/{myQuery}/")
    suspend fun getJobsQueryAsString(
        @Path("myQuery") userQuery: String
    ): Response<String>

    @GET("/jobs/{jobId}/")
    suspend fun getOneJobData(
        @Path("jobId") jobId: String
    ): Response<String>

}