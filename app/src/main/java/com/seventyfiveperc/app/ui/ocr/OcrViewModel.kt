package com.seventyfiveperc.app.ui.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.seventyfiveperc.app.data.local.AttendanceDao
import com.seventyfiveperc.app.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.math.abs

enum class OcrType {
    TIMETABLE, CALENDAR, GENERIC
}

class OcrViewModel(private val dao: AttendanceDao) : ViewModel() {
    private val legendMap = mutableMapOf<String, String>()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    
    private val _syncSummary = MutableStateFlow<SyncSummary?>(null)
    val syncSummary = _syncSummary.asStateFlow()

    private val _pendingData = MutableStateFlow<PendingSyncData?>(null)
    val pendingData = _pendingData.asStateFlow()

    private val _rawText = MutableStateFlow<String?>(null)
    val rawText = _rawText.asStateFlow()

    data class SyncSummary(
        val subjectsAdded: Int = 0,
        val entriesAdded: Int = 0,
        val eventsAdded: Int = 0
    )

    data class PendingSyncData(
        val subjects: List<Subject> = emptyList(),
        val timetableEntries: List<TimetableEntry> = emptyList(),
        val academicEvents: List<AcademicEvent> = emptyList()
    )

    private val dayFuzzyMap = mapOf(
        Calendar.MONDAY to listOf("MON", "MONDAY", "M0N", "MOND", "MO"),
        Calendar.TUESDAY to listOf("TUE", "TUESDAY", "TUES", "TUE5", "TU"),
        Calendar.WEDNESDAY to listOf("WED", "WEDNESDAY", "WEDN", "WE"),
        Calendar.THURSDAY to listOf("THU", "THURSDAY", "THUR", "THUЯ", "TH"),
        Calendar.FRIDAY to listOf("FRI", "FRIDAY", "FR1", "FR"),
        Calendar.SATURDAY to listOf("SAT", "SATURDAY", "5AT", "SA"),
        Calendar.SUNDAY to listOf("SUN", "SUNDAY", "5UN", "SU")
    )

    fun processImage(context: Context, uri: Uri, type: OcrType = OcrType.GENERIC) {
        viewModelScope.launch {
            _isProcessing.value = true
            _message.value = null
            _rawText.value = null
            try {
                Log.d("OcrViewModel", "Processing image URI: $uri")
                
                // Use InputImage.fromFilePath to automatically handle EXIF orientation
                val image = try {
                    InputImage.fromFilePath(context, uri)
                } catch (e: Exception) {
                    Log.e("OcrViewModel", "Failed to create InputImage from URI", e)
                    null
                }
                
                if (image == null) {
                    Log.e("OcrViewModel", "InputImage is null")
                    _message.value = "Sync Failed: Could not load image. 💀"
                    return@launch
                }

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(image).await()
                
                _rawText.value = result.text
                Log.d("OcrViewModel", "OCR Result text length: ${result.text.length}")
                Log.d("OcrViewModel", "Found ${result.textBlocks.size} text blocks")
                
                result.textBlocks.forEachIndexed { index, block ->
                    Log.d("OcrViewModel", "Block $index: ${block.text}")
                    Log.d("OcrViewModel", "Block $index Bounds: ${block.boundingBox}")
                }

                if (result.text.isNotBlank()) {
                    Log.d("OcrViewModel", "Text preview: ${result.text.take(500)}")
                }
                
                legendMap.clear()
                extractLegend(result)
                
                // If no text, try a simpler fallback for low-light images
                if (result.text.isBlank()) {
                    Log.w("OcrViewModel", "No text detected in image. Trying with sharp/high-contrast (implied by ML Kit automatically)")
                    _message.value = "Sync Failed: No text detected. Check clarity! 💀"
                    return@launch
                }

                val text = result.text.uppercase()
                val hasDays = text.contains(Regex("MON|TUE|WED|THU|FRI|SAT|SUN|MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY|MO|TU|WE|TH|FR|SA"))
                val hasDates = text.contains(Regex("""\d{1,2}[/-]\d{1,2}""")) || 
                              text.contains(Regex("JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC", RegexOption.IGNORE_CASE))

                var syncCount = 0
                
                if (type == OcrType.TIMETABLE || (type == OcrType.GENERIC && hasDays)) {
                    syncCount = parseTimetableSpatial(result)
                    if (syncCount == 0) syncCount = parseListFormat(result)
                } 
                
                if (syncCount == 0 && (type == OcrType.CALENDAR || (type == OcrType.GENERIC && hasDates))) {
                    syncCount = parseCalendarSpatial(result)
                }

                // Global Fallbacks
                if (syncCount == 0) syncCount = parseSimpleRegex(result)
                if (syncCount == 0) syncCount = parseListFormat(result)
                if (syncCount == 0 && type == OcrType.GENERIC) syncCount = parseRawTextFallback(result)
                
                if (syncCount > 0) {
                    _message.value = "Extraction Complete! Review items to sync. 🚀"
                } else {
                    Log.d("OcrViewModel", "Parsing yielded 0 results despite text length ${result.text.length}")
                    _message.value = "Extraction Failed: Text found but grid was unclear. Check History. 💀"
                }
            } catch (e: Exception) {
                Log.e("OcrViewModel", "OCR Error", e)
                _message.value = "Error: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun applyPendingSync() {
        viewModelScope.launch {
            val data = _pendingData.value ?: return@launch
            _isProcessing.value = true
            
            var subjectsAdded = 0
            var entriesAdded = 0
            var eventsAdded = 0

            // 1. Sync Subjects and track new IDs
            val existingSubjects = dao.getAllSubjects().first()
            val idMap = mutableMapOf<Long, Long>() // Old ID to New DB ID

            data.subjects.forEach { subject ->
                val existing = existingSubjects.find { 
                    it.name.equals(subject.name, true) || 
                    (subject.subjectCode.isNotEmpty() && it.subjectCode.equals(subject.subjectCode, true))
                }
                
                if (existing == null) {
                    val newId = dao.insertSubject(subject.copy(id = 0))
                    idMap[subject.id] = newId
                    subjectsAdded++
                } else {
                    idMap[subject.id] = existing.id
                }
            }

            // 2. Sync Timetable
            val existingTimetable = dao.getAllTimetableEntries().first()
            data.timetableEntries.forEach { entry ->
                val newSubId = idMap[entry.subjectId] ?: entry.subjectId
                val isDuplicate = existingTimetable.any { 
                    it.dayOfWeek == entry.dayOfWeek && it.startTime == entry.startTime && it.subjectId == newSubId 
                }
                if (!isDuplicate) {
                    dao.insertTimetableEntry(entry.copy(id = 0, subjectId = newSubId))
                    entriesAdded++
                }
            }

            // 3. Sync Events
            val existingEvents = dao.getAllAcademicEvents().first()
            data.academicEvents.forEach { event ->
                val isDuplicate = existingEvents.any { 
                    it.date == event.date && it.name.equals(event.name, true)
                }
                if (!isDuplicate) {
                    dao.insertAcademicEvent(event.copy(id = 0))
                    eventsAdded++
                }
            }

            _syncSummary.value = SyncSummary(subjectsAdded, entriesAdded, eventsAdded)
            _message.value = "Sync Successful! Added $subjectsAdded subjects, $entriesAdded classes, $eventsAdded events. 🚀"
            _pendingData.value = null
            _isProcessing.value = false
        }
    }

    fun discardPending() {
        _pendingData.value = null
        _message.value = "Changes discarded."
    }

    private fun addPendingSubject(subject: Subject): Long {
        val current = _pendingData.value ?: PendingSyncData()
        val existing = current.subjects.find { 
            it.name.equals(subject.name, true) || 
            (subject.subjectCode.isNotEmpty() && it.subjectCode.equals(subject.subjectCode, true))
        }
        
        if (existing != null) return existing.id
        
        // Use a negative temporary ID to distinguish from real DB IDs
        val tempId = -(current.subjects.size.toLong() + 1)
        val newSubject = subject.copy(id = tempId)
        _pendingData.value = current.copy(subjects = current.subjects + newSubject)
        return tempId
    }

    private fun addPendingTimetableEntry(entry: TimetableEntry) {
        val current = _pendingData.value ?: PendingSyncData()
        _pendingData.value = current.copy(timetableEntries = current.timetableEntries + entry)
    }

    private fun addPendingAcademicEvent(event: AcademicEvent) {
        val current = _pendingData.value ?: PendingSyncData()
        _pendingData.value = current.copy(academicEvents = current.academicEvents + event)
    }

    private suspend fun parseTimetableSpatial(result: Text): Int {
        val timeRegex = Regex("""(\d{1,2}[:.][0-5]\d(?:\s*-\s*\d{1,2}[:.][0-5]\d)?(?:\s*[AP]M)?)""", RegexOption.IGNORE_CASE)
        val dayCoords = mutableMapOf<Int, Pair<Int, Int>>() // dayCode to (x, y)
        val timeCoords = mutableMapOf<String, Pair<Int, Int>>() // time to (x, y)
        val elements = mutableListOf<TextElementInfo>()
        
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text.uppercase().replace(" ", "")
                val bounds = line.boundingBox ?: return@forEach
                
                var matchedDay = false
                for ((dayCode, patterns) in dayFuzzyMap) {
                    if (patterns.any { text == it || (text.length > 2 && text.startsWith(it)) }) {
                        dayCoords[dayCode] = bounds.centerX() to bounds.centerY()
                        matchedDay = true
                        break
                    }
                }
                
                if (!matchedDay) {
                    timeRegex.find(line.text.uppercase())?.let { match ->
                        timeCoords[normalizeTime(match.value)] = bounds.centerX() to bounds.centerY()
                    }
                }
                
                val lb = line.boundingBox ?: return@forEach
                elements.add(TextElementInfo(line.text, lb.centerX(), lb.centerY(), lb.height()))
            }
        }

        if (dayCoords.isEmpty()) return 0

        val dayXVar = if (dayCoords.size > 1) dayCoords.values.map { it.first }.let { it.maxOrNull()!! - it.minOrNull()!! } else 0
        val dayYVar = if (dayCoords.size > 1) dayCoords.values.map { it.second }.let { it.maxOrNull()!! - it.minOrNull()!! } else 0
        val daysAreRows = dayYVar > dayXVar

        var itemsAdded = 0
        val cells = elements.groupBy { el ->
            val day = if (daysAreRows) {
                dayCoords.minByOrNull { abs(it.value.second - el.y) }?.key
            } else {
                dayCoords.minByOrNull { abs(it.value.first - el.x) }?.key
            }
            
            val time = if (timeCoords.isNotEmpty()) {
                if (daysAreRows) {
                    timeCoords.minByOrNull { abs(it.value.first - el.x) }?.key
                } else {
                    timeCoords.minByOrNull { abs(it.value.second - el.y) }?.key
                }
            } else null
            
            day to time
        }

        cells.forEach { (key, cellElements) ->
            val (dayCode, startTime) = key
            if (dayCode != null) {
                // Join lines with newline to handle multi-subject slots
                val cellText = cellElements.sortedBy { it.y }
                    .joinToString("\n") { it.text }
                
                if (cellText.length > 2 && !isJustMetadata(cellText)) {
                    if (syncTimetableEntry(cellText, dayCode, startTime ?: "09:00")) itemsAdded++
                }
            }
        }
        return itemsAdded
    }

    private fun normalizeTime(time: String): String {
        val upper = time.uppercase().replace(".", ":").replace(" ", "")
        val isPM = upper.contains("PM")
        val isAM = upper.contains("AM")
        val base = upper.replace("AM", "").replace("PM", "")
        
        // Take start time if a range is provided (e.g. 09:00-10:00)
        val startTimeStr = base.split("-").first().trim()
        val parts = startTimeStr.split(":")
        var hour = parts[0].toIntOrNull() ?: 9
        var minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        if (isPM && hour < 12) hour += 12
        if (isAM && hour == 12) hour = 0
        
        // Snap to nearest 5 minutes for clean UI
        minute = ((minute + 2) / 5) * 5
        if (minute >= 60) {
            minute = 0
            hour = (hour + 1) % 24
        }
        
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private suspend fun parseCalendarSpatial(result: Text): Int {
        val monthNames = "JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC"
        val monthRegex = Regex("""($monthNames)\s+(\d{4})""", RegexOption.IGNORE_CASE)
        val monthHeaders = mutableListOf<Triple<Int, IntRange, Int>>() // monthIndex, X-range, Year
        val elements = mutableListOf<TextElementInfo>()

        // 1. Detect Month Columns
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text.uppercase()
                val bounds = line.boundingBox ?: return@forEach
                monthRegex.find(text)?.let { match ->
                    val mIndex = getMonthIndex(match.groupValues[1])
                    val year = match.groupValues[2].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    monthHeaders.add(Triple(mIndex, bounds.left..bounds.right, year))
                }
                line.elements.forEach { el ->
                    val eb = el.boundingBox ?: return@forEach
                    elements.add(TextElementInfo(el.text, eb.centerX(), eb.centerY(), eb.height()))
                }
            }
        }

        if (monthHeaders.isEmpty()) return parseCalendarLineBased(result)

        // 2. Define Column Boundaries
        val sortedHeaders = monthHeaders.sortedBy { it.second.first }
        val columns = sortedHeaders.mapIndexed { i, header ->
            val left = if (i == 0) 0 else (header.second.first + sortedHeaders[i-1].second.last) / 2
            val right = if (i == sortedHeaders.size - 1) 10000 else (header.second.last + sortedHeaders[i+1].second.first) / 2
            header.first to (left..right) to header.third
        }

        // 3. Group by Rows and Parse Cells
        var added = 0
        val avgHeight = if (elements.isNotEmpty()) elements.map { it.h }.average().toInt() else 25
        val rowThreshold = (avgHeight * 1.2).toInt().coerceAtLeast(15) 

        elements.groupBy { it.y / rowThreshold }.values.forEach { rowEls ->
            columns.forEach { (meta, year) ->
                val (mIndex, xRange) = meta
                val colEls = rowEls.filter { it.x in xRange }.sortedBy { it.x }
                
                if (colEls.isNotEmpty()) {
                    val cellText = colEls.joinToString(" ") { it.text }
                    // Look for day number (1-31)
                    val dayMatch = Regex("""\b([1-9]|[12]\d|3[01])\b""").find(cellText)
                    if (dayMatch != null) {
                        val day = dayMatch.groupValues[1].toInt()
                        val reason = cellText.replace(dayMatch.value, "").trim()
                        
                        if (reason.length > 2 && !isJustMetadata(reason)) {
                            saveAcademicEvent(createTimestamp(day, mIndex, year), reason, cellText)
                            added++
                        }
                    }
                }
            }
        }
        return if (added > 0) added else parseCalendarLineBased(result)
    }

    private suspend fun parseCalendarLineBased(result: Text): Int {
        val monthNames = "JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC"
        val dateRegex = Regex("""(\d{1,2})(?:st|nd|rd|th)?[/-](\d{1,2})(?:st|nd|rd|th)?([/-](\d{2,4}))?""")
        val rangeRegex = Regex("""(\d{1,2})(?:st|nd|rd|th)?\s*[-TO]+\s*(\d{1,2})(?:st|nd|rd|th)?\s*($monthNames)""", RegexOption.IGNORE_CASE)
        val rangeRegexAlt = Regex("""(\d{1,2})(?:st|nd|rd|th)?\s*($monthNames)\s*[-TO]+\s*(\d{1,2})(?:st|nd|rd|th)?\s*($monthNames)""", RegexOption.IGNORE_CASE)
        val rangeRegexMonthFirst = Regex("""($monthNames)\s+(\d{1,2})(?:st|nd|rd|th)?\s*[-TO]+\s*(\d{1,2})(?:st|nd|rd|th)?""", RegexOption.IGNORE_CASE)
        
        var eventsAdded = 0
        result.textBlocks.forEach { block ->
            val lines = block.lines
            lines.forEachIndexed { index, line ->
                val lineText = line.text.trim()
                
                rangeRegex.find(lineText)?.let { match ->
                    val startDay = match.groupValues[1].toInt()
                    val endDay = match.groupValues[2].toInt()
                    val month = getMonthIndex(match.groupValues[3])
                    val reason = extractReason(lineText, match.value, lines, index)
                    for (d in startDay..endDay) {
                        saveAcademicEvent(createTimestamp(d, month), reason, block.text)
                        eventsAdded++
                    }
                } ?: rangeRegexMonthFirst.find(lineText)?.let { match ->
                    val month = getMonthIndex(match.groupValues[1])
                    val startDay = match.groupValues[2].toInt()
                    val endDay = match.groupValues[3].toInt()
                    val reason = extractReason(lineText, match.value, lines, index)
                    for (d in startDay..endDay) {
                        saveAcademicEvent(createTimestamp(d, month), reason, block.text)
                        eventsAdded++
                    }
                } ?: rangeRegexAlt.find(lineText)?.let { match ->
                    val startDay = match.groupValues[1].toInt()
                    val startMonth = getMonthIndex(match.groupValues[2])
                    val endDay = match.groupValues[3].toInt()
                    val endMonth = getMonthIndex(match.groupValues[4])
                    val reason = extractReason(lineText, match.value, lines, index)
                    if (startMonth == endMonth) {
                        for (d in startDay..endDay) {
                            saveAcademicEvent(createTimestamp(d, startMonth), reason, block.text)
                            eventsAdded++
                        }
                    } else {
                        saveAcademicEvent(createTimestamp(startDay, startMonth), reason, block.text)
                        saveAcademicEvent(createTimestamp(endDay, endMonth), reason, block.text)
                        eventsAdded += 2
                    }
                } ?: dateRegex.find(lineText)?.let { match ->
                    val day = match.groupValues[1].toInt()
                    val month = match.groupValues[2].toInt() - 1
                    val year = match.groupValues[4].let { 
                        if (it.isEmpty()) Calendar.getInstance().get(Calendar.YEAR)
                        else if (it.length == 2) 2000 + it.toInt() else it.toInt()
                    }
                    saveAcademicEvent(createTimestamp(day, month, year), lineText.replace(match.value, "").trim(), block.text)
                    eventsAdded++
                }
            }
        }
        return eventsAdded
    }

    private fun extractReason(lineText: String, matchValue: String, lines: List<Text.Line>, currentIndex: Int): String {
        var reason = lineText.replace(matchValue, "").trim()
        if (reason.length < 3 && currentIndex + 1 < lines.size) {
            val nextLine = lines[currentIndex + 1].text.trim()
            if (!isJustMetadata(nextLine)) reason = nextLine
        }
        return reason
    }

    private fun createTimestamp(day: Int, month: Int, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
        return Calendar.getInstance().apply { 
            set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) 
        }.timeInMillis
    }

    private suspend fun parseSimpleRegex(result: Text): Int {
        val dayPattern = Regex("(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY|MON|TUE|WED|THU|FRI|SAT|SUN|MO|TU|WE|TH|FR|SA|SU)", RegexOption.IGNORE_CASE)
        var added = 0
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text.trim()
                dayPattern.find(text)?.let { match ->
                    val dayCode = getDayCode(match.value)
                    if (dayCode != -1) {
                        val remaining = text.replace(match.value, "").replace(Regex("[:\\-]"), "").trim()
                        if (syncTimetableEntry(remaining, dayCode, "09:00")) added++
                    }
                }
            }
        }
        return added
    }

    private suspend fun parseListFormat(result: Text): Int {
        var currentDay = -1
        var added = 0
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text.trim()
                val dayCode = getDayCode(text)
                if (dayCode != -1) {
                    currentDay = dayCode
                } else if (currentDay != -1 && text.length > 3 && !isJustMetadata(text)) {
                    if (syncTimetableEntry(text, currentDay, "09:00")) added++
                }
            }
        }
        return added
    }

    private suspend fun parseRawTextFallback(result: Text): Int {
        var added = 0
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text.trim()
                if (text.length in 4..40 && !text.contains(Regex("\\d{2,}:\\d{2}")) && !isJustMetadata(text) && text.any { it.isLetter() }) {
                    if (syncTimetableEntry(text, Calendar.MONDAY, "09:00")) added++
                }
            }
        }
        return added
    }

    private fun getDayCode(text: String): Int {
        val upper = text.uppercase().trim()
        for ((code, patterns) in dayFuzzyMap) {
            if (patterns.any { upper == it || (upper.length > 2 && upper.startsWith(it)) }) return code
        }
        return -1
    }

    private suspend fun syncTimetableEntry(name: String, day: Int, start: String): Boolean {
        // Split by common separators and newlines for multiple subjects (e.g. "Maths / Physics" or multi-line)
        val potentialSubjects = name.split(Regex("""[/|&+\n]|\bOR\b""", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.length > 2 && !isJustMetadata(it) }
            
        var anyAdded = false
        potentialSubjects.forEach { rawPart ->
            if (extractAndAddPendingSubject(rawPart, day, start)) anyAdded = true
        }
        return anyAdded
    }

    private suspend fun extractAndAddPendingSubject(name: String, day: Int, start: String): Boolean {
        val originalName = name.trim()
        val isLab = originalName.contains(Regex("LAB|PRACTICAL|TUTORIAL|PRAC|TUT", RegexOption.IGNORE_CASE))
        
        // 1. Extract Teacher Name if present
        val teacherMatch = Regex("""(PROF\.?|DR\.?)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)""", RegexOption.IGNORE_CASE).find(originalName)
        val teacherName = teacherMatch?.groupValues?.get(2) ?: ""

        // 2. Advanced cleaning for ERP noise
        var cleanName = originalName.replace(Regex("""\d{1,2}[:.]\d{2}.*"""), "") // Times
            .replace(Regex("""\(L-T-P:.*\)""", RegexOption.IGNORE_CASE), "") // Credits
            .replace(Regex("""(ROOM|RM|BLK|FL|SEC|PROF|DR\.|VENUE|LEC|TUT|LAB|BATCH|ATTENDANCE|CREDITS|FACULTY|LECTURE|THEORY|PRACTICAL|COURSE|CODE|SEM|SL\.?\s?NO)\s*[:\-]?\s*.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b\d[A-Z\d]{2,}\b"""), "") // Batch info
            .replace(Regex("""\b[A-Z]{1,2}\d{2,}\b"""), "") // Room info
            .replace(Regex("""\b[A-Z]{1,4}[-\s]?\d{2,}[A-Z\d]*\b"""), "") // Course codes
            .replace(Regex("""[()\[\]+{}\-:_]""", RegexOption.IGNORE_CASE), " ").trim()

        // 3. Apply Legend Mapping
        val words = cleanName.split(Regex("""[\s/,\-]+""")).filter { it.isNotBlank() }
        var foundCode = ""
        for (word in words) {
            val upperWord = word.uppercase().trim()
            if (legendMap.containsKey(upperWord)) {
                cleanName = legendMap[upperWord]!!
                foundCode = upperWord
                break
            }
        }
        
        cleanName = cleanName.replace(Regex("""\s+"""), " ").trim()
        if (cleanName.length < 3 || isJustMetadata(cleanName)) return false

        // 4. Create Pending Subject and Entry
        val tempId = addPendingSubject(Subject(
            name = cleanName, 
            subjectCode = foundCode,
            teacherName = teacherName,
            colorHex = generateSubjectColor(cleanName)
        ))

        addPendingTimetableEntry(TimetableEntry(
            subjectId = tempId,
            dayOfWeek = day,
            startTime = start,
            endTime = estimateEndTime(start, isLab),
            isLab = isLab
        ))
        
        return true
    }

    private fun generateSubjectColor(name: String): String {
        val colors = listOf("#6200EE", "#03DAC6", "#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#E91E63")
        return colors[abs(name.hashCode()) % colors.size]
    }

    private suspend fun saveAcademicEvent(timestamp: Long, reason: String, originalText: String) {
        // Clean event name: remove leading/trailing date info and separators
        val cleanReason = reason.replace(Regex("""^\d{1,2}(?:st|nd|rd|th)?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\d{1,2}(?:st|nd|rd|th)?$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^[.\-:\s]+"""), "")
            .trim()
            
        if (cleanReason.length < 2 || isJustMetadata(cleanReason)) return

        val textUpper = "$cleanReason $originalText".uppercase()
        val type = when {
            textUpper.contains(Regex("HOLIDAY|OFF|VACATION|BREAK|LEAVE|NO CLASS|RECES|FESTIVAL|DIWALI|EID|CHRISTMAS|PONGAL|HOLI|INDEPENDENCE|REPUBLIC|GANDHI|JAYANTI")) -> EventType.HOLIDAY
            textUpper.contains(Regex("EXAM|TEST|QUIZ|MIDTERM|FINALS|ASSESSMENT|MSE|ESE|CAT|INTERNAL|EXTERNAL|PRACTICAL|VIVA|SESSIONAL")) -> EventType.EXAM
            textUpper.contains(Regex("WORKSHOP|SEMINAR|WEBINAR|CONFERENCE|FEST|EVENT|CULTURAL|SPORTS|MEET|CELEBRATION|PLACEMENT|DRIVE")) -> EventType.EVENT
            else -> EventType.EVENT
        }
        
        addPendingAcademicEvent(AcademicEvent(
            name = cleanReason,
            date = timestamp,
            type = type,
            isHoliday = type == EventType.HOLIDAY,
            description = originalText.take(500)
        ))
    }

    private fun isJustMetadata(text: String): Boolean {
        val upper = text.uppercase().trim()
        val metadataKeywords = listOf(
            "ROOM", "FLOOR", "BLOCK", "BUILDING", "LECTURE", "CLASS", "VENUE", 
            "PROF", "DR.", "LAB", "TUTORIAL", "ATTENDANCE", "PRESENT", "ABSENT", 
            "TOTAL", "SEMESTER", "BATCH", "GROUP", "SECTION", "CREDITS", "ACADEMIC", "YEAR",
            "SN", "TIME", "DAYS", "SIGNATURE", "STAMP", "DATE", "RECESS", "INTERACTION", "MENTOR", "SL.NO",
            "LUNCH", "BREAK", "INTERVAL", "TEA", "COFFEE", "SPORTS", "ASSEMBLY", "LIBRARY", "OFFICE"
        )
        if (metadataKeywords.any { upper == it || (upper.length > 3 && it.startsWith(upper)) }) return true
        if (upper.matches(Regex("""(ROOM|RM|BLK|FL|SEC|ID|UID|DATE|TIME|DAY|SEM|BATCH|COURSE|CODE|SL\.?\s?NO)\s*[:\-]\s*.*"""))) return true
        if (upper.matches(Regex("""\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\b"""))) return true
        if (upper.matches(Regex("""\d+.*%"""))) return true 
        if (upper.matches(Regex("""\d+"""))) return true
        if (upper.length < 2) return true
        return false
    }

    private fun extractLegend(result: Text) {
        legendMap.clear()
        result.textBlocks.forEach { block ->
            val blockText = block.text.uppercase()
            // Look for keywords that suggest this block is a legend
            if (blockText.contains(Regex("LEGEND|ABBR|COURSE|SUBJECT|NAME|CODE", RegexOption.IGNORE_CASE))) {
                block.lines.forEach { line ->
                    val text = line.text.trim()
                    
                    // Format 1: ABBR - Full Name or ABBR : Full Name
                    val match1 = Regex("""^([A-Z\d]{2,6})\s*[-:]\s*(.+)$""", RegexOption.IGNORE_CASE).find(text)
                    // Format 2: CODE Full Name (e.g., "CS301 Operating Systems")
                    val match2 = Regex("""^([A-Z]{1,4}\d{2,5}[A-Z\d]*)\s+([A-Z].+)$""", RegexOption.IGNORE_CASE).find(text)
                    // Format 3: [ABBR] [CODE] [NAME]
                    val match3 = Regex("""^([A-Z\d]{2,6})\s+([A-Z\d\-]{3,10})\s+(.+)$""", RegexOption.IGNORE_CASE).find(text)
                    
                    val (abbr, name) = when {
                        match3 != null -> match3.groupValues[1] to match3.groupValues[3]
                        match2 != null -> match2.groupValues[1] to match2.groupValues[2]
                        match1 != null -> match1.groupValues[1] to match1.groupValues[2]
                        else -> null to ""
                    }
                    
                    if (abbr != null && name.length > 3 && !isJustMetadata(name)) {
                        legendMap[abbr.uppercase()] = name.trim()
                        Log.d("OcrViewModel", "Legend Found: $abbr -> ${name.trim()}")
                    }
                }
            }
        }
    }

    private fun estimateEndTime(start: String, isLab: Boolean = false): String {
        val parts = start.split(":")
        var hour = parts[0].toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val duration = if (isLab) 2 else 1
        hour = (hour + duration) % 24
        
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun getMonthIndex(month: String): Int = when (month.uppercase().take(3)) {
        "JAN" -> 0; "FEB" -> 1; "MAR" -> 2; "APR" -> 3; "MAY" -> 4; "JUN" -> 5
        "JUL" -> 6; "AUG" -> 7; "SEP" -> 8; "OCT" -> 9; "NOV" -> 10; "DEC" -> 11
        else -> 0
    }

    data class TextElementInfo(val text: String, val x: Int, val y: Int, val h: Int)
    fun clearMessage() { _message.value = null }
}
