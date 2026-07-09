package com.example.jobaggregator.Parsers

import android.content.Context
import com.example.jobaggregator.supportingData.maxCityInputLenght
import com.example.jobaggregator.supportingData.maxJobTitleInputLenght

class UserQueryManager(appContext: Context) {

    private val context = appContext

    fun test(){
        val result = convertUserInputToWorkUa("smila", "Android")
    }

    public fun convertUserInputToWorkUa(city: String =  "", jobTitle: String = ""): String{

        var workUaQuery = ""

        val jobFullQueryTemplate = "jobs-%s-%s"
        val jobShortQueryTemplate = "jobs-%s"

        //TODO need to add here .tolovercase to inputs and city formater which format city input to lathing letters

        if (city.isNotEmpty() && jobTitle.isNotEmpty()){
            workUaQuery = String.format(jobFullQueryTemplate, city, jobTitle)
        }else if(city.isNotEmpty()){
            workUaQuery = String.format(jobShortQueryTemplate, city)

        }else if(jobTitle.isNotEmpty()){
            workUaQuery = String.format(jobShortQueryTemplate, jobTitle)
        }

        return workUaQuery
    }

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

