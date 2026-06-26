package com.example.jobaggregator.Parsers

import android.content.Context
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.jobaggregator.supportingData.rabotaUaRenderedVacancyPageByteSize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONTokener
import kotlin.coroutines.resumeWithException

class WebViewPool(
    context: Context,
    private val poolSize: Int = 5
) {
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
        Log.d("WebViewPool", "Warmed up $poolSize WebViews")
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
        url: String,
        minHtmlBytes: Int = 0,
        timeoutMs: Long = 15_000
    ): String {
        if (!initialized) warmUp()

        val webView = pool.receive() // suspends here if all 5 are busy
        return try {
            withTimeout(timeoutMs) {
                fetchHtml(webView, url, minHtmlBytes)
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
        minHtmlBytes: Int
    ): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    view.evaluateJavascript(
                        "(function(){return document.documentElement.outerHTML;})();"
                    ) { result ->
                        if (result.toByteArray().size > minHtmlBytes) {
                            val html = unescapeJsString(result)
                            if (continuation.isActive) continuation.resume(html) {}
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


suspend fun parseAllVacancies(
    pool: WebViewPool,
    searchUrl: String,
    totalPages: Int
): List<String> = coroutineScope {
    (1..totalPages).map { pageIndex ->
        async {
            val url = buildPageUrl(searchUrl, pageIndex)
            val html = pool.renderPage(url, minHtmlBytes = rabotaUaRenderedVacancyPageByteSize)
            parseVacanciesCardsIdFromHtml(html)
        }
    }.awaitAll().flatten()
}


class VacancyViewModel(context: Context) : ViewModel() {
    private val webViewPool = WebViewPool(context, poolSize = 5)

    fun parse(searchUrl: String, totalPages: Int) {
        viewModelScope.launch {
            val vacancies = parseAllVacancies(webViewPool, searchUrl, totalPages)
            // ...
        }
    }

    override fun onCleared() {
        viewModelScope.launch { webViewPool.shutdown() }
    }
}

