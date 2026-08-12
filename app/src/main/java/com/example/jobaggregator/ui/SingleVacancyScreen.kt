package com.example.jobaggregator.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SingleVacancyScreen(context: Context, vacancyIdInDb: Int) {

    //Modifier settings for current Screen
    val modifier = Modifier
    modifier.fillMaxSize().padding(horizontal = 10.dp)

    Scaffold (modifier = modifier,
        topBar ={TopBar()}, bottomBar = {BottomBar()},
        content = {MainContent()})
}

@Composable
private fun TopBar(){


}

@Composable
private fun MainContent(){


}

@Composable
private fun BottomBar(){

}
