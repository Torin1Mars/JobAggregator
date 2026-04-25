package com.example.jobaggregator

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.jobaggregator.ViewModels.MainViewModel
import com.example.jobaggregator.ui.theme.JobAggregatorTheme
import dagger.hilt.android.AndroidEntryPoint
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.chromium.ChromiumDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.DesiredCapabilities
import java.net.URL

@AndroidEntryPoint
class MainActivity:ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CoroutineScope(Dispatchers.Default).launch {

        }


            /*
            val caps = DesiredCapabilities()
            caps.setCapability("platformName", "Android")
            caps.setCapability("automationName", "UiAutomator2") // Mandatory
            caps.setCapability("browserName", "Chrome")
            caps.setCapability("appium:noReset", true)

            val options = ChromeOptions()
            options.setExperimentalOption("androidPackage", "com.android.chrome")

            options.addArguments("--disable-popup-blocking")

            caps.setCapability(ChromeOptions.CAPABILITY, options)


            val driver = AndroidDri
            driver.get("https://selenium.dev")

            driver.quit()

            Log.d("MyTag", driver.title)*/


        setContent {
            val vm : MainViewModel = hiltViewModel()

            JobAggregatorTheme {
                // TODO
            }
        }
    }
}
