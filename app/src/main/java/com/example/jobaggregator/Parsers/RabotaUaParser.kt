package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.rabotaUaParerRanderDelay
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.format.DateTimeFormatter


class RabotaUaParser(context : Context) {

    val appContext = context

    var respondHtmlPage : String = ""
    private val jobQueryTemplate  = "%s/%s"

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun test (){

        var htmlStrRespond by rememberSaveable { mutableStateOf<String>("") }
        var needToCheckPagesCount by rememberSaveable { mutableStateOf<Boolean>(true) }

        if (needToCheckPagesCount) {
            getParsedPage(rabotaUaUrl+"/zapros/kyiv", { it -> htmlStrRespond = it })
            needToCheckPagesCount = false
        }

        if (!htmlStrRespond.isBlank()){
            Log.d("MyTag", "OK")
            //var pages = checkHowManyPages(respond.value)
        }
    }

    private fun updateRespondValue(newValue : String){
        respondHtmlPage = newValue
    }

    private fun checkHowManyPages(htmlPage: String): Int? {
        var pagesCount : Int? = null

        val document = Ksoup.parse(htmlPage)
        val paginationStatusBar = document.selectFirst(".paginator")

        val lastPageCountElement = paginationStatusBar?.select("a.ng-star-inserted")?.get(3);

        if (lastPageCountElement !== null){
            pagesCount = lastPageCountElement.text().toInt()
        }

        return pagesCount
    }
}

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun getParsedPage(urlQuery: String, updateRespond:(newValue: String)-> Unit) {
        //Parsing screen is running in hide mode
        var rawHtmlPage by remember { mutableStateOf<String>("") }

        var pageRenderWasFinished by remember {mutableStateOf<Boolean>(false)}

        Box(modifier = Modifier.height(0.dp).width(0.dp)) {
            AndroidView(factory = { context ->
                val view = WebView(context)

                view.apply {
                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    addJavascriptInterface(
                        WebPageInterface { html ->
                            rawHtmlPage = html
                        },
                        "AndroidInterface"
                    )

                    webViewClient = object : WebViewClient() {

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.loadUrl("javascript:window.AndroidInterface.getHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")

                            if (!pageRenderWasFinished){
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(rabotaUaParerRanderDelay)
                                    updateRespond(rawHtmlPage)

                                    pageRenderWasFinished = true
                                }
                            }
                        }
                    }

                    loadUrl(urlQuery)
                }
            })
        }
    }

        fun parseVacanciesList(htmlRespond: String) {
            val vacanciesCardsList = mutableListOf<Element>()

            val document = Ksoup.parse(htmlRespond)
            val jobsListElement =
                document.selectFirst("alliance-jobseeker-mobile-vacancies-list") //"alliance-jobseeker-mobile-vacancies-list:nth-child(2)"

            if (jobsListElement != null) {
                val vacanciesCards = jobsListElement.select("alliance-vacancy-card-mobile")
                vacanciesCardsList.addAll(vacanciesCards)
            }

            Log.d("MyTag", "Size is ${vacanciesCardsList.size}")
        }

        fun getJobsIdList(htmlPage: String): MutableList<String> {
            val jobsIds = mutableListOf<String>()

            val document = Ksoup.parse(htmlPage)

            val jobsElement = document.selectFirst("#pjax-jobs-list")
            val selection = jobsElement!!.select("#pjax-jobs-list > a")

            selection.forEach { it ->
                jobsIds.add(it.attr("name"))
            }

            return jobsIds
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun parseJob(jobHtmlPage: String, jobUrl: String, thisJobId: String): JobCard {

            val document = Ksoup.parse(jobHtmlPage)

            val element = document.selectFirst("time.text-default-7")
            val rawDate = element!!.attr("datetime").substringBefore(" ")
            val formater = DateTimeFormatter.ofPattern(dateFormat)
            val date = LocalDate.parse(rawDate).format(formater).toString()

            val jobTitle: String = document.selectFirst("#h1-name")?.text() ?: "Empty"
            val jobDescription: String? = document.selectFirst("#job-description")?.text()

            val blockLocationCompanySalary = document.selectFirst("ul.sm\\:mt-xl")

            val jobLocation =
                blockLocationCompanySalary?.selectFirst("[title=Адреса роботи]")?.parent()?.text()?.substringBefore(".")
            val jobCompany: String? =
                blockLocationCompanySalary?.selectFirst("[title=Дані про компанію]")?.parent()?.selectFirst(".inline")
                    ?.text()
            val jobSalary: String? =
                blockLocationCompanySalary?.selectFirst("[title=Зарплата]")?.nextElementSibling()?.text()

            val card = JobCard(
                jobIdOnWebsite = thisJobId,
                publicationDate = date,
                jobTitle = jobTitle,
                jobDescription = jobDescription,
                jobLocation = jobLocation,
                jobCompany = jobCompany,
                jobSalary = jobSalary,
                jobUrl = jobUrl
            )

            return card
        }


 class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
     @JavascriptInterface
     fun getHtml(page: String) {
         onWebPageReceived(page)
     }
 }
