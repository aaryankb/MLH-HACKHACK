package com.moss.hybridtranslator.harness

import com.moss.hybridtranslator.data.model.Speaker
import com.moss.hybridtranslator.data.model.TranslationTurn
import com.moss.hybridtranslator.services.GeminiGemmaService
import com.moss.hybridtranslator.services.MediaPipeGemmaService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ModelHarnessTest {

    private lateinit var harness: ModelHarness
    private lateinit var skill: GlossaryLookupSkill
    private lateinit var geminiService: GeminiGemmaService
    private lateinit var mediaPipeService: MediaPipeGemmaService

    @Before
    fun setUp() {
        skill = GlossaryLookupSkill(null)
        geminiService = GeminiGemmaService(apiKey = "", modelName = "gemma-4-it")
        mediaPipeService = MediaPipeGemmaService(null)
        harness = ModelHarness(skill, geminiService, mediaPipeService)
    }

    @Test
    fun testPromptTemplateComplianceWithPlanMd() {
        val skillOutput = skill.execute(
            userContext = "Emergency medical diagnosis",
            sourceText = "Patient requires epinephrine",
            targetLangCode = "kn"
        )

        val history = listOf(
            TranslationTurn(
                id = "1",
                speaker = Speaker.USER_A,
                sourceLanguage = "en",
                targetLanguage = "kn",
                sourceText = "Hello doctor",
                translatedText = "ನಮಸ್ಕಾರ ವೈದ್ಯರೇ",
                isOffline = false
            )
        )

        val prompt = harness.buildSystemPrompt(
            targetLanguage = "Kannada",
            userContext = "Emergency medical diagnosis",
            skillOutput = skillOutput,
            history = history,
            sourceText = "Patient requires epinephrine"
        )

        // Strict template check from Section 5 of plan.md
        assertTrue(prompt.contains("You are an expert, context-aware translator."))
        assertTrue(prompt.contains("Translate the following text into Kannada."))
        assertTrue(prompt.contains("Conversation Context: Emergency medical diagnosis"))
        assertTrue(prompt.contains("Domain Terminology (Provided by Glossary Agent Skill):"))
        assertTrue(prompt.contains("Previous Chat History:"))
        assertTrue(prompt.contains("Hello doctor"))
        assertTrue(prompt.contains("Text to Translate: Patient requires epinephrine"))
    }

    @Test
    fun testOfflineRouteUsesMediaPipeAndSkillGlossary() = runBlocking {
        val result = harness.translate(
            userContext = "Emergency medical diagnosis",
            sourceText = "The patient has severe anaphylaxis",
            targetLanguageName = "Kannada",
            targetLanguageCode = "kn",
            isOfflineMode = true,
            history = emptyList()
        )

        assertTrue("Execution must be offline", result.isOffline)
        assertTrue("Engine must be MediaPipe Edge", result.engineName.contains("MediaPipe"))
        assertTrue("Skill should have matched medical domain", result.skillOutput.matchedDomain == "medical")
        assertTrue("Result should reflect translation output", result.translatedText.isNotBlank())
    }
}

