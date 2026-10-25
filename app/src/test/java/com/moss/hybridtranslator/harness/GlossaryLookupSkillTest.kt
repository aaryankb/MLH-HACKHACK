package com.moss.hybridtranslator.harness

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GlossaryLookupSkillTest {

    private lateinit var skill: GlossaryLookupSkill

    @Before
    fun setUp() {
        skill = GlossaryLookupSkill(null) // Uses embedded fallback datasets for pure unit tests
    }

    @Test
    fun testMedicalDomainExtractionAndKannadaResolution() {
        val output = skill.execute(
            userContext = "Emergency medical diagnosis at field triage",
            sourceText = "The patient is having severe anaphylaxis and needs epinephrine.",
            targetLangCode = "kn"
        )

        assertTrue("Skill should be triggered for medical emergency", output.isSkillTriggered)
        assertEquals("medical", output.matchedDomain)
        assertTrue("Glossary should contain anaphylaxis", output.glossaryTerms.containsKey("anaphylaxis"))
        assertTrue("Glossary should contain epinephrine", output.glossaryTerms.containsKey("epinephrine"))

        val anaphylaxisKn = output.glossaryTerms["anaphylaxis"]
        assertNotNull(anaphylaxisKn)
        assertTrue("Should contain Kannada translation", anaphylaxisKn!!.contains("ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್") || anaphylaxisKn.contains("ಅಲರ್ಜಿ"))

        assertTrue("System instruction prompt should be generated", output.systemInstructionPrompt.contains("CRITICAL DOMAIN TERMINOLOGY"))
    }

    @Test
    fun testDisasterReliefDomainExtractionAndHindiResolution() {
        val output = skill.execute(
            userContext = "Disaster relief flood camp",
            sourceText = "Please direct the refugees to the evacuation center and provide potable water.",
            targetLangCode = "hi"
        )

        assertTrue("Skill should be triggered for disaster relief", output.isSkillTriggered)
        assertEquals("disaster_relief", output.matchedDomain)
        assertTrue("Glossary should contain potable water", output.glossaryTerms.containsKey("potable water"))
        val waterHi = output.glossaryTerms["potable water"]
        assertTrue("Should contain Hindi translation for potable water", waterHi!!.contains("पानी") || waterHi.contains("पेयजल"))
    }

    @Test
    fun testGeneralContextDoesNotTriggerFalsePositives() {
        val output = skill.execute(
            userContext = "Casual chat",
            sourceText = "Good morning, how are you doing today?",
            targetLangCode = "kn"
        )

        assertFalse("General greeting should not trigger specialized skill", output.isSkillTriggered)
        assertTrue("Glossary terms should be empty for general talk", output.glossaryTerms.isEmpty())
    }
}

