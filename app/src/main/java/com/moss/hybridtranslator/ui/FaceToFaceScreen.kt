package com.moss.hybridtranslator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moss.hybridtranslator.ui.components.AgentSkillInspectorDialog
import com.moss.hybridtranslator.ui.components.ApiKeyDialog
import com.moss.hybridtranslator.ui.components.ModelDownloadDialog

// Apple Translate Color Palette
val AppleBg = Color(0xFF000000)
val AppleCard = Color(0xFF1C1C1E)
val AppleCardBorder = Color(0xFF2C2C2E)
val AppleSecondaryText = Color(0xFF8E8E93)
val AppleBlue = Color(0xFF0A84FF)
val AppleGreen = Color(0xFF30D158)
val AppleOrange = Color(0xFFFF9F0A)
val ApplePillBg = Color(0xFF2C2C2E)

@Composable
fun FaceToFaceScreen(viewModel: TranslatorViewModel) {
    val context = LocalContext.current
    val userALang by viewModel.userALanguage.collectAsState()
    val userBLang by viewModel.userBLanguage.collectAsState()
    val contextSetter by viewModel.contextSetter.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val lastResult by viewModel.lastExecutionResult.collectAsState()

    val sourceText by viewModel.currentSourceText.collectAsState()
    val translatedText by viewModel.currentTranslatedText.collectAsState()

    val showInspector by viewModel.showInspectorDialog.collectAsState()
    val showDownload by viewModel.showDownloadDialog.collectAsState()
    val showApiKey by viewModel.showApiKeyDialog.collectAsState()

    var showContextDialog by remember { mutableStateOf(false) }
    var tempContextText by remember { mutableStateOf(contextSetter) }

    val isListening by viewModel.speechService.isListening.collectAsState()
    val audioRms by viewModel.speechService.audioRms.collectAsState()

    val presetContexts = listOf(
        "Emergency medical diagnosis",
        "Disaster relief & rescue rations",
        "Legal asylum consultation",
        "General daily conversation"
    )

    val quickSamplePhrases = listOf(
        "The patient is experiencing severe anaphylaxis.",
        "Where is the nearest potable water shelter?",
        "Please provide an affidavit for the court bail.",
        "Do you need immediate medical attention?"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppleBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // TOP HEADER: Apple Minimalist Style
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Translate",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOffline) Color(0xFF3B2500) else Color(0xFF0C2444))
                            .clickable { viewModel.toggleOfflineMode() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) AppleOrange else AppleBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOffline) "Offline: Gemma 4 E2B" else "Cloud: Gemma 4",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOffline) AppleOrange else AppleBlue
                        )
                    }
                }

                // Action Bar Tools
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Skill Inspector
                    IconButton(
                        onClick = { viewModel.showInspectorDialog.value = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Agent Skill Inspector",
                            tint = AppleGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Download Manager
                    IconButton(
                        onClick = { viewModel.showDownloadDialog.value = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Offline Models",
                            tint = AppleOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = { viewModel.showApiKeyDialog.value = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppleSecondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // CONTEXT SETTER PILL (Agent Skill Driver)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppleCard)
                    .border(1.dp, AppleCardBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        tempContextText = contextSetter
                        showContextDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = AppleGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DOMAIN CONTEXT (Agent Skill Active)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSecondaryText
                        )
                        Text(
                            text = contextSetter.ifBlank { "Tap to configure context..." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Context",
                    tint = AppleSecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // LANGUAGE PAIR SELECTOR BAR (Apple Style)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppleCard)
                    .border(1.dp, AppleCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Language Picker
                LanguagePickerDropdown(
                    current = userALang,
                    onSelect = { viewModel.setUserALanguage(it) },
                    modifier = Modifier.weight(1f)
                )

                // Swap Button
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ApplePillBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Languages",
                        tint = AppleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Target Language Picker
                LanguagePickerDropdown(
                    current = userBLang,
                    onSelect = { viewModel.setUserBLanguage(it) },
                    modifier = Modifier.weight(1f),
                    isTarget = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // CARD 1 (TOP): SOURCE LANGUAGE CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, AppleCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Card Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userALang.name.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSecondaryText,
                            letterSpacing = 1.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sourceText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        viewModel.speechService.speak(sourceText, userALang.code)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Listen to source",
                                        tint = AppleSecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { viewModel.clearSourceText() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text",
                                        tint = AppleSecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Source Input Text Field
                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = { viewModel.updateSourceText(it) },
                        placeholder = {
                            Text(
                                "Enter text or tap the microphone...",
                                color = AppleSecondaryText.copy(alpha = 0.6f),
                                fontSize = 17.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = AppleBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 150.dp)
                    )

                    // Quick Action: Translate Button (if typing)
                    if (sourceText.isNotBlank() && !isTranslating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.translateCurrentText(autoSpeak = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Translate", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // CARD 2 (BOTTOM): TARGET TRANSLATION CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, if (isTranslating) AppleBlue.copy(alpha = 0.5f) else AppleCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Target Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userBLang.name.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleBlue,
                            letterSpacing = 1.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTranslating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AppleBlue,
                                    strokeWidth = 2.dp
                                )
                            } else if (translatedText.isNotBlank()) {
                                // Copy Button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Translation", translatedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Translation",
                                        tint = AppleSecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Speak Audio Button
                                IconButton(
                                    onClick = {
                                        viewModel.speechService.speak(translatedText, userBLang.code)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak Translation",
                                        tint = AppleBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Translated Content
                    Text(
                        text = if (isTranslating) "Translating..." else translatedText.ifBlank { "Translation will appear here" },
                        fontSize = 19.sp,
                        fontWeight = if (translatedText.isNotBlank() && !isTranslating) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (translatedText.isNotBlank() && !isTranslating) Color.White else AppleSecondaryText.copy(alpha = 0.5f),
                        lineHeight = 26.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Agent Skill Badge (Glossary Terms Indicator)
                    val triggeredSkill = lastResult?.skillOutput
                    if (triggeredSkill != null && triggeredSkill.isSkillTriggered && triggeredSkill.glossaryTerms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF132F20))
                                .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.showInspectorDialog.value = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AppleGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val termCount = triggeredSkill.glossaryTerms.size
                            Text(
                                text = "Agent Skill: $termCount specialized term(s) injected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // QUICK SCENARIO PHRASES
            // ==========================================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSamplePhrases) { phrase ->
                    Text(
                        text = phrase,
                        fontSize = 12.sp,
                        color = AppleSecondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleCard)
                            .border(1.dp, AppleCardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.updateSourceText(phrase)
                                viewModel.translateCurrentText(autoSpeak = true)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // BOTTOM: APPLE TRANSLATE MICROPHONE BUTTON
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Waveform / Pulsing Ring
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isListening) 1.25f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    if (isListening) {
                        // Pulsing background glow
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(AppleBlue.copy(alpha = 0.25f))
                        )
                    }

                    // Main Mic Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.radialGradient(listOf(Color(0xFFFF3B30), Color(0xFFD70015)))
                                else Brush.radialGradient(listOf(AppleBlue, Color(0xFF0066CC)))
                            )
                            .clickable { viewModel.toggleSpeechRecognition() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Listening" else "Speak",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isListening) "Listening in ${userALang.name}..." else "Tap to Speak",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) Color(0xFFFF453A) else AppleSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // DIALOGS: Context, Inspector, Download, Settings
    // ==========================================
    if (showContextDialog) {
        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            containerColor = AppleCard,
            title = {
                Text(
                    "Select Operational Context",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Sets the domain scenario for the Agent Skill to extract specialized terminology.",
                        fontSize = 12.sp,
                        color = AppleSecondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempContextText,
                        onValueChange = { tempContextText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AppleBlue,
                            unfocusedBorderColor = AppleCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Preset Scenarios:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleSecondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    presetContexts.forEach { preset ->
                        Text(
                            text = "• $preset",
                            fontSize = 12.sp,
                            color = AppleBlue,
                            modifier = Modifier
                                .clickable { tempContextText = preset }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setContext(tempContextText)
                        showContextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                ) {
                    Text("Apply Context")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextDialog = false }) {
                    Text("Cancel", color = AppleSecondaryText)
                }
            }
        )
    }

    if (showInspector) {
        AgentSkillInspectorDialog(
            result = lastResult,
            onDismiss = { viewModel.showInspectorDialog.value = false }
        )
    }

    if (showDownload) {
        ModelDownloadDialog(
            mediaPipeService = viewModel.mediaPipeService,
            onDismiss = { viewModel.showDownloadDialog.value = false }
        )
    }

    if (showApiKey) {
        ApiKeyDialog(
            initialKey = viewModel.geminiService.getApiKey(),
            initialModel = viewModel.geminiService.getModelName(),
            onSave = { key, model ->
                viewModel.saveApiKey(key, model)
            },
            onDismiss = { viewModel.showApiKeyDialog.value = false }
        )
    }
}

@Composable
fun LanguagePickerDropdown(
    current: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier,
    isTarget: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = if (isTarget) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = current.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isTarget) AppleBlue else Color.White
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isTarget) AppleBlue else AppleSecondaryText
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppleCard)
        ) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = lang.name,
                            color = if (lang.code == current.code) AppleBlue else Color.White,
                            fontWeight = if (lang.code == current.code) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
