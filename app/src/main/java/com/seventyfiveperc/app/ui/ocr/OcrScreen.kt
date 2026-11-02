package com.seventyfiveperc.app.ui.ocr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.seventyfiveperc.app.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(viewModel: OcrViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()
    val rawText by viewModel.rawText.collectAsState()
    var showDebug by remember { mutableStateOf(false) }
    var pendingType by remember { mutableStateOf(OcrType.GENERIC) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.processImage(context, it, pendingType) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Background decorative glow
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 50.dp)
                .blur(80.dp)
                .background(LimeGreen.copy(alpha = 0.1f), RoundedCornerShape(125.dp))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "INSTANT SYNC",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = LimeGreen
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = OffWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glass Card for Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassWhite)
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = LimeGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Upload ERP Screenshot",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = OffWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Stop manually entering subjects. Screenshot your ERP dashboard and let AURA handle the heavy lifting.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SecondaryGrey,
                                textAlign = TextAlign.Center
                            ),
                            lineHeight = 20.sp
                        )
                    }
                }

                // Upload Options Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OcrOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "TIMETABLE",
                        icon = Icons.Default.CalendarMonth,
                        color = NeonCyan,
                        onClick = { 
                            pendingType = OcrType.TIMETABLE
                            launcher.launch("image/*") 
                        }
                    )
                    OcrOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "CALENDAR",
                        icon = Icons.Default.Event,
                        color = HotMagenta,
                        onClick = { 
                            pendingType = OcrType.CALENDAR
                            launcher.launch("image/*") 
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OcrOptionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "GENERIC ATTENDANCE",
                    icon = Icons.Default.Sync,
                    color = LimeGreen,
                    onClick = { 
                        pendingType = OcrType.GENERIC
                        launcher.launch("image/*") 
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.aura_4),
                            contentDescription = "Processing",
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "EXTRACTING DATA...",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = LimeGreen,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = message != null || viewModel.pendingData.collectAsState().value != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val pendingData by viewModel.pendingData.collectAsState()
                    val msg = message

                    Column(modifier = Modifier.padding(top = 32.dp)) {
                        if (pendingData != null) {
                            SyncReviewCard(
                                data = pendingData!!,
                                onConfirm = { viewModel.applyPendingSync() },
                                onDiscard = { viewModel.discardPending() }
                            )
                        } else if (msg != null) {
                            val isError = msg.contains("Error", ignoreCase = true) || msg.contains("Failed", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isError) BloodRed.copy(alpha = 0.1f) else LimeGreen.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        if (isError) BloodRed.copy(alpha = 0.4f) else LimeGreen.copy(alpha = 0.4f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isError) Icons.Default.ErrorOutline else Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = if (isError) BloodRed else LimeGreen
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (isError) "SYNC FAILED" else "SYNC SUCCESSFUL",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (isError) BloodRed else LimeGreen
                                            )
                                        )
                                        Text(
                                            msg,
                                            style = MaterialTheme.typography.bodySmall.copy(color = OffWhite)
                                        )
                                        if (isError && !rawText.isNullOrBlank()) {
                                            TextButton(onClick = { showDebug = !showDebug }) {
                                                Text(if (showDebug) "HIDE RAW TEXT" else "VIEW RAW TEXT", color = LimeGreen, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDebug && !rawText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGrey.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                            Text("DEBUG: DETECTED TEXT", style = MaterialTheme.typography.labelSmall, color = SecondaryGrey)
                            Text(rawText ?: "", style = MaterialTheme.typography.bodySmall, color = OffWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncReviewCard(
    data: OcrViewModel.PendingSyncData,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LimeGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "REVIEW EXTRACTION",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = LimeGreen,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            ReviewItem("Subjects Found", data.subjects.size.toString())
            ReviewItem("Classes Mapped", data.timetableEntries.size.toString())
            ReviewItem("Academic Events", data.academicEvents.size.toString())
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed.copy(alpha = 0.5f))
                ) {
                    Text("DISCARD")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = Color.Black)
                ) {
                    Text("SYNC NOW")
                }
            }
        }
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(LimeGreen, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = SecondaryGrey)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = OffWhite)
    }
}

@Composable
fun OcrOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title, 
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = color
                )
            )
        }
    }
}
