package com.moss.hybridtranslator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.moss.hybridtranslator.services.MediaPipeGemmaService
import kotlinx.coroutines.launch

@Composable
fun ModelDownloadDialog(
    mediaPipeService: MediaPipeGemmaService,
    onSwitchToLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress by mediaPipeService.downloadProgress.collectAsState()
    val isReady by mediaPipeService.isModelReady.collectAsState()
    val isDownloading by mediaPipeService.isDownloading.collectAsState()
    val modelLocation by mediaPipeService.modelLocation.collectAsState()
    val modelSizeBytes by mediaPipeService.modelSizeBytes.collectAsState()
    val statusMsg by mediaPipeService.statusMessage.collectAsState()

    var showCustomUrlField by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFFF9F0A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Offline Edge Model",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Google Gemma 4 E2B (Native MediaPipe Edge)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "Powered by Google MediaPipe Tasks GenAI. No external daemons (like Ollama) required — runs completely native via Android GPU/CPU.",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Storage & Location Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Engine Status:", fontSize = 12.sp, color = Color(0xFF8E8E93))
                            Text(
                                text = if (isReady) "Ready for Inference" else if (isDownloading) "Downloading ($progress%)" else "Not Ready",
                                color = if (isReady) Color(0xFF30D158) else if (isDownloading) Color(0xFF0A84FF) else Color(0xFFFF9F0A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Persistent File Location:", fontSize = 11.sp, color = Color(0xFF8E8E93))
                        Text(
                            text = modelLocation,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        if (modelSizeBytes > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Size:", fontSize = 12.sp, color = Color(0xFF8E8E93))
                                val sizeMb = modelSizeBytes / (1024 * 1024)
                                Text(
                                    text = if (sizeMb > 1024) String.format("%.2f GB", sizeMb / 1024f) else "$sizeMb MB",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (statusMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusMsg,
                                fontSize = 11.sp,
                                color = if (statusMsg.startsWith("Download Error")) Color(0xFFFF453A) else Color(0xFF30D158)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        color = Color(0xFF0A84FF),
                        trackColor = Color(0xFF2C2C2E),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloading from HuggingFace CDN: $progress%",
                        fontSize = 12.sp,
                        color = Color(0xFF0A84FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action 1: Switch to Local Model
                if (isReady) {
                    Button(
                        onClick = {
                            onSwitchToLocal()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ Switch to Local Model (Faster Response)",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action 2: Download Full 2.0GB Weights
                Button(
                    onClick = {
                        coroutineScope.launch {
                            mediaPipeService.downloadModel(customUrl.ifBlank { null })
                        }
                    },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloading) "Downloading..." else "Download Gemma 4 E2B (2.0GB)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 3: Instant Edge Package (Zero Wait)
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            mediaPipeService.createLocalEdgePackage()
                        }
                    },
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instant Edge Setup (No Wait)",
                        color = Color(0xFFFF9F0A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom URL toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomUrlField = !showCustomUrlField }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showCustomUrlField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCustomUrlField) "Hide custom download URL" else "Download from custom URL or HuggingFace...",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                if (showCustomUrlField) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = { Text("https://huggingface.co/.../gemma-4-E2B-it-web.task", fontSize = 11.sp, color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color(0xFF2C2C2E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ADB push instruction
                Text(
                    text = "Tip: You can push any model via USB instantly:",
                    fontSize = 10.sp,
                    color = Color(0xFF8E8E93)
                )
                Text(
                    text = "adb push <file>.task /sdcard/Download/hybrid_translator/",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFF0A84FF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2C2C2E))
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action: Re-scan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { mediaPipeService.checkLocalModel() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF8E8E93))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-scan Folders", fontSize = 11.sp, color = Color(0xFF8E8E93))
                    }

                    Text(
                        text = "Persistent across rebuilds",
                        fontSize = 10.sp,
                        color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
