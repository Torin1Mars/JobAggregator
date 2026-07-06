package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.dateFormat
import com.example.jobaggregator.supportingData.monthUa
import com.example.jobaggregator.supportingData.rabotaUaFullyRenderedVacancyPageLenght
import com.example.jobaggregator.supportingData.rabotaUaMaxRuningWebViewsInOnes
import com.example.jobaggregator.supportingData.rabotaUaParserRenderDelay
import com.example.jobaggregator.supportingData.rabotaUaUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONTokener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach


class WebViewPool(context: Context) {
    private val poolSize: Int = rabotaUaMaxRuningWebViewsInOnes

    // Use applicationContext — these WebViews live as long as the pool does,
    // so holding an Activity context here would leak the Activity.
    private val appContext = context.applicationContext

    // The channel IS the pool: capacity = poolSize, pre-filled with WebViews.
    // receive() = "borrow one" (suspends if all 5 are checked out).
    // send()    = "return it".
    private val pool = Channel<WebView>(capacity = poolSize)

    private val initMutex = Mutex()
    private var initialized = false

    /** Creates the WebViews once. Safe to call multiple times — only runs once. */
    suspend fun warmUp() = initMutex.withLock {
        //if (pool.equals(10)) return@withLock
        if (initialized) return@withLock

        withContext(Dispatchers.Main) {
            repeat(poolSize) {
                pool.trySend(createWebView())
            }
        }
        initialized = true
        Log.d("MyTag", "Warmed up $poolSize WebViews")
    }

    private fun createWebView(): WebView = WebView(appContext).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    suspend fun renderPage(
        url: String
    ): String {
        if (!initialized) warmUp()

        var webView = pool.receive() // suspends here if all views are busy

        suspend fun resetForRetry() {
            withContext(Dispatchers.Main) {
                webView.stopLoading()

                webView.clearCache(false)
                webView.loadUrl("about:blank")
                webView.webViewClient = object : WebViewClient() {}
            }
        }

        return try {
            var renderedPage = ""

            while (renderedPage.isBlank()) {
                renderedPage = try {
                    withTimeout(rabotaUaParserRenderDelay) {
                        fetchHtml(webView, url, rabotaUaFullyRenderedVacancyPageLenght)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.d("MyTag", "Timeout acceded!")
                    ""
                } catch (e: Exception) {
                    Log.d("MyTag", "Other exception exceeded!")
                    ""
                }

                if (renderedPage.isNotEmpty()) break // success, stop retrying
                else resetForRetry()
            }

            renderedPage
        } finally {
            // Reset the instance before it goes back in the pool — detach the
            // listener so a late/dangling JS callback from this load can't
            // fire into the next borrower's continuation.
            withContext(Dispatchers.Main) {
                webView.stopLoading()
                webView.webViewClient = object : WebViewClient() {}
            }

            pool.send(webView) // hand it back — wakes up the next waiter, if any
        }
    }

    private suspend fun fetchHtml(
        webView: WebView,
        url: String,
        minHtmlLenght: Int
    ): String = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String) {
                        view.evaluateJavascript(
                            "(function(){return document.documentElement.outerHTML;})();"
                        ) { result ->
                            if (result.length > minHtmlLenght) {

                                Log.d("MyTag", "Page rendered ${result.length}")

                                val html = unescapeJsString(result)
                                if (continuation.isActive){
                                    continuation.resume(html) {}
                                }
                            }

                            /*else: page hasn't fully rendered yet — do nothing and
                            let the withTimeout() in renderPage() catch a stuck page
                            instead of hanging forever*/
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (continuation.isActive) {
                            /*continuation.resumeWithException(
                                Exception("WebView load failed: ${error.description}")
                            )*/

                            Log.d("MyTag", "View received error")
                            webView.stopLoading()
                            webView.webViewClient = object : WebViewClient() {}

                            continuation.resume("") {}
                            continuation.cancel(null)

                        }
                    }
                }

                /*continuation.invokeOnCancellation {
                    //webView.reload()
                    //webView.stopLoading()
                }*/

                webView.loadUrl(url)
            }
    }

    /** Call when you're truly done (e.g. ViewModel.onCleared) to free native resources. */
    suspend fun shutdown() = withContext(Dispatchers.Main) {
        repeat(poolSize) {
            val webView = pool.tryReceive().getOrNull() ?: return@repeat
            destroyWebViewSafely(webView)

        }
        pool.close()

        Log.d("MyTag", "Pool was closed !")
    }

    private suspend fun destroyWebViewSafely(webView: WebView) {
        withContext(Dispatchers.Main.immediate) {
            webView.apply {
                stopLoading()
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")

                webViewClient = object : WebViewClient() {}
                removeAllViews()
                destroy()
            }
        }
    }
}

private fun unescapeJsString(raw: String): String = try {
    JSONTokener(raw).nextValue() as String
} catch (e: Exception) {
    raw.trim().removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun parseVacanciesJobCards(
    pool: WebViewPool,
    jobCardsQueries: List<String>
): List<JobCard> = coroutineScope {
    val parsedVacancies = mutableListOf<JobCard>()


    (0..jobCardsQueries.size - 1).map { pageIndex ->
        async {
            val currentPageQueryLink = jobCardsQueries[pageIndex]

            if (!currentPageQueryLink.isBlank()) {
                val html = pool.renderPage(currentPageQueryLink)
                val currentVacancyCard: JobCard? = parseVacancyCard(html)

                currentVacancyCard?.let { parsedVacancies.add(currentVacancyCard) }

                Log.d("MyTag", "Parsed vacancies : ${parsedVacancies.size}")
            }
        }
    }.awaitAll()

    return@coroutineScope parsedVacancies
}

suspend fun checkHowManyPagesInRespond(
    searchingUrl: String,
    pool: WebViewPool
): Int {
    var pagesCount = 0
    pagesCount = coroutineScope {
        var count :Int = 0

        Log.d("MyTag", searchingUrl)
        val html = pool.renderPage(searchingUrl)

        try{
            count = getPagesCount(html)
        }catch (e: Exception){
            Log.d("MyTag", "Error while checking pages count")
            Log.d("MyTag", e.message.toString())
        }

        return@coroutineScope count
    }

    return pagesCount
}

suspend fun parseJobCardsIds(
    pool: WebViewPool,
    searchUrl: String,
    totalPages: Int
): List<String>{
    val parsedVacanciesIdsList = coroutineScope {
        val parsedVacanciesOnPage = mutableListOf<String>()

        (1..totalPages).forEach { currentPageIndex ->
            async {
                var renderedRawHtml = ""

                suspend fun renderHtmlPage(index: Int) {
                    val url = buildPageUrl(searchUrl, index)
                    renderedRawHtml = pool.renderPage(url)
                }

                renderHtmlPage(currentPageIndex)

                if (renderedRawHtml.isBlank()) {

                    Log.d("MyTag", "We got here ")
                    repeat(1) { renderHtmlPage(currentPageIndex) }
                } else {
                    parsedVacanciesOnPage.addAll(collectVacanciesLinksOnPage(renderedRawHtml))
                }
                return@async parsedVacanciesOnPage
            }

        }

        return@coroutineScope parsedVacanciesOnPage
    }
    return parsedVacanciesIdsList
}

fun collectVacanciesLinksOnPage (rawHtmlRespond: String): List <String>{

    val vacanciesIdCardsList = mutableListOf<String>()
    val jobVacancyFullQueryTemplate = "%s%s"

    val document = Ksoup.parse(rawHtmlRespond)
    val vacanciesBoxElement = document.selectFirst("alliance-jobseeker-mobile-vacancies-list:nth-child(2) > div:nth-child(1)")
    val vacanciesListElement = vacanciesBoxElement?.select("alliance-vacancy-card-mobile")

    var vacancyQuery = ""

    vacanciesListElement?.forEach { vacancy ->
        vacancyQuery = vacancy.child(0).attr("href")

        if (!vacancyQuery.isBlank()) {
            vacanciesIdCardsList.add(String.format(jobVacancyFullQueryTemplate,rabotaUaUrl, vacancyQuery))
        }
    }

    return vacanciesIdCardsList
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

/////////////////Supporting functions /////////////////////////

private fun buildPageUrl(searchUrl: String, pageNumber: Int): String {
    if (pageNumber <= 1) return searchUrl
    val separator = if (searchUrl.contains("?")) "&" else "?"
    return "$searchUrl${separator}page=$pageNumber"
}

private fun parseVacanciesCardsIdFromHtml(html: String): List<String> {
    val htmlDoc = Ksoup.parse(html)

    val vacanciesListElement = htmlDoc?.select("alliance-vacancy-card-mobile")

    val foundedVacanciesQueriesList  = mutableListOf<String>()

    var vacancyQuery: String = ""

    if (vacanciesListElement != null){
        vacanciesListElement.forEach { vacancy ->
            vacancyQuery = vacancy.child(0).attr("href")

            vacancyQuery.let { it-> foundedVacanciesQueriesList.add(it)}
        }
    }

    return foundedVacanciesQueriesList
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseVacancyCard(jobHtmlPage: String): JobCard? {

    fun convertReceivedDate(receivedData: String?): LocalDate {

        if (!receivedData.isNullOrEmpty()){
            val day  = receivedData.substringBefore(" ").toInt()

            var monthText = receivedData.substringAfter(" ").substringBefore(" ")

            val month = monthUa.indexOf(monthText)

            val year = receivedData.substringAfter(" ").substringAfter(" ").toInt()

            return LocalDate.of(year,month,day)
        }else
            return LocalDate.of(2000, 1, 1)

    }

    fun collectJobCardData(parsedDocument: Document, dataElement : Element?): JobCard{

        var rawDate = dataElement?.let { it.text() }

        val date = convertReceivedDate(rawDate)

        val formater = DateTimeFormatter.ofPattern(dateFormat)
        val dateStr = date.format(formater).toString()

        val jobTitle: String = parsedDocument.selectFirst("[data-id=vacancy-title]")?.text() ?: "Empty"

        val companyLinkElement = parsedDocument.selectFirst(
            "alliance-hot-vacancies-list-mobile a[href^=/company]")

        val IdElement = parsedDocument.selectFirst("alliance-shared-footer-languages a[href^=/company]")
        var jobId = ""
        IdElement?.let {jobId = it.attr("href") }

        val jobDescription: String? = parsedDocument.selectFirst("#description-wrap")?.text() ?: "Empty"

        val jobLocation =
            parsedDocument.selectFirst("[data-id=vacancy-city]")?.parent()?.text()?:"No information"

        val jobCompany: String? =
            parsedDocument.selectFirst("a[href^=/company] > span")?.text()?:"No information"

        val jobSalary: String? =
            parsedDocument.selectFirst("[data-id=vacancy-salary-from-to]")?.text() ?: "Empty"

        var jobUrl = ""
        if (jobId.isNotBlank()){
            jobUrl = rabotaUaUrl+jobId
        }

        val card = JobCard(
            jobIdOnWebsite = jobId,
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

    try {
        val document = Ksoup.parse(jobHtmlPage)
        val element = document.selectFirst("span.santa-text-white:nth-child(1)")

        val parsedJobCard  = collectJobCardData(document, element)

        return parsedJobCard

    }catch (e: Exception){
        return null
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////


