package com.example.jobaggregator.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.aiModule.ChatGptViewModel2
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.ui.theme.AccentGreen
import com.example.jobaggregator.ui.theme.BackgroundBlack
import com.example.jobaggregator.ui.theme.BorderGray
import com.example.jobaggregator.ui.theme.SurfaceDark
import com.example.jobaggregator.ui.theme.SurfaceDarkElevated
import com.example.jobaggregator.ui.theme.TextPrimary
import com.example.jobaggregator.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun AiFilterUi(context: Context){
    val mainVM: MainViewModel = hiltViewModel()
    val gptVM :ChatGptViewModel2 = viewModel()

    var userPrompt by remember { mutableStateOf<String>("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
            thickness = 3.dp,
            color = AccentGreen
        )

        PromptTextField(
            initialValue = userPrompt,
            onValueChange = { newText -> userPrompt = newText },
            label = "AI prompt",
            placeholder = "You can say AI to choose better vacancies from whole poole",
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {runNewQuery(mainVM, gptVM, userPrompt)},
            enabled = userPrompt.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Green,
                contentColor = BackgroundBlack,
                disabledContainerColor = SurfaceDarkElevated,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                "Filter with AI",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }
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

@Composable
fun PromptTextField(
    initialValue: String,
    onValueChange: (String)-> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = initialValue,
        onValueChange = onValueChange,
        modifier = Modifier.padding(horizontal = 5.dp).defaultMinSize(minHeight = 100.dp),
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextSecondary) },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGreen,
            unfocusedBorderColor = BorderGray,
            disabledBorderColor = BorderGray,
            focusedLabelColor = AccentGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = AccentGreen,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            disabledContainerColor = SurfaceDark
        )
    )
}

