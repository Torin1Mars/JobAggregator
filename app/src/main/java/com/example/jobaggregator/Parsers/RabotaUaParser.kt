package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.jobaggregator.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.monthUa
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject


class RabotaUaParser @Inject constructor(context: Context,
                                         rabotaUaViewModel: WebViewProducerViewModel) {

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

        val webViewProducer = RabotaUaWebViewProducer(appContext)

        if (needToCheckPagesCount) {
            webViewProducer.ProduseUserQuerry(mutableListOf<String>(baseQuery),{renderedPagesList ->htmlStrRespond = renderedPagesList[0]})
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
                Log.d("MyTag", "Jobcards : " + vacanciesJobCardsList.size.toString())
                //Parsing Next Page
                currentParsedPageIndex += 1

                Log.d("MyTag", "Starting next page :")

                Log.d("MyTag", "Parsed "+ currentParsedPageIndex)
                currentParsedPage = vacanciesMainPagesList[currentParsedPageIndex]
            }
        }

        if (needToCollectMainVacanciesPages){
            CollectMainPages(totalPagesInRespond, queryInitialLink,
                {pagesList -> vacanciesMainPagesList.addAll(pagesList)})
        }

        if (vacanciesMainPagesList.size == totalPagesInRespond){
            needToCollectMainVacanciesPages = false
            startingParsingByPages = true
        }

        if (startingParsingByPages){
            Log.d("MyTag", "Parsing main pages by each page started")
            //Parsing page by page
            //Starting parsing from first element

            //TODO Continuing here :
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

        val jobVacancyFullQueryTemplate = "%s%s"

        var needToCollectVacanciesLinks by remember { mutableStateOf<Boolean>(true) }
        var needToStartParseAllVacancies by remember { mutableStateOf<Boolean>(false) }
        var parsingByPagesHasFinished by remember { mutableStateOf<Boolean>(false) }
        var parsingByJobCardsFinished by remember { mutableStateOf<Boolean>(false) }

        val vacanciesIdQueriesList = remember { mutableStateListOf<String>() }
        val renderedHtmlPagesList = remember { mutableStateListOf<String>() }

        val foundedJobsCardsList = remember { mutableStateListOf<JobCard>() }

        val webViewProducer = RabotaUaWebViewProducer(appContext)

        fun resetToDefault(){
            needToCollectVacanciesLinks = true
            needToStartParseAllVacancies = false
            parsingByPagesHasFinished = false
            parsingByJobCardsFinished = false

            vacanciesIdQueriesList.clear()
            renderedHtmlPagesList.clear()
            foundedJobsCardsList.clear()
        }

        fun parseVacanciesJobCards() {
            if (renderedHtmlPagesList.isNotEmpty()){
                runCatching {
                    renderedHtmlPagesList.forEach { htmlVacancy ->

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

            //This function sending parsed vacancies to upper hierarchy
            finishParsing()
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
                        vacanciesIdQueriesList.add(String.format(jobVacancyFullQueryTemplate,rabotaUaUrl, vacancyQuery))
                    }
                }
            }

            needToCollectVacanciesLinks = false

            if (vacanciesIdQueriesList.isNotEmpty()){
                needToStartParseAllVacancies = true
            }
        }

        if (needToStartParseAllVacancies) {
            Log.d("MyTag", "Loading vacancies page started")
            webViewProducer.ProduseUserQuerry(vacanciesIdQueriesList, {renderedPagesList ->renderedHtmlPagesList.addAll(renderedPagesList) })
            needToStartParseAllVacancies = false
        }

        if (renderedHtmlPagesList.size == vacanciesIdQueriesList.size) {
            parseVacanciesJobCards()
        }

    }

    @Composable
    fun CollectMainPages(totalPagesInRespond: Int, queryInitialLink: String,
                         returnParsedPages:(returningPagesList: List<String>)-> Unit) {

        val webViewProducer = remember { RabotaUaWebViewProducer(appContext) }

        val maximumParsingViews by remember { mutableStateOf<Int>(4) }
        var rendersAreRunning by remember { mutableStateOf<Boolean>(false) }

        val queryTemplate by remember { mutableStateOf<String>("%s/params;page=%d") }
        val vacanciesMainPagesList = remember { mutableStateListOf<String>() }

        val tempList = remember { mutableStateListOf<String>() }

        //Starting to collect main pages from second page
        var lastRunningParsingIndex by remember { mutableStateOf<Int>(2) }

        fun resetValues() {
            vacanciesMainPagesList.clear()
            rendersAreRunning = false
            lastRunningParsingIndex = 1
        }

        @Composable
        fun StartParsingNewPagesPull() {
            val currentPullQueriesList = mutableListOf<String>()

            (0..maximumParsingViews).forEach {

                if (lastRunningParsingIndex > totalPagesInRespond) {
                    //Do nothing
                } else {
                    val pageQuery = String.format(queryTemplate, queryInitialLink, lastRunningParsingIndex)
                    tempList.add(pageQuery)

                    currentPullQueriesList.add(pageQuery)

                    lastRunningParsingIndex++
                }
            }

            //Sending queries to rendering
            if (currentPullQueriesList.isNotEmpty()) {
                webViewProducer.ProduseUserQuerry(currentPullQueriesList, { renderedPagesList ->
                    vacanciesMainPagesList.addAll(renderedPagesList);
                    rendersAreRunning = false
                })

                rendersAreRunning = true
            }
        }

        if (!rendersAreRunning && lastRunningParsingIndex < totalPagesInRespond) {
            StartParsingNewPagesPull()
        }

        if (lastRunningParsingIndex > totalPagesInRespond && !rendersAreRunning) {
            //Parsing has finished
            returnParsedPages(vacanciesMainPagesList)
            resetValues()

            Log.d("MyTag", "Parsing main pages have finished " + vacanciesMainPagesList.size+1)
        }
    }


}
