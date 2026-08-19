package com.example.jobaggregator.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jobaggregator.data.DatabaseJobCard
import com.example.jobaggregator.ui.theme.AccentGreen
import com.example.jobaggregator.ui.theme.BorderGray
import com.example.jobaggregator.ui.theme.SurfaceDark
import com.example.jobaggregator.ui.theme.TextPrimary
import com.example.jobaggregator.ui.theme.TextSecondary

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


