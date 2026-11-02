package com.seventyfiveperc.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class CalendarState(
    val selectedDate: Long = System.currentTimeMillis(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val academicEvents: List<AcademicEvent> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val monthRecords: Map<Int, List<AttendanceRecord>> = emptyMap(), // Day of month -> records
    val monthEvents: Map<Int, List<AcademicEvent>> = emptyMap()
)

class CalendarViewModel(private val dao: AttendanceDao) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    
    val state: StateFlow<CalendarState> = combine(
        _selectedDate,
        dao.getAllSubjects(),
        dao.getAllAttendance(),
        dao.getAllAcademicEvents()
    ) { selectedDate, subjects, allAttendance, allEvents ->
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val dayRecords = allAttendance.filter { it.date in startOfDay..endOfDay }
        val dayEvents = allEvents.filter { it.date in startOfDay..endOfDay }
        
        // Month records for the calendar view
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        
        val monthEnd = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis

        val monthAttendance = allAttendance.filter { it.date in monthStart..monthEnd }
            .groupBy { 
                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
            }
        
        val monthEventsMap = allEvents.filter { it.date in monthStart..monthEnd }
            .groupBy {
                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
            }

        CalendarState(
            selectedDate = selectedDate,
            attendanceRecords = dayRecords,
            academicEvents = dayEvents,
            subjects = subjects,
            monthRecords = monthAttendance,
            monthEvents = monthEventsMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState())

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    fun updateAttendance(record: AttendanceRecord) {
        viewModelScope.launch {
            dao.updateAttendance(record)
        }
    }

    fun deleteAttendance(record: AttendanceRecord) {
        viewModelScope.launch {
            dao.deleteAttendance(record)
        }
    }

    fun addAttendance(subjectId: Long, status: AttendanceStatus, date: Long) {
        viewModelScope.launch {
            dao.insertAttendance(AttendanceRecord(
                subjectId = subjectId,
                status = status,
                date = date
            ))
        }
    }

    fun addAcademicEvent(name: String, date: Long, type: EventType, isHoliday: Boolean) {
        viewModelScope.launch {
            dao.insertAcademicEvent(AcademicEvent(
                name = name,
                date = date,
                type = type,
                isHoliday = isHoliday
            ))
        }
    }

    fun updateAcademicEvent(event: AcademicEvent) {
        viewModelScope.launch {
            dao.updateAcademicEvent(event)
        }
    }

    fun deleteAcademicEvent(event: AcademicEvent) {
        viewModelScope.launch {
            dao.deleteAcademicEvent(event)
        }
    }
}
