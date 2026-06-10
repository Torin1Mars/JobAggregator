package com.example.jobaggregator.ViewModels

import android.content.Context
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
import android.util.Log
import androidx.compose.runtime.MutableState

@HiltViewModel
class RabotaUaParserViewModel @Inject constructor(context: Context,
                                                        val appDbDao: JobsDbDao): ViewModel() {
    private val appContext = context

    internal data class PairedWebView(val viewRenderingPage: MutableState<String>, val currentWebView: WebView)

    private val runningViewsWithPagesList = mutableStateListOf<PairedWebView>()
    private val queriesList = mutableStateListOf<String>()
    private var checkerThread by mutableStateOf<Job?>(null)

    public fun watchOnCurrentWebView(htmlPage: MutableState<String>, webView: WebView?) {
        webView.let { it -> runningViewsWithPagesList.add(PairedWebView(htmlPage, it!!)) }

        runCheckerThread()
    }

    private fun runCheckerThread() {
        if (checkerThread == null){

            val checkerThread = viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    //Checking loading progress
                    while (runningViewsWithPagesList.isNotEmpty()) {

                        runningViewsWithPagesList.forEach { it ->
                            if (it.viewRenderingPage.value.toByteArray().size > rabotaUaRenderedVacancyPageByteSize) {
                                closeWebView(it.currentWebView)

                                Log.d("MyTag", "It have been removed")

                                //runningViewsWithPagesList.remove(it)
                            }
                        }

                        delay(100L)
                    }
                }
            }
        }
    }

    private fun closeWebView(currentWebView: WebView?){

        if (currentWebView != null){
            currentWebView.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                webChromeClient = null
            }

            currentWebView.destroy()
        }

    }


}
