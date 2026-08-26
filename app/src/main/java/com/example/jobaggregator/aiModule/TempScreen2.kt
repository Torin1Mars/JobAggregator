package com.example.jobaggregator.aiModule

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGptScreen(context:Context) {
    var userPrompt by remember { mutableStateOf<String>("") }

    val mainVM : MainViewModel = hiltViewModel()

    val gptViewModel: ChatGptViewModel2 = viewModel()
    val uiState by gptViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ask ChatGPT :", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = userPrompt,
            onValueChange = {it-> userPrompt = it},
            label = { Text("Enter your prompt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {runNewQuery(mainVM, gptViewModel, userPrompt)},
            enabled = uiState == VacancyAiUiState.Idle || uiState == VacancyAiUiState.Success::class,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState == VacancyAiUiState.Success::class) "Asking..." else "Send")
        }

        /*
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        uiState.error?.let { err ->
            Text(
                text = "Error: $err",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.response.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.response,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }*/
    }
}

private fun runNewQuery(mainVM: MainViewModel, gptVM: ChatGptViewModel2, userPrompt: String){
    CoroutineScope(Dispatchers.IO).launch{
        val vacanciesList = getCurrentVacanciesList(mainVM)
        gptVM.askAi(vacanciesList, userPrompt)
    }
}

private suspend fun getCurrentVacanciesList(mainVM: MainViewModel): List<JobCard>{
    val databaseJobCardList =  mainVM.getVacanciesList()
    val unpackedJobCardList = mutableListOf<JobCard>()

    if (databaseJobCardList.isEmpty()){
        return emptyList()
    }else{
        databaseJobCardList.forEach {vacancyCard->
            unpackedJobCardList.add(vacancyCard.jobCard)
        }
        return unpackedJobCardList
    }
}

