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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jobaggregator.ViewModels.WebViewProducerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RabotaUaWebViewProducer(appContext: Context) {

    @Composable
    public fun ProduseUserQuerry(userQuery : String, additional:()-> Unit){
        val producerViewModel :WebViewProducerViewModel = hiltViewModel()
        val queries = producerViewModel.webViewsTabsFlow.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            producerViewModel.addNewWebView(userQuery)
        }

        //TODO Conting to working here this shit doesen't working
        Log.d("MyTag", "Size in function "+ queries.value.size)

        LazyColumn(modifier = Modifier.height(0.dp).width(0.dp)) {
            queries.value.forEach { currentQuery ->
                item(key = currentQuery.viewId) {
                    GetParsedPage(currentQuery.viewQuery, {})
                }
            }
        }

    }


    @Composable
    private fun GetParsedPage(urlQuery: String, returnPageInRawHtml:(htmlString: String)-> Unit) {

        val webViewViewModel: WebViewProducerViewModel = hiltViewModel()


        var rawHtmlPage = remember { mutableStateOf<String>("") }
        var currentWebView by remember { mutableStateOf<WebView?>(null) }

        val desktopUserAgent by remember { mutableStateOf<String>("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") }

        var pageHasBeenLoad by remember { mutableStateOf<Boolean>(false) }

        fun closeWebViewAndSendRespond(){
            //Sending rendered page
            returnPageInRawHtml(rawHtmlPage.value)

            Log.d("MyTag", "View was rendered")

            //Returning to default settings
            rawHtmlPage.value = ""
            pageHasBeenLoad = false

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

                    currentWebView = webView

                    //webViewViewModel.watchOnCurrentWebView (rawHtmlPage, webView, {closeWebViewAndSendRespond()} )

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
