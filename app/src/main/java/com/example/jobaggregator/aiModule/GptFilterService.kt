package com.example.jobaggregator.aiModule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.data.JobCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class GptFilterService() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun buildVacancyQueryMessages(
        vacancies: List<JobCard>,
        userRequest: String
    ): List<ChatMessage> {
        val vacanciesJson = json.encodeToString(ListSerializer(JobCard.serializer()),vacancies.take(vacanciesForFilterCount) )

        return listOf(
            ChatMessage(
                role = ChatRole.System,
                content = initialModelInstructions
            ),

            ChatMessage(
                role = ChatRole.User,
                content = userRequest
            ),

            ChatMessage(
                role = ChatRole.User,
                content = "Vacancies (JSON):\n$vacanciesJson"
            )
        )
    }

    suspend fun queryVacanciesWithAi(
        openAI: OpenAI,
        vacancies: List<JobCard>,
        userRequest: String
    ): VacancyAiAnswer {
        val request = ChatCompletionRequest(
            model = ModelId(gptModelTitle),
            messages = buildVacancyQueryMessages(vacancies, userRequest),
            responseFormat = ChatResponseFormat.JsonObject
        )

        val completion = openAI.chatCompletion(request)
        val content = completion.choices.first().message.content
            ?: error("Empty response from model")

        return json.decodeFromString(VacancyAiAnswer.serializer(), content)
    }
}

//Model's answer model
@Serializable
data class VacancyAiAnswer(
    val matchedVacancyIds: List<String>,
    val explanation: String
)
