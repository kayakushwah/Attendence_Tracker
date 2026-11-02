package com.seventyfiveperc.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.AttendanceRecord
import com.seventyfiveperc.app.data.models.Subject
import com.seventyfiveperc.app.logic.AssistantEngine
import com.seventyfiveperc.app.logic.AttendanceCalculator
import com.seventyfiveperc.app.logic.AttendanceStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.util.Calendar

data class DashboardState(
    val subjects: List<Subject> = emptyList(),
    val overallAttendance: Double = 0.0,
    val subjectStats: Map<Long, AttendanceStats> = emptyMap(),
    val aiSuggestion: String = "Hello! I'm calculating your academic health...",
    val aiSuggestionImage: Int = com.seventyfiveperc.app.R.drawable.aura_1,
    val todayAgenda: List<AgendaItem> = emptyList(),
    val auraScore: Int = 0
)

data class AgendaItem(
    val subject: Subject,
    val entry: com.seventyfiveperc.app.data.models.TimetableEntry,
    val isMarked: Boolean = false
)

class DashboardViewModel(private val dao: AttendanceDao) : ViewModel() {

    private val assistant = AssistantEngine()
    
    val state: StateFlow<DashboardState> = combine(
        dao.getAllSubjects(),
        dao.getAllAttendance(),
        dao.getAllTimetableEntries(),
        dao.getAllAcademicEvents()
    ) { subjects, attendance, allTimetable, events ->
        val currentDay = getCurrentDayOfWeek()
        val todayTimetable = allTimetable.filter { it.dayOfWeek == currentDay }
        
        val statsMap = subjects.associate { sub ->
            sub.id to AttendanceCalculator.calculateStats(
                attendance.filter { it.subjectId == sub.id },
                sub.minRequiredPercentage
            )
        }
        
        val overall = if (statsMap.isEmpty()) 0.0 else statsMap.values.map { it.percentage }.average()
        
        // Aura Score: Weighted by attendance but with a "vibe" multiplier
        val auraScore = (overall * 10).toInt().coerceIn(0, 1000)
        
        val agenda = todayTimetable.mapNotNull { entry ->
            val sub = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val marked = attendance.any { 
                it.subjectId == sub.id && it.date >= today 
            }
            AgendaItem(sub, entry, marked)
        }
        
        val suggestionResponse = assistant.getDecisionWithContext("bunk today", subjects, attendance.groupBy { it.subjectId }, allTimetable, events)
        
        DashboardState(
            subjects = subjects,
            overallAttendance = overall,
            subjectStats = statsMap,
            aiSuggestion = suggestionResponse.text,
            aiSuggestionImage = suggestionResponse.imageRes ?: com.seventyfiveperc.app.R.drawable.aura_1,
            todayAgenda = agenda,
            auraScore = auraScore
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    private fun getCurrentDayOfWeek(): Int {
        // AttendanceDao/Room expects Calendar.SUNDAY(1) to Calendar.SATURDAY(7)
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    fun markAttendance(subjectId: Long, present: Boolean) {
        viewModelScope.launch {
            dao.insertAttendance(
                AttendanceRecord(
                    subjectId = subjectId,
                    date = System.currentTimeMillis(),
                    status = if (present) com.seventyfiveperc.app.data.models.AttendanceStatus.PRESENT else com.seventyfiveperc.app.data.models.AttendanceStatus.ABSENT
                )
            )
        }
    }

    fun addSubject(name: String, goal: Int) {
        viewModelScope.launch {
            dao.insertSubject(
                Subject(
                    name = name,
                    minRequiredPercentage = goal
                )
            )
        }
    }
}
