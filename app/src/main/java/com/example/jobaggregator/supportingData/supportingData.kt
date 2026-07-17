package com.example.jobaggregator.supportingData

import kotlin.time.Duration.Companion.seconds

val maxCityInputLenght: Int  = 15
val maxJobTitleInputLenght: Int  = 100

val workUaUrl: String  = "https://www.work.ua"
val workUaParserCheckingPagesDelay = 5.seconds
val workUaParserRenderDelay = 60.seconds

val rabotaUaUrl: String  = "https://robota.ua"
val rabotaUaFullyRenderedGeneralPageLenght = 250_000
val rabotaUaFullyRenderedVacancyPageLenght = 100_000
val rabotaUaMaxRuningWebViewsInOnes = 6
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