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

    fun getApiKey(): String = apiKey

    fun getModelName(): String = modelName

    suspend fun generateTranslation(
        systemPrompt: String,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "[Online Mode: No Gemini API Key configured. Tap the Settings icon in the center bar to enter your Gemini API key.]"
        }

        try {
            val model = generativeModel ?: GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            ).also { generativeModel = it }

            val response = model.generateContent(systemPrompt)
            response.text?.trim() ?: "[Translation returned empty]"
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Unknown error"
            // If the key doesn't have access to the specific gemma-4-it alias, provide seamless fallback
            if (errorMsg.contains("not found", ignoreCase = true) && modelName.startsWith("gemma-4")) {
                try {
                    val fallbackModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = apiKey
                    )
                    val fallbackResp = fallbackModel.generateContent(systemPrompt)
                    val text = fallbackResp.text?.trim() ?: "[Translation returned empty]"
                    "$text\n\n[Gemma 4 notice: Fallback generated using gemini-1.5-flash because $modelName was not found on this API key tier]"
                } catch (fallbackEx: Exception) {
                    "[Gemini API ($modelName) Error: $errorMsg. Please check your API key in Settings.]"
                }
            } else {
                "[Gemini API ($modelName) Error: $errorMsg]"
            }
        }
    }
}

