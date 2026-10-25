package com.moss.hybridtranslator.harness

import android.content.Context
import com.moss.hybridtranslator.data.model.DomainDataset
import com.moss.hybridtranslator.data.model.SkillOutput
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * Standardized Agent Skill adhering to the Agent Skills Open Standard.
 * Corresponds to agent-skills/glossary-lookup/skill.json
 */
class GlossaryLookupSkill(private val context: Context? = null) {

    private val json = Json { ignoreUnknownKeys = true }
    private val loadedDomains = mutableMapOf<String, DomainDataset>()

    init {
        loadSkillDatasets()
    }

    private fun loadSkillDatasets() {
        val domainFiles = listOf("medical", "disaster_relief", "legal")
        for (domain in domainFiles) {
            try {
                val content = if (context != null) {
                    context.assets.open("agent-skills/glossary-lookup/data/$domain.json").bufferedReader().use { it.readText() }
                } else {
                    getFallbackDataset(domain)
                }
                val dataset = json.decodeFromString<DomainDataset>(content)
                loadedDomains[domain] = dataset
            } catch (e: Exception) {
                // Fallback to embedded definitions if asset loading fails
                val fallbackContent = getFallbackDataset(domain)
                val dataset = json.decodeFromString<DomainDataset>(fallbackContent)
                loadedDomains[domain] = dataset
            }
        }
    }

    /**
     * Executes the Glossary Lookup skill given user context, source speech, and target language.
     */
    fun execute(
        userContext: String,
        sourceText: String,
        targetLangCode: String
    ): SkillOutput {
        val combinedInput = "$userContext $sourceText".lowercase()
        var bestDomain: String? = null
        var maxKeywordMatches = 0

        for ((domain, dataset) in loadedDomains) {
            var matches = 0
            if (userContext.lowercase().contains(domain)) {
                matches += 3 // High weight if explicitly named in Context Setter
            }
            for (kw in dataset.keywords) {
                if (combinedInput.contains(kw.lowercase())) {
                    matches++
                }
            }
            if (matches > maxKeywordMatches) {
                maxKeywordMatches = matches
                bestDomain = domain
            }
        }

        if (bestDomain == null || maxKeywordMatches == 0) {
            return SkillOutput(
                matchedDomain = "general",
                glossaryTerms = emptyMap(),
                systemInstructionPrompt = "",
                isSkillTriggered = false
            )
        }

        val dataset = loadedDomains[bestDomain]!!
        val resolvedTerms = mutableMapOf<String, String>()

        for ((term, translations) in dataset.terms) {
            if (combinedInput.contains(term.lowercase())) {
                val translation = translations[targetLangCode] 
                    ?: translations["en"] 
                    ?: translations.values.firstOrNull() 
                    ?: term
                resolvedTerms[term] = translation
            }
        }

        // If context was explicitly set for this domain but no individual term appeared yet,
        // provide the top canonical terms for that domain to prime the LLM.
        if (resolvedTerms.isEmpty() && userContext.isNotBlank()) {
            for ((term, translations) in dataset.terms.entries.take(5)) {
                val translation = translations[targetLangCode] ?: translations.values.firstOrNull() ?: term
                resolvedTerms[term] = translation
            }
        }

        val promptBuilder = StringBuilder()
        if (resolvedTerms.isNotEmpty()) {
            promptBuilder.append("CRITICAL DOMAIN TERMINOLOGY (Agent Skill: glossary-lookup [${bestDomain.uppercase()}]):\n")
            promptBuilder.append("You MUST adhere strictly to the following translations for specialized concepts:\n")
            resolvedTerms.forEach { (src, tgt) ->
                promptBuilder.append("- \"$src\" -> \"$tgt\"\n")
            }
        }

        return SkillOutput(
            matchedDomain = bestDomain,
            glossaryTerms = resolvedTerms,
            systemInstructionPrompt = promptBuilder.toString().trim(),
            isSkillTriggered = resolvedTerms.isNotEmpty()
        )
    }

    private fun getFallbackDataset(domain: String): String {
        return when (domain) {
            "medical" -> """
                {
                  "domain": "medical",
                  "keywords": ["doctor", "patient", "allergy", "anaphylaxis", "injection", "medicine", "hospital", "clinic", "fever", "heart", "pain", "blood", "antibiotic", "sepsis", "epinephrine"],
                  "terms": {
                    "anaphylaxis": {
                      "kn": "ತೀವ್ರ ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ (ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್)",
                      "hi": "गंभीर एलर्जिक रिएक्शन (एनाफिलेक्सिस)",
                      "te": "తీవ్రమైన అలెర్జీ ప్రతిస్పందన",
                      "ta": "கடுமையான ஒவ்வாமை எதிர்வினை",
                      "es": "anafilaxia"
                    },
                    "epinephrine": {
                      "kn": "ಎಪಿನೆಫ್ರಿನ್ (ಜೀವ ಉಳಿಸುವ ಚುಚ್ಚುಮದ್ದು)",
                      "hi": "एपिनेफ्रिन (जीवन रक्षक इंजेक्शन)",
                      "te": "ఎపినెఫ్రిన్ ఇంజెక్షన్",
                      "ta": "எபினெப்ரின் ஊசி",
                      "es": "epinefrina"
                    },
                    "antibiotic": {
                      "kn": "ಆಂಟಿಬಯೋಟಿಕ್ (ಬ್ಯಾಕ್ಟೀರಿಯಾ ನಿರೋಧಕ)",
                      "hi": "एंटीबायोटिक (जीवाणुरोधी दवा)",
                      "te": "యాంటీబయాటిక్",
                      "ta": "நுண்ணுயிர் எதிர்ப்பி",
                      "es": "antibiótico"
                    },
                    "sepsis": {
                      "kn": "ರಕ್ತದಲ್ಲಿನ ಸೋಂಕು (ಸೆಪ್ಸಿಸ್)",
                      "hi": "रक्त संक्रमण (सेप्सिस)",
                      "te": "రక్త సంక్రమణ",
                      "ta": "இரத்த நச்சுத்தொற்று",
                      "es": "sepsis"
                    }
                  }
                }
            """.trimIndent()
            "disaster_relief" -> """
                {
                  "domain": "disaster_relief",
                  "keywords": ["flood", "earthquake", "evacuation", "shelter", "rations", "potable water", "relief", "rescue", "camp"],
                  "terms": {
                    "potable water": {
                      "kn": "ಕುಡಿಯಲು ಯೋಗ್ಯವಾದ ಶುದ್ಧ ನೀರು",
                      "hi": "पीने योग्य स्वच्छ पानी",
                      "te": "త్రాగునీరు",
                      "ta": "குடிநீர்",
                      "es": "agua potable"
                    },
                    "evacuation center": {
                      "kn": "ಸ್ಥಳಾಂತರ ಕೇಂದ್ರ / ಪರಿಹಾರ ಶಿಬಿರ",
                      "hi": "निकासी केंद्र / राहत शिविर",
                      "te": "పునరావాస కేంద్రం",
                      "ta": "நிவாரண முகாம்",
                      "es": "centro de evacuación"
                    },
                    "emergency rations": {
                      "kn": "ತುರ್ತು ಆಹಾರ ಪಡಿತರ",
                      "hi": "आपातकालीन खाद्य राशन",
                      "te": "అత్యవసర ఆహార రేషన్",
                      "ta": "அவசர உணவுப் பங்கீடுகள்",
                      "es": "raciones de comida de emergencia"
                    }
                  }
                }
            """.trimIndent()
            else -> """
                {
                  "domain": "legal",
                  "keywords": ["court", "police", "affidavit", "custody", "bail", "lawyer", "rights", "witness"],
                  "terms": {
                    "affidavit": {
                      "kn": "ಪ್ರಮಾಣಪತ್ರ (ಅಫಿಡವಿಟ್)",
                      "hi": "शपथ पत्र (हलफनामा)",
                      "te": "ప్రమాణ పత్రం",
                      "ta": "பிரமாண பத்திரம்",
                      "es": "declaración jurada"
                    },
                    "bail": {
                      "kn": "ಜಾಮೀನು",
                      "hi": "जमानत",
                      "te": "బెయిలు",
                      "ta": "ஜாமீன்",
                      "es": "fianza"
                    }
                  }
                }
            """.trimIndent()
        }
    }
}

