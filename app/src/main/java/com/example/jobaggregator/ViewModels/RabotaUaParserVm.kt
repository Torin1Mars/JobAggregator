package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.Parsers.WebViewPool
import com.example.jobaggregator.Parsers.checkHowManyPagesInRespond
import com.example.jobaggregator.Parsers.parseJobCardsIds
import com.example.jobaggregator.Parsers.parseVacanciesJobCards
import com.example.jobaggregator.Parsers.runVacanciesCountChecking
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.rabotaUaMaxRuningWebViewsInOnes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RabotaUaParserVm @Inject constructor(@ApplicationContext appContext: Context) : ViewModel()
{
    private val context = appContext
    private var webViewPool :WebViewPool? = null

    private val _vacanciesCount = MutableStateFlow<Int?>(null)

    private val _respondPagesCount = MutableStateFlow<Int?>(null)
    private val _vacanciesIds = MutableStateFlow<List<String>>(emptyList())
    private val _vacanciesJobCards = MutableStateFlow<List<JobCard>>(emptyList())

    val vacanciesCount = _vacanciesCount.asStateFlow()

    val respondPagesCount: StateFlow<Int?> = _respondPagesCount.asStateFlow()
    val vacanciesIds: StateFlow<List<String>> = _vacanciesIds.asStateFlow()
    val vacanciesJobCards = _vacanciesJobCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkVacanciesCount(searchingUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            webViewPool = WebViewPool(context, 1)

            _vacanciesCount.value = runVacanciesCountChecking(searchingUrl, webViewPool!!)

            webViewPool!!.shutdown()
            webViewPool = null
            _isLoading.value = false
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun runParsing(searchingUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            _runNewParsing(searchingUrl)

            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun _runNewParsing(userQuery: String){

        //Checking how many pages with vacancies in query respond
        webViewPool = WebViewPool(context, rabotaUaMaxRuningWebViewsInOnes)
        _respondPagesCount.value = checkHowManyPagesInRespond(userQuery, webViewPool!!)

        //Collecting vacancies cards Id's
        if (respondPagesCount.value!=null){
            //Checking pages count finished, beginning parsing
            Log.d("MyTag", "Beginning parsing Vacancies Id's")

            Log.d("MyTag", respondPagesCount.value!!.toString())
            _vacanciesIds.value = parseJobCardsIds(webViewPool!!, userQuery, respondPagesCount.value!!)

            //Swapping back to default value
            _respondPagesCount.value = null
        }

        //Run parsing vacancies cards
        if (vacanciesIds.value.isNotEmpty()){
            Log.d("MyTag", "Vacancy parsing started")
            webViewPool?.let { it-> _vacanciesJobCards.value = parseVacanciesJobCards(it, vacanciesIds.value)}
        }

        Log.d("MyTag", "All vacancies parsing finished !!!")
        Log.d("MyTag", "${vacanciesJobCards.value.size}")

        webViewPool!!.shutdown()
        webViewPool = null
    }

    fun cleanAfterParsing(){
        _vacanciesCount.value = null

        _respondPagesCount.value = null
        _vacanciesIds.value = emptyList()
        _vacanciesJobCards.value = emptyList()
    }

    override fun onCleared() {
        webViewPool?.let { viewModelScope.launch { it.shutdown();
            webViewPool = null }  }
    }
}

