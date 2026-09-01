package com.example.jobaggregator

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.aiModule.ChatGptViewModel
import com.example.jobaggregator.ui.AiAnswerScreen
import com.example.jobaggregator.ui.AllVacanciesScreen
import com.example.jobaggregator.ui.Screens
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
fun AppNavigatour(navController: NavHostController,
                  context: Context){

    NavHost(navController = navController,
        startDestination = "home_graph"){

        navigation (startDestination = Screens.MainScreen.route, route = "home_graph"){


            composable(route = Screens.MainScreen.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("home_graph")
                }
                val mainViewModel: MainViewModel = hiltViewModel(parentEntry)
                val gptViewModel: ChatGptViewModel = hiltViewModel(parentEntry)

                //Content
                MainScreen(context, navController, mainViewModel, gptViewModel)
            }

            composable(route = Screens.AllVacanciesListScreen.route) {backStackEntry->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("home_graph")
                }
                val mainViewModel: MainViewModel = hiltViewModel(parentEntry)

                AllVacanciesScreen(context, navController, mainViewModel)
            }

            composable(route = Screens.AiAnswerScreen.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("home_graph")
                }
                val mainViewModel: MainViewModel = hiltViewModel(parentEntry)
                val gptViewModel: ChatGptViewModel = hiltViewModel(parentEntry)

                //Content
                AiAnswerScreen(context, mainViewModel, gptViewModel)
            }
        }
    }

}
