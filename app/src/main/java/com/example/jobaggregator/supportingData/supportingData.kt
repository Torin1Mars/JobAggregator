package com.example.jobaggregator.supportingData

import kotlin.time.Duration.Companion.seconds

val workUaUrl: String  = "https://www.work.ua"

val rabotaUaUrl: String  = "https://robota.ua"
val rabotaUaFullyRenderedVacancyPageLenght = 150_000
val rabotaUaMaxRuningWebViewsInOnes = 5
val rabotaUaParerRenderDelay = 20.seconds

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