package com.example.jobaggregator

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.jobaggregator.ksoup.WorkUaParser
import com.example.jobaggregator.retrofit.RetrofitObj
import com.example.jobaggregator.ui.theme.JobAggregatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val parser = WorkUaParser()

        parser.startTesting()

        setContent {
            JobAggregatorTheme {
                // TODO
            }
        }
    }
}
