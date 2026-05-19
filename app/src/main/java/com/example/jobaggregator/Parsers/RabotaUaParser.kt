package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.jobaggregator.R
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
import androidx.compose.ui.res.stringResource
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
import java.util.Locale


class RabotaUaParser(context : Context) {

    val appContext = context

    private val jobQueryTemplate = "%s/%s/"

    private val jobsCardsList =  mutableStateListOf<JobCard>()

    fun getJobsCardsList(): MutableList<JobCard>{
        return jobsCardsList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun test() {

        var htmlStrRespond by rememberSaveable { mutableStateOf<String>("") }

        var needToCheckPagesCount by rememberSaveable { mutableStateOf<Boolean>(true) }
        var pagesCountChecked by rememberSaveable { mutableStateOf<Boolean>(false) }

        var pagesInRespond by rememberSaveable { mutableStateOf<Int>(0) }

        val baseQuery by remember { mutableStateOf<String>(rabotaUaUrl + "/zapros/smila" )}

        if (needToCheckPagesCount) {
            GetParsedPage(baseQuery, { it -> htmlStrRespond = it })

            if (!htmlStrRespond.isBlank()) {
                pagesInRespond = getPagesCount(htmlStrRespond)

                needToCheckPagesCount = false
                pagesCountChecked = true
            }else
                pagesInRespond = 0
        }

        //Doing parsing
        if (pagesCountChecked) {

            //Doing parsing according to
            when (pagesInRespond) {
                0 -> {Toast.makeText(appContext, R.string.searchGotZeroResultMessage, Toast.LENGTH_SHORT).show()}//Do nothing

                1 -> ParseSinglePageRespond(htmlStrRespond, {jobCardsParsedList ->jobsCardsList.addAll(jobCardsParsedList)})
                else -> ParseSeveralPagesRespond(pagesInRespond, baseQuery, htmlStrRespond, {jobCardsParsedList ->jobsCardsList.addAll(jobCardsParsedList)})
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun ParseSeveralPagesRespond(totalPagesInRespond:Int,
                                         queryInitialLink: String,
                                         initialPageRespond: String,
                                         returnParsedVacancies:(vacanciesList: MutableList<JobCard>)-> Unit) {

        val queryTemplate = "%s/params;page=%d"

        var needToParseFirstPageVacancies by remember { mutableStateOf<Boolean>(true) }
        var needToGetRestPagesVacancies by remember { mutableStateOf<Boolean>(false) }

        val vacanciesList = remember {mutableStateListOf<JobCard>()}

        //For dev
        if (vacanciesList.isNotEmpty()){
            Log.d("MyTag", vacanciesList.size.toString())
        }

        //Initially we already have first responded page so we need to parse vacancies from here

        if (needToParseFirstPageVacancies){
            Log.d("MyTag", queryInitialLink)
            ParseSinglePageRespond(initialPageRespond,{jobCardsParsedList -> vacanciesList.addAll(jobCardsParsedList)})

            needToParseFirstPageVacancies = false
            needToGetRestPagesVacancies = true
        }

        if (!needToParseFirstPageVacancies && needToGetRestPagesVacancies){
            (2..totalPagesInRespond).forEach { page ->
                val currentQuery = String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, page)

                //TODO Here I need to implement parsing by several pages respond
                //it can be possible to use existing single page parsing approach
                GetParsedPage()
                ParseSinglePageRespond(currentQuery,{jobCardsParsedList -> vacanciesList.addAll(jobCardsParsedList)})
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun ParseSinglePageRespond(htmlPage: String,
                                       returnParsedVacancies:(vacanciesList: MutableList<JobCard>)-> Unit) {

        var needToCollectVacanciesLinks by remember { mutableStateOf<Boolean>(true) }
        var needToGetAllVacancies by remember { mutableStateOf<Boolean>(false) }
        var parsingByPagesHasFinished by remember { mutableStateOf<Boolean>(false) }
        var parsingByJobCards by remember { mutableStateOf<Boolean>(false) }

        val vacanciesQueryList = remember { mutableStateListOf<String>() }
        val vacanciesHtmlPagesList = remember { mutableStateListOf<String>() }

        val foundedJobsCardsList = remember { mutableStateListOf<JobCard>() }

        if (needToCollectVacanciesLinks) {
            val document = Ksoup.parse(htmlPage)

            val vacanciesBoxElement =
                document.selectFirst("alliance-jobseeker-mobile-vacancies-list:nth-child(2) > div:nth-child(1)")
            val vacanciesListElement = vacanciesBoxElement?.select("alliance-vacancy-card-mobile")

            var vacancyQuery = ""

            if (vacanciesListElement != null){
                vacanciesListElement.forEach { vacancy ->
                    vacancyQuery = vacancy.child(0).attr("href")

                    if (!vacancyQuery.isBlank()) {
                        vacanciesQueryList.add(vacancyQuery)
                    }
                    needToGetAllVacancies = true
                }

                needToCollectVacanciesLinks = false

            }else {
                Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
            }
        }

        //All of this stages would'nt start to work if in upper logic document haven't successfully parsed
        if (!needToCollectVacanciesLinks && needToGetAllVacancies) {
            vacanciesQueryList.forEach { currentVacancy ->
                val vacancyQuery = jobQueryTemplate.format(rabotaUaUrl, currentVacancy)
                GetParsedPage(vacancyQuery, { it -> vacanciesHtmlPagesList.add(it)})
            }

            if (vacanciesQueryList.size == vacanciesHtmlPagesList.size) {
                needToGetAllVacancies = false
                parsingByPagesHasFinished = true
            }

            if (parsingByPagesHasFinished && !parsingByJobCards) {
                runCatching {
                    vacanciesHtmlPagesList.forEach { htmlVacancy->

                        val vacancyJobCard = parseVacancyCard(htmlVacancy)
                        //Adding pared data to main container
                        foundedJobsCardsList.add(vacancyJobCard)
                    }

                    returnParsedVacancies(foundedJobsCardsList)

                    //It means that all our parsing has been finished
                    parsingByJobCards = true

                }.onFailure {
                    Toast.makeText(appContext, R.string.errorJobCardLoading, Toast.LENGTH_SHORT).show()
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

                                    val vacancyId = urlQuery.substringAfter("robota.ua//").removeSuffix("/")

                                    rawHtmlPage = rawHtmlPage + "ORIGIN VACANCY ID =$vacancyId"
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
    fun parseVacancyCard(jobHtmlPage: String):JobCard {

        val document = Ksoup.parse(jobHtmlPage)

        fun convertRecivedDate(receivedData: String): LocalDate {
            val day  = receivedData.substringBefore(" ").toInt()

            var monthText = receivedData.substringAfter(" ").substringBefore(" ")

            val month = monthUa.indexOf(monthText)

            val year = receivedData.substringAfter(" ").substringAfter(" ").toInt()

            return LocalDate.of(year,month,day)
        }

        val vacancyId =  document.body().ownText().substringAfter("ORIGIN VACANCY ID =")

        val element = document.selectFirst("span.santa-text-white:nth-child(1)")
        var rawDate = element!!.text()

        val date = convertRecivedDate(rawDate)

        val formater = DateTimeFormatter.ofPattern(dateFormat)
        val dateStr = date.format(formater).toString()

        val jobTitle: String = document.selectFirst("[data-id=vacancy-title]")?.text() ?: "Empty"
        val jobDescription: String? = document.selectFirst("#description-wrap")?.text() ?: "Empty"

        val jobLocation =
            document.selectFirst("[data-id=vacancy-city]")?.parent()?.text()?:"No information"

        val jobCompany: String? =
            document.selectFirst("a[href^=/company] > span")?.text()?:"No information"

        val jobSalary: String? =
            document.selectFirst("[data-id=vacancy-salary-from-to]")?.text() ?: "Empty"

        val jobUrl = rabotaUaUrl+"/"+vacancyId

        val card = JobCard(
            jobIdOnWebsite = vacancyId,
            publicationDate = dateStr,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            jobLocation = jobLocation,
            jobCompany = jobCompany,
            jobSalary = jobSalary,
            jobUrl = jobUrl
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

