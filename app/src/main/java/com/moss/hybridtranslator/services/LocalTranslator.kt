package com.moss.hybridtranslator.services

import kotlinx.coroutines.flow.StateFlow

/**
 * Result of an on-device translation.
 *
 * [engineLabel] is deliberately part of the result so the harness and UI can
 * report the engine that actually produced the text instead of hard-coding a
 * model name.
 */
data class LocalTranslationOutput(
    val text: String,
    val engineLabel: String,
    val usedFallback: Boolean = false
)

/**
 * Contract used by [com.moss.hybridtranslator.harness.ModelHarness] for the
 * offline path. Keeping this as an interface makes the harness unit-testable
 * without loading a multi-gigabyte model.
 */
interface LocalTranslator {
    val isModelReady: StateFlow<Boolean>

    suspend fun generateTranslation(
        systemPrompt: String,
        userText: String,
        targetLanguage: String,
        targetLanguageCode: String,
        userContext: String,
        fallbackGlossary: Map<String, String>
    ): LocalTranslationOutput

    fun close()
}
