package com.seventyfiveperc.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.seventyfiveperc.app.data.models.AcademicEvent
import com.seventyfiveperc.app.data.models.AttendanceRecord
import com.seventyfiveperc.app.data.models.Subject
import com.seventyfiveperc.app.data.models.TimetableEntry

@Database(entities = [Subject::class, AttendanceRecord::class, TimetableEntry::class, AcademicEvent::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
