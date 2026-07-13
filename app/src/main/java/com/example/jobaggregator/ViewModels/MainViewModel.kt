package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard

import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context,
                                        private val jobsDatabase: JobsDbDao,
                                        private val workUaParserVm: WorkUaParserVm,
                                        private val rabotaUaParserVm: RabotaUaParserVm): ViewModel() {


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

    public fun checkVacanciesCount(workUaQuery: String = "", rabotaUaQuery: String = ""){

        if (workUaQuery.isNotBlank()){
            workUaParserVm.checkVacanciesCountByQuery(workUaQuery)
        }

        //TODO next
        if (rabotaUaQuery.isNotBlank()){
            //rabotaUaViewModel
        }




    }




}
