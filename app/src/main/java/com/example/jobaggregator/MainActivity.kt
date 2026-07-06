package com.example.jobaggregator

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.jobaggregator.ViewModels.RabotaUaParserVm
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity:ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            VacancyParserScreen(currentContext = applicationContext)
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VacancyParserScreen(currentContext: Context)  {
    val webViewsModel: RabotaUaParserVm = viewModel()
    /*    factory = viewModelFactory {
            initializer { RabotaUaParserVm(context.applicationContext, ) }
        }
    )*/

    val isLoading by webViewsModel.isLoading.collectAsState()
    val vacancies by webViewsModel.vacanciesIds.collectAsState()
    val errorMessage by webViewsModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button(colors = if (isLoading){
            ButtonDefaults.buttonColors(containerColor = Color.Red)
        } else{ButtonDefaults.buttonColors(containerColor = Color.Green)},

            onClick = {webViewsModel.parseUserQuery("https://rabota.ua/zapros/smila")} )
        {
            Text(if (isLoading) "Loading..." else "Parse vacancies")
        }

        errorMessage?.let { Text("Error: $it") }
        Text("Found ${vacancies.size} vacancies")
    }

}



