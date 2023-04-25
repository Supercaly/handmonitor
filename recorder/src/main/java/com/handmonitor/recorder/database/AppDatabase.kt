package com.handmonitor.recorder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.handmonitor.recorder.data.Recording
import com.handmonitor.recorder.database.AppDatabase.Companion.getDatabase
import com.handmonitor.recorder.database.dao.RecordingDao

/**
 *
 * Class representing the local app database.
 *
 * The local database in intended to be instanced only
 * one time, so this class uses the Singleton design
 * pattern. Use [AppDatabase.getDatabase] to retrieve the
 * global instance of the database.
 *
 * ```
 * val db = AppDatabase.getDatabase(context)
 * // do something with DAO ...
 * ```
 * @see[getDatabase]
 */
@Database(
    version = 1,
    entities = [Recording::class]
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var mInstance: AppDatabase? = null

        /**
         * Access the unique instance of [AppDatabase].
         *
         * The first time this method is called the global instance
         * is created and initialized, so it can take a little more
         * time, then the next calls will always return the same database.
         *
         * @param[context] The [Context] used to initialize the database.
         * @return Global instance of [AppDatabase].
         */
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

    /**
     * Access an instance of [RecordingDao].
     * @return [RecordingDao].
     */
    abstract fun recordingDao(): RecordingDao
}
