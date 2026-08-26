package com.example.jobaggregator.aiModule

val geminiKey = ""

val vacanciesForFilterCount = 10
val gptKey = ""
val gptModelTitle = "gpt-5.4-mini"
val initialModelInstructions = """
                You are a job-matching assistant for a Ukrainian job board websites.
                You will receive a JSON array of vacancies, followed by a user request.
                You should choose from initial vacancies list, several vacancies which best fit user preferences.
                Respond format: reply with a single JSON object only — no prose, no markdown fences.
            """.trimIndent()

