package com.seventyfiveperc.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.seventyfiveperc.app.alert.AlertManager
import com.seventyfiveperc.app.speech.GoogleSpeechEngine
import com.seventyfiveperc.app.speech.NameDetector
import com.seventyfiveperc.app.speech.VoskSpeechEngine
import com.seventyfiveperc.app.data.local.AppDatabase
import com.seventyfiveperc.app.data.models.AttendanceRecord
import com.seventyfiveperc.app.data.models.AttendanceStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Foreground Service that keeps speech recognition running
 * even when the app is in the background.
 */
class ListeningService : Service() {

    companion object {
        private const val TAG = "ListeningService"
        const val ACTION_START = "com.seventyfiveperc.START_LISTENING"
        const val ACTION_STOP = "com.seventyfiveperc.STOP_LISTENING"
        const val EXTRA_USE_OFFLINE = "use_offline"
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_ROLL_NUMBER = "roll_number"
        const val EXTRA_EXTRA_KEYWORDS = "extra_keywords"
        const val EXTRA_SENSITIVITY = "sensitivity"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var alertManager: AlertManager
    private lateinit var nameDetector: NameDetector
    private var googleEngine: GoogleSpeechEngine? = null
    private var voskEngine: VoskSpeechEngine? = null

    private lateinit var database: AppDatabase

    private var wakeLock: PowerManager.WakeLock? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _lastDetection = MutableStateFlow<NameDetector.DetectionResult?>(null)
    val lastDetection: StateFlow<NameDetector.DetectionResult?> = _lastDetection.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _engineMode = MutableStateFlow("offline") // "offline" or "online"
    val engineMode: StateFlow<String> = _engineMode.asStateFlow()

    // Cooldown to prevent spamming alerts
    private var lastAlertTime = 0L
    private val alertCooldownMs = 15000L  // 15 seconds between alerts

    inner class LocalBinder : Binder() {
        fun getService(): ListeningService = this@ListeningService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(this)
        nameDetector = NameDetector()
        database = AppDatabase.getDatabase(this)
        Log.d(TAG, "ListeningService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val useOffline = intent.getBooleanExtra(EXTRA_USE_OFFLINE, true)
                val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
                val rollNumber = intent.getStringExtra(EXTRA_ROLL_NUMBER) ?: ""
                val extraKeywords = intent.getStringExtra(EXTRA_EXTRA_KEYWORDS) ?: ""
                val sensitivity = intent.getStringExtra(EXTRA_SENSITIVITY) ?: "medium"

                startForegroundListening(useOffline, userName, rollNumber, extraKeywords, sensitivity)
            }
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundListening(
        useOffline: Boolean,
        userName: String,
        rollNumber: String,
        extraKeywords: String,
        sensitivity: String
    ) {
        Log.d(TAG, "Starting foreground service...")
        // Start as foreground service with persistent notification
        val notification = alertManager.createListeningNotification()
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground(
                    alertManager.getListeningNotificationId(), 
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(alertManager.getListeningNotificationId(), notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // On Android 12+, we might get ForegroundServiceStartNotAllowedException
            }
            return
        }

        // Acquire wake lock to prevent CPU sleep
        acquireWakeLock()

        // Configure name detector
        nameDetector.setKeywords(userName, rollNumber, extraKeywords)
        nameDetector.setSensitivity(sensitivity)

        // Ensure we have RECORD_AUDIO permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start listening.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Set up speech recognition callback
        val onSpeechResult: (String) -> Unit = { text ->
            _transcript.value = text
            checkForName(text)
        }

        if (useOffline) {
            _engineMode.value = "offline"
            val keywords = mutableListOf<String>()
            if (userName.isNotBlank()) keywords.add(userName)
            if (rollNumber.isNotBlank()) keywords.add(rollNumber)
            if (extraKeywords.isNotBlank()) {
                keywords.addAll(extraKeywords.split(",").map { it.trim() })
            }
            startVoskEngine(onSpeechResult, keywords)
        } else {
            _engineMode.value = "online"
            startGoogleEngine(onSpeechResult)
        }

        _isListening.value = true
        Log.d(TAG, "Foreground listening started (mode: ${_engineMode.value})")
    }

    private fun startVoskEngine(onResult: (String) -> Unit, keywords: List<String> = emptyList()) {
        voskEngine = VoskSpeechEngine(this).apply {
            onFinalResult = onResult
            onPartialResult = { partial ->
                _transcript.value = partial
                // Also check partial results for faster detection
                checkForName(partial)
            }

            serviceScope.launch {
                audioLevel.collect { _audioLevel.value = it }
            }

            initModel(
                onReady = { startListening(keywords) },
                onError = { error ->
                    Log.e(TAG, "Vosk init failed: $error, falling back to Google")
                    // Fallback to Google if Vosk fails
                    _engineMode.value = "online"
                    startGoogleEngine(onResult)
                }
            )
        }
    }

    private fun startGoogleEngine(onResult: (String) -> Unit) {
        googleEngine = GoogleSpeechEngine(this).apply {
            onFinalResult = onResult
            onPartialResult = { partial ->
                _transcript.value = partial
                checkForName(partial)
            }
            startListening()
        }
    }

    private fun checkForName(text: String) {
        val result = nameDetector.analyze(text)
        if (result.matched) {
            _lastDetection.value = result

            // Check cooldown to avoid spamming
            val now = System.currentTimeMillis()
            if (now - lastAlertTime > alertCooldownMs) {
                lastAlertTime = now
                alertManager.fireAttendanceAlert(
                    result.matchedKeyword,
                    result.confidence,
                    result.isPreAlert
                )
                
                // Only auto-mark if it's an actual match for the user, not a pre-alert
                if (!result.isPreAlert) {
                    autoMarkAttendance()
                    
                    // Stop listening automatically after marking attendance to save battery/data
                    stopListening()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                Log.d(TAG, "🚨 DETECTION: ${result.matchedKeyword} (pre-alert: ${result.isPreAlert})")
            }
        }
    }

    private fun autoMarkAttendance() {
        serviceScope.launch {
            val now = Calendar.getInstance()
            val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

            val timetable = database.attendanceDao().getTimetableForDay(dayOfWeek).first()
            
            // Find class happening now
            val currentClass = timetable.find { entry ->
                currentTime >= entry.startTime && currentTime <= entry.endTime
            }

            currentClass?.let { entry ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Check if already marked
                val alreadyMarked = database.attendanceDao().getAllAttendance().first().any {
                    it.subjectId == entry.subjectId && it.date >= today
                }

                if (!alreadyMarked) {
                    val record = AttendanceRecord(
                        subjectId = entry.subjectId,
                        date = System.currentTimeMillis(),
                        status = AttendanceStatus.PRESENT,
                        reason = "Auto-detected by AI Assistant"
                    )
                    database.attendanceDao().insertAttendance(record)
                    Log.d(TAG, "✅ Auto-marked attendance for Subject ID: ${entry.subjectId}")
                }
            }
        }
    }

    fun stopListening() {
        if (!_isListening.value) return
        
        _isListening.value = false
        googleEngine?.stopListening()
        googleEngine = null
        
        voskEngine?.stopListening()
        voskEngine?.release()
        voskEngine = null
        
        releaseWakeLock()
        Log.d(TAG, "Listening stopped and resources released")
    }

    /**
     * Switch between offline and online mode while listening.
     */
    fun switchMode(useOffline: Boolean, userName: String = "", rollNumber: String = "", extraKeywords: String = "") {
        googleEngine?.stopListening()
        voskEngine?.stopListening()

        val onResult: (String) -> Unit = { text ->
            _transcript.value = text
            checkForName(text)
        }

        if (useOffline) {
            _engineMode.value = "offline"
            val keywords = mutableListOf<String>()
            if (userName.isNotBlank()) keywords.add(userName)
            if (rollNumber.isNotBlank()) keywords.add(rollNumber)
            if (extraKeywords.isNotBlank()) {
                keywords.addAll(extraKeywords.split(",").map { it.trim() })
            }
            startVoskEngine(onResult, keywords)
        } else {
            _engineMode.value = "online"
            startGoogleEngine(onResult)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AttendAi:ListeningWakeLock"
        ).apply {
            acquire(2 * 60 * 60 * 1000L)  // 2 hours max for long lectures
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        voskEngine?.release()
        serviceScope.cancel()
        Log.d(TAG, "ListeningService destroyed")
    }
}
