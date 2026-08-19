package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard

import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context,
                                        private val vacanciesDatabase: JobsDbDao,
                                        val workUaParserVm: WorkUaParserVm,
                                        val rabotaUaParserVm: RabotaUaParserVm): ViewModel() {

    var currentWorkUaQuery = ""
    val workUaVacanciesCount =  workUaParserVm.vacanciesCount

    val workUaIsLoading =  workUaParserVm.isLoading
    val workUaVacanciesCards =  workUaParserVm.vacanciesJobCards
    val workUaErrorMessage =  workUaParserVm.error

    //////////////////////////////////////////////////////////////////////////
    var currentRabotaUaQuery = ""
    val rabotaUaVacanciesCount = rabotaUaParserVm.vacanciesCount

    val rabotaUaIsLoading = rabotaUaParserVm.isLoading
    val rabotaUaVacanciesCards = rabotaUaParserVm.vacanciesJobCards
    val rabotaUaErrorMessage = rabotaUaParserVm.error

    val dbVacanciesFlow = vacanciesDatabase.get_all_Jobs()
    val dbCountFlow = vacanciesDatabase.getDbCountFlow()

    init {
        viewModelScope.launch {
            // Triggering database creation
            vacanciesDatabase.get_all_Jobs().firstOrNull()
        }
    }

    //__________________________________________________________________________//
    val vacanciesCountHasBeenChecked = combine (workUaIsLoading, rabotaUaIsLoading) {

        val workUaCount = workUaVacanciesCount.value?: 0
        val rabotaUaCount = workUaVacanciesCount.value?: 0

        if(workUaCount>0 || rabotaUaCount>0){
            if (!workUaIsLoading.value && !rabotaUaIsLoading.value){
                return@combine true
            }
        }
        return@combine false
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(500), initialValue =  false )

    val parsersBusyStatus = combine(workUaIsLoading, rabotaUaIsLoading)
    {
        workUaStatus, rabotaUaStatus ->
        if (workUaStatus || rabotaUaStatus) {
            true
        }else{
            false
        }
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(500), initialValue =  false )

    public fun runCheckVacanciesCount(workUaQuery: String = "", rabotaUaQuery: String = ""){

        if (workUaQuery.isNotBlank()){

            currentWorkUaQuery = workUaQuery
            workUaParserVm.checkVacanciesCountByQuery(currentWorkUaQuery)
        }

        /*
        if (rabotaUaQuery.isNotBlank()){
            currentRabotaUaQuery = rabotaUaQuery
            rabotaUaParserVm.checkVacanciesCount(currentRabotaUaQuery)
        }*/
    }

    public fun runVacanciesParsing(context: Context){
        if (currentWorkUaQuery.isNotBlank()){
            workUaParserVm.runParsing(context,currentWorkUaQuery, {addParsedVacanciesToDb()})
        }

        /*
        if (currentRabotaUaQuery.isNotBlank()){
            rabotaUaParserVm.runParsing(currentRabotaUaQuery)
        }*/
    }

    private fun addParsedVacanciesToDb(){
        CoroutineScope(Dispatchers.IO).launch {
            val formatedList = formatJobCardsList(workUaVacanciesCards.value.toMutableList())

            vacanciesDatabase.deleteDb()
            vacanciesDatabase.addJobCardList(formatedList)

            Log.d("MyTag", "In database now: ${vacanciesDatabase.getVacanciesCount()}")
        }
    }

    private fun formatJobCardsList(jobsCardList: MutableList<JobCard>): MutableList<DatabaseJobCard>{
        val databaseJobCardList = mutableListOf<DatabaseJobCard>()

        jobsCardList.forEach { card->
            databaseJobCardList.add(DatabaseJobCard(
                publicationDate = card.publicationDate,
                jobCard = card
            ))
        }
        return databaseJobCardList
    }

}
