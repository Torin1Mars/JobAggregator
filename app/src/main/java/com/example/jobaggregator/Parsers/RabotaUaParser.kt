package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


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

        val vacanciesPagesList = remember {mutableStateListOf<String>(initialPageRespond)}
        val vacanciesJobCardsList = remember {mutableStateListOf<JobCard>()}
        var currentParsedPage by remember {mutableStateOf<String>("")}

        var currentParsedPageIndex by remember {mutableStateOf<Int>(0)}

        var doingParsingByPages by remember { mutableStateOf<Boolean>(false) }
        var parsingHasFinished by remember { mutableStateOf<Boolean>(false) }

        //Changing
        var needCollectVacanciesGeneralPages by remember { mutableStateOf<Boolean>(true) }

        fun startingParseNextPage(){
            //Need to check if this page not last

            if (currentParsedPageIndex == vacanciesPagesList.size-1){
                doingParsingByPages = false
                parsingHasFinished = true

            }else{
                Log.d("MyTag", "Starting next page")

                Log.d("MyTag", "Jobcards : " + vacanciesJobCardsList.size.toString())
                //Parsing Next Page
                currentParsedPageIndex += 1

                Log.d("MyTag", "Parsed "+ currentParsedPageIndex)
                currentParsedPage = vacanciesPagesList[currentParsedPageIndex]
            }
        }

        if (needCollectVacanciesGeneralPages){
            (totalPagesInRespond-1..totalPagesInRespond).forEach { page ->

                val currentQuery = String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, page)
                GetParsedPage(currentQuery, {it-> vacanciesPagesList.add(it)})
            }

            needCollectVacanciesGeneralPages = false
        }

        if (vacanciesPagesList.size == 3){
            currentParsedPage = vacanciesPagesList[0]
            doingParsingByPages = true
        }

        if (doingParsingByPages){
            //Parsing page by page
            //Starting parsing from first element
            ParseSinglePageRespond(currentParsedPage,
                returnParsedVacancies = {jobCardsParsedList -> vacanciesJobCardsList.addAll(jobCardsParsedList)},
                finishParsing = {startingParseNextPage()} )
        }

        if (parsingHasFinished){
            Log.d("MyTag", "Finished : ${vacanciesJobCardsList.size}")
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
        var vacanciesQueryesListMiddle by remember { mutableStateOf<Int>(0) }
        val vacanciesHtmlPagesList = remember { mutableStateListOf<String>() }

        val foundedJobsCardsList = remember { mutableStateListOf<JobCard>() }

        var parsingFunctionsRunning by remember { mutableStateOf<Int?>(null) }

        fun resetToDefault(){
            needToCollectVacanciesLinks = true
            needToParseAllVacancies = false
            parsingByPagesHasFinished = false
            parsingByJobCardsFinished = false
            parsingFunctionsRunning = null

            vacanciesQueryesList.clear()
            vacanciesHtmlPagesList.clear()
            foundedJobsCardsList.clear()
        }


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
                }

                needToParseAllVacancies = true
                vacanciesQueryesListMiddle = vacanciesQueryesList.size.floorDiv(2)

                Log.d("MyTag", vacanciesQueryesList.size.toString())
                Log.d("MyTag", vacanciesQueryesListMiddle.toString())

            }

            needToCollectVacanciesLinks = false
        }

        //All of this stages wouldn't start to work if in upper logic document haven't successfully parsed
        if (!needToCollectVacanciesLinks && needToParseAllVacancies) {

            if (vacanciesHtmlPagesList.isEmpty() && parsingFunctionsRunning == null){
                Log.d("MyTag", "Starting first part")
                //First half
                (0.. vacanciesQueryesListMiddle-1).forEach { querryIndex->
                    val currentVacancyQuery = jobQueryTemplate.format(rabotaUaUrl, vacanciesQueryesList[querryIndex])
                    //Increasing on one
                    parsingFunctionsRunning = (parsingFunctionsRunning ?:0) +1

                    NewGetParsedPage(currentVacancyQuery, { it -> vacanciesHtmlPagesList.add(it);
                        parsingFunctionsRunning = parsingFunctionsRunning?.dec();
                        Log.d("MyTag", "Vacancy parsed" +"-" + parsingFunctionsRunning.toString())})

                    /*
                    GetParsedPage(currentVacancyQuery, { it -> vacanciesHtmlPagesList.add(it);
                        parsingFunctionsRunning = parsingFunctionsRunning?.dec();
                        Log.d("MyTag", "Vacancy parsed" +"-" + parsingFunctionsRunning.toString() )})*/

                }

            }else if (vacanciesHtmlPagesList.size == vacanciesQueryesListMiddle && parsingFunctionsRunning == 0 ){

                Log.d("MyTag", "Starting second part")
                //Second half
                (vacanciesQueryesListMiddle .. vacanciesQueryesList.size-1).forEach { querryIndex->
                    val currentVacancyQuery = jobQueryTemplate.format(rabotaUaUrl, vacanciesQueryesList[querryIndex])
                    parsingFunctionsRunning = parsingFunctionsRunning?.inc()

                    GetParsedPage(currentVacancyQuery, { it -> vacanciesHtmlPagesList.add(it);
                        parsingFunctionsRunning = parsingFunctionsRunning?.dec();
                        Log.d("MyTag", "Vacancy parsed" +"-" + parsingFunctionsRunning.toString())})
                }
            }
        }

        if (parsingFunctionsRunning == 0 && vacanciesHtmlPagesList.size > vacanciesQueryesListMiddle+1) {
            parsingByPagesHasFinished = true
            Log.d("MyTag", "parsing By Pages Has Finished")
        }

        if (parsingByPagesHasFinished && !parsingByJobCardsFinished) {
            runCatching {
                vacanciesHtmlPagesList.forEach { htmlVacancy ->

                    val vacancyJobCard = parseVacancyCard(htmlVacancy)
                    //Adding pared data to main container
                    foundedJobsCardsList.add(vacancyJobCard)
                }

            }.onFailure {
                Toast.makeText(appContext, R.string.errorJobCardLoading, Toast.LENGTH_SHORT).show()
            }

            returnParsedVacancies(foundedJobsCardsList)

            //It means that all our parsing has been finished
            parsingByJobCardsFinished = true
            resetToDefault()

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

        var renderNotStarted by remember {mutableStateOf<Boolean>(false)}
        var parsingTimeExpired  by remember {mutableStateOf<Boolean>(false)}

        /*LaunchedEffect(Unit) {
            delay(rabotaUaParerRenderDelay+1.seconds) // Wait for 5seconds
            parsingTimeExpired = true // Update state to trigger recomposition
        }*/

        fun close_and_cleen_view(webView: WebView?){
            rawHtmlPage = ""
            renderNotStarted = false
            parsingTimeExpired = false

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

                            if (!renderNotStarted){

                                if (parsingTimeExpired){
                                    Log.d("MyTag", "TIME EXPIRED !!!")
                                    close_and_cleen_view(view)

                                    //Return Empty page
                                    returnPageInRawHtml(rawHtmlPage)
                                }

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
                            renderNotStarted = true
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
                            view?.loadUrl("javascript:window.AndroidInterface.getHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")}


                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            //If render got failure
                            val didCrash = detail?.didCrash() ?: false

                            view?.let {
                                val parent = it.parent as? android.view.ViewGroup
                                parent?.removeView(it)
                                close_and_cleen_view(it)
                            }

                            returnPageInRawHtml(rawHtmlPage)
                            return true
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


    @Composable
    fun NewGetParsedPage(urlQuery: String, returnPageInRawHtml:(htmlString: String)-> Unit) {

        var rawHtmlPage by remember { mutableStateOf<String>("") }

        val desktopUserAgent by remember { mutableStateOf<String>("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") }
        var currentWebView by remember { mutableStateOf<WebView?>(null) }

        var renderHasFinished by remember { mutableStateOf<Boolean>(false) }

        //TODO okay , and here its need to add producer thread control

        fun close_current_view(webView: WebView?){
            rawHtmlPage = ""
            renderHasFinished = false

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

                val webView = WebView(context)
                currentWebView = webView

                webView.apply {
                    settings.userAgentString = desktopUserAgent

                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    addJavascriptInterface(
                        WebPageInterface2 (onWebPageReceived = { html ->
                            rawHtmlPage = html}, {returnPageInRawHtml (rawHtmlPage);
                            close_current_view(currentWebView) }),
                        "AndroidInterface"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            view?.postDelayed({
                                view.evaluateJavascript("javascript:window.AndroidInterface.getHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');", null)
                            },rabotaUaParerRenderDelay.toLong(DurationUnit.MILLISECONDS))
                        }
                    }
                    loadUrl(urlQuery)
                }
            })

        }
    }

    private inner class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
        @JavascriptInterface
        fun getHtml(page: String) {
            onWebPageReceived(page)
            Log.d("MyTag", "Seams like its parsed1")
        }
    }

    private inner class WebPageInterface2(private val onWebPageReceived: (String) -> Unit, private val closeWebView:()-> Unit) {
        @JavascriptInterface
        fun getHtml(page: String) {
            onWebPageReceived(page)

            closeWebView()
            Log.d("MyTag", "Seams like its parsed2")
        }
    }


}
