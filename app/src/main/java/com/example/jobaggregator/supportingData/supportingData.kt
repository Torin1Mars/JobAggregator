package com.example.jobaggregator.supportingData

import kotlin.time.Duration.Companion.seconds

val maxCityInputLenght: Int  = 15
val maxJobTitleInputLenght: Int  = 100

val workUaUrl: String  = "https://www.work.ua"
val workUaParserRenderDelay = 60.seconds

val rabotaUaUrl: String  = "https://robota.ua"
val rabotaUaFullyRenderedVacancyPageLenght = 120_000
val rabotaUaMaxRuningWebViewsInOnes = 7
val rabotaUaParserRenderDelay = 20.seconds



val dateFormat : String = "dd-MM-yyyy"
val monthUa: List<String> = listOf("січня",
        "лютого",
        "березня",
        "квітня",
        "травня",
        "червня",
        "липня",
        "серпня",
        "вересня",
        "жовтня",
        "листопада",
        "грудня")