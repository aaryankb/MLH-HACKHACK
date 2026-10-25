package com.moss.hybridtranslator.services

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cloud Translation Service targeting Google Gemma 4 via the Google Generative AI / Gemini API.
 * Meets requirements for the "Best Use of Gemma 4" hackathon challenge.
 */
class GeminiGemmaService(
    private var apiKey: String = "",
    private var modelName: String = "gemma-4-26b-a4b-it"
) {

    private var generativeModel: GenerativeModel? = null

    init {
        updateApiKey(apiKey, modelName)
    }

    fun updateApiKey(newKey: String, newModelName: String = modelName) {
        this.apiKey = newKey.trim()
        val cleanedModel = newModelName.trim().ifBlank { "gemma-4-26b-a4b-it" }
        // Map legacy or failing aliases to active working Gemma 4 model
        this.modelName = if (cleanedModel == "gemma-4-it" || cleanedModel == "gemma-4-31b-it") {
            "gemma-4-26b-a4b-it"
        } else {
            cleanedModel
        }

        if (apiKey.isNotBlank()) {
            try {
                this.generativeModel = GenerativeModel(
                    modelName = this.modelName,
                    apiKey = this.apiKey
                )
            } catch (e: Exception) {
                this.generativeModel = null
            }
        } else {
            this.generativeModel = null
        }
    }

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun getApiKey(): String = apiKey

    fun getModelName(): String = modelName

    suspend fun generateTranslation(
        systemPrompt: String,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "[Online Mode: No Gemini API Key configured. Tap the Settings icon in the center bar to enter your Gemini API key.]"
        }

        // Ordered fallback cascade: User selected model -> Verified Gemma 4 -> Gemini 3.5 Flash
        val modelCandidates = listOf(
            modelName,
            "gemma-4-26b-a4b-it",
            "gemini-3.5-flash",
            "gemini-flash-latest"
        ).distinct()

        var lastException: Exception? = null

        for (candidate in modelCandidates) {
            try {
                val model = GenerativeModel(
                    modelName = candidate,
                    apiKey = apiKey
                )

                val response = model.generateContent(systemPrompt)

                // For thinking models (like Gemma 4), extract the final text part
                val candidatePart = response.candidates.firstOrNull()?.content?.parts
                val finalAnswer = if (!candidatePart.isNullOrEmpty()) {
                    // Filter for the non-thought part or the last part
                    val lastPartText = candidatePart.lastOrNull()?.asTextOrNull()
                    if (!lastPartText.isNullOrBlank()) lastPartText else response.text
                } else {
                    response.text
                }

                val result = finalAnswer?.trim()
                if (!result.isNullOrBlank()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                lastException = e
                // Continue to next candidate model if 500 Internal Error or 404 Not Found occurs
                continue
            }
        }

        // If all candidates in cascade failed, return user-actionable guidance
        val errorMsg = lastException?.localizedMessage ?: lastException?.message ?: "Unknown error"
        "[Gemini API ($modelName) Error: $errorMsg. Please verify your API key in Settings.]"
    }

    private fun com.google.ai.client.generativeai.type.Part.asTextOrNull(): String? {
        return try {
            val field = this.javaClass.getDeclaredField("text")
            field.isAccessible = true
            field.get(this) as? String
        } catch (_: Exception) {
            this.toString()
        }
    }
}
