package com.example.jobaggregator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jobaggregator.ui.theme.JobAggregatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            JobAggregatorTheme {
                // TODO
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val vacancy  = parsingExample()

            vacancy.forEach { it -> Log.d("MyTag", it.attr("name")) }
        }
    }
}
