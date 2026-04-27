package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj_RabotaUA
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.userAgent
import okhttp3.internal.wait


import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RabotaUaParser(context : Context) {

    val appContext = context
    private val retrofitInstance: RetrofitObj_RabotaUA = RetrofitObj_RabotaUA

    private val jobQueryTemplate  = "%s/%s"

    val jobsCardsList = mutableListOf<JobCard>()

    @Composable
    fun webParsedScreen(){
        var pageIsLoad by remember { mutableStateOf<Boolean>(false)}

        var rawHtmlPage by remember{ mutableStateOf<String>("") }

        if (pageIsLoad){
            Log.d("MyTag", rawHtmlPage)

        }


        Box(modifier = Modifier.height(0.dp).width(0.dp)){
            AndroidView(factory = { context ->
                WebView(context).apply {

                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    addJavascriptInterface(
                        WebPageInterface{html-> rawHtmlPage = html},
                        "AndroidInterface")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageIsLoad = true

                            view?.loadUrl("javascript:window.AndroidInterface.getHtml(document.getElementsByTagName('html')[0].innerHTML);")
                        }


                    }
                    loadUrl(rabotaUaUrl)
                }
            })
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseByQuery(query: String ="zapros/smila"){


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

 class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
     @JavascriptInterface
     fun getHtml(page: String) {
         onWebPageReceived(page)
     }
 }
