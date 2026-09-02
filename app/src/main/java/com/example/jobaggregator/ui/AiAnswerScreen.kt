package com.example.jobaggregator.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.aiModule.ChatGptViewModel
import com.example.jobaggregator.aiModule.VacancyAiAnswer
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.ui.theme.AccentGreen
import com.example.jobaggregator.ui.theme.BackgroundBlack
import io.ktor.util.debug.initContextInDebugMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun AiAnswerScreen(context: Context, mainViewModel: MainViewModel, gptViewModel: ChatGptViewModel){

    val mainVM = mainViewModel
    val gptVM = gptViewModel

    val gptAnswer = gptVM.aiReplyState.value
    val _chosenVacanciesList = MutableStateFlow<List<JobCard>>(emptyList())
    val chosenVacanciesList by _chosenVacanciesList.collectAsState()

    //Loading Ai chosen vacancies
    LaunchedEffect (Unit){
        val gptChosenVacanciesList = gptAnswer.matchedList

        if (gptChosenVacanciesList.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("MyTag", "Size: ${chosenVacanciesList.size}")
                _chosenVacanciesList.value = mainVM.getVacanciesByListIds(gptChosenVacanciesList)

                Log.d("MyTag", "Size: ${chosenVacanciesList.size}")
            }
        }
    }


    //TODO continuing here :
    Scaffold(containerColor = BackgroundBlack,
        topBar = {},
        content = {MainContent(gptAnswer, chosenVacanciesList)},
        bottomBar = {})
}

@Composable
private fun MainContent(aiAnswer: VacancyAiAnswer, chosenJobCardsList: List<JobCard>){

    Column(modifier = Modifier.fillMaxWidth().padding(top = 35.dp)
        .padding(horizontal = 10.dp)) {

        if (aiAnswer.explanation.isNotBlank()){
            Text(text = "Ai answer:",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            AiExplanation(aiAnswer.explanation)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
            thickness = 3.dp,
            color = AccentGreen
        )

        if (chosenJobCardsList.isNotEmpty()){
            Text(text = "Vacancies:",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            MatchedVacancies(chosenJobCardsList)
        }
    }
}

@Composable
private fun AiExplanation(aiExplanation: String) {
    Surface(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.onPrimary) {

        Text(modifier = Modifier.padding(15.dp),
            text = aiExplanation,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
private fun MatchedVacancies(matchedVacanciesList: List<JobCard>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        matchedVacanciesList.forEach { vacancyCard->
            UiVacancyCard(vacancyCard, Color.Blue)

            HorizontalDivider(modifier = Modifier.padding(vertical = 15.dp),
                thickness = 3.dp,
                color = Color.Blue
            )
        }
    }
}
