package com.example.jobaggregator.ui

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.data.JobCard
import com.example.jobaggregator.ui.theme.AccentGreen
import com.example.jobaggregator.ui.theme.BorderGray
import com.example.jobaggregator.ui.theme.SurfaceDark
import com.example.jobaggregator.ui.theme.TextPrimary
import com.example.jobaggregator.ui.theme.TextSecondary

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AllVacanciesScreen(context : Context, navHostController : NavHostController, mainVM: MainViewModel){

    val mainViewModel = mainVM
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
    Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center) {
        Text(
            text = "Founded Vacancies",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MainContent(vacanciesList: List<DatabaseJobCard>) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 50.dp).padding(horizontal = 10.dp))
    {
        items(count = vacanciesList.size, key = {vacanciesList[it].idInDb}){vacancyItem->
            UiVacancyCard(vacanciesList[vacancyItem].jobCard, BorderGray)

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                thickness = 3.dp,
                color = AccentGreen
            )
        }
    }
}

@Composable
public fun UiVacancyCard(currentVacancyCard: JobCard, borderColor: Color) {
    val vacancyCard = currentVacancyCard

    val vacancyURl = vacancyCard.jobUrl
    val currentContext = LocalContext.current

    Spacer(Modifier.height(15.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable (
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true))
            {openVacancyCard(currentContext, vacancyURl)}
            .padding(15.dp)
    ) {
        Row() {
            Text(
                modifier = Modifier.weight(0.8f),
                text = vacancyCard.jobTitle,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )

            Text(
                modifier = Modifier.weight(0.2f),
                text = vacancyCard.publicationDate,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        vacancyCard.jobCompany?.let { it ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Company: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            vacancyCard.jobLocation?.let { it ->
                Text(
                    text = "Location: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            vacancyCard.jobSalary?.let { it ->
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Salary: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = vacancyCard.jobUrl,
            style = MaterialTheme.typography.labelSmall,
            color = AccentGreen,
            fontWeight = FontWeight.Medium)

        vacancyCard.jobDescription?.let { it->
            Spacer(Modifier.height(10.dp))

            Spacer(Modifier.height(10.dp))

            VacancyDescription(it)
        }

    }
}

private fun openVacancyCard(context: Context, vacancyURL: String) {
    val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(vacancyURL))
    context.startActivity(urlIntent)
}

@Composable
fun VacancyDescription(showingText: String) {
    var text by remember { mutableStateOf(showingText) }
    var isExpanded by remember { mutableStateOf(false) }

    val maxLines = if (isExpanded) Int.MAX_VALUE else 3

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Description :") },
        maxLines = maxLines,
        readOnly = true,
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {isExpanded = !isExpanded}
                )
            }
    )
}

