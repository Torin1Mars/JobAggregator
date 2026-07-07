package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.supportingData.workUaParserRenderDelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class WorkUaParserVm @Inject constructor(@ApplicationContext context: Context,
    val workUaParser: WorkUaParser,
    val mainVM: MainViewModel): ViewModel(){

    private val _respondPagesCount = MutableStateFlow<Int?>(null)
    private val _vacanciesIds = MutableStateFlow<List<String>>(emptyList())
    private val _vacanciesJobCards = MutableStateFlow<List<JobCard>>(emptyList())

    val respondPagesCount: StateFlow<Int?> = _respondPagesCount.asStateFlow()
    val vacanciesIds: StateFlow<List<String>> = _vacanciesIds.asStateFlow()
    val vacanciesJobCards = _vacanciesJobCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun runParsing(){
        viewModelScope.launch {
            try{
                withTimeoutOrNull (workUaParserRenderDelay){
                    //Just for testing

                    Log.d("MyTag", "Parsing Started")
                    workUaParser.parseByQuery() }

            }catch (e: TimeoutCancellationException){
                Log.d("MyTag", "Timeout acceded!")

            }catch (e: Exception){
                Log.d("MyTag", "Parsing error acceded!")
            }
        }
    }

    override fun onCleared(){
        //Do cleaning
    }

}
