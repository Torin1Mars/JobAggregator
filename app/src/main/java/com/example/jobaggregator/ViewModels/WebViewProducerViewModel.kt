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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class WebViewProducerViewModel @Inject constructor(context: Context,
                                                   val appDbDao: JobsDbDao): ViewModel() {
    private val appContext = context

    private var viewsIdCounter: Int = 0
    private val webViewsItemsList = MutableStateFlow(mutableListOf<WebViewItem>())


    val webViewsTabsFlow : StateFlow<List<WebViewItem>> = webViewsItemsList.asStateFlow()

    public fun addNewWebView(viewQuery: String){
        webViewsItemsList.value.add(WebViewItem(viewId = viewsIdCounter.toString(), viewQuery = viewQuery))
        viewsIdCounter+=1


        Log.d("MyTag", "Size = "+webViewsItemsList.value.size)
        Log.d("MyTag", "Counter= "+viewsIdCounter.toString())
    }

    public fun closeView(viewId: String){
        //It's remove current element from common collection
        webViewsItemsList.value = webViewsItemsList.value.filter{it.viewId != viewId}.toMutableStateList()
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

data class WebViewItem(val viewId: String, val viewQuery: String)
