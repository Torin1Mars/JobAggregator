package com.example.jobaggregator.ViewModels

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.domain.JobsDbDao
import com.example.jobaggregator.supportingData.rabotaUaRenderedVacancyPageByteSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.toMutableStateList
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class WebViewProducerViewModel @Inject constructor(context: Context,
                                                   val appDbDao: JobsDbDao): ViewModel() {
    private val appContext = context

    private var _viewsIdCounter: Int = 0

    private val _webViewsQueriesList = MutableStateFlow(mutableListOf<WebViewQueryItem>())
    private val _webRunningViewsItemsList = mutableStateListOf<WebViewItem>()

    private var _checkerThread: Job? = null

    val webViewsTabsFlow  = _webViewsQueriesList.asStateFlow()

    public fun addNewWebViewQuery(viewQuery: String){
        _webViewsQueriesList.value.add(WebViewQueryItem(viewId = _viewsIdCounter.toString(), viewQuery = viewQuery))

        _viewsIdCounter+=1
    }

    public fun watchWebView(currentView: WebView, currentViewHtmlPage: MutableState<String>, currentViewQuery: String ){
        _webRunningViewsItemsList.add(WebViewItem(view = currentView, viewHtmlPage = currentViewHtmlPage, viewQuery = currentViewQuery))

        if (_checkerThread==null){
            runCheckerThread()
        }
    }

    private fun runCheckerThread() {
        _checkerThread = CoroutineScope(Dispatchers.IO).launch {
            val currentHtmlPage: String = ""
            while (_webRunningViewsItemsList.isNotEmpty()){
                _webRunningViewsItemsList.forEach { currentWebViewItem ->

                    if (currentWebViewItem.viewHtmlPage.value.toByteArray().size > rabotaUaRenderedVacancyPageByteSize){
                        //it means that our html Page was completely rendered
                        _webRunningViewsItemsList.remove(currentWebViewItem)

                        _webViewsQueriesList.value.forEach { webViewQuerry->
                            if (webViewQuerry.viewQuery==currentWebViewItem.viewQuery){
                                _webViewsQueriesList.value.remove(webViewQuerry)
                            }
                        }

                        if (_webViewsQueriesList.value.size == 0){
                            //It means that all webViews were rendered
                            _viewsIdCounter = 0

                            Log.d("MyTag", "All Views were rendered")
                        }

                    }
                }

                delay(100L)
            }
        }

    }

    public fun closeView(viewId: String){
        //It's remove current element from common collection
        _webViewsQueriesList.value = _webViewsQueriesList.value.filter{it.viewId != viewId}.toMutableStateList()
    }

    /*internal data class PairedWebView(val viewRenderingPage: MutableState<String>, val currentWebView: WebView, val closeThisView:()-> Unit)


    private val runningViewsWithPagesList = mutableStateListOf<PairedWebView>()
    private val queriesList = mutableStateListOf<String>()
    private var checkerThread by mutableStateOf<Job?>(null)

    public fun watchOnCurrentWebView(htmlPage: MutableState<String>, webView: WebView?, closeThisView:()-> Unit) {
        webView.let { it -> runningViewsWithPagesList.add(PairedWebView(htmlPage, it!!, closeThisView )) }

        runCheckerThread()
    }

    private fun runCheckerThread() {
        if (checkerThread == null){

            val checkerThread = viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    //Checking loading progress
                    while (runningViewsWithPagesList.isNotEmpty()) {

                        //Here we're checking all active views if they completely finished their renders
                        runningViewsWithPagesList.forEach { it ->
                            if (it.viewRenderingPage.value.toByteArray().size > rabotaUaRenderedVacancyPageByteSize) {
                                it.closeThisView()
                                runningViewsWithPagesList.remove(it)
                            }
                        }

                        delay(100L)
                    }
                }
            }
        }
    }*/
}

//////////////////////////////////////////////////////////////////////////////////////////

data class WebViewQueryItem(val viewId: String, val viewQuery: String)

data class WebViewItem(val view: WebView, val viewHtmlPage: MutableState<String>, val viewQuery: String)
