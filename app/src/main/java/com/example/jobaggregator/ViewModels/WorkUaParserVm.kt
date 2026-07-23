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
    val workUaParser: WorkUaParser): ViewModel(){

    private val _vacanciesCount = MutableStateFlow<Int?>(null)

    private val _respondPagesCount = MutableStateFlow<Int?>(null)
    private val _vacanciesIds = MutableStateFlow<List<String>>(emptyList())
    private val _vacanciesJobCards = MutableStateFlow<List<JobCard>>(emptyList())

    val vacanciesCount = _vacanciesCount.asStateFlow()
    val vacanciesIds: StateFlow<List<String>> = _vacanciesIds.asStateFlow()
    val vacanciesJobCards = _vacanciesJobCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkVacanciesCountByQuery(convertedQuery: String ){
        viewModelScope.launch {
            try{
                withTimeoutOrNull (workUaParserRenderDelay){
                    _isLoading.value = true

                    Log.d("MyTag", "Checking started")
                    workUaParser.checkVacanciesCountByQuery(convertedQuery, _vacanciesCount)

                    Log.d("MyTag", "Checking finished")
                }

            }catch (e: TimeoutCancellationException){
                Log.d("MyTag", "Timeout acceded!")

            }catch (e: Exception){
                Log.d("MyTag", "Parsing error acceded!")
            }finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun runParsing(query: String, addParsedVacanciesToDb:()-> Unit){
        viewModelScope.launch {
            try{
                withTimeoutOrNull (workUaParserRenderDelay){
                    _isLoading.value = true

                    Log.d("MyTag", "Parsing Started")
                    workUaParser.parseByQuery(query)
                    _vacanciesJobCards.value = workUaParser.getParsedJobsCardsList()

                    addParsedVacanciesToDb()
                }
            }catch (e: TimeoutCancellationException){
                Log.d("MyTag", "Timeout acceded!")

            }catch (e: Exception){
                Log.d("MyTag", "Parsing error acceded!")
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun cleanAfterParsing(){
        _vacanciesCount.value = null

        _respondPagesCount.value = null
        _vacanciesIds.value = emptyList()
        _vacanciesJobCards.value = emptyList()
    }

    override fun onCleared(){

    }
}
