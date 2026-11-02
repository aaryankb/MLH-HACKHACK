package com.moss.hybridtranslator.services

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-device Gemma translation backed by Google LiteRT-LM.
 *
 * The model is a single `.litertlm` bundle downloaded from the LiteRT
 * community on Hugging Face. It is stored in app-specific external storage so
 * it survives app updates and does not require broad storage permissions.
 *
 * If the model is missing, still loading, or cannot run on this device, the
 * service falls back to the curated emergency phrasebook. That fallback is
 * always reported as such through [LocalTranslationOutput.engineLabel] — it is
 * never disguised as a neural model.
 */
class LocalGemmaService(private val context: Context? = null) : LocalTranslator, AutoCloseable {

    companion object {
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

        /** Approximate size of the current Gemma 4 E2B LiteRT-LM bundle. */
        const val EXPECTED_MODEL_BYTES = 2_588_147_712L

        private const val MIN_MODEL_BYTES = 1_500_000_000L
        private const val FREE_SPACE_BUFFER_BYTES = 256L * 1024 * 1024
        private const val MODEL_MAGIC = "LITERTLM"
        private const val DOWNLOAD_BUFFER_BYTES = 256 * 1024
        private const val LOAD_RETRY_COOLDOWN_MS = 60_000L
        private const val TAG = "LocalGemmaService"

        private val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMutex = Mutex()
    private val inferenceMutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var engineBackend: String? = null

    @Volatile
    private var modelFile: File? = null

    @Volatile
    private var activeConnection: HttpURLConnection? = null

    @Volatile
    private var lastFailedPath: String? = null

    @Volatile
    private var lastFailureAt: Long = 0L

    private val downloadCancelled = AtomicBoolean(false)
    private val downloadActive = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadBytesDownloaded = MutableStateFlow(0L)
    val downloadBytesDownloaded: StateFlow<Long> = _downloadBytesDownloaded.asStateFlow()

    private val _downloadTotalBytes = MutableStateFlow(0L)
    val downloadTotalBytes: StateFlow<Long> = _downloadTotalBytes.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    override val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _isModelFilePresent = MutableStateFlow(false)
    val isModelFilePresent: StateFlow<Boolean> = _isModelFilePresent.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _modelLocation = MutableStateFlow("")
    val modelLocation: StateFlow<String> = _modelLocation.asStateFlow()

    private val _modelSizeBytes = MutableStateFlow(0L)
    val modelSizeBytes: StateFlow<Long> = _modelSizeBytes.asStateFlow()

    private val _statusMessage = MutableStateFlow("Checking for an offline model…")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _engineName = MutableStateFlow("Offline phrasebook")
    val engineName: StateFlow<String> = _engineName.asStateFlow()

    init {
        checkLocalModel()
    }

    /**
     * Re-checks the known model locations. If a complete bundle is found it is
     * loaded in the background. Pass [forceReload] after the user explicitly
     * taps "Retry load" to ignore the failure cooldown.
     */
    fun checkLocalModel(forceReload: Boolean = false) {
        if (closed.get()) return
        val file = findModelFile()

        if (file == null) {
            closeEngine()
            modelFile = null
            _isModelFilePresent.value = false
            _isModelReady.value = false
            _isLoading.value = false
            _modelSizeBytes.value = 0L
            _downloadProgress.value = 0
            _modelLocation.value = getPreferredModelFile().absolutePath
            _engineName.value = "Offline phrasebook"

            val legacy = findLegacyTaskFile()
            if (legacy != null) {
                _lastError.value =
                    "Found the old MediaPipe file '${legacy.name}'. LiteRT-LM cannot load .task bundles; delete it and download the .litertlm model."
                _statusMessage.value = "Incompatible .task model found — download the LiteRT-LM model"
            } else {
                _lastError.value = null
                _statusMessage.value = "Offline model not installed (${formatBytes(EXPECTED_MODEL_BYTES)})"
            }
            return
        }

        modelFile = file
        _isModelFilePresent.value = true
        _modelSizeBytes.value = file.length()
        _modelLocation.value = file.absolutePath

        val loaded = engine
        if (loaded != null && engineBackend != null) {
            _isModelReady.value = true
            _engineName.value = "Gemma 4 E2B · LiteRT-LM ($engineBackend)"
            _statusMessage.value = "Ready · running on $engineBackend"
            _downloadProgress.value = 100
            _lastError.value = null
            return
        }

        if (forceReload) {
            lastFailedPath = null
            lastFailureAt = 0L
        }

        if (file.length() < MIN_MODEL_BYTES) {
            _isModelReady.value = false
            _lastError.value =
                "Model file is incomplete (${formatBytes(file.length())} of about ${formatBytes(EXPECTED_MODEL_BYTES)}). Delete it and download again."
            _statusMessage.value = "Incomplete model file"
            _engineName.value = "Offline phrasebook (incomplete model)"
            return
        }

        scope.launch { loadEngine(file, forceReload) }
    }

    fun startDownload(customDownloadUrl: String? = null) {
        if (closed.get() || _isDownloading.value || _isLoading.value) return
        scope.launch { downloadModel(customDownloadUrl) }
    }

    /**
     * Downloads (or resumes downloading) the LiteRT-LM bundle. The partial file
     * is kept on failure so a later attempt can resume with an HTTP Range
     * request instead of restarting a 2.6 GB transfer.
     */
    suspend fun downloadModel(customDownloadUrl: String? = null) = withContext(Dispatchers.IO) {
        if (closed.get()) return@withContext
        if (context == null) {
            _statusMessage.value = "Downloads require an Android context"
            return@withContext
        }
        if (_isDownloading.value || _isLoading.value) return@withContext
        if (!downloadActive.compareAndSet(false, true)) return@withContext

        _isDownloading.value = true
        downloadCancelled.set(false)
        _lastError.value = null
        _downloadProgress.value = 0
        _downloadBytesDownloaded.value = 0L
        _downloadTotalBytes.value = 0L
        _statusMessage.value = "Connecting to Hugging Face…"

        val targetFile = getPreferredModelFile()
        val partialFile = File(targetFile.parentFile, "${targetFile.name}.part")
        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            targetFile.parentFile?.mkdirs()
            val downloadUrl = customDownloadUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_MODEL_URL

            var existingBytes = if (partialFile.isFile) partialFile.length() else 0L
            var url = URL(downloadUrl)
            var conn: HttpURLConnection = openConnection(url, existingBytes)
            connection = conn
            activeConnection = conn
            var responseCode = conn.responseCode
            var redirectCount = 0

            while (responseCode in REDIRECT_CODES && redirectCount < 6) {
                val location = conn.getHeaderField("Location")
                if (location.isNullOrBlank()) break
                conn.disconnect()
                url = URL(url, location)
                conn = openConnection(url, existingBytes)
                connection = conn
                activeConnection = conn
                responseCode = conn.responseCode
                redirectCount++
            }

            if (responseCode == HttpURLConnection.HTTP_OK && existingBytes > 0) {
                // The server ignored our Range request; start over.
                partialFile.delete()
                existingBytes = 0L
            }

            if (responseCode == 416 && existingBytes > 0) {
                // The partial file is already at least as large as the remote
                // file. Validate and use it if possible.
                if (existingBytes >= MIN_MODEL_BYTES && hasValidHeader(partialFile)) {
                    finalizeDownload(partialFile, targetFile)
                    return@withContext
                }
                partialFile.delete()
                throw IOException("Resume state was invalid. Tap Download to start again.")
            }

            if (responseCode !in 200..299) {
                throw IOException("Server returned HTTP $responseCode")
            }

            val contentLength = conn.contentLengthLong
            val appending = responseCode == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0
            val totalBytes = when {
                appending && contentLength > 0 -> existingBytes + contentLength
                contentLength > 0 -> contentLength
                else -> 0L
            }

            val requiredBytes = when {
                totalBytes > 0 -> totalBytes - existingBytes
                else -> EXPECTED_MODEL_BYTES - existingBytes
            }
            checkFreeSpace(targetFile.parentFile, requiredBytes)

            _downloadTotalBytes.value = totalBytes
            _downloadBytesDownloaded.value = existingBytes
            _statusMessage.value = "Downloading Gemma 4 E2B…"

            input = conn.inputStream
            output = FileOutputStream(partialFile, appending)
            val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
            var downloaded = existingBytes
            var lastUiUpdate = 0L

            while (true) {
                if (downloadCancelled.get()) {
                    _statusMessage.value =
                        "Download paused at ${formatBytes(downloaded)}. Tap Download to resume."
                    return@withContext
                }
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                downloaded += read

                val now = System.currentTimeMillis()
                if (now - lastUiUpdate >= 200) {
                    lastUiUpdate = now
                    publishProgress(downloaded, totalBytes)
                }
            }

            output.flush()
            output.close()
            output = null
            input.close()
            input = null
            conn.disconnect()
            connection = null
            activeConnection = null

            if (partialFile.length() < MIN_MODEL_BYTES) {
                throw IOException(
                    "Downloaded file is too small (${formatBytes(partialFile.length())}). " +
                        "The URL may not point to a LiteRT-LM B8 bundle."
                )
            }
            if (!hasValidHeader(partialFile)) {
                throw IOException(
                    "Downloaded file is not a LiteRT-LM model (missing LITERTLM header). " +
                        "Use gemma-4-E2B-it.litertlm, not the MediaPipe .task file."
                )
            }

            finalizeDownload(partialFile, targetFile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            if (downloadCancelled.get()) {
                _statusMessage.value = "Download paused. Tap Download to resume."
            } else {
                val message = e.message ?: e.javaClass.simpleName
                _lastError.value = message
                _statusMessage.value = "Download failed: $message"
                logError("Model download failed", e)
            }
        } finally {
            try {
                output?.close()
            } catch (_: Exception) {
            }
            try {
                input?.close()
            } catch (_: Exception) {
            }
            try {
                connection?.disconnect()
            } catch (_: Exception) {
            }
            activeConnection = null
            _isDownloading.value = false
            downloadActive.set(false)
        }
    }

    private suspend fun finalizeDownload(partialFile: File, targetFile: File) {
        if (targetFile.exists()) targetFile.delete()
        if (!partialFile.renameTo(targetFile)) {
            partialFile.copyTo(targetFile, overwrite = true)
            partialFile.delete()
        }

        _downloadProgress.value = 100
        _downloadBytesDownloaded.value = targetFile.length()
        _downloadTotalBytes.value = targetFile.length()
        _statusMessage.value = "Download complete. Loading model…"

        modelFile = targetFile
        _isModelFilePresent.value = true
        _modelSizeBytes.value = targetFile.length()
        _modelLocation.value = targetFile.absolutePath
        _isDownloading.value = false

        loadEngine(targetFile, forceReload = true)
    }

    fun cancelDownload() {
        if (!_isDownloading.value) return
        downloadCancelled.set(true)
        _statusMessage.value = "Stopping download…"
        try {
            activeConnection?.disconnect()
        } catch (_: Exception) {
        }
    }

    /**
     * Runs a translation through LiteRT-LM. The system prompt is passed as the
     * conversation's system instruction and [userText] is the actual user
     * message, which is the correct separation for chat-tuned models.
     */
    override suspend fun generateTranslation(
        systemPrompt: String,
        userText: String,
        targetLanguage: String,
        targetLanguageCode: String,
        userContext: String,
        fallbackGlossary: Map<String, String>
    ): LocalTranslationOutput = withContext(Dispatchers.IO) {
        if (engine == null) {
            val file = findModelFile()
            if (file != null) {
                loadEngine(file)
            }
        }

        val activeEngine = engine
        if (activeEngine == null) {
            val reason = when {
                findModelFile() == null -> "model not installed"
                _isLoading.value -> "model still loading"
                else -> "model unavailable"
            }
            return@withContext phrasebookFallback(
                userText = userText,
                targetLanguageCode = targetLanguageCode,
                userContext = userContext,
                glossary = fallbackGlossary,
                reason = reason
            )
        }

        inferenceMutex.withLock {
            var conversation: Conversation? = null
            try {
                val config = ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    samplerConfig = SamplerConfig(
                        topK = 40,
                        topP = 0.95,
                        temperature = 0.2
                    ),
                    maxOutputToken = 640,
                    thinkingConfig = ThinkingConfig(enableThinking = false)
                )
                conversation = activeEngine.createConversation(config)
                val response: Message = conversation.sendMessage(userText)
                val text = extractText(response).trim()

                if (text.isBlank()) {
                    throw IllegalStateException("The model returned an empty response")
                }

                LocalTranslationOutput(
                    text = text,
                    engineLabel = "Gemma 4 E2B · LiteRT-LM (${engineBackend ?: "CPU"})",
                    usedFallback = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                logError("On-device inference failed", t)
                _lastError.value = t.message ?: t.javaClass.simpleName
                phrasebookFallback(
                    userText = userText,
                    targetLanguageCode = targetLanguageCode,
                    userContext = userContext,
                    glossary = fallbackGlossary,
                    reason = "inference error"
                )
            } finally {
                try {
                    conversation?.close()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private suspend fun loadEngine(file: File, forceReload: Boolean = false) {
        if (closed.get()) return
        if (!forceReload && engine != null) return

        loadMutex.withLock {
            if (closed.get()) return
            if (!forceReload && engine != null) return

            val now = System.currentTimeMillis()
            if (!forceReload &&
                lastFailedPath == file.absolutePath &&
                now - lastFailureAt < LOAD_RETRY_COOLDOWN_MS
            ) {
                return
            }

            val ctx = context ?: return
            if (!file.isFile || file.length() < MIN_MODEL_BYTES) {
                _isModelReady.value = false
                _lastError.value = "Model file is incomplete"
                _statusMessage.value = "Model file is incomplete"
                _engineName.value = "Offline phrasebook (incomplete model)"
                return
            }
            if (!hasValidHeader(file)) {
                _isModelReady.value = false
                _lastError.value =
                    "This file is not a LiteRT-LM model. Use a .litertlm bundle instead of a MediaPipe .task file."
                _statusMessage.value = "Invalid model format"
                _engineName.value = "Offline phrasebook (invalid model)"
                return
            }

            closeEngine()
            _isLoading.value = true
            _isModelReady.value = false
            _lastError.value = null
            _engineName.value = "Loading Gemma 4 E2B…"
            _statusMessage.value = "Loading the model into memory (this can take a few seconds)…"

            val failures = mutableListOf<String>()
            val cacheDir = File(ctx.cacheDir, "litertlm").apply { mkdirs() }
            val backends = listOf(
                "GPU" to Backend.GPU(),
                "CPU" to Backend.CPU()
            )

            for ((backendName, backend) in backends) {
                var candidate: Engine? = null
                try {
                    val config = EngineConfig(
                        modelPath = file.absolutePath,
                        backend = backend,
                        maxNumTokens = 4096,
                        cacheDir = cacheDir.absolutePath
                    )
                    candidate = Engine(config)
                    candidate.initialize()

                    if (closed.get()) {
                        try {
                            candidate.close()
                        } catch (_: Throwable) {
                        }
                        return
                    }

                    engine = candidate
                    engineBackend = backendName
                    modelFile = file
                    lastFailedPath = null
                    lastFailureAt = 0L
                    _isModelReady.value = true
                    _isLoading.value = false
                    _engineName.value = "Gemma 4 E2B · LiteRT-LM ($backendName)"
                    _statusMessage.value = "Ready · running on $backendName"
                    _downloadProgress.value = 100
                    return
                } catch (t: Throwable) {
                    failures += "$backendName: ${t.message ?: t.javaClass.simpleName}"
                    logError("LiteRT-LM $backendName initialization failed", t)
                    try {
                        candidate?.close()
                    } catch (_: Throwable) {
                    }
                }
            }

            engine = null
            engineBackend = null
            lastFailedPath = file.absolutePath
            lastFailureAt = System.currentTimeMillis()
            _isLoading.value = false
            _isModelReady.value = false
            _engineName.value = "Offline phrasebook (model failed to load)"
            _lastError.value = failures.joinToString("; ")
            _statusMessage.value = "Could not load the model: ${failures.lastOrNull() ?: "unknown error"}"
        }
    }

    private fun phrasebookFallback(
        userText: String,
        targetLanguageCode: String,
        userContext: String,
        glossary: Map<String, String>,
        reason: String
    ): LocalTranslationOutput {
        val result = LocalEdgeTranslationEngine.translate(
            sourceText = userText,
            targetLanguageCode = targetLanguageCode,
            userContext = userContext,
            glossary = glossary
        )

        val primary = result.primary.ifBlank { userText }
        val alternatives = result.alternatives.take(2)

        val json = buildJsonObject {
            put("primary", primary)
            putJsonArray("alternatives") {
                alternatives.forEach { (text, type, confidence) ->
                    addJsonObject {
                        put("text", text)
                        put("type", type)
                        put("confidence", confidence)
                    }
                }
            }
        }.toString()

        return LocalTranslationOutput(
            text = json,
            engineLabel = "Emergency phrasebook (offline fallback · $reason)",
            usedFallback = true
        )
    }

    private fun extractText(message: Message): String {
        val text = message.contents.contents
            .mapNotNull { content -> (content as? Content.Text)?.text }
            .joinToString("")
        return text.ifBlank { message.toString() }
    }

    fun getPreferredStorageDirectory(): File {
        val ctx = context ?: return File(System.getProperty("java.io.tmpdir") ?: ".", "hybrid_translator")
        val external = ctx.getExternalFilesDir("models")
        if (external != null) {
            if (!external.exists()) external.mkdirs()
            return external
        }
        val internal = File(ctx.filesDir, "models")
        if (!internal.exists()) internal.mkdirs()
        return internal
    }

    fun getPreferredModelFile(): File = File(getPreferredStorageDirectory(), MODEL_FILE_NAME)

    private fun findModelFile(): File? {
        return modelCandidates().firstOrNull { file ->
            file.isFile && file.length() > 0
        }
    }

    private fun modelCandidates(): List<File> {
        val candidates = mutableListOf<File>()
        val ctx = context
        if (ctx != null) {
            ctx.getExternalFilesDir("models")?.let { candidates += File(it, MODEL_FILE_NAME) }
            ctx.getExternalFilesDir(null)?.let { candidates += File(it, MODEL_FILE_NAME) }
            candidates += File(File(ctx.filesDir, "models"), MODEL_FILE_NAME)
            candidates += File(ctx.filesDir, MODEL_FILE_NAME)
        }
        return candidates.distinctBy { it.absolutePath }
    }

    private fun findLegacyTaskFile(): File? {
        val ctx = context ?: return null
        val names = listOf(
            "gemma-4-e2b.task",
            "gemma-4-E2B-it-web.task",
            "gemma-4-e2b.bin"
        )
        val dirs = listOfNotNull(
            ctx.getExternalFilesDir("models"),
            ctx.getExternalFilesDir(null),
            ctx.filesDir,
            File(ctx.filesDir, "models")
        )
        return dirs.asSequence()
            .flatMap { dir -> names.asSequence().map { File(dir, it) } }
            .firstOrNull { it.isFile && it.length() > 0 }
    }

    private fun openConnection(url: URL, rangeStart: Long): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", "HybridTranslator/1.0 (Android)")
        connection.setRequestProperty("Accept", "*/*")
        if (rangeStart > 0) {
            connection.setRequestProperty("Range", "bytes=$rangeStart-")
        }
        return connection
    }

    private fun publishProgress(downloaded: Long, total: Long) {
        _downloadBytesDownloaded.value = downloaded
        _downloadTotalBytes.value = total
        if (total > 0) {
            val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
            _downloadProgress.value = progress
            _statusMessage.value =
                "Downloading ${formatBytes(downloaded)} / ${formatBytes(total)} ($progress%)"
        } else {
            _statusMessage.value = "Downloading ${formatBytes(downloaded)}…"
        }
    }

    private fun checkFreeSpace(directory: File?, requiredBytes: Long) {
        if (directory == null || requiredBytes <= 0) return
        val usable = directory.usableSpace
        if (usable > 0 && usable < requiredBytes + FREE_SPACE_BUFFER_BYTES) {
            throw IOException(
                "Not enough free storage. Need ${formatBytes(requiredBytes + FREE_SPACE_BUFFER_BYTES)}, " +
                    "only ${formatBytes(usable)} available."
            )
        }
    }

    private fun hasValidHeader(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(MODEL_MAGIC.length)
                stream.read(header) == header.size &&
                    String(header, Charsets.US_ASCII) == MODEL_MAGIC
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun closeEngine() {
        try {
            engine?.close()
        } catch (t: Throwable) {
            logError("Failed to close LiteRT-LM engine", t)
        }
        engine = null
        engineBackend = null
    }

    private fun logError(message: String, throwable: Throwable) {
        if (context != null) {
            try {
                Log.e(TAG, message, throwable)
            } catch (_: Throwable) {
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / 1_000_000.0
        return if (mb >= 1000) String.format("%.2f GB", mb / 1000.0) else String.format("%.0f MB", mb)
    }

    override fun close() {
        closed.set(true)
        cancelDownload()
        scope.cancel()
        closeEngine()
        _isModelReady.value = false
        _isLoading.value = false
        _engineName.value = "Offline phrasebook"
    }
}
