package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.monthUa
import com.example.jobaggregator.supportingData.rabotaUaParerRenderDelay
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.format.DateTimeFormatter


class RabotaUaParser(context : Context) {

    val appContext = context

    var respondHtmlPage: String = ""
    private val jobQueryTemplate = "%s/%s/"

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun test() {

        var htmlStrRespond by rememberSaveable { mutableStateOf<String>("") }

        var needToCheckPagesCount by rememberSaveable { mutableStateOf<Boolean>(true) }
        var pagesCountChecked by rememberSaveable { mutableStateOf<Boolean>(false) }

        var pagesInRespond by rememberSaveable { mutableStateOf<Int>(0) }


        if (needToCheckPagesCount) {
            GetParsedPage(rabotaUaUrl + "/zapros/smila", { it -> htmlStrRespond = it })

            if (!htmlStrRespond.isBlank()) {
                pagesInRespond = getPagesCount(htmlStrRespond)

                needToCheckPagesCount = false
                pagesCountChecked = true
            }
        }

        //Doing parsing
        if (pagesCountChecked) {
            //Temporary
            pagesInRespond = 1

            when (pagesInRespond) {
                0 -> {}//Do nothing
                1 -> ParseSinglePageRespond(htmlStrRespond)
                else -> ParseSeveralPagesRespond(htmlStrRespond)
            }
        }
    }

    @Composable
    private fun ParseSeveralPagesRespond(htmlPage: String) {


    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun ParseSinglePageRespond(htmlPage: String) {

        var needToCollectVacanciesLinks by remember { mutableStateOf<Boolean>(true) }
        var needToGetAllVacancies by remember { mutableStateOf<Boolean>(false) }
        var parsingHasFinished by remember { mutableStateOf<Boolean>(false) }

        val vacanciesQueryList = remember { mutableStateListOf<String>() }
        val vacanciesHtmlPagesList = remember { mutableStateListOf<String>() }

        if (needToCollectVacanciesLinks) {
            val document = Ksoup.parse(htmlPage)

            val vacanciesBoxElement =
                document.selectFirst("alliance-jobseeker-mobile-vacancies-list:nth-child(2) > div:nth-child(1)")
            val vacanciesListElement = vacanciesBoxElement?.select("alliance-vacancy-card-mobile")

            var vacancyQuery = ""
            vacanciesListElement.let { it ->
                it!!.forEach { vacancy ->
                    vacancyQuery = vacancy.child(0).attr("href")

                    if (!vacancyQuery.isBlank()) {
                        vacanciesQueryList.add(vacancyQuery)
                    }

                    needToGetAllVacancies = true

                }
            }
            needToCollectVacanciesLinks = false
        }

        if (!needToCollectVacanciesLinks && needToGetAllVacancies) {
            vacanciesQueryList.forEach { currentVacancy ->
                val vacancyQuery = jobQueryTemplate.format(rabotaUaUrl, currentVacancy)
                GetParsedPage(vacancyQuery, { it -> vacanciesHtmlPagesList.add(it)})
            }

            if (vacanciesQueryList.size == vacanciesHtmlPagesList.size) {
                needToGetAllVacancies = false
                parsingHasFinished = true
            }

            if (parsingHasFinished) {
                vacanciesHtmlPagesList.forEach { htmlVacancy->
                    parseVacancyCard(htmlVacancy)
                }

            }

        }
    }


    private fun getPagesCount(htmlPage: String): Int {
        var pagesCount : Int = 0

        val document = Ksoup.parse(htmlPage)
        val paginationStatusBar = document.selectFirst(".paginator")

        //val lastPageCountElement = paginationStatusBar?.select("a.ng-star-inserted")?.get(3)
        val lastPageCountElement = paginationStatusBar?.select("a:nth-last-child(2)")


        if (lastPageCountElement != null){
            pagesCount = lastPageCountElement.text().toInt()
        }

        return pagesCount
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun GetParsedPage(urlQuery: String, updateVacancyData:(newValue: String)-> Unit) {
        //Parsing screen is running in hide mode
        var rawHtmlPage by remember { mutableStateOf<String>("") }

        var pageHasLoad by remember {mutableStateOf<Boolean>(false)}

        Box(modifier = Modifier.height(0.dp).width(0.dp)) {
            AndroidView(factory = { context ->
                val view = WebView(context)

                view.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress == 100) {

                            if (!pageHasLoad){
                                //Waiting till Web view fully render web page
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(rabotaUaParerRenderDelay)

                                    updateVacancyData(rawHtmlPage)
                                }
                            }

                            pageHasLoad = true
                        }
                    }
                }

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
                        }
                    }

                    loadUrl(urlQuery)
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseVacancyCard(jobHtmlPage: String): JobCard {

        val document = Ksoup.parse(jobHtmlPage)

        fun convertRecivedDate(receivedData: String): LocalDate {
            val day  = receivedData.substringBefore(" ").toInt()

            var monthText = receivedData.substringAfter(" ").substringBefore(" ")

            val month = monthUa.indexOf(monthText)

            val year = receivedData.substringAfter(" ").substringAfter(" ").toInt()

            return LocalDate.of(day, month,year)
        }

        val element = document.selectFirst("span.santa-text-white:nth-child(1)")
        var rawDate = element!!.text()

        //TODO Still fix here
        val date = convertRecivedDate(rawDate)

        val formater = DateTimeFormatter.ofPattern(dateFormat)
        val dateStr = date.format(formater).toString()

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
            jobIdOnWebsite = "thisJobId",
            publicationDate = dateStr,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            jobLocation = jobLocation,
            jobCompany = jobCompany,
            jobSalary = jobSalary,
            jobUrl = "jobUrl"
        )

        return card
    }


}


 class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
     @JavascriptInterface
     fun getHtml(page: String) {
         onWebPageReceived(page)
     }
 }
