package com.seventyfiveperc.app.ui.timetable

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.data.models.TimetableEntry
import com.seventyfiveperc.app.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: TimetableViewModel, onNavigateBack: () -> Unit) {
    var selectedDay by remember { mutableStateOf(Calendar.MONDAY) }
    val entriesWithNames by viewModel.getTimetableWithSubjectsForDay(selectedDay).collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val days = listOf(
        "MON" to Calendar.MONDAY,
        "TUE" to Calendar.TUESDAY,
        "WED" to Calendar.WEDNESDAY,
        "THU" to Calendar.THURSDAY,
        "FRI" to Calendar.FRIDAY,
        "SAT" to Calendar.SATURDAY
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Background Glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .blur(100.dp)
                .background(HotMagenta.copy(alpha = 0.15f), RoundedCornerShape(150.dp))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "UR SUFFERING SCHEDULE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = NeonCyan
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeonCyan,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Entry")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Day Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { (name, dayCode) ->
                        val isSelected = selectedDay == dayCode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else GlassWhite)
                                .clickable { selectedDay = dayCode }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureBlack else OffWhite
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (entriesWithNames.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "No suffering today? 🙃",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = SecondaryGrey,
                                            fontWeight = FontWeight.Light
                                        )
                                    )
                                    Text(
                                        "Enjoy the freedom while it lasts 🫠",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = SecondaryGrey.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    items(entriesWithNames) { (entry, subjectName) ->
                        TimetableItem(
                            entry = entry,
                            subjectName = subjectName,
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTimetableDialog(
                viewModel = viewModel,
                selectedDay = selectedDay,
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun TimetableItem(entry: TimetableEntry, subjectName: String, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Indicator
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = entry.startTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(SecondaryGrey.copy(alpha = 0.3f))
                )
                Text(
                    text = entry.endTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryGrey
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Subject Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = OffWhite
                    )
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SecondaryGrey
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entry.isLab) "LAB SESSION" else "REGULAR CLASS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (entry.isLab) LimeGreen else HotMagenta,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // More Options
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = OffWhite)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkGrey)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = BloodRed) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableDialog(
    viewModel: TimetableViewModel,
    selectedDay: Int,
    onDismiss: () -> Unit
) {
    val subjects by viewModel.allSubjects.collectAsState()
    var selectedSubjectId by remember { mutableLongStateOf(0L) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var isLab by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = {
            Text(
                "NEW SUFFERING",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown (Simple version for now)
                Text("Select Subject", color = SecondaryGrey, style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(subjects) { subject ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedSubjectId == subject.id) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedSubjectId = subject.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSubjectId == subject.id,
                                onClick = { selectedSubjectId = subject.id },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Text(subject.name, color = OffWhite)
                        }
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start (HH:mm)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End (HH:mm)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isLab,
                        onCheckedChange = { isLab = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                    )
                    Text("This is a Lab Session", color = OffWhite)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubjectId != 0L) {
                        viewModel.addEntry(selectedSubjectId, selectedDay, startTime, endTime)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PureBlack)
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = HotMagenta)
            }
        }
    )
}
