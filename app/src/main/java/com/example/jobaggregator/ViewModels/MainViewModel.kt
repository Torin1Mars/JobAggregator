package com.example.jobaggregator.ViewModels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.Parsers.WorkUaParser
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard

import com.example.jobaggregator.domain.JobsDbDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context,
                                        parser: WorkUaParser,
                                        private val jobsDatabase: JobsDbDao ): ViewModel() {

    init {
        parser.parseByQuery()

        CoroutineScope(Dispatchers.Default).launch {
            delay(5000)
            Log.d("MyTag", "List size - ${parser.jobsCardsList.size}")

            jobsDatabase.addOneJobCard(DatabaseJobCard(
                publicationDate = "Example",
                jobCard = JobCard(
                    jobIdOnWebsite ="Example",
                    publicationDate = "Example",
                    jobTitle = "Example",
                    jobDescription = "Example",
                    jobLocation = "Example",
                    jobCompany = "Example",
                    jobSalary = "Example",
                    jobUrl = "Example"
                )
            ))
        }
    }
}
