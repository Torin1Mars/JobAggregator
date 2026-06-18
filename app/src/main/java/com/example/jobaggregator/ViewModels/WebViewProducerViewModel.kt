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

    public val webViewsTabsFlow  = _webViewsQueriesList.asStateFlow()

    public fun addQueries(queriesList: List<String>){
        queriesList.forEach { query->
            _webViewsQueriesList.value.add(WebViewQueryItem(viewId = _viewsIdCounter.toString(), viewQuery = query))
            _viewsIdCounter+=1
        }
    }

    public fun watchWebView(currentView: WebView, currentViewHtmlPage: MutableState<String>, currentViewQuery: String, sendViewRespond: () -> Unit ){
        _webRunningViewsItemsList.add(WebViewItem(view = currentView, viewHtmlPage = currentViewHtmlPage, viewQuery = currentViewQuery, sendRenderedPageFromThisView = sendViewRespond))

        if (_checkerThread==null){
            _runNewCheckerThread()
        }

    }

    private fun _runNewCheckerThread() {
        _checkerThread = CoroutineScope(Dispatchers.IO).launch {

            while (_webRunningViewsItemsList.isNotEmpty()){

                val runningViewsSimpleList = _webRunningViewsItemsList.toList()

                runningViewsSimpleList.forEach { currentWebViewItem ->

                    if (currentWebViewItem.viewHtmlPage.value.toByteArray().size > rabotaUaRenderedVacancyPageByteSize){
                        //It means that our html Page was completely rendered
                        _webRunningViewsItemsList.remove(currentWebViewItem)

                        val viewsQueriesSimpleList = _webViewsQueriesList.value.toList()
                        viewsQueriesSimpleList.forEach { webViewQuerry->
                            if (webViewQuerry.viewQuery==currentWebViewItem.viewQuery){
                                _webViewsQueriesList.value.remove(webViewQuerry)
                            }
                        }

                        currentWebViewItem.sendRenderedPageFromThisView()

                        if (_webViewsQueriesList.value.size == 0){
                            //It means that all webViews were rendered
                            _viewsIdCounter = 0

                            _checkerThread = null
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

}

//////////////////////////////////////////////////////////////////////////////////////////

data class WebViewQueryItem(val viewId: String, val viewQuery: String)

data class WebViewItem(val view: WebView, val viewHtmlPage: MutableState<String>, val viewQuery: String, val sendRenderedPageFromThisView:()-> Unit)
