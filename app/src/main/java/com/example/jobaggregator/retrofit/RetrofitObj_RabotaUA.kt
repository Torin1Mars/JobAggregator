package com.example.jobaggregator.retrofit

import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.example.jobaggregator.supportingData.workUaUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitObj_RabotaUA {

    private val baseUrl = rabotaUaUrl

    public fun getBaseUrl (): String{
        return baseUrl
    }

    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    public val api by lazy {
        retrofitInstance.create(retrofitService::class.java)
    }
}