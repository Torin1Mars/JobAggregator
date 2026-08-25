package com.example.jobaggregator.aiModule

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiFilterService(GeminiApiKey: String) {
    private val apiKey = GeminiApiKey

    private val client: OkHttpClient = OkHttpClient()
    private val model: String = "gemini-3.7-flash"

    private val json = Json { ignoreUnknownKeys = true }
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    private val maxRequestAttempts = 3

    suspend fun filterItems(items: List<String>, userPrompt: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val instructions = buildString {
                    appendLine("Filter list according to user request :")
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
                    .url("$endpoint?key=$apiKey")
                    .post(body)
                    .build()

                Log.d("MyTag", body.toString())

                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Gemini request failed: ${response.code} ${response.message}")
                    }

                    val raw = executeWithRetry(httpRequest, maxRequestAttempts)
                    val parsed = json.decodeFromString<GeminiResponse>(raw)
                    val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
                    json.decodeFromString<List<String>>(text)
                }
            }
        }

    private suspend fun executeWithRetry(
        request: Request,
        maxAttempts: Int ,
        initialDelayMs: Long = 1000
    ): String {
        var attempt = 0
        var delayMs = initialDelayMs
        while (true) {
            attempt++
            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    return it.body?.string().orEmpty()
                }

                // Retry on transient server-side errors
                val retryable = it.code == 503 || it.code == 429
                if (!retryable || attempt >= maxAttempts) {
                    error("Gemini request failed: ${it.code} ${it.message}")
                }
            }
            kotlinx.coroutines.delay(delayMs)
            delayMs *= 2 // exponential backoff
        }
    }
}

