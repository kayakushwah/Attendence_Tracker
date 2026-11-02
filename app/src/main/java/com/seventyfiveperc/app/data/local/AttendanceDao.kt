package com.seventyfiveperc.app.data.local

import androidx.room.*
import com.seventyfiveperc.app.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    // Subjects
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // Attendance
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subId")
    fun getAttendanceForSubject(subId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records WHERE date >= :start AND date <= :end")
    fun getAttendanceInRange(start: Long, end: Long): Flow<List<AttendanceRecord>>

    // Timetable
    @Query("SELECT * FROM timetable")
    fun getAllTimetableEntries(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day")
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntry(entry: TimetableEntry)

    @Update
    suspend fun updateTimetableEntry(entry: TimetableEntry)

    @Delete
    suspend fun deleteTimetableEntry(entry: TimetableEntry)

    // Academic Events
    @Query("SELECT * FROM academic_events")
    fun getAllAcademicEvents(): Flow<List<AcademicEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicEvent(event: AcademicEvent)

    @Update
    suspend fun updateAcademicEvent(event: AcademicEvent)

    @Delete
    suspend fun deleteAcademicEvent(event: AcademicEvent)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?
}
