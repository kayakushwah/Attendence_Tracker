package com.seventyfiveperc.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.ui.dashboard.DashboardViewModel
import com.seventyfiveperc.app.ui.timetable.TimetableViewModel
import com.seventyfiveperc.app.ui.assistant.AssistantViewModel
import com.seventyfiveperc.app.ui.ocr.OcrViewModel
import com.seventyfiveperc.app.ui.predictor.PredictorViewModel
import com.seventyfiveperc.app.ui.profile.ProfileViewModel
import com.seventyfiveperc.app.ui.calendar.CalendarViewModel

class ViewModelFactory(private val dao: AttendanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssistantViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(OcrViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OcrViewModel(dao) as T
        }
        if (modelClass.isAssignableFrom(PredictorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PredictorViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
