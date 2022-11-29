package com.handmonitor.wear.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.handmonitor.wear.database.dao.HandEventDao
import com.handmonitor.wear.data.HandEvent

/**
 * Class representing the local app database.
 *
 * The local database in intended to be instanced only
 * one time, so this class is uses the Singleton design
 * pattern. Use [AppDatabase.getDatabase] to retrieve the
 * global instance of the database.
 *
 * ```
 * val db = AppDatabase.getDatabase(context)
 * val dao = db.handEventDao()
 * // do something with DAO ...
 * ```
 * @see[getDatabase]
 */
@Database(
    version = 1,
    entities = [HandEvent::class]
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Access the [HandEventDao] stored in the database.
     * @return The [HandEventDao].
     */
    abstract fun handEventDao(): HandEventDao

    companion object {
        @Volatile
        private var mInstance: AppDatabase? = null

        /**
         * This static method is used to access the global instance
         * of [AppDatabase].
         *
         * The first time this method is called the global instance
         * is created and initialized, so it can take a little more
         * time, then the next calls will always return the same database.
         *
         * @param[context] The [Context] used to initialize the database.
         * @return The global instance of [AppDatabase].
         */
        fun getDatabase(context: Context): AppDatabase {
            return mInstance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                mInstance = instance
                instance
            }
        }
    }
}