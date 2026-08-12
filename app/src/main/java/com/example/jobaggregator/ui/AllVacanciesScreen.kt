package com.example.jobaggregator.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.jobaggregator.ui.theme.TextPrimary

@Composable
fun AllVacanciesScreen(context : Context, navHostController : NavHostController){

    //Modifier settings for current Screen
    val modifier = Modifier
    modifier.fillMaxSize().padding(horizontal = 10.dp)

    Scaffold (modifier = modifier,
        topBar ={TopBar()}, bottomBar = {BottomBar()},
        content = {MainContent()})
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
private fun MainContent(){

}

@Composable
private fun BottomBar(){

}
