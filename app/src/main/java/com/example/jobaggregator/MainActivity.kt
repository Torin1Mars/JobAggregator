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
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jobaggregator.Parsers.WorkUaParser

import com.example.jobaggregator.ViewModels.RabotaUaParserVm
import com.example.jobaggregator.ViewModels.WorkUaParserVm
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity:ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            WorkUaParserScreen(currentContext = applicationContext)

            //RabotaUaParserScreen(currentContext = applicationContext)
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WorkUaParserScreen(currentContext: Context) {
    val webViewModel: WorkUaParserVm = viewModel()

    val isLoading by webViewModel.isLoading.collectAsState()
    val vacancies by webViewModel.vacanciesIds.collectAsState()
    val errorMessage by webViewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button(
            colors = if (isLoading) {
                ButtonDefaults.buttonColors(containerColor = Color.Red)
            } else {
                ButtonDefaults.buttonColors(containerColor = Color.Green)
            },
            onClick = { webViewModel.runParsing() })
        {
            Text(if (isLoading) "Loading..." else "Parse vacancies")
        }

        errorMessage?.let { Text("Error: $it") }
        Text("Found ${vacancies.size} vacancies")

        Text(
            modifier = Modifier.weight(1f).align(Alignment.End),
            fontSize = 16.sp,
            text = "Work.Ua Parser"
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RabotaUaParserScreen(currentContext: Context)  {
    val webViewModel: RabotaUaParserVm = viewModel()
    /*    factory = viewModelFactory {
            initializer { RabotaUaParserVm(context.applicationContext, ) }
        }
    )*/

    val isLoading by webViewModel.isLoading.collectAsState()
    val vacancies by webViewModel.vacanciesIds.collectAsState()
    val errorMessage by webViewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button(colors = if (isLoading){
            ButtonDefaults.buttonColors(containerColor = Color.Red)
        } else{ButtonDefaults.buttonColors(containerColor = Color.Green)},

            onClick = {webViewModel.parseUserQuery("https://rabota.ua/zapros/smila")} )
        {
            Text(if (isLoading) "Loading..." else "Parse vacancies")
        }

        errorMessage?.let { Text("Error: $it") }
        Text("Found ${vacancies.size} vacancies")

        Text(modifier = Modifier.weight(1f).align(Alignment.End),
            fontSize = 16.sp,
            text = "Rabota.Ua Parser")
    }

}



