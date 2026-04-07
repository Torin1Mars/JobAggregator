package com.example.jobaggregator

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements

suspend fun parsingExample(): Elements {

    val doc = Ksoup.parseGetRequest(url = "https://www.work.ua/jobs-cherkasy-python/")

    val jobsList  = doc.selectFirst("html body main#center.main-center.bg-gray div#pjax.container div.row div.col-md-8 div#pjax-jobs-list")

    val chosen2 = jobsList!!.select("#pjax-jobs-list > a")

    return chosen2
}