package com.handmonitor.wear.repository

import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class representing a repository of [HandEvent]s.
 *
 * This class has the purpose of helping managing all
 * the [HandEvent]s with methods that adds and returns
 * the events.
 *
 * @constructor[mAppDatabase] Instance of [AppDatabase].
 */
class HandEventsRepository(
    private val mAppDatabase: AppDatabase
) {
    /**
     * Add a new [HandEvent] to the database.
     *
     * This method is called every time we have a new instance
     * of [HandEvent] adding it to the local database.
     *
     * This method is a suspend function that runs in a
     * separate coroutine.
     *
     * @param[event] The new [HandEvent] to add.
     */
    suspend fun addNewEvent(event: HandEvent) {
        withContext(Dispatchers.IO) {
            mAppDatabase.handEventDao().insertEvent(event)
        }
    }
}
