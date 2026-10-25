package com.moss.hybridtranslator.data.model

import kotlinx.serialization.Serializable

enum class Speaker {
    USER_A, // Bottom user (e.g. Doctor, Aid Worker, Tourist)
    USER_B  // Top user (rotated 180°, e.g. Patient, Local Resident)
}

@Serializable
data class TranslationTurn(
    val id: String,
    val speaker: Speaker,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceText: String,
    val translatedText: String,
    val isOffline: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DomainDataset(
    val domain: String,
    val keywords: List<String>,
    val terms: Map<String, Map<String, String>>
)

@Serializable
data class SkillOutput(
    val matchedDomain: String,
    val glossaryTerms: Map<String, String>,
    val systemInstructionPrompt: String,
    val isSkillTriggered: Boolean
)

data class HarnessExecutionResult(
    val translatedText: String,
    val isOffline: Boolean,
    val engineName: String,
    val skillOutput: SkillOutput,
    val injectedSystemPrompt: String,
    val executionLatencyMs: Long
)

