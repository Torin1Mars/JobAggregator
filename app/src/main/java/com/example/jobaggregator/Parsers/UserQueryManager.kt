package com.example.jobaggregator.Parsers

import android.content.Context
import android.icu.text.Transliterator
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.jobaggregator.supportingData.maxCityInputLenght
import com.example.jobaggregator.supportingData.maxJobTitleInputLenght
import com.example.jobaggregator.supportingData.rabotaUaUrl

class UserQueryManager(appContext: Context) {

    private val context = appContext

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertUserQueryInput(queryCity: String = "", queryJobTitle: String = ""): List<String>{
        val workUaQuery = convertUserInputForWorkUa(city = queryCity, jobTitle = queryJobTitle)
        val rabotaUaQuery = convertUserInputForRabotaUa(city = queryCity, jobTitle = queryJobTitle)

        return mutableListOf<String>(workUaQuery, rabotaUaQuery)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public fun convertUserInputForWorkUa(city: String =  "", jobTitle: String = ""): String{

        val transliterator = Transliterator.getInstance("Ukrainian-Latin/BGN")
        var workUaQuery = ""

        val jobFullQueryTemplate = "jobs-%s-%s"
        val jobShortQueryTemplate = "jobs-%s"

        var convertedCity = ""
        var convertedJobTitle = ""

        if (city.isNotEmpty() && jobTitle.isNotEmpty()){
            convertedCity = transliterator.transliterate(city.lowercase())
            convertedJobTitle = transliterator.transliterate(jobTitle.lowercase())

            workUaQuery = String.format(jobFullQueryTemplate, convertedCity, convertedJobTitle)
        }
        else if(city.isNotEmpty()){
            convertedCity = transliterator.transliterate(city.lowercase())

            workUaQuery = String.format(jobShortQueryTemplate, convertedCity)
        }
        else if(jobTitle.isNotEmpty()){

            convertedJobTitle = transliterator.transliterate(jobTitle.lowercase())

            workUaQuery = String.format(jobShortQueryTemplate, convertedJobTitle)
        }

        return workUaQuery
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public fun convertUserInputForRabotaUa(city: String =  "", jobTitle: String = ""): String{

        val transliterator = Transliterator.getInstance("Ukrainian-Latin/BGN")
        var workUaQuery = ""

        val jobFullQueryTemplate = "$rabotaUaUrl/zapros/%s/%s"

        val jobShortTemplateWithCity = "$rabotaUaUrl/zapros/%s"
        val jobShortTemplateWithJob = "$rabotaUaUrl/zapros/%s/ukraine"

        var convertedCity = ""
        var convertedJobTitle = ""

        if (city.isNotEmpty() && jobTitle.isNotEmpty()){
            convertedCity = transliterator.transliterate(city.lowercase())
            convertedJobTitle = transliterator.transliterate(jobTitle.lowercase())

            workUaQuery = String.format(jobFullQueryTemplate, convertedJobTitle, convertedCity)
        }
        else if(city.isNotEmpty()){
            convertedCity = transliterator.transliterate(city.lowercase())

            workUaQuery = String.format(jobShortTemplateWithCity, convertedCity)
        }
        else if(jobTitle.isNotEmpty()){

            convertedJobTitle = transliterator.transliterate(jobTitle.lowercase())

            workUaQuery = String.format(jobShortTemplateWithJob, convertedJobTitle)
        }

        return workUaQuery
    }






    //////////////////////////////////////////ADDITIONAL////////////////////////////////////
    public fun checkUserInput (jobLocation : String, jobTitle : String): List<Boolean>{

        val checkingResult = mutableListOf<Boolean>()

        checkingResult.add(checkCityInput(jobLocation))
        checkingResult.add(checkJobTitleInput(jobTitle))

        return checkingResult
    }

    private fun checkCityInput (userInput : String): Boolean {
        var status = false

        userInput.any{it.isDigit()}
        if (userInput.isNotBlank() ){
            if (userInput.length > maxCityInputLenght){
                //Do nothing
            }else{
                if (userInput.any{it.isLetter()}){
                    status = true
                }
            }
        }
        return status
    }

    private fun checkJobTitleInput (userInput : String): Boolean {
        var status = false

        userInput.any{it.isDigit()}
        if (userInput.isNotBlank() ){
            if (userInput.length > maxJobTitleInputLenght){
                //Do nothing
            }else{
                status = true
            }
        }
        return status
    }



}

