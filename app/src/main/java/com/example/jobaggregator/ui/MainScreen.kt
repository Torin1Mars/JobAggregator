package com.example.jobaggregator.ui.com.example.jobaggregator.ui.com.example.jobaggregator.com.example.jobaggregator.ui.com.example.jobaggregator

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.jobaggregator.Parsers.UserQueryManager
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.ui.Screens
import com.example.jobaggregator.ui.theme.AccentGreen
import com.example.jobaggregator.ui.theme.BackgroundBlack
import com.example.jobaggregator.ui.theme.BorderGray
import com.example.jobaggregator.ui.theme.ErrorRed
import com.example.jobaggregator.ui.theme.SurfaceDark
import com.example.jobaggregator.ui.theme.SurfaceDarkElevated
import com.example.jobaggregator.ui.theme.TextPrimary
import com.example.jobaggregator.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreen (context: Context,  navHostController: NavHostController ) {
    Scaffold(containerColor = BackgroundBlack,
        topBar = {},
        content = {MainScreenMainContent(context, navHostController)},
        bottomBar = {})
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreenMainContent(context: Context, navHostController: NavHostController ){
    val mainVM: MainViewModel = hiltViewModel()

    var vacancyQuery by remember { mutableStateOf<String>("") }
    var cityQuery by remember { mutableStateOf<String>("") }
    var userPrompt by remember { mutableStateOf<String>("") }

    val errorMessage by remember { mutableStateOf<String?>("") }

    val parsersLoadingStatus by mainVM.parsersBusyStatus.collectAsState()
    val vacanciesCountHasBeenChecked by mainVM.vacanciesCountHasBeenChecked.collectAsState()

    val workUaFoundedVacanciesCount by mainVM.workUaVacanciesCount.collectAsState()
    val workUaErrors by mainVM.workUaErrorMessage.collectAsState()

    val rabotaUaFoundedVacanciesCount by mainVM.rabotaUaVacanciesCount.collectAsState()
    val rabotaUaErrors by mainVM.rabotaUaErrorMessage.collectAsState()

    val vacanciesDbCount  by mainVM.dbCountFlow.collectAsState(0, Dispatchers.IO)

    val showPromptInput by mutableStateOf<Boolean>(false)

    fun resetUserInputs() {
        vacancyQuery = ""
        cityQuery = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(25.dp))

        Text(
            text = "Job Search",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        UserTextField(
            initialValue = vacancyQuery,
            onValueChange = { newText -> vacancyQuery = newText },
            label = "Vacancy",
            placeholder = "e.g. Android Developer",
            enabled = !parsersLoadingStatus
        )

        Spacer(Modifier.height(12.dp))

        UserTextField(
            initialValue = cityQuery,
            onValueChange = { newText -> cityQuery = newText },
            label = "City",
            placeholder = "e.g. Kyiv",
            enabled = !parsersLoadingStatus
        )

        Spacer(Modifier.height(15.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { checkVacancies("", "", mainVM) },
                enabled = !parsersLoadingStatus && vacancyQuery.isNotBlank() || !parsersLoadingStatus && cityQuery.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = BackgroundBlack,
                    disabledContainerColor = SurfaceDarkElevated,
                    disabledContentColor = TextSecondary
                )
            ) {
                if (parsersLoadingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Check vacancies",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Button(
                onClick = { resetUserInputs() },
                enabled = vacancyQuery.isNotBlank() || cityQuery.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = BackgroundBlack,
                    disabledContainerColor = SurfaceDarkElevated,
                    disabledContentColor = TextSecondary
                )
            ) {
                Text(
                    "Clear",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            //if (vacanciesCountHasBeenChecked) {
            if (true) {
                Column(
                    modifier = Modifier.padding(vertical = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    workUaFoundedVacanciesCount?.let { it ->
                        Text(
                            "$it vacancies was found !",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 20.sp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 3.dp,
                        color = AccentGreen
                    )

                    Button(
                        onClick = { mainVM.runVacanciesParsing(context) },
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = BackgroundBlack,
                            disabledContainerColor = SurfaceDarkElevated,
                            disabledContentColor = TextSecondary
                        )
                    ) {
                        Text(
                            "Get vacancies",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Button(
                        onClick = {openFoundedVacanciesScreen(context, navHostController, vacanciesDbCount)},
                        enabled = if (vacanciesDbCount>0) true else false,
                        modifier = Modifier
                            .padding(top = 10.dp)
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
                            "Open vacancies",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }        }

        if (vacanciesDbCount>0) {
            Column(modifier = Modifier.padding(vertical = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
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
                    onClick = { resetUserInputs() },
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

        Spacer(Modifier.height(15.dp))
        when {
            errorMessage != null -> {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun UserTextField(
    initialValue: String,
    onValueChange : (String)-> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean
) {
    OutlinedTextField(
        value = initialValue,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
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

@Composable
private fun PromptTextField(
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

@Composable
private fun UiVacancyCard(vacancy: DatabaseJobCard) {
    val jobCard = vacancy.jobCard
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(16.dp)
    ) {
        Text(
            text = jobCard.jobTitle,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )

        jobCard.jobCompany?.let {
            Spacer(Modifier.height(10.dp))
            Text(
            text = jobCard.jobCompany,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
            Spacer(Modifier.height(10.dp))}

        Row {

            jobCard.jobLocation?.let {it->
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            jobCard.jobSalary?.let {it->
                Spacer(Modifier.width(10.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun checkVacancies(vacancyCityQuery: String, vacancyTitleQuery: String, mainVm: MainViewModel){
    val manager = UserQueryManager()
    val convertedQuery = manager.convertUserQueryInput("сміла")

    mainVm.runCheckVacanciesCount(workUaQuery = convertedQuery[0])
}

@RequiresApi(Build.VERSION_CODES.O)
private fun openFoundedVacanciesScreen(context: Context, navHostController: NavHostController, dbCount: Int){

    if (dbCount > 0 ){
        navHostController.navigate(Screens.AllVacanciesListScreen.route)
    }else {
        Toast.makeText(context, "Don't have loaded vacancies yet !", Toast.LENGTH_SHORT)
    }
}
