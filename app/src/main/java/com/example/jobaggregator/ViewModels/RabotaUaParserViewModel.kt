package com.example.jobaggregator.ViewModels

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RabotaUaParserViewModel @Inject constructor(context: Context,
                                                        val appDbDao: JobsDbDao): ViewModel()
{
    private val appContext = context

    private val runningViews = mutableStateListOf<WebView>()
    private val queriesList = mutableStateListOf<String>()


    // TODO here
    public fun catchCurrentVebView(webView: WebView){
        runningViews.add(webView)




    }

    private fun runCheckerThread(){

        viewModelScope.launch{
            try {
                withContext(Dispatchers.IO) {
                    // checking loading progress
                    while (runningViews.size !=0){


                    }
                    delay(100L)

                }



            }catch (e: Exception) {
                //TODO handle this exception
            }
        }
    }





}
