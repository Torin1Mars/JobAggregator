package com.example.jobaggregator.Parsers

import android.content.Context
import android.graphics.DiscretePathEffect
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jobaggregator.ViewModels.RabotaUaParserViewModel
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.monthUa
import com.example.jobaggregator.supportingData.rabotaUaMaxParsedPagesInOnes
import com.example.jobaggregator.supportingData.rabotaUaParerRenderDelay
import com.example.jobaggregator.supportingData.rabotaUaRenderedVacancyPageByteSize
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject


class RabotaUaParser @Inject constructor(context : Context,
                                         rabotaUaViewModel: RabotaUaParserViewModel) {

    val appContext = context
    val rabotaUaParserViewModel = rabotaUaViewModel

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
            GetOneParsedPage(baseQuery, { it -> htmlStrRespond = it })
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

                   1 ->  NewParseSinglePageRespond(htmlStrRespond,
                       {jobCardsParsedList ->jobsCardsList.addAll(jobCardsParsedList)},
                       {vacancyParsingFinished = true})
                   else -> NewParseSeveralPagesRespond(pagesInRespond, baseQuery, htmlStrRespond, {jobCardsParsedList ->jobsCardsList.addAll(jobCardsParsedList)})
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


        var needCollectVacanciesOnFirstPage by remember { mutableStateOf<Boolean>(true) }

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

        if (needCollectVacanciesOnFirstPage){
            (totalPagesInRespond-1..totalPagesInRespond).forEach { page ->

                val currentQuery = String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, page)
                GetParsedPage(currentQuery, {it-> vacanciesPagesList.add(it)})
            }

            needCollectVacanciesOnFirstPage = false
        }

        if (vacanciesPagesList.size == 3){
            currentParsedPage = vacanciesPagesList[0]
            doingParsingByPages = true
        }

        if (doingParsingByPages){
            //Parsing page by page
            //Starting parsing from first element
            NewParseSinglePageRespond (currentParsedPage,
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

                    GetOneParsedPage(currentVacancyQuery, { it -> vacanciesHtmlPagesList.add(it);
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

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun NewParseSeveralPagesRespond(totalPagesInRespond:Int,
                                         queryInitialLink: String,
                                         initialPageRespond: String,
                                         returnParsedVacancies:(vacanciesList: MutableList<JobCard>)-> Unit) {


        val vacanciesMainPagesList = remember {mutableStateListOf<String>(initialPageRespond)}
        val vacanciesJobCardsList = remember {mutableStateListOf<JobCard>()}
        var currentParsedPage by remember {mutableStateOf<String>(initialPageRespond)}

        var currentParsedPageIndex by remember {mutableStateOf<Int>(1)} //Starting parse from second page

        var needToCollectMainVacanciesPages by remember { mutableStateOf<Boolean>(true) }
        var startingParsingByPages by remember { mutableStateOf<Boolean>(false) }
        var parsingHasFinished by remember { mutableStateOf<Boolean>(false) }

        fun startingParseNextPage(){
            //Need to check if this page not last

            if (currentParsedPageIndex == vacanciesMainPagesList.size-1){
                startingParsingByPages = false
                parsingHasFinished = true

            }else{
                Log.d("MyTag", "Starting next page")

                Log.d("MyTag", "Jobcards : " + vacanciesJobCardsList.size.toString())
                //Parsing Next Page
                currentParsedPageIndex += 1

                Log.d("MyTag", "Parsed "+ currentParsedPageIndex)
                currentParsedPage = vacanciesMainPagesList[currentParsedPageIndex]
            }
        }

        if (needToCollectMainVacanciesPages){
            CollectMainPages(totalPagesInRespond, queryInitialLink,
                {pagesList -> vacanciesMainPagesList.addAll(pagesList)})
        }

        if (vacanciesMainPagesList.size == totalPagesInRespond){
            Log.d("MyTag", "MainPages have been parsed")

            needToCollectMainVacanciesPages = false
            startingParsingByPages = true
        }

        //TODO Seams like we reaching 9 page and thread error appear here:
        if (startingParsingByPages){
            Log.d("MyTag", "Parsing main pages by Job Cards started")
            //Parsing page by page
            //Starting parsing from first element

            NewParseSinglePageRespond (currentParsedPage,
                returnParsedVacancies = {jobCardsParsedList -> vacanciesJobCardsList.addAll(jobCardsParsedList)},
                finishParsing = {startingParseNextPage()} )
        }

        if (parsingHasFinished){
            Log.d("MyTag", "Finished : ${vacanciesJobCardsList.size}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun NewParseSinglePageRespond(rawHtml: String,
                                       returnParsedVacancies:(vacanciesList: MutableList<JobCard>)-> Unit,
                                       finishParsing:()-> Unit){

        var needToCollectVacanciesLinks by remember { mutableStateOf<Boolean>(true) }
        var needToParseAllVacancies by remember { mutableStateOf<Boolean>(false) }
        var parsingByPagesHasFinished by remember { mutableStateOf<Boolean>(false) }
        var parsingByJobCardsFinished by remember { mutableStateOf<Boolean>(false) }

        val vacanciesQueryesList = remember { mutableStateListOf<String>() }
        val vacanciesHtmlPagesList = remember { mutableStateListOf<String>() }

        val foundedJobsCardsList = remember { mutableStateListOf<JobCard>() }

        var parsingFunctionsAreRunning by remember { mutableStateOf<Int?>(null) }

        fun resetToDefault(){
            needToCollectVacanciesLinks = true
            needToParseAllVacancies = false
            parsingByPagesHasFinished = false
            parsingByJobCardsFinished = false
            parsingFunctionsAreRunning = null

            vacanciesQueryesList.clear()
            vacanciesHtmlPagesList.clear()
            foundedJobsCardsList.clear()
        }

        fun parseVacanciesJobCards() {
            if (vacanciesHtmlPagesList.isNotEmpty()){
                runCatching {
                    vacanciesHtmlPagesList.forEach { htmlVacancy ->

                        val vacancyJobCard = parseVacancyCard(htmlVacancy)
                        //Adding pared data to main container
                        foundedJobsCardsList.add(vacancyJobCard)
                    }

                }.onFailure {
                    Toast.makeText(appContext, R.string.errorJobCardParsing, Toast.LENGTH_SHORT).show()
                }
            }

            //All our parsing has been finished
            returnParsedVacancies(foundedJobsCardsList)
            resetToDefault()

            Log.d("MyTag", "Page with vacancies has been parsed")

            //This is upper hierarchy function
            finishParsing()
        }

        // TODO its need to add in this function timer which will be control maximum parsing time here


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
            }

            needToCollectVacanciesLinks = false

            if (vacanciesQueryesList.isNotEmpty()){
                needToParseAllVacancies = true
            }
        }

        if (needToParseAllVacancies) {
            //Changing from initial null to 0
            parsingFunctionsAreRunning = (parsingFunctionsAreRunning ?:0)

            runCatching {
                Column(modifier = Modifier.height(0.dp).width(0.dp)) {
                    (vacanciesQueryesList).forEach { vacancyQuery ->
                        parsingFunctionsAreRunning = parsingFunctionsAreRunning?.inc()

                        val currentVacancyFullLink = rabotaUaUrl + vacancyQuery
                        GetOneParsedPage (currentVacancyFullLink,
                            {parsedVacancyPage->vacanciesHtmlPagesList.add(parsedVacancyPage);
                                parsingFunctionsAreRunning = parsingFunctionsAreRunning?.dec()})
                    }
                }

            }.onFailure {
                Toast.makeText(appContext, R.string.errorJobCardLoading, Toast.LENGTH_SHORT).show()
            }

            needToParseAllVacancies = false
        }

        if (parsingFunctionsAreRunning == 0 && !needToParseAllVacancies) {
            parseVacanciesJobCards()

            parsingFunctionsAreRunning = null
        }
    }

    @Composable
    fun CollectMainPages(totalPagesInRespond: Int, queryInitialLink: String,
                         returnParsedPages:(returningPagesList: List<String>)-> Unit) {

        var templist = remember { MutableList(totalPagesInRespond){ "" }.toMutableStateList() }

        var needToParse by remember { mutableStateOf<Boolean>(true) }
        val queryTemplate by remember { mutableStateOf<String>("%s/params;page=%d") }
        val vacanciesMainPagesList =  remember { mutableStateListOf<String>() }

        var currentRunningParsingIndex by remember { mutableStateOf<Int>(0) }
        var currentParsedPageIndex by remember { mutableStateOf<Int>(1) } //We are starting to collect main pages from second page

        fun resetValues(){
            needToParse = false
            vacanciesMainPagesList.clear()
            currentRunningParsingIndex = 0
            currentParsedPageIndex = 1
        }

        @Composable
        fun StartParsingNewPagesPull () {
            /*LazyColumn(modifier = Modifier.height(1.dp).width(1.dp)) {

                    items (count = templist.size, key = {pageIndex -> pageIndex }){pageIndex->
                        val currentQuery = String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, pageIndex)

                        Log.d("MyTag", "Querry sended " + pageIndex)
                        GetOneParsedPage (currentQuery,
                            {parsedVacancyPage->vacanciesMainPagesList.add(parsedVacancyPage);
                                currentRunningParsingIndex = currentRunningParsingIndex.dec();
                                Log.d("MyTag", "Parsed " + currentQuery)}
                        )
                    }
                }
            }*/

            Column(modifier = Modifier.height(0.dp).width(0.dp)) {
                thismainloop@ for (pageIndex in currentParsedPageIndex + 1..totalPagesInRespond) {

                    if (currentRunningParsingIndex < rabotaUaMaxParsedPagesInOnes) {
                        val currentQuery =
                            String.format(locale = Locale.US, format = queryTemplate, queryInitialLink, pageIndex)

                        GetOneParsedPage(
                            currentQuery,
                            { parsedVacancyPage ->
                                vacanciesMainPagesList.add(parsedVacancyPage);
                                currentRunningParsingIndex = currentRunningParsingIndex.dec();
                                Log.d("MyTag", "Parsed " + currentQuery)
                            }
                        )

                        currentParsedPageIndex = currentParsedPageIndex.inc()
                        currentRunningParsingIndex = currentRunningParsingIndex.inc()
                    } else {
                        break@thismainloop
                    }
                }

            }
        }


        if (needToParse && currentRunningParsingIndex == 0 && vacanciesMainPagesList.size != totalPagesInRespond ){
            StartParsingNewPagesPull()
        }

        if (currentRunningParsingIndex == 0 && vacanciesMainPagesList.size == totalPagesInRespond-1 ){
          //Parsing has finished
            returnParsedPages (vacanciesMainPagesList)

            resetValues()
        }
    }

    @Composable
    fun GetOneParsedPage(urlQuery: String, returnPageInRawHtml:(htmlString: String)-> Unit) {

        var rawHtmlPage = remember { mutableStateOf<String>("") }

        val desktopUserAgent by remember { mutableStateOf<String>("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") }

        var pageHasBeenLoad by remember { mutableStateOf<Boolean>(false) }

        fun closeWebView(webView: WebView?){
            //returning to default settings
            rawHtmlPage.value = ""
            pageHasBeenLoad = false

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

        //TODO Seams like this approach make sense and now it's need to improve this logic in side special class VebParser Producer
        /*
        DisposableEffect (Unit){

            val checkingThread = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val stringPageRespond = rawHtmlPage
                    val byteStringSize = stringPageRespond.toByteArray().size

                    if (byteStringSize >= rabotaUaRenderedVacancyPageByteSize) {
                        Log.d("MyTag", rawHtmlPage.encodeToByteArray().size.toString())
                        pageHasBeenLoad = true

                        //Adding this vacancy http string link to this general html page
                        val vacancyId = urlQuery.substringAfter("robota.ua//").removeSuffix("/")
                        rawHtmlPage = rawHtmlPage + "ORIGIN VACANCY ID =$vacancyId"

                        returnPageInRawHtml(rawHtmlPage)

                        break
                    }
                    delay(100L)
                }
            }

            onDispose {
                //checkingThread.cancel()
                //closeWebView(currentWebView)
            }

            }*/


        /*
        if (!pageHasBeenLoad) {

            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val stringPageRespond = rawHtmlPage
                    val byteStringSize = stringPageRespond.toByteArray().size

                    if (byteStringSize >= rabotaUaRenderedVacancyPageByteSize) {
                        Log.d("MyTag", rawHtmlPage.encodeToByteArray().size.toString())
                        pageHasBeenLoad = true

                        //Adding this vacancy http string link to this general html page
                        val vacancyId = urlQuery.substringAfter("robota.ua//").removeSuffix("/")
                        rawHtmlPage = rawHtmlPage + "ORIGIN VACANCY ID =$vacancyId"

                        returnPageInRawHtml(rawHtmlPage)

                        withContext(Dispatchers.Main) {
                            closeWebView(currentWebView)
                        }

                        break
                    }
                    delay(1000L)
                }
            }
        }*/

        //Parsing screen is running in hide mode
        Box(modifier = Modifier.height(0.dp).width(0.dp)) {
            AndroidView(factory = { context ->

                val webView = WebView(context)

                webView.apply {
                    settings.userAgentString = desktopUserAgent

                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    //TODO Its nice and its need to add here boolean switcher
                    rabotaUaParserViewModel.watchOnCurrentWebView(rawHtmlPage, webView)

                    /*addJavascriptInterface(
                        WebPageInterface2 (onWebPageReceived = { html ->
                            rawHtmlPage = html}, {returnPageInRawHtml (rawHtmlPage);
                            close_current_view(currentWebView) }),
                        "AndroidInterface"
                    )*/

                    addJavascriptInterface(
                        WebPageInterface (onWebPageReceived = { html ->
                            rawHtmlPage.value = html}),
                        "AndroidInterface"
                    )

                    webViewClient = object : WebViewClient() {

                        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    private inner class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
        @JavascriptInterface
        fun getHtml(page: String) {
            onWebPageReceived(page)
        }

    }

}
