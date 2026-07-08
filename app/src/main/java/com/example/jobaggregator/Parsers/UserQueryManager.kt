package com.example.jobaggregator.Parsers

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class UserQueryManager(appContext: Context) {

    private val context = appContext

    var citiesListRu = null
    var citiesListUa = null

    var JobsTitles  = null

    init {

    }

    fun test(){
        loadCitiesList(context)
    }

    private fun loadCitiesList(context: Context) {
        //TODO I think that this logic can be simplyfided with checking on only if user input is a char sequence

        val document  = context.assets.open("ukrainian_cities_ru.json").bufferedReader().use { it-> it.readText() }

        val userData = Json.decodeFromString< List<cityItem>>(document)
    }

    public fun validateUserInputCity(userInput: String): Boolean{
        var status: Boolean = false

        return status
    }

    public fun validateUserInputJobTitle(): Boolean{
        var status: Boolean = false

        return status
    }

    @Serializable
    internal data class cityItem (
        val title: String,
        val type: String,
        val region : String,
        val population : Int
    )


}