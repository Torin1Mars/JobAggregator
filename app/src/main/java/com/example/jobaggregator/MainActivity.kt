package com.example.jobaggregator

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController
import com.example.jobaggregator.ui.AllVacanciesScreen
import com.example.jobaggregator.ui.Screens
import com.example.jobaggregator.ui.SingleVacancyScreen
import com.example.jobaggregator.ui.com.example.jobaggregator.ui.com.example.jobaggregator.com.example.jobaggregator.ui.com.example.jobaggregator.MainScreen
import com.example.jobaggregator.ui.theme.JobAggregatorTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class MainActivity:ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navHostController = rememberNavController()
            JobAggregatorTheme(
                darkTheme = true,
                content = {AppNavigatour(navHostController, this)}
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavigatour(navController: NavHostController, context: Context){
    NavHost(navController = navController,
        startDestination = Screens.MainScreen.route){

        composable(route = Screens.MainScreen.route){
            MainScreen(context, navController)
        }

        composable(route = Screens.AllVacanciesListScreen.route) {
            AllVacanciesScreen(context, navController)
        }

        composable(route = Screens.SingleVacancyScreen.route + "/" +"{vacancyDbId}") {
            backStackEntry ->
            val vacancyId = backStackEntry.arguments?.getString("vacancyId")

            vacancyId?.let {it->
                SingleVacancyScreen(context, vacancyIdInDb = it.toInt() )
            }?: Toast.makeText(context, "Error, try open this vacancy card again", Toast.LENGTH_SHORT)
        }
    }
}
