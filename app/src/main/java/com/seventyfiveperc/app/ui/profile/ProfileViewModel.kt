package com.seventyfiveperc.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.Subject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val dao: AttendanceDao) : ViewModel() {

    val subjects = dao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            dao.deleteSubject(subject)
        }
    }

    fun addSubject(name: String, goal: Int) {
        viewModelScope.launch {
            dao.insertSubject(Subject(name = name, minRequiredPercentage = goal))
        }
    }
}
