package com.example.jobaggregator.Parsers

import android.content.Context
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj_RabotaUA

class RabotaUaParser(context : Context) {

    val appContext = context
    private val retrofitInstance: RetrofitObj_RabotaUA = RetrofitObj_RabotaUA

    private val jobQueryTemplate  = "%s/jobs/%s"

    val jobsCardsList = mutableListOf<JobCard>()

}