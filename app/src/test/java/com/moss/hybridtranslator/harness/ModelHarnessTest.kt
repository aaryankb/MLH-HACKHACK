package com.moss.hybridtranslator.harness

import com.moss.hybridtranslator.data.model.Speaker
import com.moss.hybridtranslator.data.model.TranslationTurn
import com.moss.hybridtranslator.services.GeminiGemmaService
import com.moss.hybridtranslator.services.LocalTranslationOutput
import com.moss.hybridtranslator.services.LocalTranslator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModelHarnessTest {

    private lateinit var harness: ModelHarness
    private lateinit var skill: GlossaryLookupSkill
    private lateinit var geminiService: GeminiGemmaService
    private lateinit var localTranslator: FakeLocalTranslator

    @Before
    fun setUp() {
        skill = GlossaryLookupSkill(null)
        geminiService = GeminiGemmaService(apiKey = "", modelName = "gemma-4-it")
        localTranslator = FakeLocalTranslator(
            output = """
                {
                  "primary": "ರೋಗಿಗೆ ತೀವ್ರ ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್ ಇದೆ.",
                  "alternatives": [
                    { "text": "ರೋಗಿಗೆ ತಕ್ಷಣದ ಚಿಕಿತ್ಸೆ ಅಗತ್ಯವಿದೆ.", "type": "Emergency", "confidence": 94 }
                  ]
                }
            """.trimIndent()
        )
        harness = ModelHarness(skill, geminiService, localTranslator)
    }

    @Test
    fun testSystemPromptContainsInstructionsButNotTheUserText() {
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
            history = history
        )

        assertTrue(prompt.contains("You are an expert, context-aware translator."))
        assertTrue(prompt.contains("Translate the following text into Kannada."))
        assertTrue(prompt.contains("Conversation Context: Emergency medical diagnosis"))
        assertTrue(prompt.contains("Domain Terminology (Provided by Glossary Agent Skill):"))
        assertTrue(prompt.contains("Previous Chat History:"))
        assertTrue(prompt.contains("Hello doctor"))
        assertTrue(prompt.contains("provided in the next user message"))

        // The source text must be sent as the user message, not duplicated in
        // the system instruction.
        assertFalse(prompt.contains("Patient requires epinephrine"))
    }

    @Test
    fun testOfflineRouteUsesLocalEngineAndPassesUserTextSeparately() = runBlocking {
        val sourceText = "The patient has severe anaphylaxis"

        val result = harness.translate(
            userContext = "Emergency medical diagnosis",
            sourceText = sourceText,
            targetLanguageName = "Kannada",
            targetLanguageCode = "kn",
            isOfflineMode = true,
            history = emptyList()
        )

        assertTrue("Execution must be offline", result.isOffline)
        assertTrue("Engine label must identify LiteRT-LM", result.engineName.contains("LiteRT-LM"))
        assertTrue("Skill should have matched medical domain", result.skillOutput.matchedDomain == "medical")
        assertEquals("ರೋಗಿಗೆ ತೀವ್ರ ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್ ಇದೆ.", result.translatedText)
        assertEquals("The source text must be the user message", sourceText, localTranslator.lastUserText)
        assertFalse(
            "The source text must not be duplicated in the system prompt",
            localTranslator.lastSystemPrompt.contains(sourceText)
        )
        assertEquals("Emergency medical diagnosis", localTranslator.lastUserContext)
    }

    private class FakeLocalTranslator(
        private val output: String
    ) : LocalTranslator {
        override val isModelReady = MutableStateFlow(true)

        var lastSystemPrompt: String = ""
        var lastUserText: String = ""
        var lastUserContext: String = ""

        override suspend fun generateTranslation(
            systemPrompt: String,
            userText: String,
            targetLanguage: String,
            targetLanguageCode: String,
            userContext: String,
            fallbackGlossary: Map<String, String>
        ): LocalTranslationOutput {
            lastSystemPrompt = systemPrompt
            lastUserText = userText
            lastUserContext = userContext
            return LocalTranslationOutput(
                text = output,
                engineLabel = "Gemma 4 E2B · LiteRT-LM (Test)"
            )
        }

        override fun close() = Unit
    }
}
