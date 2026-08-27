package com.example.jobaggregator.aiModule

import kotlin.time.Duration.Companion.seconds

//Gpt settings
val vacanciesForFilterCount = 5
val gptAnswerDelay = 20.seconds
val gptKey = ""
val gptModelTitle = "gpt-5.4-mini"
val initialModelInstructions = """
                You are a job-matching assistant for a Ukrainian job board websites.
                You will receive a JSON array of vacancies, followed by a user request.
                You should choose from initial vacancies list, several vacancies which best fit user preferences.                
            """.trimIndent()

val respondFormatInstructions = """Respond format: reply with a single JSON object.
                Respond should contain list with matched vacancies id's else return empty list,and string with short explanation why you choose this variant no prose, no markdown fences."
                Format: matchedList: List<String>, explanation: String.
             """.trimIndent()
