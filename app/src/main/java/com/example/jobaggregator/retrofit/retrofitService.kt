package com.example.jobaggregator.retrofit

import retrofit2.http.GET

interface retrofitService {

    @GET("jobs-cherkasy-python/")
    suspend fun getPageAsString(): String
}