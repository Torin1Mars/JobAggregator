package com.example.jobaggregator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jobaggregator.retrofit.RetrofitObj
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

        val workUaRetrofit: RetrofitObj = RetrofitObj

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val currentResponse = workUaRetrofit.api.getPageAsString()

                if (!currentResponse.isEmpty()) {
                    Log.d("MyTag", currentResponse)

                } else {
                    //Do nothing
                }
                val temp = currentResponse

            } catch (e: Exception) {
                Log.d("Error", e.message.toString())
            }

        }
    }
}
