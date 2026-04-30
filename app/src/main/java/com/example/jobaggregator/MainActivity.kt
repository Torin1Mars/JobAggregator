package com.example.jobaggregator

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.Parsers.RabotaUaParser
import com.example.jobaggregator.ViewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity:ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: MainViewModel = hiltViewModel()
            val parser: RabotaUaParser = RabotaUaParser(this)
            parser.test()
        }
    }
}



