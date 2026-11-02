package com.seventyfiveperc.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subjectCode: String = "",
    val teacherName: String = "",
    val minRequiredPercentage: Int = 75,
    val colorHex: String = "#6200EE",
    val totalLecturesPlanned: Int = 0
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val date: Long, // Timestamp
    val status: AttendanceStatus,
    val reason: String? = null,
    val isLecture: Boolean = true, // false for lab
    val durationHours: Double = 1.0
)

enum class AttendanceStatus {
    PRESENT, ABSENT, CANCELLED, HOLIDAY
}

@Entity(tableName = "academic_events")
data class AcademicEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: Long, // Timestamp
    val type: EventType,
    val description: String? = null,
    val isHoliday: Boolean = false
)

enum class EventType {
    HOLIDAY, EXAM, EVENT
}

@Entity(tableName = "timetable")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // Use java.util.Calendar constants (e.g., Calendar.MONDAY = 2)
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val isLab: Boolean = false
)
