package com.example.jobaggregator.ViewModels

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.jobaggregator.ksoup.WorkUaParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject



@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context, parser : WorkUaParser): ViewModel() {

    init {
        parser.parseByQuery()

        CoroutineScope(Dispatchers.Default).launch {
            delay(10000)
            Log.d("MyTag", "List size - ${parser.jobsCardsList.size}")
        }
    }
}
