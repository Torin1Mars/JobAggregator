package com.example.jobaggregator.ksoup

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj
import com.example.jobaggregator.supportingData.dateFormat
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.String

class WorkUaParser() {

    private val retrofitInstance: RetrofitObj = RetrofitObj
    private val jobQueryTemplate  = "%s/jobs/%s"

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

        CoroutineScope(Dispatchers.Main).launch {
            jobsIds.forEach { id ->
                try {
                    val currentResponse = retrofitInstance.api.getOneJobData(id)
                    if (currentResponse.isSuccessful) {
                        val currentJobUrl = jobQueryTemplate.format(retrofitInstance.getBaseUrl(), id)

                        val newJob = parseJob(currentResponse.body()!!, Url(urlString = currentJobUrl))
                        jobsCards.add(newJob)

                    } else {
                        //Do nothing
                    }
                } catch (e: Exception) {
                    e.message?.let { Log.d("MyTag", it) }
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
    fun parseJob(jobHtmlPage: String, jobUrl: Url): JobCard{

        val document = Ksoup.parse(jobHtmlPage)

        val element = document.selectFirst("time.text-default-7")
        val rawDate = element!!.attr("datetime")
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        val date = LocalDate.parse(rawDate.substringBefore(" "), formatter)

        val jobTitle: String = document.selectFirst("#h1-name")?.text() ?: "Empty"
        val jobDescription: String? = document.selectFirst("#job-description")?.text()

        val blockLocationCompanySalary = document.selectFirst("ul.sm\\:mt-xl")

        val jobLocation  = blockLocationCompanySalary?.selectFirst("[title=Адреса роботи]")?.parent()?.text()?.substringBefore(".")
        val jobCompany: String? = blockLocationCompanySalary?.selectFirst("[title=Дані про компанію]")?.parent()?.selectFirst(".inline")?.text()
        val jobSalary: String? = blockLocationCompanySalary?.selectFirst("[title=Зарплата]")?.nextElementSibling()?.text()

        val card  = JobCard(publicationDate = date,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            jobLocation = jobLocation,
            jobCompany = jobCompany,
            jobSalary = jobSalary,
            jobUrl = jobUrl)

        return card
    }
}
