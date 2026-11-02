package com.seventyfiveperc.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.data.AppPreferences
import com.seventyfiveperc.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.seventyfiveperc.app.ui.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    val subjects by viewModel.subjects.collectAsState()
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectGoal by remember { mutableStateOf("75") }

    var name by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var extraKeywords by remember { mutableStateOf("") }
    var useOffline by remember { mutableStateOf(true) }
    var sensitivity by remember { mutableStateOf("medium") }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        name = prefs.userName.first()
        rollNumber = prefs.rollNumber.first()
        extraKeywords = prefs.extraKeywords.first()
        useOffline = prefs.useOfflineMode.first()
        sensitivity = prefs.sensitivity.first()
        isLoaded = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "PROFILE SETUP", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 2.sp,
                            color = NeonCyan
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { paddingValues ->
        if (!isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Column {
                Text(
                    text = "PUT ME ON GAME 🎮",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = NeonCyan)
                )
                Text(
                    text = "Tell AURA how your name gets called so it can save your degree.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryGrey
                )
            }

            // ===== NAME FIELD =====
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassWhite,
                    focusedLabelColor = NeonCyan,
                    cursorColor = NeonCyan,
                    unfocusedLabelColor = SecondaryGrey,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite
                )
            )

            // ===== ROLL NUMBER FIELD =====
            OutlinedTextField(
                value = rollNumber,
                onValueChange = { rollNumber = it },
                label = { Text("Roll Number") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NeonCyan) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassWhite,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = SecondaryGrey,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite
                )
            )

            // ===== SPEECH ENGINE TOGGLE =====
            Text("ENGINE SETTINGS", style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GlassWhite),
                border = BorderStroke(1.dp, if (useOffline) HotMagenta.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (useOffline) "🔇 OFFLINE (VOSK)" else "🌐 ONLINE (GOOGLE)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = if (useOffline) HotMagenta else NeonCyan)
                        )
                        Text(
                            text = if (useOffline) "No wifi needed. Low latency." else "Higher accuracy, needs internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryGrey
                        )
                    }
                    Switch(
                        checked = useOffline,
                        onCheckedChange = { useOffline = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = HotMagenta,
                            uncheckedTrackColor = NeonCyan
                        )
                    )
                }
            }

            // ===== SAVE BUTTON =====
            Button(
                onClick = {
                    if (name.isBlank() && rollNumber.isBlank()) {
                        Toast.makeText(context, "Bruh, enter your details!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        prefs.saveProfile(name.trim(), rollNumber.trim(), extraKeywords.trim(), useOffline)
                        prefs.setSensitivity(sensitivity)
                        Toast.makeText(context, "Locked In! ✅", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("LOCK IT IN 🔒", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = PureBlack))
            }

            Divider(color = GlassWhite, modifier = Modifier.padding(vertical = 8.dp))

            // ===== SUBJECT MANAGEMENT =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR SUBJECTS 📚", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = NeonCyan))
                IconButton(onClick = { showAddSubjectDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonCyan)
                }
            }

            if (subjects.isEmpty()) {
                Text("No subjects found. Use the + to add or Sync ERP in dashboard.", color = SecondaryGrey, style = MaterialTheme.typography.bodySmall)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { subject ->
                    SubjectListItem(subject = subject, onDelete = { viewModel.deleteSubject(subject) })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("75") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("ADD SUBJECT", color = NeonCyan, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Subject Name") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )
                OutlinedTextField(
                    value = goal, 
                    onValueChange = { goal = it }, 
                    label = { Text("Attendance Goal %") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, goal.toIntOrNull() ?: 75) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("ADD", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = SecondaryGrey) }
        }
    )
}

@Composable
fun SubjectListItem(subject: com.seventyfiveperc.app.data.models.Subject, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = subject.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = OffWhite))
                Text(text = "Goal: ${subject.minRequiredPercentage}%", style = MaterialTheme.typography.bodySmall, color = SecondaryGrey)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SecondaryGrey)
            }
        }
    }
}
