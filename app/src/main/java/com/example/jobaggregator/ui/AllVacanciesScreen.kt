package com.example.jobaggregator.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.ui.theme.BorderGray
import com.example.jobaggregator.ui.theme.SurfaceDark
import com.example.jobaggregator.ui.theme.TextPrimary

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AllVacanciesScreen(context : Context, navHostController : NavHostController){

    val mainViewModel = hiltViewModel<MainViewModel>()
    val vacanciesList = mainViewModel.dbVacanciesFlow.collectAsState(initial = emptyList())
    //Modifier settings for current Screen
    val modifier = Modifier
    modifier.fillMaxSize().padding(horizontal = 10.dp)

    Scaffold (modifier = modifier,
        topBar ={TopBar()},
        content = {MainContent(vacanciesList.value)})
}

@Composable
private fun TopBar(){
    Text(
        text = "Job Search",
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MainContent(vacanciesList: List<DatabaseJobCard>) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 20.dp).padding(horizontal = 10.dp))
    {
        items(count = vacanciesList.size, key = {vacanciesList[it].idInDb}){vacancyItem->
            val vacancyJobCard = vacanciesList[vacancyItem].jobCard
            UiVacancyCard(vacancyJobCard)
        }
    }
}

@Composable
private fun UiVacancyCard(CurrentVacancyCard: JobCard) {
    Spacer(Modifier.height(10.dp))

    Text(
        text = CurrentVacancyCard.jobTitle,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Medium
    )

    //TODO Need to set up proper behaviour this screen

    /*
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(2.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(16.dp)
    ) {
        Text(
            text = CurrentVacancyCard.jobTitle,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )

        CurrentVacancyCard.jobCompany?.let { it->
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))}

        Row {
            CurrentVacancyCard.jobLocation?.let {it->
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            CurrentVacancyCard.jobSalary?.let {it->
                Spacer(Modifier.width(10.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }*/
}
