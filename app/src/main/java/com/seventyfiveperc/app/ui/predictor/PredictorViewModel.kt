package com.seventyfiveperc.app.ui.predictor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.Subject
import com.seventyfiveperc.app.data.models.AttendanceStatus
import com.seventyfiveperc.app.logic.AttendanceCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.seventyfiveperc.app.data.models.TimetableEntry

data class PredictorState(
    val subjects: List<Subject> = emptyList(),
    val timetable: List<TimetableEntry> = emptyList(),
    val subjectStats: Map<Long, SubjectAttendanceStats> = emptyMap(),
    val overallPrediction: PredictionResult? = null,
    val subjectPredictions: Map<Long, PredictionResult> = emptyMap()
)

data class SubjectAttendanceStats(
    val present: Int,
    val total: Int,
    val weeklyFrequency: Int,
    val totalPlanned: Int = 0
)

data class PredictionResult(
    val finalPercentage: Double,
    val isSafe: Boolean,
    val breakdown: String = "",
    val requiredToReachGoal: Int = 0
)

class PredictorViewModel(private val dao: AttendanceDao) : ViewModel() {
    
    private val _state = MutableStateFlow(PredictorState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.getAllSubjects(),
                dao.getAllAttendance(),
                dao.getAllTimetableEntries()
            ) { subjects, records, timetable ->
                val statsMap = subjects.associate { subject ->
                    val subjectRecords = records.filter { it.subjectId == subject.id }
                    val present = subjectRecords.count { it.status == AttendanceStatus.PRESENT }
                    val total = subjectRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.ABSENT }
                    val freq = timetable.count { it.subjectId == subject.id }
                    subject.id to SubjectAttendanceStats(present, total, freq, subject.totalLecturesPlanned)
                }
                Triple(subjects, timetable, statsMap)
            }.collect { (subjects, timetable, statsMap) ->
                _state.update { it.copy(
                    subjects = subjects,
                    timetable = timetable,
                    subjectStats = statsMap
                ) }
            }
        }
    }

    fun runFullSimulation(weeksRemaining: Int, attendRate: Int, goal: Int) {
        val currentState = _state.value
        var totalPresent = 0
        var totalLectures = 0
        val newSubjectPredictions = mutableMapOf<Long, PredictionResult>()
        
        currentState.subjects.forEach { subject ->
            val stats = currentState.subjectStats[subject.id] ?: return@forEach
            val remainingForSubject = stats.weeklyFrequency * weeksRemaining
            val willAttend = (remainingForSubject * (attendRate / 100.0)).toInt()
            
            val finalPresent = stats.present + willAttend
            val finalTotal = stats.total + remainingForSubject
            
            val finalPerc = if (finalTotal == 0) 0.0 else (finalPresent.toDouble() / finalTotal.toDouble()) * 100.0
            
            totalPresent += finalPresent
            totalLectures += finalTotal

            // Calculate how many they actually NEED to attend out of remaining to be safe
            val targetRatio = subject.minRequiredPercentage / 100.0
            val minNeededPresentTotal = kotlin.math.ceil(targetRatio * finalTotal).toInt()
            val neededFromRemaining = (minNeededPresentTotal - stats.present).coerceIn(0, remainingForSubject)

            newSubjectPredictions[subject.id] = PredictionResult(
                finalPercentage = finalPerc,
                isSafe = finalPerc >= subject.minRequiredPercentage,
                breakdown = "Attend at least $neededFromRemaining/$remainingForSubject remaining classes.",
                requiredToReachGoal = neededFromRemaining
            )
        }
        
        val overallPerc = if (totalLectures == 0) 0.0 else (totalPresent.toDouble() / totalLectures.toDouble()) * 100.0
        
        _state.update { it.copy(
            overallPrediction = PredictionResult(
                finalPercentage = overallPerc,
                isSafe = overallPerc >= goal,
                breakdown = "Overall vibe: ${if (overallPerc >= goal) "Slaying" else "Cooked"}."
            ),
            subjectPredictions = newSubjectPredictions
        ) }
    }

    fun predictForSubject(subjectId: Long, weeksRemaining: Int, attendRate: Int) {
        val stats = _state.value.subjectStats[subjectId] ?: return
        val subject = _state.value.subjects.find { it.id == subjectId } ?: return
        
        val remainingForSubject = stats.weeklyFrequency * weeksRemaining
        val willAttend = (remainingForSubject * (attendRate / 100.0)).toInt()
        
        val finalPresent = stats.present + willAttend
        val finalTotal = stats.total + remainingForSubject
        
        val finalPerc = if (finalTotal == 0) 0.0 else (finalPresent.toDouble() / finalTotal.toDouble()) * 100.0
        
        val newPredictions = _state.value.subjectPredictions.toMutableMap()
        newPredictions[subjectId] = PredictionResult(
            finalPercentage = finalPerc,
            isSafe = finalPerc >= subject.minRequiredPercentage,
            breakdown = "Predicted based on your current habits."
        )
        
        _state.update { it.copy(subjectPredictions = newPredictions) }
    }
}
