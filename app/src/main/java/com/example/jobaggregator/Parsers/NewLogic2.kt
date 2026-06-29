package com.example.jobaggregator.Parsers

import android.content.Context
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.jobaggregator.ViewModels.WebViewViewModel
import com.example.jobaggregator.supportingData.rabotaUaFullyRenderedVacancyPageLenght
import com.example.jobaggregator.supportingData.rabotaUaMaxRuningWebViewsInOnes
import com.example.jobaggregator.supportingData.rabotaUaParerRenderDelay
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONTokener
import kotlin.collections.forEach
import kotlin.coroutines.resumeWithException


class WebViewPool(
    context: Context
) {
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

    /** Creates the 5 WebViews once. Safe to call multiple times — only runs once. */
    suspend fun warmUp() = initMutex.withLock {
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
        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    /**
     * Borrows a WebView from the pool, loads [url], waits for the rendered
     * HTML to exceed [minHtmlBytes], and returns it as a String.
     * Always returns the WebView to the pool, even on error/timeout/cancellation.
     */
    suspend fun renderPage(
        url: String
    ): String {
        if (!initialized) warmUp()

        val webView = pool.receive() // suspends here if all 5 are busy
        return try {
            withTimeout(rabotaUaParerRenderDelay) {
                fetchHtml(webView, url, rabotaUaFullyRenderedVacancyPageLenght)
            }
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
                        // else: page hasn't fully rendered yet — do nothing and
                        // let the withTimeout() in renderPage() catch a stuck page
                        // instead of hanging forever.
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            Exception("WebView load failed: ${error.description}")
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                webView.stopLoading()
            }

            webView.loadUrl(url)
        }
    }

    /** Call when you're truly done (e.g. ViewModel.onCleared) to free native resources. */
    suspend fun shutdown() = withContext(Dispatchers.Main) {
        repeat(poolSize) {
            val webView = pool.tryReceive().getOrNull() ?: return@repeat
            webView.apply {
                stopLoading()
                webViewClient = object : WebViewClient() {}
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
        }
        pool.close()
    }
}

private fun unescapeJsString(raw: String): String = try {
    JSONTokener(raw).nextValue() as String
} catch (e: Exception) {
    raw.trim().removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
}


/*
suspend fun parseAllVacancies(
    pool: WebViewPool,
    searchUrl: String
): List<String> = coroutineScope {

    //TODO Continuing to set up it here:

    (1..totalPages).map { pageIndex ->
        async {
            val url = buildPageUrl(searchUrl, pageIndex)
            val html = pool.renderPage(url)
            parseVacanciesCardsIdFromHtml(html)
        }
    }.awaitAll().flatten()
}*/

suspend fun checkHowManyPagesInRespond(
    searchingUrl: String,
    pool: WebViewPool
): Int {
    var pagesCount = 0
    coroutineScope {
            async {
                Log.d("MyTag", "Checking is running")
                //Closing all previously active views
                pool.shutdown()

                val html = pool.renderPage(searchingUrl)
                pagesCount = getPagesCount(html)
            }
        }.await()

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

    /*
    // PLACEHOLDER selector — adjust to the real card container class.
    val cards = doc.select("div.vacancy-card, article.card-vacancy")

    return cards.mapNotNull { card ->
        val title = card.selectFirst(".vacancy-card__title, a.title")?.text() ?: return@mapNotNull null
        val link = card.selectFirst("a")?.absUrl("href") ?: ""
        val company = card.selectFirst(".vacancy-card__company, .company-name")?.text() ?: ""
        val city = card.selectFirst(".vacancy-card__city, .city")?.text()
        val salary = card.selectFirst(".vacancy-card__salary, .salary")?.text()

        Vacancy(title = title, company = company, city = city, salary = salary, url = link)
    }*/
}
///////////////////////////////////////////////////////////////////////////////////////////////

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

@Composable
private fun StartRunUserQuery(userQuery: String, viewModel : WebViewViewModel){

    val needToCheckRespondPagesCount by remember { mutableStateOf<Boolean>(true)  }
    val respondPagesCount by viewModel.respondPagesCount.collectAsState()

    if (needToCheckRespondPagesCount) {
        viewModel.parseUserQuery(userQuery)
    }


    //viewModel.parseUserQuery("https://rabota.ua/zapros/smila")
}


@Composable
fun VacancyParserScreen2(currentContext: Context)  {
    val context = currentContext
    val webViewsModel: WebViewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WebViewViewModel(context.applicationContext) }
        }
    )

    val isLoading by webViewsModel.isLoading.collectAsState()
    val vacancies by webViewsModel.vacancies.collectAsState()
    val errorMessage by webViewsModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button(colors = if (isLoading){
            ButtonDefaults.buttonColors(containerColor = Color.Red)
        } else{ButtonDefaults.buttonColors(containerColor = Color.Green)},

            onClick = {webViewsModel.parseUserQuery("https://rabota.ua/zapros/smila")} )
        {
            Text(if (isLoading) "Loading..." else "Parse vacancies")
        }

        errorMessage?.let { Text("Error: $it") }
        Text("Found ${vacancies.size} vacancies")
    }

}
