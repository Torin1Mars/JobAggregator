package com.example.jobaggregator.Parsers

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import com.example.jobaggregator.ViewModels.WebViewQueryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class RabotaUaWebViewProducer(appContext: Context) {

    @Composable
    public fun ProduseUserQuerry(userQueriesList : List<String>, returnRenderedPages:(List<String>)-> Unit) {

        val producerViewModel :WebViewProducerViewModel = hiltViewModel()
        val htmlQueries = mutableStateListOf<WebViewQueryItem>()
        val queriesFromViewModel by producerViewModel.webViewsTabsFlow.collectAsStateWithLifecycle()

        var queriesWasInsertedToViewModel by remember { mutableStateOf<Boolean>(false) }

        val fullyRenderedHtmlPagesList = remember { mutableStateListOf<String>() }


        fun finishCurrentParsing(){
            fullyRenderedHtmlPagesList.addAll(producerViewModel.fullyRenderedPagesList)
            returnRenderedPages(fullyRenderedHtmlPagesList)
            Log.d("MyTag", "Fully rendered pages have sent back " + fullyRenderedHtmlPagesList.size)

            fullyRenderedHtmlPagesList.clear()

            producerViewModel.fullyRenderedPagesList.clear()
            producerViewModel.renderingHasFinished = false

            queriesWasInsertedToViewModel = false
        }

        if (!queriesWasInsertedToViewModel){
            producerViewModel.addQueries(userQueriesList)
            queriesWasInsertedToViewModel = true

            producerViewModel.finisherParsing = {finishCurrentParsing()}

            //We need to convert queries list from View model to Mutable State list
            // for proper Compose behavior if run this logic in loops
            htmlQueries.clear()
            htmlQueries.addAll(queriesFromViewModel.toList())

            runWebViewViews(htmlQueries, producerViewModel)
        }
    }

    @Composable
    fun runWebViewViews(viewsQueries : SnapshotStateList<WebViewQueryItem>, producerVM : WebViewProducerViewModel){

        Column(modifier = Modifier.height(0.dp).width(0.dp)) {
            viewsQueries.forEach { htmlQuery->
               key(htmlQuery.viewId){
                   GetParsedPage(htmlQuery.viewQuery, producerVM)
               }
            }
        }
    }

    @Composable
    private fun GetParsedPage(urlQuery: String, drivingViewModel : WebViewProducerViewModel) {

        var rawHtmlPage = remember { mutableStateOf<String>("") }
        var currentWebView by remember { mutableStateOf<WebView?>(null) }

        val desktopUserAgent by remember { mutableStateOf<String>("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") }

        fun closeThisWebView(){
            CoroutineScope(Dispatchers.Main).launch {
                if (currentWebView != null){
                    (currentWebView!!.parent as? ViewGroup)?.removeView(currentWebView)

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

        //Parsing screen is running in hide mode
        Box(modifier = Modifier.height(0.dp).width(0.dp)) {
            AndroidView(factory = { context ->

                val webView = WebView(context)

                webView.apply {
                    settings.userAgentString = desktopUserAgent

                    settings.useWideViewPort = true
                    settings.javaScriptEnabled = true

                    drivingViewModel.watchWebView(webView, urlQuery, rawHtmlPage, {closeThisWebView()} )

                    addJavascriptInterface(
                        WebPageInterface (onWebPageReceived = { html ->
                            rawHtmlPage.value = html}),
                        "AndroidInterface"
                    )

                    Log.d("MyTag", "View was started")

                    webViewClient = object : WebViewClient() {

                        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            view?.loadUrl("javascript:window.AndroidInterface.getHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                        }
                    }

                    loadUrl(urlQuery)

                    Log.d("MyTag", "View started")
                }
            })
        }

    }

    private inner class WebPageInterface(private val onWebPageReceived: (String) -> Unit) {
        @JavascriptInterface
        fun getHtml(page: String) {
            onWebPageReceived(page)

            Log.d("MyTag", "Page was rendered " + page.toByteArray().size.toString())
        }
    }

}
