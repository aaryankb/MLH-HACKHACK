package com.moss.hybridtranslator.services

import org.junit.Assert.*
import org.junit.Test

class LocalEdgeTranslationEngineTest {

    @Test
    fun testHindiTranslationsDoNotOutputEnglish() {
        val testPhrases = listOf(
            "hello man",
            "where is the train station",
            "i need water",
            "call an ambulance",
            "how much is this",
            "my head hurts",
            "can you help me",
            "i want food"
        )

        for (phrase in testPhrases) {
            val result = LocalEdgeTranslationEngine.translate(
                sourceText = phrase,
                targetLanguageCode = "hi"
            )

            assertTrue("Primary translation should not be blank for '$phrase'", result.primary.isNotBlank())
            assertNotEquals("Translation for '$phrase' must not simply return the English input", phrase, result.primary)
            // Assert that output contains Devanagari Unicode range (\u0900-\u097F)
            val hasDevanagari = result.primary.any { it in '\u0900'..'\u097F' }
            assertTrue("Translation for '$phrase' must be in Devanagari script: ${result.primary}", hasDevanagari)
            assertTrue("Alternatives should be provided for '$phrase'", result.alternatives.isNotEmpty())
        }
    }

    @Test
    fun testKannadaTranslationsDoNotOutputEnglish() {
        val testPhrases = listOf(
            "hello man",
            "where is the hospital",
            "i need water",
            "call an ambulance",
            "how much is this",
            "what is your name",
            "thank you very much"
        )

        for (phrase in testPhrases) {
            val result = LocalEdgeTranslationEngine.translate(
                sourceText = phrase,
                targetLanguageCode = "kn"
            )

            assertTrue("Primary translation should not be blank for '$phrase'", result.primary.isNotBlank())
            assertNotEquals("Translation for '$phrase' must not simply return the English input", phrase, result.primary)
            // Assert that output contains Kannada Unicode range (\u0C80-\u0CFF)
            val hasKannada = result.primary.any { it in '\u0C80'..'\u0CFF' }
            assertTrue("Translation for '$phrase' must be in Kannada script: ${result.primary}", hasKannada)
            assertTrue("Alternatives should be provided for '$phrase'", result.alternatives.isNotEmpty())
        }
    }

    @Test
    fun testTeluguAndTamilTranslations() {
        val teResult = LocalEdgeTranslationEngine.translate("where is the hospital", "te")
        assertTrue("Telugu output must be in Telugu script", teResult.primary.any { it in '\u0C00'..'\u0C7F' })

        val taResult = LocalEdgeTranslationEngine.translate("where is the hospital", "ta")
        assertTrue("Tamil output must be in Tamil script", taResult.primary.any { it in '\u0B80'..'\u0BFF' })
    }

    @Test
    fun testSpanishTranslations() {
        val esResult = LocalEdgeTranslationEngine.translate("hello man", "es")
        assertTrue("Spanish output must contain Hola", esResult.primary.contains("Hola", ignoreCase = true))
    }
}

