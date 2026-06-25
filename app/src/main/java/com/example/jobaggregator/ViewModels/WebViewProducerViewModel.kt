package com.example.jobaggregator.ViewModels

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.domain.JobsDbDao
import com.example.jobaggregator.supportingData.rabotaUaRenderedVacancyPageByteSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class WebViewProducerViewModel @Inject constructor(context: Context,
                                                   val appDbDao: JobsDbDao): ViewModel() {
    private val appContext = context

    private var _viewsIdCounter: Int = 0

    private val _webViewsQueriesList = MutableStateFlow(mutableListOf<WebViewQueryItem>())

    private val _webRunningViewsItemsList = mutableStateListOf<WebViewItem>()
    private val _fullyRenderedViewsList  = mutableStateListOf<WebViewItem>()
    public var fullyRenderedPagesList  = mutableListOf<String>()
    public var renderingHasFinished by  mutableStateOf<Boolean>(false)

    public var finisherParsing:()-> Unit = {}

    private var _checkingIsRunning by mutableStateOf<Boolean>(false)
    private var _checkerThread: Job? = null

    public val webViewsTabsFlow  = _webViewsQueriesList.asStateFlow()

    public fun addQueries(queriesList: List<String>){
        queriesList.forEach { query->
            _webViewsQueriesList.value.add(WebViewQueryItem(viewId = _viewsIdCounter.toString(), viewQuery = query))
            _viewsIdCounter+=1
        }
    }

    public fun watchWebView(currentView: WebView, viewQuery : String, viewHtmlPage : MutableState<String>, closeThisView: () -> Unit ){
        _webRunningViewsItemsList.add(WebViewItem(view = currentView, viewSearchingQuery = viewQuery, viewHtmlPage = viewHtmlPage, closeThisView = closeThisView))

        if (!_checkingIsRunning){
            _checkingIsRunning = true
            _runNewCheckerThread()
        }
    }

    private fun _runNewCheckerThread() {
        _checkerThread = CoroutineScope(Dispatchers.IO).launch {
            while (_checkingIsRunning){

                _webRunningViewsItemsList.forEach { webViewItem->
                    if (webViewItem.viewHtmlPage.value.toByteArray().size > rabotaUaRenderedVacancyPageByteSize){

                        if (webViewItem in _fullyRenderedViewsList) {
                            //Do nothing, it means that we need to skip it
                        }else{
                            _fullyRenderedViewsList.add(webViewItem)
                        }
                    }
                }

                if (_fullyRenderedViewsList.size == _webRunningViewsItemsList.size){
                    //It means that all our views have been rendered
                    _fullyRenderedViewsList.forEach { webViewItem ->
                        fullyRenderedPagesList.add(webViewItem.viewHtmlPage.value)
                    }

                    _webRunningViewsItemsList.forEach { workingWebView->
                        workingWebView.closeThisView()
                    }

                    //Resetting to default values

                    _fullyRenderedViewsList.clear()
                    _webViewsQueriesList.value.clear()
                    _webRunningViewsItemsList.clear()

                    _checkingIsRunning = false
                    renderingHasFinished = true

                    //Finishing in upper hierarchy function
                    finisherParsing()
                }

                delay(200L)
            }
        }
    }

}

//////////////////////////////////////////////////////////////////////////////////////////

data class WebViewQueryItem(val viewId: String, val viewQuery: String)

data class WebViewItem(val view: WebView ,val viewSearchingQuery: String,  val viewHtmlPage: MutableState<String>, val closeThisView:()-> Unit)
