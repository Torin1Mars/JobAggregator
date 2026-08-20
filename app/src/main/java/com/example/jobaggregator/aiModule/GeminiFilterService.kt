package com.example.jobaggregator.aiModule

import android.R.attr.apiKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class GeminiFilterService(private val client: OkHttpClient = OkHttpClient(),
                          private val model: String = "gemini-flash-latest"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    val currentKey = geminiKey

    suspend fun filterItems(items: List<String>, userPrompt: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val instructions = buildString {
                    appendLine("You filter a list of items based on a user's request.")
                    appendLine("Return ONLY items that appear in the list below, unchanged.")
                    appendLine("If nothing matches, return an empty array.")
                    appendLine()
                    appendLine("List:")
                    items.forEach { appendLine("- $it") }
                    appendLine()
                    append("User request: $userPrompt")
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(instructions)))),
                    generationConfig = GenerationConfig()
                )

                val body = json.encodeToString(GeminiRequest.serializer(), request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url("$endpoint?key=$currentKey")
                    .post(body)
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Gemini request failed: ${response.code} ${response.message}")
                    }
                    val raw = response.body?.string().orEmpty()
                    val parsed = json.decodeFromString<GeminiResponse>(raw)
                    val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
                    json.decodeFromString<List<String>>(text)
                }
            }
        }
}

