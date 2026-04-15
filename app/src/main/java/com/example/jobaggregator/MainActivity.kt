package com.example.jobaggregator

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.ksoup.WorkUaParser
import com.example.jobaggregator.ui.theme.JobAggregatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val parser = WorkUaParser(context = this)

        parser.parseByQuery("jobs-smila")

        setContent {

            val vm : MainViewModel = hiltViewModel()

            JobAggregatorTheme {
                // TODO
            }
        }
    }
}
