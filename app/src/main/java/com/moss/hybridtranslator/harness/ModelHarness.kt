package com.moss.hybridtranslator.harness

import com.moss.hybridtranslator.data.model.HarnessExecutionResult
import com.moss.hybridtranslator.data.model.TranslationTurn
import com.moss.hybridtranslator.services.GeminiGemmaService
import com.moss.hybridtranslator.services.MediaPipeGemmaService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Custom Model Harness managing prompt generation, Agent Skill tool execution,
 * conversation memory, and hybrid routing between Gemini Gemma 4 (Online) and
 * MediaPipe Gemma 4 E2B (Offline Edge).
 */
class ModelHarness(
    private val glossaryLookupSkill: GlossaryLookupSkill,
    private val geminiService: GeminiGemmaService,
    private val mediaPipeService: MediaPipeGemmaService
) {

    private val json = Json { prettyPrint = true }

    /**
     * Executes the translation workflow through the harness.
     */
    suspend fun translate(
        userContext: String,
        sourceText: String,
        targetLanguageName: String,
        targetLanguageCode: String,
        isOfflineMode: Boolean,
        history: List<TranslationTurn>
    ): HarnessExecutionResult {
        val startTime = System.currentTimeMillis()

        // Phase 1: Agent Skill Execution
        val skillOutput = glossaryLookupSkill.execute(
            userContext = userContext,
            sourceText = sourceText,
            targetLangCode = targetLanguageCode
        )

        // Phase 2: System Prompt Construction (Conforming to plan.md template)
        val systemPrompt = buildSystemPrompt(
            targetLanguage = targetLanguageName,
            userContext = userContext,
            skillOutput = skillOutput,
            history = history,
            sourceText = sourceText
        )

        // Phase 3: Hybrid Routing
        val translatedText: String
        val engineName: String

        if (isOfflineMode) {
            engineName = "MediaPipe Gemma 4 E2B (Edge)"
            translatedText = mediaPipeService.generateTranslation(
                systemPrompt = systemPrompt,
                userText = sourceText,
                targetLanguage = targetLanguageName,
                fallbackGlossary = skillOutput.glossaryTerms
            )
        } else {
            engineName = "Gemini API (Gemma 4 Cloud)"
            translatedText = geminiService.generateTranslation(
                systemPrompt = systemPrompt,
                userText = sourceText
            )
        }

        val latency = System.currentTimeMillis() - startTime

        return HarnessExecutionResult(
            translatedText = sanitizeOutput(translatedText),
            isOffline = isOfflineMode,
            engineName = engineName,
            skillOutput = skillOutput,
            injectedSystemPrompt = systemPrompt,
            executionLatencyMs = latency
        )
    }

    /**
     * Builds the exact prompt template specified in Section 5 of plan.md
     */
    fun buildSystemPrompt(
        targetLanguage: String,
        userContext: String,
        skillOutput: com.moss.hybridtranslator.data.model.SkillOutput,
        history: List<TranslationTurn>,
        sourceText: String
    ): String {
        val contextStr = if (userContext.isNotBlank()) userContext else "None provided"
        val glossaryJson = if (skillOutput.glossaryTerms.isNotEmpty()) {
            json.encodeToString(skillOutput.glossaryTerms)
        } else {
            "None required"
        }

        val historyWindow = history.takeLast(3).joinToString("\n") { turn ->
            "- [${turn.speaker} (${turn.sourceLanguage}->${turn.targetLanguage})]: \"${turn.sourceText}\" -> \"${turn.translatedText}\""
        }.ifBlank { "No prior turns" }

        return """
You are an expert, context-aware translator. 
Translate the following text into $targetLanguage. 
Respond ONLY with the translated text. Do not include notes or explanations. 

Conversation Context: $contextStr
Domain Terminology (Provided by Glossary Agent Skill): $glossaryJson
Previous Chat History:
$historyWindow

Text to Translate: $sourceText
""".trimIndent()
    }

    private fun sanitizeOutput(rawText: String): String {
        return rawText
            .replace(Regex("^(Translation:|Translated text:)", RegexOption.IGNORE_CASE), "")
            .trim()
            .trim('"', '\'')
    }
}

