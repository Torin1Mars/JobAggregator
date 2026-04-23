package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.AngularRespond
import com.example.jobaggregator.retrofit.RetrofitObj_RabotaUA
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RabotaUaParser(context : Context) {

    val appContext = context
    private val retrofitInstance: RetrofitObj_RabotaUA = RetrofitObj_RabotaUA

    private val jobQueryTemplate  = "%s/%s"

    val jobsCardsList = mutableListOf<JobCard>()

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseByQuery(query: String ="zapros/smila"){

        //TODO Set up this logic :


        CoroutineScope(Dispatchers.Default).launch{
            val respond = RetrofitObj_RabotaUA.api.test()

            delay(5000)

            val data = respond.execute().body()
            data?.message.let{Log.d("MyTag", data!!.message)}
        }


        RetrofitObj_RabotaUA.api.test()
            .enqueue(object :Callback<AngularRespond> {
            override fun onResponse(call: Call<AngularRespond>, response: Response<AngularRespond>) {
                if (response.isSuccessful) {
                    val data = response.body()

                    data?.message.let{Log.d("MyTag", data!!.message)}
                     // Handle parsed data
                }
            }

            override fun onFailure(call: Call<AngularRespond>, t: Throwable) {
                // Handle error
            }
        })


        /*val userQuery: String = query

        try {
            val currentResponse = retrofitInstance.api.getJobsQueryAsString("")
            val htmlPageInString = currentResponse.body()!!

            if (currentResponse.isSuccessful) {

                val howMuchPages = checkIfSeveralPages(htmlPageInString)

                if (howMuchPages > 1){

                    ( 1 .. howMuchPages).forEach { page->

                        try{
                            val localResponse = retrofitInstance.api.getJobsInPage(userQuery = userQuery, pageNum = page)
                            val htmlPageInString2 = localResponse.body()!!

                            val jobsList = getJobsIdList(htmlPageInString2)
                            val foundedJobs = getJobsById(jobsList)

                            jobsCardsList += foundedJobs
                            Log.d("MyTag", "Page $page vacancies - ${foundedJobs.size}")


                        }catch (e: Exception){
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
                            }

                            Log.d("MyTag", e.message.toString())
                        }
                    }

                    Log.d("MyTag", jobsCardsList.size.toString())

                }else{

                    try {
                        val jobsList = getJobsIdList(htmlPageInString)
                        val foundedJobs = getJobsById(jobsList)

                        jobsCardsList += foundedJobs

                    }catch (e: Exception){
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
                        }

                        Log.d("MyTag", e.message.toString())
                    }
                }

            } else {
                //Do nothing
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
                }
                Log.d("MyTag", "Couldn't load initial request")
            }

        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
            }

            Log.d("MyTag", e.message.toString())
        }*/
    }

    fun getJobsIdList(htmlPage: String): MutableList<String>{
        val jobsIds = mutableListOf<String>()

        val document = Ksoup.parse(htmlPage)

        val jobsElement  = document.selectFirst("#pjax-jobs-list")
        val selection = jobsElement!!.select("#pjax-jobs-list > a")

        selection.forEach {
                it->jobsIds.add(it.attr("name"))
        }

        return jobsIds
    }

    private fun checkIfSeveralPages(htmlPage: String): Int{
        var pagesCount = 0

        val document = Ksoup.parse(htmlPage)

        val navBarElement  = document.selectFirst("div.paginator.santa-text-16.ng-star-inserted")
        val lastPageNumber  = navBarElement?.selectFirst("li:nth-last-child(2) .side-btn")?.text()

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

                    val newJob = parseJob(currentResponse.body()!!, jobUrl = currentJobUrl, id)
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
    fun parseJob(jobHtmlPage: String, jobUrl: String, thisJobId: String): JobCard{

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

        val card  = JobCard(
            jobIdOnWebsite = thisJobId ,
            publicationDate = date,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            jobLocation = jobLocation,
            jobCompany = jobCompany,
            jobSalary = jobSalary,
            jobUrl = jobUrl)

        return card
    }


}