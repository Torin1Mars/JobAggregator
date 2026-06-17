package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RabotaUaWebViewProducer(appContext: Context) {

    @Composable
    public fun ProduseUserQuerry(userQueriesList : List<String>, returnRenderedPages:(List<String>)-> Unit) {

        var queriesInsertedToViewModel by remember { mutableStateOf<Boolean>(false) }
        val producerViewModel :WebViewProducerViewModel = hiltViewModel()
        val queries by producerViewModel.webViewsTabsFlow.collectAsState()

        val renderedHtmlPagesList = remember { mutableStateListOf<String>() }

        if (!queriesInsertedToViewModel){
            producerViewModel.addQueries(userQueriesList)
            queriesInsertedToViewModel = true
        }

        LazyColumn(modifier = Modifier.height(0.dp).width(0.dp)) {
            queries.forEach { currentQuery ->

                item(key = currentQuery.viewId) {
                    GetParsedPage(currentQuery.viewQuery, producerViewModel,  {renderedPage-> renderedHtmlPagesList.add(renderedPage)})
                }
            }
        }

        if (renderedHtmlPagesList.size == userQueriesList.size){
            returnRenderedPages(renderedHtmlPagesList)

            Log.d("MyTag", "Rendered pages have sent")
        }

    }


    @Composable
    private fun GetParsedPage(urlQuery: String, drivingViewModel : WebViewProducerViewModel, returnPageInRawHtml:(htmlString: String)-> Unit) {

        var rawHtmlPage = remember { mutableStateOf<String>("") }
        var currentWebView by remember { mutableStateOf<WebView?>(null) }

        val desktopUserAgent by remember { mutableStateOf<String>("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") }

        fun closeThisWebViewAndSendRespond(){
            //Sending rendered page
            returnPageInRawHtml(rawHtmlPage.value)

            Log.d("MyTag", "View was fully rendered")

            //Returning to default settings
            rawHtmlPage.value = ""

            CoroutineScope(Dispatchers.Main).launch {
                if (currentWebView != null){
                    //(currentWebView!!.parent as? ViewGroup)?.removeView(currentWebView)

                    currentWebView?.apply {
                        stopLoading()
                        clearHistory()
                        removeAllViews()
                        webChromeClient = null
                    }

                    currentWebView?.destroy()
                }
            }

            Log.d("MyTag", "View was cleaned")
        }

        fun closeThisWebViewAndSendRespondV2(){
            //Sending rendered page
            returnPageInRawHtml(rawHtmlPage.value)

            Log.d("MyTag", "View was fully rendered")
        }

        //Parsing screen is running in hide mode
        Box(modifier = Modifier.height(0.dp).width(0.dp)) {
            AndroidView(factory = { context ->

                val webView = WebView(context)

                webView.apply {
                    settings.userAgentString = desktopUserAgent

                    Log.d("MyTag", "View started")

                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    drivingViewModel.watchWebView(webView, rawHtmlPage, urlQuery, {closeThisWebViewAndSendRespond()} )

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

            Log.d("MyTag", "View rendered" + page.toByteArray().size)
        }
    }

}
