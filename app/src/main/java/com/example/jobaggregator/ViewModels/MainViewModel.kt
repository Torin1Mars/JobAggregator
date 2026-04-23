package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.Parsers.RabotaUaParser
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard

import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context,
                                        workUaParser: WorkUaParser,
                                        rabotaUaParser: RabotaUaParser,
                                        private val jobsDatabase: JobsDbDao ): ViewModel() {

    init {
        rabotaUaParser.parseByQuery()
        /*CoroutineScope(Dispatchers.Default).launch {

            workUaParser.parseByQuery()

            Log.d("MyTag", "List size - ${workUaParser.jobsCardsList.size}")

            jobsDatabase.deleteDb()
            jobsDatabase.addJobCardList(formatJobCardsList(workUaParser.jobsCardsList))

        }*/
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
