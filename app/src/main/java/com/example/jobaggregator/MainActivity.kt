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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.ui.com.example.jobaggregator.ui.com.example.jobaggregator.com.example.jobaggregator.ui.com.example.jobaggregator.MainScreen
import com.example.jobaggregator.ui.theme.JobAggregatorTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class MainActivity:ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            JobAggregatorTheme(
                darkTheme = true,
                content = {MainScreen(this)}
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CommonScreen(currentContext: Context) {
    val mainViewModel: MainViewModel = viewModel()

    val parsersLoadingStatus by mainViewModel.parsersBusyStatus.collectAsState()
    val vacanciesCountHasBeenChecked by mainViewModel.vacanciesCountHasBeenChecked.collectAsState()

    val workUaFoundedVacanciesCount by mainViewModel.workUaVacanciesCount.collectAsState()
    val workUaErrors by mainViewModel.workUaErrorMessage.collectAsState()

    val rabotaUaFoundedVacanciesCount by mainViewModel.rabotaUaVacanciesCount.collectAsState()
    val rabotaUaErrors by mainViewModel.rabotaUaErrorMessage.collectAsState()


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // First functional button
        Button(colors = if (parsersLoadingStatus){
            ButtonDefaults.buttonColors(containerColor = Color.Red)
        } else{ButtonDefaults.buttonColors(containerColor = Color.Green)},
            onClick = { mainViewModel.runCheckVacanciesCount() })
        {
            Text(if (parsersLoadingStatus) "Parsers are working..." else "Run new parsing")
        }

        // Second functional button
        Button(colors = if (!vacanciesCountHasBeenChecked ){
            ButtonDefaults.buttonColors(containerColor = Color.Red)
        } else{ButtonDefaults.buttonColors(containerColor = Color.Green)},
            onClick = {mainViewModel.runVacanciesParsing(currentContext)})
        {
            Text(if (vacanciesCountHasBeenChecked ) "Run parsing" else "Run new parsing")
        }

        workUaErrors?.let { Text("Error: $it") }
        Text("Found:  $workUaFoundedVacanciesCount workUa vacancies")

        rabotaUaErrors?.let { Text("Error: $it") }
        Text("Found:  $rabotaUaFoundedVacanciesCount rabotaUa vacancies")

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 2.dp, color = Color.Blue )

        Text(modifier = Modifier.padding(bottom = 10.dp, end = 10.dp),
            fontSize = 16.sp,
            text = "Common Screen")
    }
}

