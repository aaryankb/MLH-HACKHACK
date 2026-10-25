package com.moss.hybridtranslator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moss.hybridtranslator.data.model.HarnessExecutionResult
import com.moss.hybridtranslator.data.model.Speaker
import com.moss.hybridtranslator.data.model.TranslationTurn
import com.moss.hybridtranslator.harness.GlossaryLookupSkill
import com.moss.hybridtranslator.harness.ModelHarness
import com.moss.hybridtranslator.services.GeminiGemmaService
import com.moss.hybridtranslator.services.MediaPipeGemmaService
import com.moss.hybridtranslator.services.SpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class LanguageOption(val name: String, val code: String)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("English", "en"),
    LanguageOption("Kannada (ಕನ್ನಡ)", "kn"),
    LanguageOption("Hindi (हिंदी)", "hi"),
    LanguageOption("Telugu (తెలుగు)", "te"),
    LanguageOption("Tamil (தமிழ்)", "ta"),
    LanguageOption("Spanish (Español)", "es")
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val glossarySkill = GlossaryLookupSkill(application)
    val geminiService = GeminiGemmaService()
    val mediaPipeService = MediaPipeGemmaService(application)
    val speechService = SpeechService(application)

    private val harness = ModelHarness(glossarySkill, geminiService, mediaPipeService)

    private val _userALanguage = MutableStateFlow(SUPPORTED_LANGUAGES[0]) // English
    val userALanguage: StateFlow<LanguageOption> = _userALanguage.asStateFlow()

    private val _userBLanguage = MutableStateFlow(SUPPORTED_LANGUAGES[1]) // Kannada
    val userBLanguage: StateFlow<LanguageOption> = _userBLanguage.asStateFlow()

    private val _contextSetter = MutableStateFlow("Emergency medical diagnosis")
    val contextSetter: StateFlow<String> = _contextSetter.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _turns = MutableStateFlow<List<TranslationTurn>>(emptyList())
    val turns: StateFlow<List<TranslationTurn>> = _turns.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<HarnessExecutionResult?>(null)
    val lastExecutionResult: StateFlow<HarnessExecutionResult?> = _lastExecutionResult.asStateFlow()

    // Dialog visibility states
    val showInspectorDialog = MutableStateFlow(false)
    val showDownloadDialog = MutableStateFlow(false)
    val showApiKeyDialog = MutableStateFlow(false)

    init {
        // Pre-populate with a demonstrative introductory turn
        viewModelScope.launch {
            val introTurn = TranslationTurn(
                id = UUID.randomUUID().toString(),
                speaker = Speaker.USER_A,
                sourceLanguage = "en",
                targetLanguage = "kn",
                sourceText = "The patient is experiencing severe anaphylaxis.",
                translatedText = "ರೋಗಿಯು ತೀವ್ರ ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ (ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್) ಅನುಭವಿಸುತ್ತಿದ್ದಾರೆ.",
                isOffline = false
            )
            _turns.value = listOf(introTurn)
        }
    }

    fun setUserALanguage(language: LanguageOption) {
        _userALanguage.value = language
    }

    fun setUserBLanguage(language: LanguageOption) {
        _userBLanguage.value = language
    }

    fun setContext(newContext: String) {
        _contextSetter.value = newContext
    }

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
    }

    fun setOfflineMode(offline: Boolean) {
        _isOfflineMode.value = offline
    }

    fun translateTurn(speaker: Speaker, sourceText: String) {
        if (sourceText.isBlank()) return

        viewModelScope.launch {
            _isTranslating.value = true

            val (srcLang, tgtLang) = if (speaker == Speaker.USER_A) {
                userALanguage.value to userBLanguage.value
            } else {
                userBLanguage.value to userALanguage.value
            }

            val result = harness.translate(
                userContext = _contextSetter.value,
                sourceText = sourceText,
                targetLanguageName = tgtLang.name,
                targetLanguageCode = tgtLang.code,
                isOfflineMode = _isOfflineMode.value,
                history = _turns.value
            )

            _lastExecutionResult.value = result

            val newTurn = TranslationTurn(
                id = UUID.randomUUID().toString(),
                speaker = speaker,
                sourceLanguage = srcLang.code,
                targetLanguage = tgtLang.code,
                sourceText = sourceText,
                translatedText = result.translatedText,
                isOffline = result.isOffline
            )

            _turns.value = _turns.value + newTurn
            _isTranslating.value = false

            // Automatically speak the translated text aloud for the listener
            speechService.speak(result.translatedText, tgtLang.code)
        }
    }

    fun startSpeechInput(speaker: Speaker) {
        val langCode = if (speaker == Speaker.USER_A) userALanguage.value.code else userBLanguage.value.code
        speechService.startListening(langCode) { recognized ->
            translateTurn(speaker, recognized)
        }
    }

    fun stopSpeechInput() {
        speechService.stopListening()
    }

    fun speakTurn(turn: TranslationTurn) {
        speechService.speak(turn.translatedText, turn.targetLanguage)
    }

    fun saveApiKey(key: String, model: String) {
        geminiService.updateApiKey(key, model)
    }

    override fun onCleared() {
        super.onCleared()
        speechService.destroy()
    }
}

