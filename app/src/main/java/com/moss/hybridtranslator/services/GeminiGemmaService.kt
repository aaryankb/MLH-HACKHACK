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
    private var modelName: String = "gemma-4-it"
) {

    private var generativeModel: GenerativeModel? = null

    init {
        updateApiKey(apiKey, modelName)
    }

    fun updateApiKey(newKey: String, newModelName: String = modelName) {
        this.apiKey = newKey.trim()
        this.modelName = newModelName.trim().ifBlank { "gemma-4-it" }

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

    fun getModelName(): String = modelName

    suspend fun generateTranslation(
        systemPrompt: String,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "[Online Mode: No Gemini API Key set in Settings. Please enter your Gemini API key to query $modelName.]"
        }

        try {
            val model = generativeModel ?: GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            ).also { generativeModel = it }

            val response = model.generateContent(
                """
                $systemPrompt
                """.trimIndent()
            )

            response.text?.trim() ?: "[Translation returned empty]"
        } catch (e: Exception) {
            // If the key doesn't have access to the specific gemma-4-it alias or runs into a quota error,
            // provide a graceful informative message.
            "[Gemini API ($modelName) Error: ${e.localizedMessage ?: e.message}]"
        }
    }
}

