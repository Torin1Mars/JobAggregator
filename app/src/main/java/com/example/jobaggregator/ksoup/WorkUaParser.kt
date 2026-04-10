package com.example.jobaggregator.ksoup

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.times
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj
import com.example.jobaggregator.supportingData.dateFormat
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.String

class WorkUaParser() {

    private val retrofitInstance: RetrofitObj = RetrofitObj
    private val jobQueryTemplate  = "%s/jobs/%s"

    private val jobsCardsList = mutableListOf<JobCard>()

    @RequiresApi(Build.VERSION_CODES.O)
    fun startTesting(){

        val userQuery: String = "jobs-cherkasy"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentResponse = retrofitInstance.api.getJobsQueryAsString(userQuery)
                val htmlPageInString = currentResponse.body()!!

                if (currentResponse.isSuccessful) {


                    val howMuchPages = checkIfSeveralPages(htmlPageInString)

                    if (howMuchPages > 1){
                        ( 1 .. howMuchPages).forEach { page->

                            //TODO pack it in try catch block :
                            val localResponse = retrofitInstance.api.getJobsInPage(userQuery = userQuery, pageNum = page)
                            val localHtmlPageInString = localResponse.body()!!

                            val jobsList = getJobsIdList(htmlPageInString)
                        }



                    }else{
                        val jobsList = getJobsIdList(htmlPageInString)
                        val foundedJobs = getJobsById(jobsList)

                        jobsCardsList += foundedJobs
                    }

                } else {
                    //Do nothing
                }
                val temp = currentResponse

            } catch (e: Exception) {
                Log.d("MyTag", e.message.toString())
            }

            Log.d("MyTag", jobsCardsList.size.toString())
        }
    }

    suspend fun getJobsIdList(htmlPage: String): MutableList<String>{
        val jobsIds = mutableListOf<String>()

        val document = Ksoup.parse(htmlPage)

        val jobsElement  = document.selectFirst("html body main#center.main-center.bg-gray div#pjax.container div.row div.col-md-8 div#pjax-jobs-list")
        val selection = jobsElement!!.select("#pjax-jobs-list > a")

        selection.forEach {
                it->jobsIds.add(it.attr("name"))
        }

        return jobsIds
    }

    private fun checkIfSeveralPages(htmlPage: String): Int{
        var pagesCount = 0

        val document = Ksoup.parse(htmlPage)

        val navBarElement  = document.selectFirst("ul.pagination")
        val lastPageNumber  = navBarElement?.selectFirst("li:nth-last-child(2) a")?.text()

        if (lastPageNumber.isNullOrEmpty()){
            pagesCount = 1
        }else{
            pagesCount = lastPageNumber.toInt()
        }

        return pagesCount
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJobsById(jobsIds: MutableList<String>): MutableList<JobCard>{

        val jobsList = mutableListOf<JobCard>()

        jobsIds.forEach { id ->
            try {
                val currentResponse = retrofitInstance.api.getOneJobData(id)
                if (currentResponse.isSuccessful) {
                    val currentJobUrl = jobQueryTemplate.format(retrofitInstance.getBaseUrl(), id)

                    val newJob = parseJob(currentResponse.body()!!, jobUrl = currentJobUrl)
                    jobsList.add(newJob)

                } else {
                //Do nothing
                }
            } catch (e: Exception) {
                e.message?.let { Log.d("MyTag", it) }
            }
        }

        return jobsList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun parseJob(jobHtmlPage: String, jobUrl: String): JobCard{

        val document = Ksoup.parse(jobHtmlPage)

        val element = document.selectFirst("time.text-default-7")
        val rawDate = element!!.attr("datetime").substringBefore(" ")
        val formater = DateTimeFormatter.ofPattern(dateFormat)
        val date = LocalDate.parse(rawDate).format(formater).toString()

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
