package com.example.jobaggregator.retrofit

import kotlinx.serialization.Serializable

@Serializable
data class AngularRespond(
        val id: Int,
        val message: String,
        val status: String
    )
