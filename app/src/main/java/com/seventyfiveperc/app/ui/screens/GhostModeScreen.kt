package com.seventyfiveperc.app.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlin.math.min
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.seventyfiveperc.app.R
import androidx.compose.material.icons.filled.Dashboard
import com.seventyfiveperc.app.data.AppPreferences
import com.seventyfiveperc.app.service.ListeningService
import com.seventyfiveperc.app.speech.NameDetector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.seventyfiveperc.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AttendAiTheme {
        MainScreen(onNavigateToProfile = {}, onNavigateToDashboard = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    // Overlay Permission State
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    // Check Overlay Permission
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    // Check Battery Optimization
    fun checkBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    // Effect to check permissions
    LaunchedEffect(Unit) {
        if (!checkOverlayPermission()) {
            showOverlayDialog = true
        }
        if (!checkBatteryOptimizations()) {
            showBatteryDialog = true
        }
    }

    // User profile state
    val userName by prefs.userName.collectAsState(initial = "")
    val rollNumber by prefs.rollNumber.collectAsState(initial = "")
    val extraKeywords by prefs.extraKeywords.collectAsState(initial = "")
    val isProfileSet by prefs.isProfileSet.collectAsState(initial = false)
    val useOffline by prefs.useOfflineMode.collectAsState(initial = true)

    var editName by remember(userName) { mutableStateOf(userName) }
    var editRoll by remember(rollNumber) { mutableStateOf(rollNumber) }
    var editKeywords by remember(extraKeywords) { mutableStateOf(extraKeywords) }
    var isEditing by remember { mutableStateOf(false) }

    // Listening state
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var detectionMessage by remember { mutableStateOf<String?>(null) }
    var audioLevel by remember { mutableStateOf(0f) }

    // Service binding
    var service by remember { mutableStateOf<ListeningService?>(null) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as ListeningService.LocalBinder
                service = localBinder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    // Bind to service
    DisposableEffect(Unit) {
        val intent = Intent(context, ListeningService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    // Observe service state
    LaunchedEffect(service) {
        service?.let { svc ->
            launch { svc.isListening.collect { isListening = it } }
            launch { svc.transcript.collect { transcript = it } }
            launch { svc.audioLevel.collect { audioLevel = it } }
            launch {
                svc.lastDetection.collect { detection ->
                    detection?.let {
                        if (it.matched) {
                            val pct = (it.confidence * 100).toInt()
                            detectionMessage = if (it.isPreAlert) {
                                "⚠️ YO! Roll number ${it.matchedKeyword} just called. You're up next, wake up! ⚡"
                            } else {
                                "🚨 BRO THEY CALLED YOU: \"${it.matchedKeyword}\" ($pct% match)"
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            startListeningService(context, prefs, scope)
        } else {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Pulse animation for the mic button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val micButtonColor by animateColorAsState(
        targetValue = if (isListening) LimeGreen else NeonCyan,
        animationSpec = tween(500),
        label = "micColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Attend.ai 🚀",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Online/Offline toggle chip
                    AssistChip(
                        onClick = {
                            scope.launch {
                                prefs.saveProfile(
                                    name = prefs.userName.first(),
                                    rollNumber = prefs.rollNumber.first(),
                                    extraKeywords = prefs.extraKeywords.first(),
                                    useOffline = !useOffline
                                )
                            }
                        },
                        label = {
                            Text(
                                if (useOffline) "Offline" else "Online",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (useOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Aura Persona Image
            BoxWithConstraints(modifier = Modifier.padding(horizontal = 24.dp)) {
                val boxSize = min(maxWidth * 0.4f, 160.dp)
                val auraRes = if (isListening) {
                    // Pick an active aura
                    R.drawable.aura_1
                } else {
                    R.drawable.aura_4
                }
                
                Image(
                    painter = painterResource(id = auraRes),
                    contentDescription = "Aura Persona",
                    modifier = Modifier
                        .size(boxSize)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            4.dp,
                            Brush.linearGradient(listOf(NeonCyan, HotMagenta)),
                            RoundedCornerShape(24.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            if (isEditing) {
                ProfileEditCard(
                    name = editName,
                    onNameChange = { editName = it },
                    roll = editRoll,
                    onRollChange = { editRoll = it },
                    keywords = editKeywords,
                    onKeywordsChange = { editKeywords = it },
                    onSave = {
                        scope.launch {
                            prefs.saveProfile(editName, editRoll, editKeywords, useOffline)
                            isEditing = false
                        }
                    }
                )
            } else {
                Text(
                    text = if (!isProfileSet) "who r u? set profile first 🛑"
                    else if (isListening) "ears open fr... (listening) 👂✨"
                    else "tap to survive attendance today 💀💅",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 24.dp).clickable { isEditing = true }
                )

                if (isProfileSet) {
                    Text(
                        text = "on watch for: $userName ${if (rollNumber.isNotBlank()) "/ $rollNumber" else ""} 👀",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp).clickable { isEditing = true }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== BIG MIC BUTTON - Responsive size =====
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 240.dp)
            ) {
                val micSize = min(maxWidth * 0.5f, 200.dp)
                val buttonSize = micSize * 0.8f

                // Outer pulse ring (only when listening)
                if (isListening) {
                    // Audio Level Ring
                    Box(
                        modifier = Modifier
                            .size(micSize + (audioLevel * 1.5f).dp)
                            .clip(CircleShape)
                            .background(micButtonColor.copy(alpha = 0.2f))
                    )

                    Box(
                        modifier = Modifier
                            .size(micSize)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(micButtonColor.copy(alpha = pulseAlpha))
                    )
                }

                // Main mic button
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isProfileSet) {
                            onNavigateToProfile()
                            return@FilledIconButton
                        }

                        if (isListening) {
                            val stopIntent = Intent(context, ListeningService::class.java).apply {
                                action = ListeningService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(buttonSize)
                        .shadow(
                            elevation = if (isListening) 24.dp else 8.dp,
                            shape = CircleShape,
                            ambientColor = micButtonColor,
                            spotColor = micButtonColor
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = micButtonColor
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isListening) "Stop listening" else "Start listening",
                        modifier = Modifier.size(buttonSize * 0.4f),
                        tint = Color.White
                    )
                }
            }

            // Mode indicator
            if (isListening) {
                val mode = service?.engineMode?.collectAsState()?.value ?: "offline"
                Text(
                    text = if (mode == "offline") "🔇 offline vibe check (vosk)" else "🌐 online carry (google)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== DETECTION ALERT CARD =====
            AnimatedVisibility(
                visible = detectionMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(LimeGreen, NeonCyan)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LimeGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = detectionMessage ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = LimeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { detectionMessage = null }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== LIVE TRANSCRIPT =====
            if (isListening && transcript.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "YAPPING LOGS 🗣️",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ===== SETUP PROMPT (if no profile) =====
            if (!isProfileSet) {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HotMagenta.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👋 yo, welcome to Attend.ai!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "drop your name and roll number so we can save your attendance fr fr.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan
                            )
                        ) {
                            Text("Set Up Profile ⚡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ===== OVERLAY PERMISSION DIALOG =====
        if (showOverlayDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayDialog = false },
                title = { Text("Display over other apps") },
                text = {
                    Text("To show alerts while you're using other apps (like Instagram or Games), we need the 'Display over other apps' permission.")
                },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        showOverlayDialog = false
                    }) {
                        Text("Go to Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayDialog = false }) {
                        Text("Maybe Later")
                    }
                }
            )
        }

        // ===== BATTERY OPTIMIZATION DIALOG =====
        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                title = { Text("Long Lecture Support 🔋") },
                text = {
                    Text("To keep listening for 1-2 hours without Android killing the app, please disable battery optimization for Attend.ai.")
                },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        showBatteryDialog = false
                    }) {
                        Text("Disable Optimization")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryDialog = false }) {
                        Text("I'll take the risk")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileEditCard(
    name: String,
    onNameChange: (String) -> Unit,
    roll: String,
    onRollChange: (String) -> Unit,
    keywords: String,
    onKeywordsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("SET YOUR GHOST PROFILE 👻", style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Black))
            
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name", color = SecondaryGrey) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = OffWhite, unfocusedTextColor = OffWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )

            OutlinedTextField(
                value = roll,
                onValueChange = onRollChange,
                label = { Text("Roll Number", color = SecondaryGrey) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = OffWhite, unfocusedTextColor = OffWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )

            OutlinedTextField(
                value = keywords,
                onValueChange = onKeywordsChange,
                label = { Text("Keywords (comma separated)", color = SecondaryGrey) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = OffWhite, unfocusedTextColor = OffWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("SAVE PROFILE ⚡", fontWeight = FontWeight.Bold, color = PureBlack)
            }
        }
    }
}

private fun startListeningService(
    context: Context,
    prefs: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        val intent = Intent(context, ListeningService::class.java).apply {
            action = ListeningService.ACTION_START
            putExtra(ListeningService.EXTRA_USER_NAME, prefs.userName.first())
            putExtra(ListeningService.EXTRA_ROLL_NUMBER, prefs.rollNumber.first())
            putExtra(ListeningService.EXTRA_EXTRA_KEYWORDS, prefs.extraKeywords.first())
            putExtra(ListeningService.EXTRA_USE_OFFLINE, prefs.useOfflineMode.first())
            putExtra(ListeningService.EXTRA_SENSITIVITY, prefs.sensitivity.first())
        }
        context.startForegroundService(intent)
    }
}
