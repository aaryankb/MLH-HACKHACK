package com.moss.hybridtranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moss.hybridtranslator.data.model.Speaker
import com.moss.hybridtranslator.data.model.TranslationTurn
import com.moss.hybridtranslator.ui.components.AgentSkillInspectorDialog
import com.moss.hybridtranslator.ui.components.ApiKeyDialog
import com.moss.hybridtranslator.ui.components.ModelDownloadDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceToFaceScreen(viewModel: TranslatorViewModel) {
    val userALang by viewModel.userALanguage.collectAsState()
    val userBLang by viewModel.userBLanguage.collectAsState()
    val contextSetter by viewModel.contextSetter.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val turns by viewModel.turns.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val lastResult by viewModel.lastExecutionResult.collectAsState()

    val showInspector by viewModel.showInspectorDialog.collectAsState()
    val showDownload by viewModel.showDownloadDialog.collectAsState()
    val showApiKey by viewModel.showApiKeyDialog.collectAsState()

    var showContextEditDialog by remember { mutableStateOf(false) }
    var tempContextText by remember { mutableStateOf(contextSetter) }

    var userATextInput by remember { mutableStateOf("") }
    var userBTextInput by remember { mutableStateOf("") }

    val isListening by viewModel.speechService.isListening.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==========================================
            // TOP HALF: USER B (Rotated 180° for Partner)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .rotate(180f) // The key face-to-face rotation!
                    .padding(16.dp)
            ) {
                UserBPanel(
                    language = userBLang,
                    turns = turns,
                    isTranslating = isTranslating,
                    isListening = isListening,
                    inputText = userBTextInput,
                    onInputTextChange = { userBTextInput = it },
                    onSend = {
                        viewModel.translateTurn(Speaker.USER_B, userBTextInput)
                        userBTextInput = ""
                    },
                    onMicClick = { viewModel.startSpeechInput(Speaker.USER_B) },
                    onSpeakTurn = { viewModel.speakTurn(it) },
                    onLanguageSelect = { viewModel.setUserBLanguage(it) }
                )
            }

            // ==========================================
            // CENTER BAR: Control, Router & Context Setter
            // ==========================================
            CenterControlBar(
                contextSetter = contextSetter,
                isOffline = isOffline,
                isTranslating = isTranslating,
                onEditContext = {
                    tempContextText = contextSetter
                    showContextEditDialog = true
                },
                onToggleOffline = { viewModel.toggleOfflineMode() },
                onOpenInspector = { viewModel.showInspectorDialog.value = true },
                onOpenDownload = { viewModel.showDownloadDialog.value = true },
                onOpenSettings = { viewModel.showApiKeyDialog.value = true }
            )

            // ==========================================
            // BOTTOM HALF: USER A (Upright for Device Holder)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                UserAPanel(
                    language = userALang,
                    turns = turns,
                    isTranslating = isTranslating,
                    isListening = isListening,
                    inputText = userATextInput,
                    onInputTextChange = { userATextInput = it },
                    onSend = {
                        viewModel.translateTurn(Speaker.USER_A, userATextInput)
                        userATextInput = ""
                    },
                    onMicClick = { viewModel.startSpeechInput(Speaker.USER_A) },
                    onSpeakTurn = { viewModel.speakTurn(it) },
                    onLanguageSelect = { viewModel.setUserALanguage(it) },
                    onQuickPromptClick = { prompt ->
                        userATextInput = prompt
                    }
                )
            }
        }
    }

    // Context Edit Dialog
    if (showContextEditDialog) {
        AlertDialog(
            onDismissRequest = { showContextEditDialog = false },
            title = { Text("Set Conversation Context") },
            text = {
                Column {
                    Text(
                        "Sets the operational scenario for the Agent Skill to extract domain-specific terminology rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempContextText,
                        onValueChange = { tempContextText = it },
                        label = { Text("Context (e.g. Medical, Disaster Relief, Legal)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preset Scenarios:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(
                            onClick = { tempContextText = "Emergency medical diagnosis" },
                            label = { Text("Medical", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { tempContextText = "Disaster relief rations distribution" },
                            label = { Text("Disaster", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { tempContextText = "Legal and refugee assistance" },
                            label = { Text("Legal", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setContext(tempContextText)
                    showContextEditDialog = false
                }) {
                    Text("Apply Context")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Inspector Dialog
    if (showInspector) {
        AgentSkillInspectorDialog(
            result = lastResult,
            onDismiss = { viewModel.showInspectorDialog.value = false }
        )
    }

    // Download Dialog
    if (showDownload) {
        ModelDownloadDialog(
            mediaPipeService = viewModel.mediaPipeService,
            onDismiss = { viewModel.showDownloadDialog.value = false }
        )
    }

    // API Key Dialog
    if (showApiKey) {
        ApiKeyDialog(
            initialModel = viewModel.geminiService.getModelName(),
            onSave = { key, model ->
                viewModel.saveApiKey(key, model)
            },
            onDismiss = { viewModel.showApiKeyDialog.value = false }
        )
    }
}

@Composable
fun CenterControlBar(
    contextSetter: String,
    isOffline: Boolean,
    isTranslating: Boolean,
    onEditContext: () -> Unit,
    onToggleOffline: () -> Unit,
    onOpenInspector: () -> Unit,
    onOpenDownload: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E293B),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Context Setter Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF334155), RoundedCornerShape(20.dp))
                    .clickable { onEditContext() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CONTEXT SETTER (Agent Skill Active)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = contextSetter.ifBlank { "Tap to set domain context..." },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Context",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Offline / Online Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .clickable { onToggleOffline() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.OfflineBolt else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (isOffline) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOffline) "Offline: Gemma 4 E2B" else "Online: Gemma 4 Cloud",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Tool Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    // Skill Inspector
                    IconButton(
                        onClick = onOpenInspector,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = "Skill Inspector",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Download Manager
                    IconButton(
                        onClick = onOpenDownload,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Model",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserBPanel(
    language: LanguageOption,
    turns: List<TranslationTurn>,
    isTranslating: Boolean,
    isListening: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onSpeakTurn: (TranslationTurn) -> Unit,
    onLanguageSelect: (LanguageOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARTNER (User B)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LanguagePickerMenu(
                current = language,
                onSelect = onLanguageSelect,
                textColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Feed
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(turns.reversed()) { turn ->
                MessageBubble(
                    turn = turn,
                    isFromCurrentUser = turn.speaker == Speaker.USER_B,
                    onSpeak = { onSpeakTurn(turn) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("ಪ್ರತಿಕ್ರಿಯಿಸಿ / Type reply...", color = Color.Gray, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .background(if (isListening) Color.Red else Color(0xFF1E293B), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .background(Color(0xFF38BDF8), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun UserAPanel(
    language: LanguageOption,
    turns: List<TranslationTurn>,
    isTranslating: Boolean,
    isListening: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onSpeakTurn: (TranslationTurn) -> Unit,
    onLanguageSelect: (LanguageOption) -> Unit,
    onQuickPromptClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PRIMARY SPEAKER (User A)",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LanguagePickerMenu(
                current = language,
                onSelect = onLanguageSelect,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Suggestions
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickPhrases = listOf(
                "The patient has severe anaphylaxis and needs epinephrine.",
                "Where is the potable water and emergency relief center?",
                "Do you have a legal affidavit or identity document?",
                "Are you experiencing any chest pain or difficulty breathing?"
            )
            items(quickPhrases) { phrase ->
                SuggestionChip(
                    onClick = { onQuickPromptClick(phrase) },
                    label = { Text(phrase, maxLines = 1, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Feed
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(turns.reversed()) { turn ->
                MessageBubble(
                    turn = turn,
                    isFromCurrentUser = turn.speaker == Speaker.USER_A,
                    onSpeak = { onSpeakTurn(turn) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("Speak or type to translate...", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .background(if (isListening) Color.Red else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak"
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    turn: TranslationTurn,
    isFromCurrentUser: Boolean,
    onSpeak: () -> Unit
) {
    val bgColor = if (isFromCurrentUser) {
        Color(0xFF2563EB)
    } else {
        Color(0xFF334155)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFromCurrentUser) "You (${turn.sourceLanguage.uppercase()})" else "Partner (${turn.sourceLanguage.uppercase()})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Play Audio",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = turn.sourceText,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Divider(
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Text(
                    text = turn.translatedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LanguagePickerMenu(
    current: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = current.code.uppercase(),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = textColor
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

