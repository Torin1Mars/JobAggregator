package com.example.jobaggregator.Parsers

import android.content.Context

class UserQueryManager(context: Context) {

    var citiesListRu = null
    var citiesListUa = null

    var JobsTitles  = null

    init {
        //load supporting data
        loadCitiesList()
    }

    private fun loadCitiesList(context) {

        context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    public fun validateUserInputCity(userInput: String): Boolean{
        var status: Boolean = false

        return status
    }

    public fun validateUserInputJobTitle(): Boolean{
        var status: Boolean = false




        return status
    }
}