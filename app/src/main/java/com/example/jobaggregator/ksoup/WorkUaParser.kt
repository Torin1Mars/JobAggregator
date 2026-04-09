package com.example.jobaggregator.ksoup

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj
import com.example.jobaggregator.supportingData.dateFormat
import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

class WorkUaParser() {

    val retrofitInstance: RetrofitObj = RetrofitObj

    @RequiresApi(Build.VERSION_CODES.O)
    fun startTesting(){

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentResponse = retrofitInstance.api.getJobsQueryAsString("jobs-cherkasy-python")

                if (currentResponse.isSuccessful) {
                    val jobsList = getJobsIdList(currentResponse.body()!!)
                    getJobsById(jobsList)

                } else {
                    //Do nothing
                }
                val temp = currentResponse

            } catch (e: Exception) {
                Log.d("MyTag", e.message.toString())
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    public fun getJobsById(jobsIds: MutableList<String>): MutableList<JobCard>{

        val jobsCards = mutableListOf<JobCard>()

        jobsIds.forEach {id->

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentResponse = retrofitInstance.api.getOneJobData(id)
                    if (currentResponse.isSuccessful){
                        jobsCards.add(parseJob(currentResponse.body()!!))
                    }else{
                        //Do nothing
                    }
                }catch (e: Exception){
                    e.message?.let { Log.d("MyTag", it)}
                }


                Log.d("MyTag", jobsCards.toString())
            }
        }



        return jobsCards
    }

    public  fun getJobsIdList(htmlPage: String): MutableList<String>{
        val jobsIds = mutableListOf<String>()

        val document = Ksoup.parse(htmlPage)

        val jobsElement  = document.selectFirst("html body main#center.main-center.bg-gray div#pjax.container div.row div.col-md-8 div#pjax-jobs-list")
        val selection = jobsElement!!.select("#pjax-jobs-list > a")

        selection.forEach {
            it->jobsIds.add(it.attr("name"))
        }

        return jobsIds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseJob(jobHtmlPage: String): JobCard{

        val document = Ksoup.parse(jobHtmlPage)

        val element = document.selectFirst("time.text-default-7")

        val rawDate = element!!.attr("datetime")

        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        val date = LocalDate.parse(rawDate.substringBefore(" "), formatter)

        return JobCard(publicationDate = date)
    }
}
