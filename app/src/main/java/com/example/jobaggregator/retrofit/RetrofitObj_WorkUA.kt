package com.example.jobaggregator.retrofit

import com.example.jobaggregator.supportingData.workUaUrl
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitObj_WorkUA {

    private val baseUrl = workUaUrl

    public fun getBaseUrl (): String{
        return baseUrl
    }

    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    public val api by lazy {
        retrofitInstance.create(retrofitService::class.java)
    }
}
