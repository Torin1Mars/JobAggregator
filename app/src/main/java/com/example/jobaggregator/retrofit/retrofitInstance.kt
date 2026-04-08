package com.example.jobaggregator.retrofit

import com.example.jobaggregator.supportingData.workUaUrl
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitObj {

    private val baseUrl = workUaUrl

    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    public val api by lazy {
        retrofitInstance.create(retrofitService::class.java)
    }
}
