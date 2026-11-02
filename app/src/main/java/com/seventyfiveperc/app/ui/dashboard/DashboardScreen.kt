package com.seventyfiveperc.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.data.models.Subject
import com.seventyfiveperc.app.logic.AttendanceStats
import com.seventyfiveperc.app.ui.components.AttendanceTrend
import com.seventyfiveperc.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAssistant: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToPredictor: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToGhostMode: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddSubject by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Attend.ai",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = NeonCyan
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = NeonCyan)
                    }
                    IconButton(onClick = onNavigateToOcr) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Sync ERP", tint = NeonCyan)
                    }
                }
            )
        },
        bottomBar = {
            ClutchNavigationBar(
                onHome = {},
                onSchedule = onNavigateToTimetable,
                onPredict = onNavigateToPredictor,
                onAssistant = onNavigateToAssistant,
                onListeningAction = onNavigateToGhostMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSubject = true },
                containerColor = HotMagenta,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(12.dp, CircleShape, ambientColor = HotMagenta, spotColor = HotMagenta)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        containerColor = PureBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Neon Glows
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .blur(100.dp)
                    .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-100).dp, y = 100.dp)
                    .blur(80.dp)
                    .background(HotMagenta.copy(alpha = 0.1f), CircleShape)
            )

            if (showAddSubject) {
                AddSubjectDialog(
                    onDismiss = { showAddSubject = false },
                    onConfirm = { name, goal ->
                        viewModel.addSubject(name, goal)
                        showAddSubject = false
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    HeroStatsSection(state.overallAttendance, state.auraScore, state.subjects.isNotEmpty())
                }

                item {
                    GhostStatusIndicator(onClick = onNavigateToGhostMode)
                }

                if (state.todayAgenda.isNotEmpty()) {
                    item {
                        Text(
                            "TODAY'S AGENDA", 
                            style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AgendaGlassCard(state.todayAgenda, onMark = { id -> viewModel.markAttendance(id, true) })
                    }
                }

                item {
                    Text(
                        "SUBJECT CARDS", 
                        style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold)
                    )
                }

                items(state.subjects.chunked(2)) { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { subject ->
                            SubjectGlassCard(
                                modifier = Modifier.weight(1f),
                                subject = subject,
                                stats = state.subjectStats[subject.id],
                                onSimulate = { /* Open Bunk Sim */ }
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                item {
                    AIAdvisorSnippet(state.aiSuggestion, state.aiSuggestionImage)
                }
            }
        }
    }
}

@Composable
fun HeroStatsSection(percentage: Double, auraScore: Int, hasSubjects: Boolean) {
    val statusText = when {
        !hasSubjects -> "no subjects yet? add some or sync ur erp 🫡"
        percentage < 75 -> "u fr fr skipped too much lmao 😭"
        percentage < 80 -> "ur edging the 75% limit bestie 🥹"
        else -> "u actually showing up? character development 🫡"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (!hasSubjects) "GETTING STARTED" else "${percentage.toInt()}% NO CAP ${if (percentage < 75) "😭" else "🫡"}",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                color = if (!hasSubjects) OffWhite else if (percentage < 75) BloodRed else NeonCyan,
                fontSize = if (!hasSubjects) 32.sp else 45.sp
            )
        )
        
        // Aura Score Badge
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, HotMagenta)))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AURA: +$auraScore",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = PureBlack
                )
            )
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium.copy(color = OffWhite),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun GhostStatusIndicator(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HotMagenta.copy(alpha = 0.15f))
            .border(1.dp, HotMagenta.copy(alpha = alpha), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👻", modifier = Modifier.graphicsLayer { this.alpha = alpha })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "GHOST IS LISTENING RN", 
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = HotMagenta
                )
            )
        }
    }
}

@Composable
fun AgendaGlassCard(agenda: List<AgendaItem>, onMark: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            agenda.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${item.subject.name} (${item.entry.startTime})", 
                            style = MaterialTheme.typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            if (item.isMarked) "marked fr 🫡" else "u can bunk this 🫠",
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryGrey)
                        )
                    }
                    if (!item.isMarked) {
                        Button(
                            onClick = { onMark(item.subject.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Quick tap", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                        }
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LimeGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectGlassCard(
    modifier: Modifier = Modifier,
    subject: Subject,
    stats: AttendanceStats?,
    onSimulate: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .border(0.5.dp, GlassWhite, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(subject.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = OffWhite))
                if (stats != null) {
                    Icon(
                        if (stats.percentage > 75) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (stats.percentage > 75) LimeGreen else BloodRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            stats?.let {
                Text(
                    "${it.percentage.toInt()}%", 
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = OffWhite)
                )
                
                AttendanceTrend(
                    trend = it.trend, 
                    lineColor = if (it.percentage > 75) NeonCyan else BloodRed,
                    modifier = Modifier.height(30.dp).padding(vertical = 4.dp)
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "safe bunks: ${it.safeBunks}", 
                        style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun AIAdvisorSnippet(suggestion: String, imageRes: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HotMagenta),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("AI Advisor", style = MaterialTheme.typography.labelSmall.copy(color = SecondaryGrey))
            Text(suggestion, style = MaterialTheme.typography.bodyMedium.copy(color = OffWhite))
        }
    }
}

@Composable
fun ClutchNavigationBar(
    onHome: () -> Unit,
    onSchedule: () -> Unit,
    onPredict: () -> Unit,
    onAssistant: () -> Unit,
    onListeningAction: () -> Unit
) {
    NavigationBar(
        containerColor = PureBlack,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, unselectedIconColor = SecondaryGrey, indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = false,
            onClick = onSchedule,
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule") },
            label = { Text("Schedule") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, unselectedIconColor = SecondaryGrey)
        )
        
        // Listening AI Button (Central Action)
        Box(
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-16).dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(HotMagenta, NeonCyan)))
                .padding(2.dp)
                .clip(CircleShape)
                .background(PureBlack)
                .clickable { onListeningAction() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Listening AI",
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        NavigationBarItem(
            selected = false,
            onClick = onPredict,
            icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Future Me") },
            label = { Text("Future Me") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, unselectedIconColor = SecondaryGrey)
        )
        NavigationBarItem(
            selected = false,
            onClick = onAssistant,
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Aura") },
            label = { Text("Aura") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, unselectedIconColor = SecondaryGrey)
        )
    }
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("75") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("Add New Subject", color = OffWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Subject Name") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite)
                )
                TextField(
                    value = goal, 
                    onValueChange = { goal = it }, 
                    label = { Text("Goal %") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, goal.toIntOrNull() ?: 75) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add", color = PureBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryGrey) }
        }
    )
}
