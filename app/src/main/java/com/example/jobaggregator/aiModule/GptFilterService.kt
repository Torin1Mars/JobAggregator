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

class GptFilterService(context: Context, vacanciesCardsList: List<JobCard>) {
    val currentContext = context
    val myVacanciesList = vacanciesCardsList

//////////////////New template:
    /*
    @Serializable
    data class Vacancy(
        val id: String,
        val title: String,
        val company: String,
        val city: String,
        val salary: String? = null,
        val url: String
    )*/

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Builds the message list sent to the model:
     *   1. system instructions
     *   2. the vacancies, serialized to JSON
     *   3. the user's request — last, as required
     */
    private fun buildVacancyQueryMessages(
        vacancies: List<JobCard>,
        userRequest: String
    ): List<ChatMessage> {
        val vacanciesJson = json.encodeToString(ListSerializer(JobCard.serializer()), vacancies)

        return listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                You are a job-matching assistant for a Ukrainian job board (robota.ua).
                You will receive a JSON array of vacancies, followed by a user request.
                Reply with a single JSON object only — no prose, no markdown fences.
            """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = "Vacancies (JSON):\n$vacanciesJson"
            ),
            ChatMessage(
                role = ChatRole.User,
                content = userRequest
            )
        )
    }

    /**
     * Sends vacancies + user request to the model and returns the parsed JSON answer.
     * `openai-client` requires the word "json" to appear somewhere in the messages
     * when responseFormat = ChatResponseFormat.JsonObject is used — it does here,
     * both in the system prompt and the "Vacancies (JSON)" label.
     *
     * @param openAI an OpenAI client instance (com.aallam.openai:openai-client)
     * @param model any JSON-mode-capable chat model
     */
    suspend fun queryVacanciesWithAi(
        openAI: OpenAI,
        vacancies: List<JobCard>,
        userRequest: String,
        modelId: String,
    ): VacancyAiAnswer {
        val request = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = buildVacancyQueryMessages(vacancies, userRequest),
            responseFormat = ChatResponseFormat.JsonObject
        )

        val completion = openAI.chatCompletion(request)
        val content = completion.choices.first().message.content
            ?: error("Empty response from model")

        return json.decodeFromString(VacancyAiAnswer.serializer(), content)
    }
}

/** Shape of the model's answer. Adjust the fields to whatever you actually need back. */
@Serializable
data class VacancyAiAnswer(
    val matchedVacancyIds: List<String>,
    val explanation: String
)
