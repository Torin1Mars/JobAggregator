package com.example.jobaggregator.ui.com.example.jobaggregator.ui.com.example.jobaggregator.com.example.jobaggregator.ui.com.example.jobaggregator

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.util.TableInfo
import com.example.jobaggregator.ViewModels.MainViewModel

// --- Palette (inline so this file has no other dependencies) ---------------
private val BackgroundBlack = Color(0xFF121212)
private val SurfaceDark = Color(0xFF1C1C1E)
private val SurfaceDarkElevated = Color(0xFF232325)
private val BorderGray = Color(0xFF3A3A3C)
private val TextPrimary = Color(0xFFEDEDED)
private val TextSecondary = Color(0xFF9A9A9E)
private val AccentGreen = Color(0xFF2ECC71)
private val ErrorRed = Color(0xFFE05B4E)

// --- Model -------------------------------------------------------------
data class Vacancy(
    val id: String,
    val title: String,
    val company: String,
    val city: String,
    val salary: String? = null,
    val url: String
)

/**
 * Stateless job search screen — main inputs (Vacancy / City) up top, and once
 * [vacancies] is non-null (i.e. parsing has completed) a second "Filter
 * results" field appears above the list.
 *
 * Everything is hoisted: wire the params up to whatever ViewModel / state
 * holder you already have. `vacancies == null` means "no search run yet",
 * an empty list means "search ran, zero results".
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JobAggregatorScreen() {
    val mainVM : MainViewModel = viewModel()

    val isLoading  by remember { mutableStateOf<Boolean>(false) }
    var vacancyQuery by remember { mutableStateOf<String>("") }
    var cityQuery by remember { mutableStateOf<String>("") }

    val visibleVacancies = remember { mutableStateListOf<Vacancy>() }

    val errorMessage by remember { mutableStateOf<String?>("") }

    Scaffold(containerColor = BackgroundBlack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                text = "Job Search",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(24.dp))

            MinimalTextField(
                initialValue = vacancyQuery,
                onValueChange = {newText ->vacancyQuery = newText },
                label = "Vacancy",
                placeholder = "e.g. Android Developer",
                enabled = !isLoading
            )

            Spacer(Modifier.height(12.dp))

            MinimalTextField(
                initialValue = cityQuery,
                onValueChange = {newText ->cityQuery = newText },
                label = "City",
                placeholder = "e.g. Kyiv",
                enabled = !isLoading
            )

            Spacer(Modifier.height(20.dp))

            Column (modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(15.dp)){

                Button(
                    onClick = {mainVM.runVacanciesParsing()},
                    enabled = !isLoading && vacancyQuery.isNotBlank() && cityQuery.isNotBlank(),
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BackgroundBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Search", fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = {vacancyQuery = ""; cityQuery = ""},
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
                    Text("Clear", fontWeight = FontWeight.Medium)
                }

            }



            Spacer(Modifier.height(24.dp))

            when {
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed
                    )
                }

                visibleVacancies == null -> {
                    if (!isLoading) {
                        Text(
                            text = "Enter a vacancy and a city to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "Parsing vacancies from robota.ua…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                else -> {
                    // Additional field, shown only once parsing has completed.
                    MinimalTextField(
                        initialValue = vacancyQuery,
                        onValueChange = {},
                        label = "Filter results",
                        placeholder = "Filter by title or company"
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "${visibleVacancies.size} of 0 vacancies",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(10.dp))

                    if (visibleVacancies.isEmpty()) {
                        Text(
                            text = "No matches for this filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(visibleVacancies, key = { it.id }) { vacancy ->
                                VacancyCard(vacancy)
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalTextField(
    initialValue: String,
    onValueChange : (String)-> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = initialValue,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
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
private fun VacancyCard(vacancy: Vacancy) {
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
            text = vacancy.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = vacancy.company,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                text = vacancy.city,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            vacancy.salary?.let { salary ->
                Spacer(Modifier.width(12.dp))
                Text(
                    text = salary,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
