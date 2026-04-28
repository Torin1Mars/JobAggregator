package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.get
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.retrofit.RetrofitObj_RabotaUA
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.wait

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RabotaUaParser(context : Context) {

    val appContext = context
    private val retrofitInstance: RetrofitObj_RabotaUA = RetrofitObj_RabotaUA

    private val jobQueryTemplate  = "%s/%s"


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun StartParsedScreen(){
        //Parsing screen is running in hide mode

        var rawHtmlPage by remember{ mutableStateOf<String>("") }

        Box(modifier = Modifier.height(0.dp).width(0.dp)){
            AndroidView(factory = { context ->
                val view = WebView(context)
                val pageIsLoad: Int = 100

                view.webChromeClient = object : WebChromeClient(){
                    override fun onProgressChanged(view: WebView?, progress: Int) {
                        if (progress == pageIsLoad) {
                           CoroutineScope(Dispatchers.IO).launch {
                               // Waiting till javascript fully execute respond page
                               delay(2.seconds)

                               parseVacanciesList(htmlRespond = rawHtmlPage)
                           }
                        }
                    }
                }

                view.apply {
                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    addJavascriptInterface(
                        WebPageInterface{html->
                            rawHtmlPage = html},
                        "AndroidInterface")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            view?.loadUrl("javascript:window.AndroidInterface.getHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")

                           /* view?.evaluateJavascript ("(function() { return document.readyState; });"){pageStatus ->
                                Log.d("MyTag", pageStatus)

                                if (pageStatus == "\"complete\"") {
                                    parseVacanciesList(rawHtmlPage)
                                }
                            }*/
                        }
                    }

                    loadUrl(rabotaUaUrl)
                }
            })
        }

    }

    fun parseVacanciesList(htmlRespond: String){
        val vacanciesCardsList = mutableListOf<Element>()

        val document = Ksoup.parse(htmlRespond)
        val jobsListElement  = document.selectFirst("alliance-jobseeker-mobile-vacancies-list") //"alliance-jobseeker-mobile-vacancies-list:nth-child(2)"

        if (jobsListElement != null){
            val vacanciesCards = jobsListElement.select("alliance-vacancy-card-mobile")
            vacanciesCardsList.addAll(vacanciesCards)
        }

        Log.d("MyTag", "Size is ${vacanciesCardsList.size}")
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
