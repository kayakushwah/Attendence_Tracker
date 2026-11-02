package com.seventyfiveperc.app.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.vosk.Model
import org.vosk.Recognizer as VoskRecognizer
import org.vosk.android.StorageService
import java.lang.reflect.Method
import kotlin.math.sqrt

class VoskSpeechEngine(private val context: Context) {

    companion object {
        private const val TAG = "VoskSpeechEngine"
        private const val SAMPLE_RATE = 16000f
        private const val BUFFER_SIZE = 3200 // 0.2 seconds
    }

    private var model: Model? = null
    private var recognizer: VoskRecognizer? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Cache the method to avoid reflection overhead in the loop
    private var acceptWaveformMethod: Method? = null

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    var onFinalResult: ((String) -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null
    
    private var audioGain = 2.5f 

    fun initModel(onReady: () -> Unit, onError: (String) -> Unit) {
        if (model != null) {
            Log.d(TAG, "Model already initialized")
            onReady()
            return
        }
        
        val assetName = "model-en-us"
        Log.d(TAG, "Initializing Vosk model from assets: $assetName")
        _isModelLoading.value = true
        
        // StorageService.unpack() typically expects a ZIP file or a folder in assets.
        // It copies it to context.filesDir / "model"
        StorageService.unpack(context, assetName, "model",
            { loadedModel ->
                Log.d(TAG, "Vosk model unpacked and loaded successfully")
                model = loadedModel
                _isModelLoading.value = false
                onReady()
            },
            { e ->
                Log.w(TAG, "Vosk model unpack failed. Attempting to load directly from assets.", e)
                try {
                    // Try to load directly if already unpacked or if StorageService supports it
                    // The path is usually filesDir + "/model" + assetName
                    val modelPath = context.getExternalFilesDir(null)?.absolutePath + "/model"
                    // If that fails, try internal
                    val internalPath = context.filesDir.absolutePath + "/model"
                    
                    Log.d(TAG, "Checking paths: $modelPath or $internalPath")
                    
                    model = Model(internalPath)
                    Log.d(TAG, "Vosk model loaded from internal filesDir")
                    _isModelLoading.value = false
                    onReady()
                } catch (ex: Exception) {
                    Log.e(TAG, "Direct load failed too", ex)
                    _isModelLoading.value = false
                    onError(e.message ?: "Unknown error")
                }
            }
        )
    }

    fun startListening(keywords: List<String> = emptyList()) {
        if (_isActive.value) {
            Log.w(TAG, "Already listening, ignoring start request")
            return
        }
        val currentModel = model
        if (currentModel == null) {
            Log.e(TAG, "Cannot start listening: Model is null")
            _error.value = "Model not initialized"
            return
        }

        try {
            Log.d(TAG, "Starting listening with keywords: $keywords")
            val grammar = if (keywords.isNotEmpty()) {
                val list = keywords.toMutableList()
                list.addAll(listOf("present", "yes", "here", "number", "roll"))
                (0..99).forEach { list.add(it.toString()) }
                val json = list.distinct().joinToString("\", \"", "[\"", "\"]")
                json
            } else null

            recognizer = if (grammar != null) {
                VoskRecognizer(currentModel, SAMPLE_RATE, grammar)
            } else {
                VoskRecognizer(currentModel, SAMPLE_RATE)
            }
            
            // Cache method once
            acceptWaveformMethod = recognizer?.javaClass?.getMethod(
                "acceptWaveform", ShortArray::class.java, Int::class.javaPrimitiveType
            )
            
            recognizer?.setMaxAlternatives(3)
            _isActive.value = true 
            startRecordingLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognizer", e)
            _error.value = e.message
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingLoop() {
        Log.d(TAG, "Starting recording loop")
        recordingJob = scope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE.toInt(), 
                AudioFormat.CHANNEL_IN_MONO, 
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize.coerceAtLeast(BUFFER_SIZE)
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord state is NOT INITIALIZED")
                _isActive.value = false
                return@launch
            }

            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to start recording (possibly permission or hardware busy)")
                _isActive.value = false
                return@launch
            }

            Log.d(TAG, "Recording started successfully")
            val buffer = ShortArray(BUFFER_SIZE)

            try {
                // Use _isActive.value to avoid shadowing with CoroutineScope.isActive
                while (_isActive.value && this.isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // RMS calculation for UI feedback
                        var sum = 0.0
                        for (i in 0 until read) sum += buffer[i] * buffer[i]
                        _audioLevel.value = (sqrt(sum / read).toFloat() / 32768.0f) * 100

                        // Apply Gain
                        for (i in 0 until read) {
                            val boosted = (buffer[i] * audioGain).toInt()
                            buffer[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                        
                        // Use cached method (fast)
                        val complete = acceptWaveformMethod?.invoke(recognizer, buffer, read) as? Boolean ?: false

                        if (complete) {
                            handleJsonResult(recognizer?.result ?: "", true)
                        } else {
                            handleJsonResult(recognizer?.partialResult ?: "", false)
                        }
                    }
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    private fun handleJsonResult(json: String, isFinal: Boolean) {
        if (json.isBlank()) return
        val text = extractText(json, if (isFinal) "text" else "partial")
        if (text.isNotBlank()) {
            if (isFinal) onFinalResult?.invoke(text) else onPartialResult?.invoke(text)
        }
        if (isFinal) {
            Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").findAll(json).forEach { match ->
                val altText = match.groupValues.getOrNull(1) ?: ""
                if (altText.isNotBlank() && altText != text) onFinalResult?.invoke(altText)
            }
        }
    }

    private fun extractText(json: String, key: String): String {
        return Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.getOrNull(1) ?: ""
    }

    fun stopListening() {
        _isActive.value = false
        recordingJob?.cancel()
        recordingJob = null
    }

    fun release() {
        stopListening()
        scope.cancel() // Prevent memory leaks
        recognizer?.close()
        model?.close()
        model = null
        acceptWaveformMethod = null
    }
}
