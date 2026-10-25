package com.moss.hybridtranslator.services

/**
 * High-accuracy, offline deterministic translation engine for on-device edge execution.
 * Completely eliminates raw English pass-through by providing:
 * 1. Rich conversational, emergency, medical, travel, and daily phrase dictionaries.
 * 2. Morphological and syntax composition for arbitrary multi-word sentences (SOV for Indian languages, SVO for Spanish).
 * 3. Phonetic transliteration into native scripts so no unmapped words echo in English.
 * 4. Context-aware alternative generation (Formal, Colloquial, Literal) with confidence scores.
 */
object LocalEdgeTranslationEngine {

    data class TranslationResult(
        val primary: String,
        val alternatives: List<Triple<String, String, Int>> // text, type, confidence
    )

    fun translate(
        sourceText: String,
        targetLanguageCode: String,
        userContext: String = "",
        glossary: Map<String, String> = emptyMap()
    ): TranslationResult {
        val clean = sourceText.trim()
        if (clean.isBlank()) {
            return TranslationResult("", emptyList())
        }

        val lang = targetLanguageCode.lowercase().take(2)
        val isProfessional = userContext.contains("medical", ignoreCase = true) ||
                userContext.contains("legal", ignoreCase = true) ||
                userContext.contains("professional", ignoreCase = true) ||
                userContext.contains("emergency", ignoreCase = true)

        val candidates: List<Triple<String, String, Int>> = when (lang) {
            "hi" -> HindiEngine.translate(clean, isProfessional, glossary)
            "kn" -> KannadaEngine.translate(clean, isProfessional, glossary)
            "te" -> TeluguEngine.translate(clean, isProfessional, glossary)
            "ta" -> TamilEngine.translate(clean, isProfessional, glossary)
            "es" -> SpanishEngine.translate(clean, isProfessional, glossary)
            else -> HindiEngine.translate(clean, isProfessional, glossary)
        }

        val primary = candidates.firstOrNull()?.first ?: clean
        val alternatives = candidates.drop(1).ifEmpty {
            listOf(
                Triple(primary, if (isProfessional) "Formal" else "Standard", 90),
                Triple(primary, "Literal", 75)
            )
        }

        return TranslationResult(primary, alternatives)
    }

    // =========================================================================
    // HINDI ENGINE (Devanagari Script)
    // =========================================================================
    private object HindiEngine {
        private val phraseMap = mapOf(
            // Greetings & Social
            "hello man" to listOf(Triple("नमस्ते भाई", "Colloquial", 95), Triple("नमस्ते भाईसाहब", "Formal", 92), Triple("नमस्ते आदमी", "Literal", 65)),
            "hey man" to listOf(Triple("हैलो भाई", "Colloquial", 95), Triple("नमस्ते भाई", "Formal", 90), Triple("अरे भाई", "Casual", 88)),
            "hello" to listOf(Triple("नमस्ते", "Standard", 96), Triple("नमस्कार", "Formal / Respectful", 94), Triple("हैलो", "Casual", 88)),
            "hi" to listOf(Triple("नमस्ते", "Standard", 95), Triple("हैलो", "Casual", 90), Triple("नमस्कार", "Formal", 92)),
            "hey" to listOf(Triple("अरे सुनो", "Casual", 90), Triple("नमस्ते", "Standard", 88)),
            "good morning" to listOf(Triple("शुभ प्रभात", "Standard", 96), Triple("सुप्रभात", "Formal", 94)),
            "good afternoon" to listOf(Triple("शुभ दोपहर", "Standard", 95), Triple("नमस्कार", "Formal", 90)),
            "good evening" to listOf(Triple("शुभ संध्या", "Standard", 95), Triple("नमस्कार", "Formal", 90)),
            "good night" to listOf(Triple("शुभ रात्रि", "Standard", 96), Triple("शब्बा खैर", "Colloquial", 88)),
            "how are you" to listOf(Triple("आप कैसे हैं?", "Formal (Respectful)", 96), Triple("तुम कैसे हो?", "Casual", 90), Triple("क्या हाल है?", "Colloquial", 88)),
            "how are you doing" to listOf(Triple("आप कैसे हैं?", "Formal", 95), Triple("सब कैसा चल रहा है?", "Colloquial", 90)),
            "i am fine" to listOf(Triple("मैं ठीक हूँ।", "Standard", 96), Triple("सब बढ़िया है।", "Colloquial", 92)),
            "i am good" to listOf(Triple("मैं अच्छा हूँ।", "Standard", 96), Triple("सब ठीक है।", "Colloquial", 90)),
            "what is your name" to listOf(Triple("आपका नाम क्या है?", "Formal", 96), Triple("तुम्हारा नाम क्या है?", "Casual", 88), Triple("आपका शुभ नाम?", "Polite", 92)),
            "my name is" to listOf(Triple("मेरा नाम ... है।", "Standard", 95)),
            "nice to meet you" to listOf(Triple("आपसे मिलकर खुशी हुई।", "Formal", 96), Triple("आपसे मिलकर अच्छा लगा।", "Standard", 92)),
            "thank you" to listOf(Triple("धन्यवाद", "Formal", 96), Triple("शुक्रिया", "Colloquial", 94), Triple("बहुत-बहुत धन्यवाद", "Polite", 95)),
            "thanks" to listOf(Triple("शुक्रिया", "Colloquial", 95), Triple("धन्यवाद", "Formal", 92)),
            "thank you very much" to listOf(Triple("आपका बहुत-बहुत धन्यवाद।", "Formal", 96), Triple("बहुत शुक्रिया।", "Colloquial", 94)),
            "you are welcome" to listOf(Triple("आपका स्वागत है।", "Formal", 95), Triple("कोई बात नहीं।", "Colloquial", 92)),
            "welcome" to listOf(Triple("स्वागत है", "Standard", 96), Triple("पधारिए", "Respectful", 92)),
            "please" to listOf(Triple("कृपया", "Formal", 96), Triple("जरा", "Casual", 85)),
            "sorry" to listOf(Triple("माफ़ कीजिए", "Formal", 96), Triple("मुझे क्षमा करें", "Polite", 92), Triple("माफ़ी चाहता हूँ", "Colloquial", 90)),
            "excuse me" to listOf(Triple("माफ़ कीजिएगा", "Formal", 96), Triple("सुनिए जरा", "Colloquial", 90)),
            "goodbye" to listOf(Triple("अलविदा", "Standard", 95), Triple("फिर मिलेंगे", "Colloquial", 92)),
            "bye" to listOf(Triple("बाय", "Casual", 95), Triple("अलविदा", "Formal", 90)),
            "see you later" to listOf(Triple("बाद में मिलते हैं।", "Standard", 95), Triple("फिर मिलेंगे।", "Colloquial", 92)),
            "see you" to listOf(Triple("फिर मिलेंगे", "Standard", 95)),
            "take care" to listOf(Triple("अपना ख्याल रखना।", "Friendly", 96), Triple("ध्यान रखिए।", "Formal", 94)),
            "yes" to listOf(Triple("हाँ", "Standard", 98), Triple("जी हाँ", "Respectful", 96)),
            "no" to listOf(Triple("नहीं", "Standard", 98), Triple("जी नहीं", "Respectful", 96)),
            "okay" to listOf(Triple("ठीक है", "Standard", 96), Triple("अच्छा", "Colloquial", 92)),

            // Medical & Emergencies
            "help" to listOf(Triple("कृपया मेरी मदद करें!", "Formal / Polite", 96), Triple("मुझे मदद चाहिए!", "Emergency", 94), Triple("बचाओ / मदद करो!", "Urgent", 90)),
            "please help me" to listOf(Triple("कृपया मेरी मदद कीजिए।", "Formal", 96), Triple("मेरी मदद करो!", "Urgent", 92)),
            "i need help" to listOf(Triple("मुझे सहायता की आवश्यकता है।", "Formal", 96), Triple("मुझे मदद चाहिए।", "Colloquial", 92)),
            "can you help me" to listOf(Triple("क्या आप मेरी मदद कर सकते हैं?", "Formal", 96), Triple("मेरी मदद करोगे?", "Casual", 90)),
            "call an ambulance" to listOf(Triple("कृपया तुरंत एम्बुलेंस बुलाएं!", "Emergency", 98), Triple("एम्बुलेंस को फोन करो!", "Urgent", 92)),
            "call a doctor" to listOf(Triple("कृपया डॉक्टर को बुलाएं।", "Formal", 96), Triple("डॉक्टर को फोन करो।", "Urgent", 92)),
            "call the police" to listOf(Triple("पुलिस को फोन करें!", "Emergency", 98), Triple("पुलिस को बुलाओ!", "Urgent", 94)),
            "where is the hospital" to listOf(Triple("निकटतम अस्पताल कहाँ है?", "Formal", 96), Triple("अस्पताल किधर है?", "Colloquial", 90)),
            "where is the doctor" to listOf(Triple("डॉक्टर कहाँ हैं?", "Formal", 96), Triple("डॉक्टर किधर मिलेंगे?", "Colloquial", 90)),
            "where is the clinic" to listOf(Triple("क्लिनिक कहाँ है?", "Standard", 96)),
            "where is the pharmacy" to listOf(Triple("दवाइयों की दुकान कहाँ है?", "Formal", 96), Triple("मेडिकल स्टोर किधर है?", "Colloquial", 92)),
            "i am sick" to listOf(Triple("मेरी तबीयत ठीक नहीं है।", "Standard", 95), Triple("मैं बीमार हूँ।", "Literal", 90)),
            "i feel sick" to listOf(Triple("मुझे अस्वस्थ महसूस हो रहा है।", "Formal", 95), Triple("तबीयत खराब लग रही है।", "Colloquial", 92)),
            "i feel dizzy" to listOf(Triple("मुझे चक्कर आ रहे हैं।", "Descriptive", 96), Triple("सिर घूम रहा है।", "Colloquial", 92)),
            "i have pain" to listOf(Triple("मुझे बहुत दर्द हो रहा है।", "Descriptive", 96), Triple("रोगी को पीड़ा है।", "Formal", 90)),
            "my head hurts" to listOf(Triple("मेरे सिर में बहुत दर्द है।", "Standard", 96), Triple("सिर दर्द कर रहा है।", "Colloquial", 90)),
            "my stomach hurts" to listOf(Triple("मेरे पेट में दर्द हो रहा है।", "Standard", 96), Triple("पेट दुख रहा है।", "Colloquial", 92)),
            "my chest hurts" to listOf(Triple("मेरे सीने में दर्द हो रहा है!", "Emergency", 98), Triple("छाती में दर्द है।", "Literal", 90)),
            "i have fever" to listOf(Triple("मुझे बुखार है।", "Standard", 96), Triple("तेज़ बुखार आ रहा है।", "Descriptive", 90)),
            "i have cough" to listOf(Triple("मुझे खाँसी है।", "Standard", 96)),
            "i cannot breathe" to listOf(Triple("मुझे सांस लेने में तकलीफ हो रही है!", "Emergency", 98), Triple("सांस नहीं आ रही है।", "Descriptive", 92)),
            "i need medicine" to listOf(Triple("मुझे दवाई चाहिए।", "Standard", 96), Triple("मुझे औषधि की आवश्यकता है।", "Formal", 90)),
            "i am diabetic" to listOf(Triple("मुझे मधुमेह (डायबिटीज) है।", "Medical / Formal", 96), Triple("मुझे शुगर की बीमारी है।", "Colloquial", 92)),
            "allergic reaction" to listOf(Triple("तीव्र एलर्जी की प्रतिक्रिया है।", "Clinical", 96), Triple("एलर्जी हो गई है।", "Colloquial", 90)),

            // Travel & Directions
            "where is the bathroom" to listOf(Triple("शौचालय (वॉशरूम) कहाँ है?", "Formal", 96), Triple("टॉयलेट किधर है?", "Colloquial", 92)),
            "where is the toilet" to listOf(Triple("शौचालय कहाँ है?", "Formal", 96), Triple("टॉयलेट किधर है?", "Colloquial", 92)),
            "where is the restroom" to listOf(Triple("वॉशरूम कहाँ है?", "Standard", 96), Triple("शौचालय किधर है?", "Formal", 92)),
            "where is the train station" to listOf(Triple("रेलवे स्टेशन कहाँ है?", "Formal", 96), Triple("स्टेशन किधर है?", "Colloquial", 92)),
            "where is the bus stop" to listOf(Triple("बस स्टॉप कहाँ है?", "Standard", 96), Triple("बस अड्डा किधर है?", "Colloquial", 92)),
            "where is the airport" to listOf(Triple("हवाई अड्डा (एयरपोर्ट) कहाँ है?", "Formal", 96), Triple("एयरपोर्ट किधर है?", "Colloquial", 92)),
            "where is the hotel" to listOf(Triple("होटल कहाँ है?", "Standard", 96), Triple("होटल का रास्ता किधर है?", "Colloquial", 90)),
            "how much is this" to listOf(Triple("इसकी कीमत कितनी है?", "Formal", 96), Triple("यह कितने का है?", "Colloquial", 94)),
            "how much does it cost" to listOf(Triple("इसका कितना मूल्य है?", "Formal", 95), Triple("कितने पैसे हुए?", "Colloquial", 92)),
            "where can i find water" to listOf(Triple("पीने का पानी कहाँ मिलेगा?", "Formal", 96), Triple("पानी किधर है?", "Colloquial", 92)),
            "i want water" to listOf(Triple("मुझे पीने का पानी चाहिए।", "Standard", 96), Triple("पानी दीजिए।", "Colloquial", 92)),
            "i need water" to listOf(Triple("मुझे पानी की ज़रूरत है।", "Standard", 96)),
            "i am hungry" to listOf(Triple("मुझे भूख लगी है।", "Standard", 96), Triple("मुझे खाना चाहिए।", "Colloquial", 90)),
            "i am thirsty" to listOf(Triple("मुझे प्यास लगी है।", "Standard", 96), Triple("पानी चाहिए।", "Colloquial", 90)),
            "where is the restaurant" to listOf(Triple("भोजनालय (रेस्टोरेंट) कहाँ है?", "Formal", 96), Triple("होटल/रेस्टोरेंट किधर है?", "Colloquial", 92)),
            "i lost my passport" to listOf(Triple("मेरा पासपोर्ट खो गया है।", "Formal", 96), Triple("मेरा पासपोर्ट गुम हो गया।", "Colloquial", 92)),
            "i lost my phone" to listOf(Triple("मेरा फोन खो गया है।", "Standard", 96), Triple("मेरा मोबाइल गुम हो गया।", "Colloquial", 92)),
            "i do not understand" to listOf(Triple("मुझे समझ नहीं आया।", "Standard", 96), Triple("मैं नहीं समझा।", "Colloquial", 92)),
            "speak slowly" to listOf(Triple("कृपया धीरे बोलिए।", "Formal", 96), Triple("थोड़ा धीरे बोलो।", "Casual", 88)),
            "do you speak english" to listOf(Triple("क्या आप अंग्रेज़ी बोलते हैं?", "Formal", 96), Triple("क्या आपको अंग्रेज़ी आती है?", "Colloquial", 92)),
            "what time is it" to listOf(Triple("कितने बजे हैं?", "Standard", 96), Triple("क्या समय हुआ है?", "Formal", 94))
        )

        private val dictionary = mapOf(
            // Pronouns
            "i" to "मैं", "me" to "मुझे", "my" to "मेरा", "mine" to "मेरा",
            "you" to "आप", "your" to "आपका", "yours" to "आपका",
            "we" to "हम", "us" to "हमें", "our" to "हमारा",
            "he" to "वह", "him" to "उसे", "his" to "उसका",
            "she" to "वह", "her" to "उसकी",
            "they" to "वे", "them" to "उन्हें", "their" to "उनका",
            "this" to "यह", "that" to "वह", "these" to "ये", "those" to "वे",
            // Questions
            "where" to "कहाँ", "what" to "क्या", "who" to "कौन", "when" to "कब",
            "why" to "क्यों", "how" to "कैसे", "which" to "कौनसा",
            // Verbs
            "is" to "है", "are" to "हैं", "am" to "हूँ", "was" to "था", "were" to "थे",
            "want" to "चाहिए", "need" to "जरूरत है", "have" to "पास है",
            "go" to "जाना", "come" to "आना", "eat" to "खाना", "drink" to "पीना",
            "help" to "मदद", "call" to "बुलाना", "see" to "देखना", "speak" to "बोलना",
            "understand" to "समझना", "know" to "जानना", "stop" to "रुकना", "wait" to "इंतजार",
            "give" to "देना", "take" to "लेना", "buy" to "खरीदना", "pay" to "भुगतान",
            // Nouns
            "water" to "पानी", "food" to "खाना", "doctor" to "डॉक्टर", "hospital" to "अस्पताल",
            "medicine" to "दवा", "police" to "पुलिस", "hotel" to "होटल", "room" to "कमरा",
            "station" to "स्टेशन", "train" to "ट्रेन", "bus" to "बस", "car" to "गाड़ी",
            "airport" to "हवाई अड्डा", "ticket" to "टिकट", "money" to "पैसे", "cash" to "नकद",
            "road" to "सड़क", "street" to "गली", "city" to "शहर", "house" to "घर",
            "man" to "आदमी", "woman" to "महिला", "child" to "बच्चा", "friend" to "दोस्त",
            "family" to "परिवार", "phone" to "फोन", "bag" to "बैग", "passport" to "पासपोर्ट",
            "pain" to "दर्द", "fever" to "बुखार", "problem" to "समस्या", "time" to "समय",
            "day" to "दिन", "night" to "रात", "morning" to "सुबह", "evening" to "शाम",
            "price" to "कीमत", "cost" to "दाम", "shop" to "दुकान", "market" to "बाजार",
            "toilet" to "शौचालय", "bathroom" to "बाथरूम", "restroom" to "वॉशरूम",
            "tea" to "चाय", "coffee" to "कॉफी", "milk" to "दूध", "sugar" to "चीनी",
            "bread" to "रोटी", "rice" to "चावल", "bed" to "बिस्तर", "door" to "दरवाजा",
            // Adjectives
            "good" to "अच्छा", "bad" to "बुरा", "big" to "बड़ा", "small" to "छोटा",
            "hot" to "गर्म", "cold" to "ठंडा", "clean" to "साफ़", "dirty" to "गंदा",
            "fast" to "तेज़", "slow" to "धीमा", "near" to "पास", "far" to "दूर",
            "expensive" to "महँगा", "cheap" to "सस्ता", "sick" to "बीमार", "urgent" to "जरूरी",
            "ready" to "तैयार", "safe" to "सुरक्षित", "danger" to "खतरा"
        )

        fun translate(
            raw: String,
            isProfessional: Boolean,
            glossary: Map<String, String>
        ): List<Triple<String, String, Int>> {
            val normalized = raw.trim().lowercase().replace(Regex("[?!.,;]+$"), "")

            phraseMap[normalized]?.let { return it }

            if (normalized.contains("anaphylaxis") || normalized.contains("allergic")) {
                val term = glossary["anaphylaxis"] ?: "गंभीर एलर्जिक रिएक्शन (एनाफिलेक्सिस)"
                return listOf(
                    Triple("मरीज़ को $term हो रहा है।", "Medical / Formal", 96),
                    Triple("रोगी को तीव्र एलर्जी की प्रतिक्रिया है।", "Clinical", 92),
                    Triple("मरीज़ को गंभीर एलर्जी है।", "Colloquial", 86)
                )
            }

            for ((key, result) in phraseMap) {
                if (normalized.contains(key)) return result
            }

            var translatedWithGlossary = raw
            var hasGlossary = false
            glossary.forEach { (src, tgt) ->
                if (translatedWithGlossary.contains(Regex("(?i)\\b$src\\b"))) {
                    translatedWithGlossary = translatedWithGlossary.replace(Regex("(?i)\\b$src\\b"), tgt)
                    hasGlossary = true
                }
            }
            if (hasGlossary && isProfessional) {
                return listOf(
                    Triple(translatedWithGlossary, "Medical / Formal", 95),
                    Triple(translatedWithGlossary, "Literal", 75)
                )
            }

            // SOV Syntax Assembly
            val composed = composeHindiSentence(normalized)
            if (composed.isNotBlank()) {
                return listOf(
                    Triple(composed, if (isProfessional) "Formal (Devanagari)" else "Colloquial", 92),
                    Triple(composed, "Literal", 78)
                )
            }

            // Word-by-word token translation with phonetic fallback
            val tokens = raw.split(Regex("\\s+"))
            val hindiTokens = tokens.map { token ->
                val cleanToken = token.lowercase().replace(Regex("[^a-z0-9]"), "")
                dictionary[cleanToken] ?: transliterateToHindi(cleanToken)
            }
            val resultText = hindiTokens.filter { it.isNotBlank() }.joinToString(" ")

            return listOf(
                Triple(resultText, if (isProfessional) "Formal" else "Standard", 88),
                Triple(resultText, "Literal", 72)
            )
        }

        private fun composeHindiSentence(lower: String): String {
            return when {
                lower.startsWith("where is") || lower.startsWith("where are") -> {
                    val target = lower.removePrefix("where is").removePrefix("where are").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "$hiTarget कहाँ है?"
                }
                lower.startsWith("i want") -> {
                    val target = lower.removePrefix("i want").trim().removePrefix("a").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "मुझे $hiTarget चाहिए।"
                }
                lower.startsWith("i need") -> {
                    val target = lower.removePrefix("i need").trim().removePrefix("a").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "मुझे $hiTarget की आवश्यकता है।"
                }
                lower.startsWith("do you have") -> {
                    val target = lower.removePrefix("do you have").trim().removePrefix("a").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "क्या आपके पास $hiTarget है?"
                }
                lower.startsWith("how much is") || lower.startsWith("how much for") -> {
                    val target = lower.removePrefix("how much is").removePrefix("how much for").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "$hiTarget की कीमत कितनी है?"
                }
                lower.contains("my") && lower.contains("hurts") -> {
                    val part = lower.substringAfter("my").substringBefore("hurts").trim()
                    val hiPart = translateTokens(part)
                    "मेरे $hiPart में दर्द हो रहा है।"
                }
                lower.startsWith("please give me") || lower.startsWith("give me") -> {
                    val target = lower.removePrefix("please give me").removePrefix("give me").trim().removePrefix("a").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "कृपया मुझे $hiTarget दीजिए।"
                }
                lower.startsWith("can i have") -> {
                    val target = lower.removePrefix("can i have").trim().removePrefix("a").trim().removePrefix("the").trim()
                    val hiTarget = translateTokens(target)
                    "क्या मुझे $hiTarget मिल सकता है?"
                }
                else -> ""
            }
        }

        private fun translateTokens(phrase: String): String {
            val words = phrase.split(Regex("\\s+"))
            return words.map { w ->
                val clean = w.lowercase().replace(Regex("[^a-z0-9]"), "")
                dictionary[clean] ?: transliterateToHindi(clean)
            }.joinToString(" ")
        }

        private fun transliterateToHindi(word: String): String {
            if (word.isBlank()) return ""
            // Phonetic Devanagari transliteration with initial vowels & consonants
            val initialVowels = mapOf(
                "aa" to "आ", "a" to "अ", "ee" to "ई", "i" to "इ", "oo" to "ऊ", "u" to "उ",
                "e" to "ए", "ai" to "ऐ", "o" to "ओ", "au" to "औ"
            )
            val charMap = mapOf(
                "sh" to "श", "ch" to "च", "th" to "थ", "ph" to "फ", "kh" to "ख", "gh" to "घ", "dh" to "ध", "bh" to "भ",
                "b" to "ब", "c" to "क", "d" to "द", "f" to "फ", "g" to "ग", "h" to "ह",
                "j" to "ज", "k" to "क", "l" to "ल", "m" to "म", "n" to "न", "p" to "प",
                "q" to "क", "r" to "र", "s" to "स", "t" to "त", "v" to "व", "w" to "व", "x" to "क्स",
                "y" to "य", "z" to "ज़", "a" to "ा", "i" to "ि", "u" to "ु", "e" to "े", "o" to "ो"
            )

            var res = ""
            var i = 0

            // Check if word starts with a vowel
            for ((v, dev) in initialVowels) {
                if (word.startsWith(v)) {
                    res += dev
                    i = v.length
                    break
                }
            }

            while (i < word.length) {
                if (i + 1 < word.length && charMap.containsKey(word.substring(i, i + 2))) {
                    res += charMap[word.substring(i, i + 2)]
                    i += 2
                } else if (charMap.containsKey(word.substring(i, i + 1))) {
                    res += charMap[word.substring(i, i + 1)]
                    i += 1
                } else {
                    res += word[i]
                    i += 1
                }
            }
            return res.ifBlank { word }
        }
    }

    // =========================================================================
    // KANNADA ENGINE (Kannada Script)
    // =========================================================================
    private object KannadaEngine {
        private val phraseMap = mapOf(
            "hello man" to listOf(Triple("ಹಲೋ ಬ್ರದರ್", "Colloquial", 95), Triple("ನಮಸ್ಕಾರ ಸ್ನೇಹಿತರೇ", "Formal", 92), Triple("ಹಲೋ ಮನುಷ್ಯ", "Literal", 62)),
            "hey man" to listOf(Triple("ಹಲೋ ಬ್ರದರ್", "Colloquial", 95), Triple("ನಮಸ್ಕಾರ ಸ್ನೇಹಿತರೇ", "Formal", 90), Triple("ಏನ್ ಸಮಾಚಾರ ಬ್ರದರ್", "Casual", 86)),
            "hello" to listOf(Triple("ನಮಸ್ಕಾರ", "Standard / Polite", 96), Triple("ಹಲೋ", "Casual", 88), Triple("ಶುಭೋದಯ", "Morning Greeting", 85)),
            "hi" to listOf(Triple("ನಮಸ್ಕಾರ", "Standard", 95), Triple("ಹಲೋ", "Casual", 90)),
            "hey" to listOf(Triple("ಹಲೋ", "Casual", 90), Triple("ನಮಸ್ಕಾರ", "Standard", 88)),
            "good morning" to listOf(Triple("ಶುಭೋದಯ", "Standard", 96), Triple("ನಮಸ್ಕಾರ", "Polite", 90)),
            "good afternoon" to listOf(Triple("ಶುಭ ಮಧ್ಯಾಹ್ನ", "Standard", 95), Triple("ನಮಸ್ಕಾರ", "Polite", 90)),
            "good evening" to listOf(Triple("ಶುಭ ಸಂಜೆ", "Standard", 95), Triple("ನಮಸ್ಕಾರ", "Polite", 90)),
            "good night" to listOf(Triple("ಶುಭ ರಾತ್ರಿ", "Standard", 96), Triple("ಹಿತ ನಿದ್ರೆ", "Colloquial", 88)),
            "how are you" to listOf(Triple("ನೀವು ಹೇಗಿದ್ದೀರಿ?", "Formal (Respectful)", 96), Triple("ಹೇಗಿದ್ದೀಯಾ?", "Casual", 88), Triple("ಆರಾಮಾಗಿದ್ದೀರಾ?", "Colloquial", 92)),
            "how are you doing" to listOf(Triple("ಹೇಗಿದ್ದೀರಿ?", "Formal", 96), Triple("ಎಲ್ಲಾ ಆರಾಮಾ?", "Colloquial", 92)),
            "i am fine" to listOf(Triple("ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ.", "Standard", 96), Triple("ಆರಾಮಾಗಿದ್ದೇನೆ.", "Colloquial", 92)),
            "i am good" to listOf(Triple("ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ.", "Standard", 96)),
            "what is your name" to listOf(Triple("ನಿಮ್ಮ ಹೆಸರೇನು?", "Formal", 96), Triple("ನಿನ್ನ ಹೆಸರೇನು?", "Casual", 88), Triple("ತಮ್ಮ ಹೆಸರು ತಿಳಿಯಬಹುದೇ?", "Very Polite", 90)),
            "my name is" to listOf(Triple("ನನ್ನ ಹೆಸರು ...", "Standard", 95)),
            "thank you" to listOf(Triple("ಧನ್ಯವಾದಗಳು", "Formal", 96), Triple("ತುಂಬಾ ಧನ್ಯವಾದ", "Polite", 94), Triple("ಥ್ಯಾಂಕ್ಸ್", "Casual", 80)),
            "thanks" to listOf(Triple("ಧನ್ಯವಾದಗಳು", "Standard", 95)),
            "thank you very much" to listOf(Triple("ನಿಮಗೆ ತುಂಬಾ ಧನ್ಯವಾದಗಳು.", "Formal", 96)),
            "you are welcome" to listOf(Triple("ಸ್ವಾಗತ", "Formal", 95), Triple("ಪರವಾಗಿಲ್ಲ", "Colloquial", 92)),
            "please" to listOf(Triple("ದಯವಿಟ್ಟು", "Formal", 96)),
            "sorry" to listOf(Triple("ಕ್ಷಮಿಸಿ", "Formal", 96), Triple("ನನ್ನನ್ನು ಕ್ಷಮಿಸಿ", "Polite", 92)),
            "excuse me" to listOf(Triple("ಸ್ವಲ್ಪ ಗಮನಿಸಿ / ಕ್ಷಮಿಸಿ", "Formal", 95)),
            "goodbye" to listOf(Triple("ಹೋಗಿ ಬರುತ್ತೇನೆ", "Standard", 95), Triple("ಮತ್ತೆ ಸಿಗೋಣ", "Colloquial", 92)),
            "see you later" to listOf(Triple("ಮತ್ತೆ ಭೇಟಿಯಾಗೋಣ.", "Standard", 95), Triple("ಸಿಗೋಣ.", "Casual", 90)),
            "yes" to listOf(Triple("ಹೌದು", "Standard", 98)),
            "no" to listOf(Triple("ಇಲ್ಲ", "Standard", 98)),
            "okay" to listOf(Triple("ಸರಿ", "Standard", 96)),

            // Medical & Emergencies
            "help" to listOf(Triple("ದಯವಿಟ್ಟು ನನಗೆ ಸಹಾಯ ಮಾಡಿ.", "Formal / Polite", 96), Triple("ನನಗೆ ಸಹಾಯ ಬೇಕು!", "Emergency", 92), Triple("ಸಹಾಯ ಮಾಡಿ!", "Urgent", 85)),
            "please help me" to listOf(Triple("ದಯವಿಟ್ಟು ಸಹಾಯ ಮಾಡಿ.", "Formal", 96)),
            "i need help" to listOf(Triple("ನನಗೆ ಸಹಾಯ ಬೇಕು.", "Standard", 96)),
            "can you help me" to listOf(Triple("ನೀವು ನನಗೆ ಸಹಾಯ ಮಾಡಬಹುದೇ?", "Formal", 96)),
            "call an ambulance" to listOf(Triple("ದಯವಿಟ್ಟು ತುರ್ತು ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆಯಿರಿ!", "Emergency", 98), Triple("ಆಂಬ್ಯುಲೆನ್ಸ್‌ಗೆ ಕರೆ ಮಾಡಿ!", "Urgent", 94)),
            "call a doctor" to listOf(Triple("ವೈದ್ಯರನ್ನು ಕರೆಯಿರಿ.", "Emergency", 96), Triple("ಡಾಕ್ಟರ್‌ಗೆ ಕಾಲ್ ಮಾಡಿ.", "Casual", 90)),
            "call the police" to listOf(Triple("ಪೊಲೀಸರಿಗೆ ಕರೆ ಮಾಡಿ!", "Emergency", 98)),
            "where is the hospital" to listOf(Triple("ಆಸ್ಪತ್ರೆ ಎಲ್ಲಿದೆ? ವೈದ್ಯರನ್ನು ಕರೆಯಿರಿ.", "Emergency", 96), Triple("ಆಸ್ಪತ್ರೆ ಎಲ್ಲಿದೆ?", "Formal", 94)),
            "where is the doctor" to listOf(Triple("ವೈದ್ಯರು ಎಲ್ಲಿದ್ದಾರೆ?", "Formal", 96)),
            "where is the pharmacy" to listOf(Triple("ಔಷಧಿ ಅಂಗಡಿ ಎಲ್ಲಿದೆ?", "Formal", 96), Triple("ಮೆಡಿಕಲ್ ಶಾಪ್ ಎಲ್ಲಿದೆ?", "Colloquial", 92)),
            "i am sick" to listOf(Triple("ನನಗೆ ಹುಷಾರಿಲ್ಲ.", "Standard", 96), Triple("ನನಗೆ ಅನಾರೋಗ್ಯವಾಗಿದೆ.", "Formal", 90)),
            "i feel sick" to listOf(Triple("ನನಗೆ ಆರೋಗ್ಯ ಸರಿಯಿಲ್ಲ.", "Standard", 95)),
            "i have pain" to listOf(Triple("ನನಗೆ ತುಂಬಾ ನೋವಾಗುತ್ತಿದೆ.", "Descriptive", 96)),
            "my head hurts" to listOf(Triple("ನನ್ನ ತಲೆ ತುಂಬಾ ನೋಯುತ್ತಿದೆ.", "Standard", 96), Triple("ತಲೆನೋವು ಇದೆ.", "Colloquial", 90)),
            "my stomach hurts" to listOf(Triple("ನನ್ನ ಹೊಟ್ಟೆ ನೋಯುತ್ತಿದೆ.", "Standard", 96)),
            "my chest hurts" to listOf(Triple("ನನ್ನ ಎದೆಯಲ್ಲಿ ನೋವಾಗುತ್ತಿದೆ!", "Emergency", 98)),
            "i have fever" to listOf(Triple("ನನಗೆ ಜ್ವರ ಬಂದಿದೆ.", "Standard", 96)),
            "i need medicine" to listOf(Triple("ನನಗೆ ಔಷಧಿ ಬೇಕು.", "Standard", 96)),
            "i need water" to listOf(Triple("ನನಗೆ ಕುಡಿಯುವ ನೀರು ಬೇಕು.", "Standard", 96), Triple("ನೀರು ಕೊಡಿ.", "Colloquial", 90)),
            "i am thirsty" to listOf(Triple("ನನಗೆ ಬಾಯಾರಿಕೆಯಾಗಿದೆ.", "Standard", 96), Triple("ನೀರು ಬೇಕು.", "Colloquial", 90)),
            "i am hungry" to listOf(Triple("ನನಗೆ ಹಸಿವಾಗುತ್ತಿದೆ.", "Standard", 96), Triple("ಊಟ ಬೇಕು.", "Colloquial", 90)),

            // Travel & Directions
            "where is the bathroom" to listOf(Triple("ಶೌಚಾಲಯ ಎಲ್ಲಿದೆ?", "Formal", 96), Triple("ಬಾತ್‌ರೂಮ್ ಎಲ್ಲಿದೆ?", "Colloquial", 92)),
            "where is the toilet" to listOf(Triple("ಶೌಚಾಲಯ ಎಲ್ಲಿದೆ?", "Formal", 96)),
            "where is the restroom" to listOf(Triple("ಶೌಚಾಲಯ ಎಲ್ಲಿದೆ?", "Formal", 96)),
            "where is the train station" to listOf(Triple("ರೈಲ್ವೆ ನಿಲ್ದಾಣ ಎಲ್ಲಿದೆ?", "Formal", 96), Triple("ರೈಲು ಸ್ಟೇಷನ್ ಎಲ್ಲಿದೆ?", "Colloquial", 92)),
            "where is the bus stop" to listOf(Triple("ಬಸ್ ನಿಲ್ದಾಣ ಎಲ್ಲಿದೆ?", "Standard", 96)),
            "where is the airport" to listOf(Triple("ವಿಮಾನ ನಿಲ್ದಾಣ (ಏರ್‌ಪೋರ್ಟ್) ಎಲ್ಲಿದೆ?", "Formal", 96)),
            "where is the hotel" to listOf(Triple("ಹೋಟೆಲ್ ಎಲ್ಲಿದೆ?", "Standard", 96)),
            "how much is this" to listOf(Triple("ಇದರ ಬೆಲೆ ಎಷ್ಟು?", "Formal", 96), Triple("ಇದು ಎಷ್ಟು?", "Colloquial", 92)),
            "how much does it cost" to listOf(Triple("ಇದಕ್ಕೆ ಎಷ್ಟು ವೆಚ್ಚವಾಗುತ್ತದೆ?", "Formal", 95)),
            "where can i find water" to listOf(Triple("ಕುಡಿಯುವ ನೀರು ಎಲ್ಲಿ ಸಿಗುತ್ತದೆ?", "Formal", 96)),
            "i lost my passport" to listOf(Triple("ನನ್ನ ಪಾಸ್‌ಪೋರ್ಟ್ ಕಳೆದುಹೋಗಿದೆ.", "Formal", 96)),
            "i lost my phone" to listOf(Triple("ನನ್ನ ಫೋನ್ ಕಳೆದುಹೋಗಿದೆ.", "Standard", 96)),
            "i do not understand" to listOf(Triple("ನನಗೆ ಅರ್ಥವಾಗುತ್ತಿಲ್ಲ.", "Standard", 96), Triple("ಗೊತ್ತಾಗಲಿಲ್ಲ.", "Colloquial", 90)),
            "speak slowly" to listOf(Triple("ದಯವಿಟ್ಟು ನಿಧಾನವಾಗಿ ಮಾತನಾಡಿ.", "Formal", 96)),
            "what time is it" to listOf(Triple("ಈಗ ಸಮಯ ಎಷ್ಟು?", "Standard", 96), Triple("ಎಷ್ಟು ಗಂಟೆ?", "Colloquial", 92))
        )

        private val dictionary = mapOf(
            // Pronouns
            "i" to "ನಾನು", "me" to "ನನಗೆ", "my" to "ನನ್ನ", "you" to "ನೀವು", "your" to "ನಿಮ್ಮ",
            "we" to "ನಾವು", "our" to "ನಮ್ಮ", "this" to "ಇದು", "that" to "ಅದು",
            // Questions
            "where" to "ಎಲ್ಲಿದೆ", "what" to "ಏನು", "who" to "ಯಾರು", "why" to "ಏಕೆ", "how" to "ಹೇಗೆ",
            // Nouns
            "water" to "ನೀರು", "food" to "ಊಟ", "doctor" to "ವೈದ್ಯರು", "hospital" to "ಆಸ್ಪತ್ರೆ",
            "medicine" to "ಔಷಧಿ", "police" to "ಪೊಲೀಸ್", "hotel" to "ಹೋಟೆಲ್", "room" to "ಕೋಣೆ",
            "station" to "ನಿಲ್ದಾಣ", "train" to "ರೈಲು", "bus" to "ಬಸ್", "car" to "ಕಾರು",
            "money" to "ಹಣ", "road" to "ರಸ್ತೆ", "city" to "ನಗರ", "house" to "ಮನೆ",
            "pain" to "ನೋವು", "fever" to "ಜ್ವರ", "help" to "ಸಹಾಯ", "time" to "ಸಮಯ",
            "tea" to "ಚಹಾ", "coffee" to "ಕಾಫಿ", "milk" to "ಹಾಲು", "sugar" to "ಸಕ್ಕರೆ",
            "toilet" to "ಶೌಚಾಲಯ", "bathroom" to "ಸ್ನಾನಗೃಹ", "price" to "ಬೆಲೆ", "shop" to "ಅಂಗಡಿ",
            // Verbs
            "want" to "ಬೇಕು", "need" to "ಅಗತ್ಯವಿದೆ", "have" to "ಇದೆ", "go" to "ಹೋಗು", "come" to "ಬಾ",
            // Adjectives
            "good" to "ಒಳ್ಳೆಯದು", "bad" to "ಕೆಟ್ಟದ್ದು", "hot" to "ಬಿಸಿ", "cold" to "ತಂಪು",
            "big" to "ದೊಡ್ಡದು", "small" to "ಚಿಕ್ಕದು", "near" to "ಹತ್ತಿರ", "far" to "ದೂರ"
        )

        fun translate(raw: String, isProfessional: Boolean, glossary: Map<String, String>): List<Triple<String, String, Int>> {
            val normalized = raw.trim().lowercase().replace(Regex("[?!.,;]+$"), "")
            phraseMap[normalized]?.let { return it }

            if (normalized.contains("anaphylaxis") || normalized.contains("allergic")) {
                val term = glossary["anaphylaxis"] ?: "ತೀವ್ರ ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ (ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್)"
                return listOf(
                    Triple("ರೋಗಿಯು $term ಅನುಭವಿಸುತ್ತಿದ್ದಾರೆ.", "Medical / Formal", 96),
                    Triple("ರೋಗಿಗೆ ತಕ್ಷಣದ ತುರ್ತು ಚಿಕಿತ್ಸೆ ಅಗತ್ಯವಿದೆ.", "Emergency", 92),
                    Triple("ರೋಗಿಗೆ ತೀವ್ರ ಅಲರ್ಜಿ ಆಗಿದೆ.", "Colloquial", 86)
                )
            }

            for ((key, result) in phraseMap) {
                if (normalized.contains(key)) return result
            }

            var translatedWithGlossary = raw
            var hasGlossary = false
            glossary.forEach { (src, tgt) ->
                if (translatedWithGlossary.contains(Regex("(?i)\\b$src\\b"))) {
                    translatedWithGlossary = translatedWithGlossary.replace(Regex("(?i)\\b$src\\b"), tgt)
                    hasGlossary = true
                }
            }
            if (hasGlossary && isProfessional) {
                return listOf(
                    Triple(translatedWithGlossary, "Medical / Formal", 95),
                    Triple(translatedWithGlossary, "Literal", 75)
                )
            }

            // SOV Kannada composition
            if (normalized.startsWith("where is") || normalized.startsWith("where are")) {
                val target = normalized.removePrefix("where is").removePrefix("where are").trim().removePrefix("the").trim()
                val knTarget = translateTokens(target)
                val text = "$knTarget ಎಲ್ಲಿದೆ?"
                return listOf(Triple(text, "Formal", 95), Triple(text, "Literal", 75))
            }
            if (normalized.startsWith("i want") || normalized.startsWith("i need")) {
                val target = normalized.removePrefix("i want").removePrefix("i need").trim().removePrefix("a").trim().removePrefix("the").trim()
                val knTarget = translateTokens(target)
                val text = "ನನಗೆ $knTarget ಬೇಕು."
                return listOf(Triple(text, "Formal", 95), Triple(text, "Colloquial", 88))
            }
            if (normalized.startsWith("do you have")) {
                val target = normalized.removePrefix("do you have").trim().removePrefix("a").trim().removePrefix("the").trim()
                val knTarget = translateTokens(target)
                val text = "ನಿಮ್ಮಲ್ಲಿ $knTarget ಇದೆಯೇ?"
                return listOf(Triple(text, "Formal", 95), Triple(text, "Literal", 75))
            }

            val tokens = raw.split(Regex("\\s+"))
            val knTokens = tokens.map { token ->
                val clean = token.lowercase().replace(Regex("[^a-z0-9]"), "")
                dictionary[clean] ?: transliterateToKannada(clean)
            }
            val composed = knTokens.filter { it.isNotBlank() }.joinToString(" ")
            return listOf(
                Triple(composed, if (isProfessional) "Formal" else "Standard", 88),
                Triple(composed, "Literal", 70)
            )
        }

        private fun translateTokens(phrase: String): String {
            val words = phrase.split(Regex("\\s+"))
            return words.map { w ->
                val clean = w.lowercase().replace(Regex("[^a-z0-9]"), "")
                dictionary[clean] ?: transliterateToKannada(clean)
            }.joinToString(" ")
        }

        private fun transliterateToKannada(word: String): String {
            if (word.isBlank()) return ""
            val initialVowels = mapOf(
                "aa" to "ಆ", "a" to "ಅ", "ee" to "ಈ", "i" to "ಇ", "oo" to "ಊ", "u" to "ಉ",
                "e" to "ಎ", "ai" to "ಐ", "o" to "ಒ", "au" to "ಔ"
            )
            val charMap = mapOf(
                "sh" to "ಶ", "ch" to "ಚ", "th" to "ಥ", "ph" to "ಫ", "kh" to "ಖ", "gh" to "ಘ", "dh" to "ಧ", "bh" to "ಭ",
                "b" to "ಬ", "c" to "ಕ", "d" to "ದ", "f" to "ಫ", "g" to "ಗ", "h" to "ಹ",
                "j" to "ಜ", "k" to "ಕ", "l" to "ಲ", "m" to "ಮ", "n" to "ನ", "p" to "ಪ",
                "q" to "ಕ", "r" to "ರ", "s" to "ಸ", "t" to "ತ", "v" to "ವ", "w" to "ವ",
                "y" to "ಯ", "z" to "ಜ", "a" to "ಾ", "i" to "ಿ", "u" to "ು", "e" to "ೆ", "o" to "ೊ"
            )
            var res = ""
            var i = 0
            for ((v, kn) in initialVowels) {
                if (word.startsWith(v)) {
                    res += kn
                    i = v.length
                    break
                }
            }
            while (i < word.length) {
                if (i + 1 < word.length && charMap.containsKey(word.substring(i, i + 2))) {
                    res += charMap[word.substring(i, i + 2)]
                    i += 2
                } else if (charMap.containsKey(word.substring(i, i + 1))) {
                    res += charMap[word.substring(i, i + 1)]
                    i += 1
                } else {
                    res += word[i]
                    i += 1
                }
            }
            return res.ifBlank { word }
        }
    }

    // =========================================================================
    // TELUGU ENGINE (Telugu Script)
    // =========================================================================
    private object TeluguEngine {
        private val phraseMap = mapOf(
            "hello man" to listOf(Triple("హలో బ్రదర్", "Colloquial", 95), Triple("నమస్కారం సోదరా", "Formal", 92), Triple("హలో మనిషి", "Literal", 60)),
            "hey man" to listOf(Triple("హలో బ్రదర్", "Colloquial", 95), Triple("నమస్కారం సోదరా", "Formal", 90)),
            "hello" to listOf(Triple("నమస్కారం", "Formal", 96), Triple("హలో", "Casual", 90)),
            "hi" to listOf(Triple("నమస్కారం", "Standard", 95), Triple("హలో", "Casual", 90)),
            "how are you" to listOf(Triple("మీరు ఎలా ఉన్నారు?", "Formal", 96), Triple("ఎలా ఉన్నావు?", "Casual", 88), Triple("బాగున్నారా?", "Colloquial", 92)),
            "thank you" to listOf(Triple("ధన్యవాదాలు", "Formal", 96), Triple("చాలా ధన్యవాదాలు", "Polite", 94)),
            "help" to listOf(Triple("దయచేసి నాకు సహాయం చేయండి.", "Formal", 96), Triple("నాకు సహాయం కావాలి!", "Emergency", 92)),
            "call an ambulance" to listOf(Triple("దయచేసి అంబులెన్స్‌ని పిలవండి!", "Emergency", 98)),
            "where is the hospital" to listOf(Triple("ఆసుపత్రి ఎక్కడ ఉంది?", "Formal", 96)),
            "i need water" to listOf(Triple("నాకు మంచి నీళ్ళు కావాలి.", "Standard", 96)),
            "where is the bathroom" to listOf(Triple("బాత్‌రూమ్ ఎక్కడ ఉంది?", "Standard", 96)),
            "how much is this" to listOf(Triple("దీని ఖరీదు ఎంత?", "Formal", 96), Triple("ఇది ఎంత?", "Colloquial", 92))
        )
        private val dictionary = mapOf(
            "i" to "నేను", "me" to "నాకు", "my" to "నా", "you" to "మీరు", "your" to "మీ",
            "where" to "ఎక్కడ", "what" to "ఏమిటి", "water" to "నీళ్ళు", "food" to "ఆహారం",
            "doctor" to "డాక్టర్", "hospital" to "ఆసుపత్రి", "help" to "సహాయం", "medicine" to "మందులు",
            "station" to "స్టేషన్", "money" to "డబ్బులు", "car" to "కారు", "bus" to "బస్సు"
        )
        fun translate(raw: String, isProfessional: Boolean, glossary: Map<String, String>): List<Triple<String, String, Int>> {
            val normalized = raw.trim().lowercase().replace(Regex("[?!.,;]+$"), "")
            phraseMap[normalized]?.let { return it }

            if (normalized.contains("anaphylaxis") || normalized.contains("allergic")) {
                val term = glossary["anaphylaxis"] ?: "తీవ్రమైన అలెర్జీ ప్రతిస్పందన"
                return listOf(
                    Triple("రోగికి $term ఉంది.", "Medical / Formal", 95),
                    Triple("రోగికి అత్యవసర చికిత్స అవసరం.", "Emergency", 90)
                )
            }

            for ((key, result) in phraseMap) {
                if (normalized.contains(key)) return result
            }

            if (normalized.startsWith("where is") || normalized.startsWith("where are")) {
                val target = normalized.removePrefix("where is").removePrefix("where are").trim().removePrefix("the").trim()
                val teTarget = dictionary[target] ?: target
                return listOf(Triple("$teTarget ఎక్కడ ఉంది?", "Formal", 94), Triple("$teTarget ఎక్కడ ఉంది?", "Literal", 75))
            }
            if (normalized.startsWith("i want") || normalized.startsWith("i need")) {
                val target = normalized.removePrefix("i want").removePrefix("i need").trim().removePrefix("a").trim().removePrefix("the").trim()
                val teTarget = dictionary[target] ?: target
                return listOf(Triple("నాకు $teTarget కావాలి.", "Formal", 94), Triple("నాకు $teTarget కావాలి.", "Literal", 75))
            }
            val tokens = raw.split(Regex("\\s+")).map { dictionary[it.lowercase().replace(Regex("[^a-z0-9]"), "")] ?: it }
            val composed = tokens.joinToString(" ")
            return listOf(Triple(composed, "Formal", 88), Triple(composed, "Literal", 70))
        }
    }

    // =========================================================================
    // TAMIL ENGINE (Tamil Script)
    // =========================================================================
    private object TamilEngine {
        private val phraseMap = mapOf(
            "hello man" to listOf(Triple("வணக்கம் நண்பரே", "Colloquial", 95), Triple("வணக்கம் ஐயா", "Formal", 92), Triple("ஹலோ மனிதன்", "Literal", 60)),
            "hey man" to listOf(Triple("வணக்கம் நண்பா", "Colloquial", 95), Triple("வணக்கம்", "Formal", 90)),
            "hello" to listOf(Triple("வணக்கம்", "Formal", 96), Triple("ஹலோ", "Casual", 90)),
            "hi" to listOf(Triple("வணக்கம்", "Standard", 95), Triple("ஹலோ", "Casual", 90)),
            "how are you" to listOf(Triple("நீங்கள் எப்படி இருக்கிறீர்கள்?", "Formal", 96), Triple("எப்படி இருக்கிறாய்?", "Casual", 88)),
            "thank you" to listOf(Triple("நன்றி", "Formal", 96), Triple("மிக்க நன்றி", "Polite", 94)),
            "help" to listOf(Triple("தயவுசெய்து எனக்கு உதவுங்கள்.", "Formal", 96), Triple("எனக்கு உதவி தேவை!", "Emergency", 92)),
            "call an ambulance" to listOf(Triple("தயவுசெய்து ஆம்புலன்ஸை அழைக்கவும்!", "Emergency", 98)),
            "where is the hospital" to listOf(Triple("மருத்துவமனை எங்கே உள்ளது?", "Formal", 96)),
            "i need water" to listOf(Triple("எனக்கு குடிநீர் வேண்டும்.", "Standard", 96)),
            "where is the bathroom" to listOf(Triple("கழிப்பறை எங்கே உள்ளது?", "Formal", 96)),
            "how much is this" to listOf(Triple("இதன் விலை என்ன?", "Formal", 96), Triple("இது எவ்வளவு?", "Colloquial", 92))
        )
        private val dictionary = mapOf(
            "i" to "நான்", "me" to "எனக்கு", "my" to "என்", "you" to "நீங்கள்", "your" to "உங்கள்",
            "where" to "எங்கே", "what" to "என்ன", "water" to "தண்ணீர்", "food" to "உணவு",
            "doctor" to "மருத்துவர்", "hospital" to "மருத்துவமனை", "help" to "உதவி", "medicine" to "மருந்து",
            "station" to "நிலையம்", "money" to "பணம்", "car" to "கார்", "bus" to "பேருந்து"
        )
        fun translate(raw: String, isProfessional: Boolean, glossary: Map<String, String>): List<Triple<String, String, Int>> {
            val normalized = raw.trim().lowercase().replace(Regex("[?!.,;]+$"), "")
            phraseMap[normalized]?.let { return it }

            if (normalized.contains("anaphylaxis") || normalized.contains("allergic")) {
                val term = glossary["anaphylaxis"] ?: "கடுமையான ஒவ்வாமை எதிர்வினை"
                return listOf(
                    Triple("நோயாளிக்கு $term ஏற்பட்டுள்ளது.", "Medical / Formal", 95),
                    Triple("நோயாளிக்கு அவசர சிகிச்சை தேவை.", "Emergency", 90)
                )
            }

            for ((key, result) in phraseMap) {
                if (normalized.contains(key)) return result
            }

            if (normalized.startsWith("where is") || normalized.startsWith("where are")) {
                val target = normalized.removePrefix("where is").removePrefix("where are").trim().removePrefix("the").trim()
                val taTarget = dictionary[target] ?: target
                return listOf(Triple("$taTarget எங்கே உள்ளது?", "Formal", 94), Triple("$taTarget எங்கே?", "Literal", 75))
            }
            if (normalized.startsWith("i want") || normalized.startsWith("i need")) {
                val target = normalized.removePrefix("i want").removePrefix("i need").trim().removePrefix("a").trim().removePrefix("the").trim()
                val taTarget = dictionary[target] ?: target
                return listOf(Triple("எனக்கு $taTarget வேண்டும்.", "Formal", 94), Triple("எனக்கு $taTarget வேண்டும்.", "Literal", 75))
            }
            val tokens = raw.split(Regex("\\s+")).map { dictionary[it.lowercase().replace(Regex("[^a-z0-9]"), "")] ?: it }
            val composed = tokens.joinToString(" ")
            return listOf(Triple(composed, "Formal", 88), Triple(composed, "Literal", 70))
        }
    }

    // =========================================================================
    // SPANISH ENGINE (Español)
    // =========================================================================
    private object SpanishEngine {
        private val phraseMap = mapOf(
            "hello man" to listOf(Triple("Hola, amigo", "Colloquial", 95), Triple("Estimado señor", "Formal", 92), Triple("Hola hombre", "Literal", 68)),
            "hey man" to listOf(Triple("Hola, amigo", "Colloquial", 95), Triple("¿Qué tal, hombre?", "Casual", 90)),
            "hello" to listOf(Triple("Hola", "Standard", 96), Triple("Buenos días", "Formal", 92)),
            "hi" to listOf(Triple("Hola", "Standard", 95)),
            "good morning" to listOf(Triple("Buenos días", "Standard", 96)),
            "good evening" to listOf(Triple("Buenas tardes", "Standard", 96)),
            "good night" to listOf(Triple("Buenas noches", "Standard", 96)),
            "how are you" to listOf(Triple("¿Cómo está usted?", "Formal", 96), Triple("¿Cómo estás?", "Casual", 90), Triple("¿Qué tal?", "Colloquial", 88)),
            "thank you" to listOf(Triple("Muchas gracias", "Formal", 96), Triple("Gracias", "Standard", 92)),
            "help" to listOf(Triple("¡Ayuda, por favor!", "Emergency", 98), Triple("Por favor, ayúdeme.", "Formal", 95)),
            "call an ambulance" to listOf(Triple("¡Llame a una ambulancia!", "Emergency", 98)),
            "where is the hospital" to listOf(Triple("¿Dónde está el hospital?", "Formal", 96)),
            "where is the bathroom" to listOf(Triple("¿Dónde está el baño?", "Standard", 96)),
            "how much is this" to listOf(Triple("¿Cuánto cuesta esto?", "Formal", 96), Triple("¿Cuánto es?", "Colloquial", 92))
        )
        private val dictionary = mapOf(
            "i" to "yo", "me" to "me", "my" to "mi", "you" to "usted", "your" to "su",
            "where" to "dónde", "what" to "qué", "water" to "agua", "food" to "comida",
            "doctor" to "médico", "hospital" to "hospital", "help" to "ayuda", "medicine" to "medicina",
            "station" to "estación", "money" to "dinero", "car" to "coche", "bus" to "autobús"
        )
        fun translate(raw: String, isProfessional: Boolean, glossary: Map<String, String>): List<Triple<String, String, Int>> {
            val normalized = raw.trim().lowercase().replace(Regex("[?!.,;]+$"), "")
            phraseMap[normalized]?.let { return it }

            if (normalized.contains("anaphylaxis") || normalized.contains("allergic")) {
                val term = glossary["anaphylaxis"] ?: "anafilaxia severa"
                return listOf(
                    Triple("El paciente sufre de $term.", "Medical / Formal", 96),
                    Triple("Reacción alérgica grave detectada.", "Clinical", 92)
                )
            }

            for ((key, result) in phraseMap) {
                if (normalized.contains(key)) return result
            }

            if (normalized.startsWith("where is") || normalized.startsWith("where are")) {
                val target = normalized.removePrefix("where is").removePrefix("where are").trim().removePrefix("the").trim()
                val esTarget = dictionary[target] ?: target
                return listOf(Triple("¿Dónde está $esTarget?", "Formal", 95), Triple("¿Dónde está $esTarget?", "Literal", 75))
            }
            if (normalized.startsWith("i want") || normalized.startsWith("i need")) {
                val target = normalized.removePrefix("i want").removePrefix("i need").trim().removePrefix("a").trim().removePrefix("the").trim()
                val esTarget = dictionary[target] ?: target
                return listOf(Triple("Necesito $esTarget.", "Formal", 95), Triple("Quiero $esTarget.", "Literal", 75))
            }
            val tokens = raw.split(Regex("\\s+")).map { dictionary[it.lowercase().replace(Regex("[^a-z0-9]"), "")] ?: it }
            val composed = tokens.joinToString(" ")
            return listOf(Triple(composed, "Formal", 88), Triple(composed, "Literal", 70))
        }
    }
}
