package com.example.jobaggregator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.aiModule.VacancyAiAnswer
import com.example.jobaggregator.ui.theme.AccentGreen

@Composable
fun AiAnswerScreen(aiAnswer : VacancyAiAnswer){

    val mainVM : MainViewModel = hiltViewModel()

    Scaffold(modifier = Modifier,
        content = {MainContent(aiAnswer, mainVM)})

}

@Composable
private fun MainContent(aiAnswer: VacancyAiAnswer, mainViewModel: MainViewModel){

    Column(modifier = Modifier.fillMaxWidth()) {
        AiExplanation(aiAnswer.explanation)

        Spacer(modifier = Modifier.height(15.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
            thickness = 3.dp,
            color = AccentGreen
        )
        MatchedVacancies(aiAnswer.matchedList, mainViewModel)
    }
}

@Composable
private fun AiExplanation(AiExplanation: String) {
    //TODO

}

@Composable
private fun MatchedVacancies(matchedVacanciesId: List<String>, mainViewModel: MainViewModel) {
    //TODO
}

