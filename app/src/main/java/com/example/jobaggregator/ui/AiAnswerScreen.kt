package com.example.jobaggregator.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.aiModule.ChatGptViewModel
import com.example.jobaggregator.aiModule.VacancyAiAnswer
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.ui.theme.AccentGreen

@Composable
fun AiAnswerScreen(context: Context){

    val mainVM : MainViewModel = hiltViewModel()
    val gptVM : ChatGptViewModel = hiltViewModel()

    val gptAnswer = gptVM.aiReplyState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize(),
        content = {MainContent(gptAnswer.value)})

}

@Composable
private fun MainContent(aiAnswer: VacancyAiAnswer){

    //TODO Need to check why gpt answers dosen't show
    Log.d("MyTag", aiAnswer.explanation)

    Column(modifier = Modifier.fillMaxSize()) {
        AiExplanation(aiAnswer.explanation)

        Spacer(modifier = Modifier.height(15.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
            thickness = 3.dp,
            color = AccentGreen
        )
        //MatchedVacancies(aiAnswer.matchedList)
    }
}

@Composable
private fun AiExplanation(AiExplanation: String) {
    Text(text = AiExplanation,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun MatchedVacancies(matchedVacanciesList: List<JobCard>) {
    //TODO
}

