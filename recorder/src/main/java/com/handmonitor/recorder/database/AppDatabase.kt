package com.handmonitor.recorder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.handmonitor.recorder.data.Recording
import com.handmonitor.recorder.database.dao.RecordingDao

@Database(
    version = 1,
    entities = [Recording::class]
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var mInstance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return mInstance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "recorder_database"
                ).build()
                mInstance = instance
                instance
            }
        }
    }

    abstract fun recordingDao(): RecordingDao
}
