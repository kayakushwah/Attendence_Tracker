package com.seventyfiveperc.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.data.models.AcademicEvent
import com.seventyfiveperc.app.data.models.AttendanceRecord
import com.seventyfiveperc.app.data.models.AttendanceStatus
import com.seventyfiveperc.app.data.models.EventType
import com.seventyfiveperc.app.data.models.Subject
import com.seventyfiveperc.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var editingEvent by remember { mutableStateOf<AcademicEvent?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "HISTORY", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 2.sp,
                            color = NeonCyan
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PureBlack)
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    containerColor = NeonCyan,
                    contentColor = PureBlack,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Add Event")
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = HotMagenta,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Record")
                }
            }
        },
        containerColor = PureBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            MonthHeader(state.selectedDate) { newDate ->
                viewModel.selectDate(newDate)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CalendarGrid(state) { day ->
                val cal = Calendar.getInstance().apply { 
                    timeInMillis = state.selectedDate
                    set(Calendar.DAY_OF_MONTH, day)
                }
                viewModel.selectDate(cal.timeInMillis)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "RECORDS & EVENTS: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(state.selectedDate))}".uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.academicEvents) { event ->
                    AcademicEventItem(
                        event = event,
                        onDelete = { viewModel.deleteAcademicEvent(event) },
                        onEdit = { editingEvent = event }
                    )
                }

                items(state.attendanceRecords) { record ->
                    val subject = state.subjects.find { it.id == record.subjectId }
                    AttendanceRecordItem(
                        subjectName = subject?.name ?: "Unknown",
                        status = record.status,
                        onDelete = { viewModel.deleteAttendance(record) },
                        onEdit = { editingRecord = record }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAttendanceDialog(
            subjects = state.subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { subjectId, status ->
                viewModel.addAttendance(subjectId, status, state.selectedDate)
                showAddDialog = false
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onConfirm = { name, type, isHoliday ->
                viewModel.addAcademicEvent(name, state.selectedDate, type, isHoliday)
                showAddEventDialog = false
            }
        )
    }

    if (editingEvent != null) {
        EditEventDialog(
            event = editingEvent!!,
            onDismiss = { editingEvent = null },
            onConfirm = { updatedEvent ->
                viewModel.updateAcademicEvent(updatedEvent)
                editingEvent = null
            }
        )
    }

    if (editingRecord != null) {
        EditAttendanceDialog(
            record = editingRecord!!,
            subjects = state.subjects,
            onDismiss = { editingRecord = null },
            onConfirm = { updatedRecord ->
                viewModel.updateAttendance(updatedRecord)
                editingRecord = null
            }
        )
    }
}

@Composable
fun MonthHeader(selectedDate: Long, onDateChange: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            cal.add(Calendar.MONTH, -1)
            onDateChange(cal.timeInMillis)
        }) {
            Icon(Icons.Default.ChevronLeft, "Prev", tint = OffWhite)
        }
        
        Text(
            monthName.uppercase(), 
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = OffWhite,
                letterSpacing = 1.sp
            )
        )

        IconButton(onClick = {
            cal.add(Calendar.MONTH, 1)
            onDateChange(cal.timeInMillis)
        }) {
            Icon(Icons.Default.ChevronRight, "Next", tint = OffWhite)
        }
    }
}

@Composable
fun CalendarGrid(state: CalendarState, onDayClick: (Int) -> Unit) {
    val cal = Calendar.getInstance().apply { 
        timeInMillis = state.selectedDate
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Adjust for Mon start
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val selectedDay = Calendar.getInstance().apply { timeInMillis = state.selectedDate }.get(Calendar.DAY_OF_MONTH)

    val days = (1..daysInMonth).toList()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(300.dp),
        userScrollEnabled = false
    ) {
        items(firstDayOfWeek) { Box(Modifier.size(40.dp)) }
        items(days.size) { index ->
            val day = days[index]
            val records = state.monthRecords[day] ?: emptyList()
            val events = state.monthEvents[day] ?: emptyList()
            val isSelected = day == selectedDay
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else GlassWhite)
                    .border(
                        1.dp, 
                        if (isSelected) NeonCyan else Color.Transparent, 
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onDayClick(day) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        day.toString(), 
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else OffWhite
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                        if (events.isNotEmpty()) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(HotMagenta))
                        }
                        if (records.isNotEmpty()) {
                            records.take(2).forEach {
                                val color = when(it.status) {
                                    AttendanceStatus.PRESENT -> LimeGreen
                                    AttendanceStatus.ABSENT -> BloodRed
                                    else -> SecondaryGrey
                                }
                                Box(Modifier.size(4.dp).clip(CircleShape).background(color))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicEventItem(
    event: AcademicEvent,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, HotMagenta.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).clickable { onEdit() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (event.isHoliday) Icons.Default.BeachAccess else Icons.Default.Event,
                        contentDescription = null,
                        tint = HotMagenta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        event.type.name, 
                        style = MaterialTheme.typography.labelSmall.copy(color = HotMagenta, fontWeight = FontWeight.Bold)
                    )
                }
                Text(event.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = OffWhite))
                if (event.isHoliday) {
                    Text("NO CLASSES 😴", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryGrey))
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = SecondaryGrey, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AttendanceRecordItem(
    subjectName: String,
    status: AttendanceStatus,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlassWhite, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).clickable { onEdit() }) {
                Text(subjectName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = OffWhite))
                Text(
                    status.name, 
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when(status) {
                            AttendanceStatus.PRESENT -> LimeGreen
                            AttendanceStatus.ABSENT -> BloodRed
                            else -> SecondaryGrey
                        }
                    )
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = SecondaryGrey, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AddAttendanceDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (Long, AttendanceStatus) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: -1L) }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("Log Attendance", color = OffWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Subject", color = SecondaryGrey, style = MaterialTheme.typography.labelMedium)
                subjects.forEach { subject ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubjectId = subject.id }
                            .padding(vertical = 4.dp),
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
                
                HorizontalDivider(color = GlassWhite)
                
                Text("Status", color = SecondaryGrey, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceStatus.entries.forEach { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatus = status },
                            label = { Text(status.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(status) {
                                    AttendanceStatus.PRESENT -> LimeGreen.copy(alpha = 0.2f)
                                    AttendanceStatus.ABSENT -> BloodRed.copy(alpha = 0.2f)
                                    else -> NeonCyan.copy(alpha = 0.2f)
                                },
                                selectedLabelColor = when(status) {
                                    AttendanceStatus.PRESENT -> LimeGreen
                                    AttendanceStatus.ABSENT -> BloodRed
                                    else -> NeonCyan
                                }
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedSubjectId != -1L) onConfirm(selectedSubjectId, selectedStatus) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add Record", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryGrey) }
        }
    )
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, EventType, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EventType.EVENT) }
    var isHoliday by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("Add Academic Event", color = OffWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name (e.g. Midterms)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )

                Text("Event Type", color = SecondaryGrey, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { 
                                selectedType = type
                                if (type == EventType.HOLIDAY) isHoliday = true
                            },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isHoliday, 
                        onCheckedChange = { isHoliday = it },
                        colors = CheckboxDefaults.colors(checkedColor = HotMagenta)
                    )
                    Text("Is this a holiday? (No classes)", color = OffWhite)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedType, isHoliday) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add Event", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryGrey) }
        }
    )
}

@Composable
fun EditEventDialog(
    event: AcademicEvent,
    onDismiss: () -> Unit,
    onConfirm: (AcademicEvent) -> Unit
) {
    var name by remember { mutableStateOf(event.name) }
    var selectedType by remember { mutableStateOf(event.type) }
    var isHoliday by remember { mutableStateOf(event.isHoliday) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("Edit Event", color = OffWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassWhite,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )

                Text("Event Type", color = SecondaryGrey, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isHoliday, 
                        onCheckedChange = { isHoliday = it },
                        colors = CheckboxDefaults.colors(checkedColor = HotMagenta)
                    )
                    Text("Is this a holiday?", color = OffWhite)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(event.copy(name = name, type = selectedType, isHoliday = isHoliday)) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Update", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryGrey) }
        }
    )
}

@Composable
fun EditAttendanceDialog(
    record: AttendanceRecord,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceRecord) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(record.status) }
    val subject = subjects.find { it.id == record.subjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGrey,
        title = { Text("Edit: ${subject?.name}", color = OffWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AttendanceStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedStatus = status }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status, 
                            onClick = { selectedStatus = status },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Text(status.name, color = OffWhite)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(record.copy(status = selectedStatus)) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Update", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryGrey) }
        }
    )
}
