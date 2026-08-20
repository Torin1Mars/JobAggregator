package com.example.jobaggregator.aiModule

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig
)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GenerationConfig(
    val responseMimeType: String = "application/json",
    val responseSchema: ResponseSchema = ResponseSchema()
)

@Serializable
data class ResponseSchema(
    val type: String = "ARRAY",
    val items: SchemaItem = SchemaItem()
)

@Serializable
data class SchemaItem(val type: String = "STRING")

@Serializable
data class GeminiResponse(val candidates: List<Candidate> = emptyList())

@Serializable
data class Candidate(val content: GeminiContent)
