package com.example.jobaggregator.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.data.JobCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

@HiltViewModel
class WorkUaParserVm @Inject constructor(@ApplicationContext context: Context,
    workUaParser: WorkUaParser,
    mainVM: MainViewModel): ViewModel(){

        val parsedVacancies = mutableListOf<JobCard>()



    fun runParsing(){
        //


    }

    override fun onCleared(){

        //Do cleaning
    }

}
