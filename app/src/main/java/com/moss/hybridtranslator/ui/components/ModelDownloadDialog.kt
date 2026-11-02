package com.moss.hybridtranslator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.moss.hybridtranslator.services.LocalGemmaService

@Composable
fun ModelDownloadDialog(
    localService: LocalGemmaService,
    onSwitchToLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress by localService.downloadProgress.collectAsState()
    val bytesDownloaded by localService.downloadBytesDownloaded.collectAsState()
    val totalBytes by localService.downloadTotalBytes.collectAsState()
    val isReady by localService.isModelReady.collectAsState()
    val filePresent by localService.isModelFilePresent.collectAsState()
    val isDownloading by localService.isDownloading.collectAsState()
    val isLoading by localService.isLoading.collectAsState()
    val modelLocation by localService.modelLocation.collectAsState()
    val modelSizeBytes by localService.modelSizeBytes.collectAsState()
    val statusMsg by localService.statusMessage.collectAsState()
    val lastError by localService.lastError.collectAsState()
    val engineName by localService.engineName.collectAsState()

    var showCustomUrlField by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }

    val statusText = when {
        isReady -> "Ready for inference"
        isLoading -> "Loading model…"
        isDownloading -> "Downloading ($progress%)"
        filePresent -> "Model file found"
        else -> "Not installed"
    }
    val statusColor = when {
        isReady -> Color(0xFF30D158)
        isDownloading || isLoading -> Color(0xFF0A84FF)
        lastError != null -> Color(0xFFFF453A)
        else -> Color(0xFFFF9F0A)
    }

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
                            text = "Offline Model",
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
                    text = "Gemma 4 E2B · LiteRT-LM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "Runs entirely on your phone with Google LiteRT-LM. No Ollama, no server, and no network needed after the download.",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status:", fontSize = 12.sp, color = Color(0xFF8E8E93))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isReady) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isReady) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = engineName,
                                fontSize = 11.sp,
                                color = Color(0xFF30D158)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Model file:", fontSize = 11.sp, color = Color(0xFF8E8E93))
                        Text(
                            text = modelLocation.ifBlank { "—" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size:", fontSize = 12.sp, color = Color(0xFF8E8E93))
                            Text(
                                text = if (modelSizeBytes > 0) {
                                    formatBytes(modelSizeBytes)
                                } else {
                                    "About ${formatBytes(LocalGemmaService.EXPECTED_MODEL_BYTES)}"
                                },
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (statusMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusMsg,
                                fontSize = 11.sp,
                                color = if (lastError != null) Color(0xFFFF453A) else statusColor
                            )
                        }

                        if (lastError != null && !isDownloading) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF453A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lastError.orEmpty(),
                                    fontSize = 10.sp,
                                    color = Color(0xFFFF453A)
                                )
                            }
                        }
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = Color(0xFF0A84FF),
                            trackColor = Color(0xFF2C2C2E),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Color(0xFF0A84FF),
                            trackColor = Color(0xFF2C2C2E),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("Downloading ")
                            append(formatBytes(bytesDownloaded))
                            if (totalBytes > 0) {
                                append(" / ")
                                append(formatBytes(totalBytes))
                            }
                            append(" · $progress%")
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF0A84FF)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isReady -> {
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
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use Offline Model",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    isLoading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading Gemma 4 E2B…")
                        }
                    }

                    isDownloading -> {
                        OutlinedButton(
                            onClick = { localService.cancelDownload() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pause Download", color = Color(0xFFFF9F0A))
                        }
                    }

                    filePresent -> {
                        Button(
                            onClick = { localService.checkLocalModel(forceReload = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Load Model",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = { localService.startDownload(customUrl.ifBlank { null }) },
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
                                text = "Download Gemma 4 E2B (2.6 GB)",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                        text = if (showCustomUrlField) "Hide custom download URL" else "Use a custom .litertlm URL",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                if (showCustomUrlField) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = {
                            Text(
                                "https://huggingface.co/.../gemma-4-E2B-it.litertlm",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        },
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

                Text(
                    text = "You can also copy the model over USB:",
                    fontSize = 10.sp,
                    color = Color(0xFF8E8E93)
                )
                Text(
                    text = "adb push ${LocalGemmaService.MODEL_FILE_NAME} ${localService.getPreferredStorageDirectory().absolutePath}/",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFF0A84FF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2C2C2E))
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { localService.checkLocalModel(forceReload = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF8E8E93)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-scan storage", fontSize = 11.sp, color = Color(0xFF8E8E93))
                    }

                    Text(
                        text = "Gemma Terms of Use apply",
                        fontSize = 10.sp,
                        color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / 1_000_000.0
    return if (mb >= 1000) String.format("%.2f GB", mb / 1000.0) else String.format("%.0f MB", mb)
}
