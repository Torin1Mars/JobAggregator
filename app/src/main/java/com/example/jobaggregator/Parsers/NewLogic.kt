package com.example.jobaggregator.Parsers

import android.content.Context
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.jobaggregator.supportingData.rabotaUaRenderedVacancyPageByteSize
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener

/**
 * One scraped vacancy card.
 *
 * NOTE: the exact field set/selectors below are placeholders. Open a
 * rabota.ua search results page in Chrome DevTools, find the real class
 * names for the vacancy card / title / company / city / salary / link,
 * and update [parseVacanciesFromHtml] accordingly. The site's markup
 * changes periodically, so this is the part you'll maintain over time.
 */
data class Vacancy(
    val title: String,
    val company: String,
    val city: String?,
    val salary: String?,
    val url: String
)

// =============================================================================
// FUNCTION 1 — top-level entry point
// =============================================================================

/**
 * Top level function. Loops through every results page and aggregates
 * the vacancies found on each one.
 *
 * @param context an Activity context (WebView needs a real Activity/UI
 *   context on most Android versions — passing an Application context
 *   can throw or misbehave).
 * @param searchUrl the listing URL WITHOUT a page parameter, e.g.
 *   "https://rabota.ua/zapros/android-developer/kyiv"
 * @param totalPages how many pages to walk through. If you don't know
 *   this up front, call [detectTotalPages] once on page 1 first (see
 *   usage example below) and pass the result in here.
 */
suspend fun parseAllVacancies(
    context: Context,
    searchUrl: String,
    totalPages: Int
): List<Vacancy> {
    val allVacancies = mutableListOf<Vacancy>()

    for (pageNumber in 1..totalPages) {
        // FUNCTION 2 runs here, once per page in the loop
        val vacanciesOnPage = parseVacancyPage(context, searchUrl, pageNumber)
        allVacancies += vacanciesOnPage
    }

    return allVacancies
}

// =============================================================================
// FUNCTION 2 — runs once per page inside the loop in function 1
// =============================================================================

/**
 * Handles exactly one listing page: builds its URL, fetches the fully
 * rendered HTML through the WebView (function 3), then parses the
 * vacancies out of that HTML.
 */
private suspend fun parseVacancyPage(
    context: Context,
    searchUrl: String,
    pageNumber: Int
): List<Vacancy> {
    val pageUrl = buildPageUrl(searchUrl, pageNumber)
    val rawHtml = fetchPageHtml(context, pageUrl) // FUNCTION 3
    // TODO Parsing itself doesn't working but in general its interesting aproach because we can use just main page vacancies data
    return parseVacanciesFromHtml(rawHtml)
}

// =============================================================================
// FUNCTION 3 — spins up the WebView, returns raw HTML, tears the WebView down
// =============================================================================

/**
 * Loads [url] in a native, invisible WebView and returns the fully
 * rendered `document.documentElement.outerHTML` once the page finishes
 * loading. Once the HTML has been captured (or an error occurs), the
 * WebView is stopped, its cache/history are cleared, and it is destroyed.
 *
 * WebView calls must happen on the main thread, hence the
 * withContext(Dispatchers.Main) wrapper.
 */
private suspend fun fetchPageHtml(
    context: Context,
    url: String
): String = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // A realistic desktop/mobile UA helps avoid sites serving a
            // stripped-down "unsupported browser" page or blocking
            // requests outright. Tune as needed for rabota.ua's actual
            // anti-bot behavior (it does run bot-protection on some routes).
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        fun closeAndClearWebView() {
            webView.apply {
                stopLoading()
                webViewClient = object : WebViewClient() {} // detach callbacks
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, finishedUrl: String) {
                // Pull the rendered DOM out as a string.
                view.evaluateJavascript(
                    "(function(){return document.documentElement.outerHTML;})();"
                ) { result ->

                    //val html = unescapeJsString(result)

                    /*closeAndClearWebView()
                    if (continuation.isActive) {
                        continuation.resume(html) {}
                        Log.d("MyTag", "Fully rendered page have sent back " + html.toByteArray().size)
                    }*/

                    if (result.toByteArray().size > rabotaUaRenderedVacancyPageByteSize){
                        val html = unescapeJsString(result)

                        closeAndClearWebView()

                        if (continuation.isActive) {
                            continuation.resume(html) {}
                        }

                        Log.d("MyTag", "Fully rendered page have sent back " + result.toByteArray().size)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                closeAndClearWebView()
                if (continuation.isActive) {
                    continuation.resumeWith(
                        Result.failure(Exception("WebView load failed: ${error.description}"))
                    )
                }
            }
        }

        // If the coroutine gets cancelled (e.g. screen rotated away,
        // scope cancelled), make sure we still clean up the WebView.
        continuation.invokeOnCancellation { closeAndClearWebView() }

        webView.loadUrl(url)
    }
}

// =============================================================================
// Small helper utilities (not part of the "3 main functions", just plumbing)
// =============================================================================

/**
 * Builds the URL for a given page number.
 *
 * TODO: confirm the real pagination parameter by browsing rabota.ua
 * manually and watching the address bar — it has historically been
 * something like "?page=2", but verify before relying on it.
 */
private fun buildPageUrl(searchUrl: String, pageNumber: Int): String {
    if (pageNumber <= 1) return searchUrl
    val separator = if (searchUrl.contains("?")) "&" else "?"
    return "$searchUrl${separator}page=$pageNumber"
}

/**
 * evaluateJavascript() returns its result as a JSON-encoded string
 * literal (quoted + escaped), e.g. "\"<html>...\"" — this unwraps it
 * back into plain text.
 */
private fun unescapeJsString(raw: String): String {
    return try {
        JSONTokener(raw).nextValue() as String
    } catch (e: Exception) {
        raw.trim().removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
    }
}

/**
 * Parses vacancy cards out of one page's raw HTML using Jsoup.
 *
 * TODO: replace the placeholder CSS selectors below with the real ones
 * from rabota.ua's current markup. Open a search results page, inspect
 * one vacancy card, and find the actual class names / data attributes.
 * Add to build.gradle: implementation("org.jsoup:jsoup:1.17.2")
 */
private fun parseVacanciesFromHtml(html: String): List<Vacancy> {
    val doc = Ksoup.parse(html)

    // PLACEHOLDER selector — adjust to the real card container class.
    val cards = doc.select("div.vacancy-card, article.card-vacancy")

    return cards.mapNotNull { card ->
        val title = card.selectFirst(".vacancy-card__title, a.title")?.text() ?: return@mapNotNull null
        val link = card.selectFirst("a")?.absUrl("href") ?: ""
        val company = card.selectFirst(".vacancy-card__company, .company-name")?.text() ?: ""
        val city = card.selectFirst(".vacancy-card__city, .city")?.text()
        val salary = card.selectFirst(".vacancy-card__salary, .salary")?.text()

        Vacancy(title = title, company = company, city = city, salary = salary, url = link)
    }
}

/**
 * Optional helper: read page 1's HTML to find the highest page number
 * in the pagination control, so you can pass a real totalPages into
 * parseAllVacancies() instead of hardcoding it.
 *
 * TODO: adjust the selector to the real pagination markup.
 */
fun detectTotalPages(html: String): Int {
    val doc = Ksoup.parse(html)
    val pageNumbers = doc.select(".pagination a, .pagination span")
        .mapNotNull { it.text().trim().toIntOrNull() }
    return pageNumbers.maxOrNull() ?: 1
}

// =============================================================================
// Example Compose usage
// =============================================================================

@Composable
fun VacancyParserScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var vacancies by remember { mutableStateOf<List<Vacancy>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Button( onClick = {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val searchUrl = "https://rabota.ua/zapros/smila"

                    // Discover total pages once, then run the main parse loop.
                    val firstPageHtml = fetchPageHtmlPublic(context, buildPageUrlPublic(searchUrl, 1))
                    val totalPages = detectTotalPages(firstPageHtml)

                    vacancies = parseAllVacancies(context, searchUrl, totalPages)
                } catch (e: Exception) {
                    errorMessage = e.message
                } finally {
                    isLoading = false
                }
            }
        }) {
            Text(if (isLoading) "Loading..." else "Parse vacancies")
        }
    }

    errorMessage?.let { Text("Error: $it") }
    Text("Found ${vacancies.size} vacancies")
}

// Public-visibility wrappers only needed because the real functions above
// are `private` to this file; if you split files, just drop the
// "Public" suffix and adjust visibility instead.
suspend fun fetchPageHtmlPublic(context: Context, url: String) = fetchPageHtml(context, url)
fun buildPageUrlPublic(searchUrl: String, pageNumber: Int) = buildPageUrl(searchUrl, pageNumber)
