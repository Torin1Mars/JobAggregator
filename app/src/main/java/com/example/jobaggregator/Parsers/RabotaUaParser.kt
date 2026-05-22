package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
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
import kotlinx.coroutines.withContext

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

        var htmlStrRespond by remember { mutableStateOf<String>("") }

        var needToCheckPagesCount by remember { mutableStateOf<Boolean>(true)}
        var pagesCountChecked by remember { mutableStateOf<Boolean>(false)}
        var vacancyParsingStarted by remember { mutableStateOf<Boolean>(false) }
        var vacancyParsingFinished by remember { mutableStateOf<Boolean>(false) }

        var pagesInRespond: Int by rememberSaveable { mutableStateOf<Int>(0) }

        val baseQuery by remember { mutableStateOf<String>(rabotaUaUrl + "/zapros/smila" )}

        if (needToCheckPagesCount) {
            GetParsedPage(baseQuery, { it -> htmlStrRespond = it })
            needToCheckPagesCount = false
        }

        if (!pagesCountChecked && !htmlStrRespond.isEmpty()) {
            pagesInRespond = getPagesCount(htmlStrRespond)

            pagesCountChecked = true
            vacancyParsingStarted = true
        }


        //ATTENTION Allow to recompose here:
        if (vacancyParsingStarted && !vacancyParsingFinished) {

               //Doing parsing according to
               when (pagesInRespond) {
                   0 -> {Toast.makeText(appContext, R.string.searchGotZeroResultMessage, Toast.LENGTH_SHORT).show()}//Do nothing

                   1 -> ParseSinglePageRespond(htmlStrRespond,
                       {jobCardsParsedList ->jobsCardsList.addAll(jobCardsParsedList)},
                       {vacancyParsingFinished = true})
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
        var firstPageVacanciesParsed by remember { mutableStateOf<Boolean>(false) }

        var vacancyGeneralPagesParsingStarted by remember { mutableStateOf<Boolean>(false) }
        var vacancyGeneralPagesParsingFinished by remember { mutableStateOf<Boolean>(false) }

        var needToParseEachVacancy by remember { mutableStateOf<Boolean>(false) }

        var pagesParsedCounter by remember { mutableStateOf<Int>(0) }
        var pagesParsedCounterStatus by remember { mutableStateOf<Boolean>(false) }

        val vacanciesPagesList = remember {mutableStateListOf<String>()}
        val vacanciesJobCardsList = remember {mutableStateListOf<JobCard>()}


        //Initially we already have first responded page so we need to parse vacancies from here
        if (needToParseFirstPageVacancies){
            //Parsing vacancies on first page
            ParseSinglePageRespond(initialPageRespond,
                {jobCardsParsedList -> vacanciesJobCardsList.addAll(jobCardsParsedList)},
                {firstPageVacanciesParsed = true})

            needToParseFirstPageVacancies = false
        }

        if (firstPageVacanciesParsed && !vacancyGeneralPagesParsingStarted){
            (totalPagesInRespond-1..totalPagesInRespond).forEach { page ->

                val currentQuery = String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, page)
                GetParsedPage(currentQuery, {it-> vacanciesPagesList.add(it)})
            }
            vacancyGeneralPagesParsingStarted = true
        }

        //Temporary
        if (!vacancyGeneralPagesParsingFinished && vacanciesPagesList.size > 1){
            vacancyGeneralPagesParsingFinished = true

            needToParseEachVacancy = true
        }


        //TODO Implement here logic where parsing goes page by page
        if (needToParseEachVacancy){
            vacanciesPagesList.forEach { vacanciesPage->
                ParseSinglePageRespond (vacanciesPage,
                    {jobCardsParsedList-> vacanciesJobCardsList.addAll(jobCardsParsedList)},
                    {pagesParsedCounter -= 1})

                pagesParsedCounter += 1
            }

            needToParseEachVacancy = false
            pagesParsedCounterStatus = true
        }


        if (pagesParsedCounter == 0 && pagesParsedCounterStatus){
            Log.d("MyTag", "Its Parsed !")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun ParseSinglePageRespond(rawHtml: String,
                                       returnParsedVacancies:(vacanciesList: MutableList<JobCard>)-> Unit,
                                       finishParsing:()-> Unit){

        var needToCollectVacanciesLinks by remember { mutableStateOf<Boolean>(true) }
        var needToParseAllVacancies by remember { mutableStateOf<Boolean>(false) }
        var parsingByPagesHasFinished by remember { mutableStateOf<Boolean>(false) }
        var parsingByJobCardsFinished by remember { mutableStateOf<Boolean>(false) }

        val vacanciesQueryesList = remember { mutableStateListOf<String>() }
        val vacanciesHtmlPagesList = remember { mutableStateListOf<String>() }

        val foundedJobsCardsList = remember { mutableStateListOf<JobCard>() }


        if (needToCollectVacanciesLinks) {
            val document = Ksoup.parse(rawHtml)

            val vacanciesBoxElement = document.selectFirst("alliance-jobseeker-mobile-vacancies-list:nth-child(2) > div:nth-child(1)")
            val vacanciesListElement = vacanciesBoxElement?.select("alliance-vacancy-card-mobile")

            var vacancyQuery = ""

            if (vacanciesListElement != null){
                vacanciesListElement.forEach { vacancy ->
                    vacancyQuery = vacancy.child(0).attr("href")

                    if (!vacancyQuery.isBlank()) {
                        vacanciesQueryesList.add(vacancyQuery)
                    }
                    needToParseAllVacancies = true
                }
            }

            needToCollectVacanciesLinks = false
        }

        //All of this stages wouldn't start to work if in upper logic document haven't successfully parsed
        if (!needToCollectVacanciesLinks && needToParseAllVacancies) {

            if (vacanciesQueryesList.isNotEmpty()){
                vacanciesQueryesList.forEach { currentVacancy ->
                    val vacancyQuery = jobQueryTemplate.format(rabotaUaUrl, currentVacancy)
                    GetParsedPage(vacancyQuery, { it -> vacanciesHtmlPagesList.add(it)})
                }
            }else{
                //In case when we don't have any vacancy link to parse
                Toast.makeText(appContext, R.string.errorDataLoading, Toast.LENGTH_SHORT).show()
            }
            needToParseAllVacancies = false
        }

        if (vacanciesQueryesList.size == vacanciesHtmlPagesList.size) {
            parsingByPagesHasFinished = true
        }

        if (parsingByPagesHasFinished && !parsingByJobCardsFinished) {
            runCatching {
                vacanciesHtmlPagesList.forEach { htmlVacancy ->

                    val vacancyJobCard = parseVacancyCard(htmlVacancy)
                    //Adding pared data to main container
                    foundedJobsCardsList.add(vacancyJobCard)
                }

                returnParsedVacancies(foundedJobsCardsList)

                //It means that all our parsing has been finished
                parsingByJobCardsFinished = true

            }.onFailure {
                Toast.makeText(appContext, R.string.errorJobCardLoading, Toast.LENGTH_SHORT).show()
            }

            //This value goes to upper hierarchy function
            finishParsing()
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
    fun GetParsedPage(urlQuery: String, returnPageInRawHtml:(htmlString: String)-> Unit) {

        var rawHtmlPage by remember { mutableStateOf<String>("") }

        var pageHasLoad by remember {mutableStateOf<Boolean>(false)}

        fun close_and_cleen_view(webView: WebView?){
            if (webView != null){
                (webView.parent as? ViewGroup)?.removeView(webView)

                webView.apply {

                    stopLoading()
                    clearHistory()
                    removeAllViews()
                    webChromeClient = null
                }

                webView.destroy()
            }

        }

        //Parsing screen is running in hide mode
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

                                    returnPageInRawHtml(rawHtmlPage)

                                    //Closing opened view
                                    withContext(Dispatchers.Main) {
                                        close_and_cleen_view(view)
                                    }
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
