package com.seventyfiveperc.app.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.logic.AssistantEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageRes: Int? = null
)

class AssistantViewModel(private val dao: AttendanceDao) : ViewModel() {

    private val assistant = AssistantEngine()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Yo! I'm AURA, your attendance plug. Ask me if you're cooked or if we're dipping today. 💅✨", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    fun sendMessage(query: String) {
        viewModelScope.launch {
            _messages.value += ChatMessage(query, true)
            _isTyping.value = true
            
            // Simulate some delay for AI feel
            kotlinx.coroutines.delay(1000)

            // Get data for reasoning
            val subjects = dao.getAllSubjects().first()
            val attendance = dao.getAllAttendance().first().groupBy { it.subjectId }
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val timetable = dao.getTimetableForDay(dayOfWeek).first()
            val academicEvents = dao.getAllAcademicEvents().first()
            
            val result = assistant.getDecisionWithContext(query, subjects, attendance, timetable, academicEvents)
            _isTyping.value = false
            _messages.value += ChatMessage(result.text, false, result.imageRes)
        }
    }
}
