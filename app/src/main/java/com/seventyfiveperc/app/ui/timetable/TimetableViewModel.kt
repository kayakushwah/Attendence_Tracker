package com.seventyfiveperc.app.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.TimetableEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class TimetableViewModel(private val dao: AttendanceDao) : ViewModel() {

    val allSubjects = dao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTimetableForDay(day: Int): StateFlow<List<TimetableEntry>> {
        return dao.getTimetableForDay(day)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getTimetableWithSubjectsForDay(day: Int): StateFlow<List<Pair<TimetableEntry, String>>> {
        return dao.getTimetableForDay(day)
            .combine(dao.getAllSubjects()) { entries, subjects ->
                entries.map { entry ->
                    val subjectName = subjects.find { it.id == entry.subjectId }?.name ?: "Unknown"
                    entry to subjectName
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addEntry(subjectId: Long, day: Int, start: String, end: String, isLab: Boolean = false) {
        viewModelScope.launch {
            dao.insertTimetableEntry(
                TimetableEntry(
                    subjectId = subjectId,
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end,
                    isLab = isLab
                )
            )
        }
    }

    fun updateEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            dao.updateTimetableEntry(entry)
        }
    }

    fun deleteEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            dao.deleteTimetableEntry(entry)
        }
    }
}
