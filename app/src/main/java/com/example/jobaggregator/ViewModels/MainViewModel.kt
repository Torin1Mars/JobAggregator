package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard

import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context,
                                        private val jobsDatabase: JobsDbDao,
                                        private val workUaParserVm: WorkUaParserVm,
                                        private val rabotaUaParserVm: RabotaUaParserVm): ViewModel() {

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

    public fun runCheckVacanciesCount(workUaQuery: String = "", rabotaUaQuery: String = ""){

        if (workUaQuery.isNotBlank()){

            currentWorkUaQuery = workUaQuery
            workUaParserVm.checkVacanciesCountByQuery(currentWorkUaQuery)
        }

        if (rabotaUaQuery.isNotBlank()){

            currentRabotaUaQuery = rabotaUaQuery
            rabotaUaParserVm.checkVacanciesCount(currentRabotaUaQuery)
        }
    }

    public fun runVacanciesParsing(){

        if (currentWorkUaQuery.isNotBlank()){
            workUaParserVm.runParsing(currentWorkUaQuery)
        }

        if (currentRabotaUaQuery.isNotBlank()){
            rabotaUaParserVm.runParsing(currentRabotaUaQuery)
        }
    }

}
