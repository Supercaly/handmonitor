package com.handmonitor.wear.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.handmonitor.wear.data.HandEvent

/**
 * Interface representing an [HandEvent] DAO.
 *
 * This class has methods to manipulate the [HandEvent]s, inserting
 * and querying the data inside a local database.
 */
@Dao
interface HandEventDao {
    /**
     * Insert a new [HandEvent] to the local database.
     * @param[event] The new [HandEvent] to insert.
     */
    @Insert
    suspend fun insertEvent(event: HandEvent)

    // TODO(#6): Add methods to query the last event and all the events after a particular date.

//    @Query("SELECT * FROM hand_events")
//    suspend fun getAllEvents(): List<HandEvent>
//
//    @Query("SELECT * FROM hand_events ORDER BY start_time DESC LIMIT 1")
//    fun getLastEvent(): Flow<HandEvent>
//
//    @Query("SELECT * FROM hand_events")
//    fun getEventsAfterDate(date: Long): Flow<List<HandEvent>>
}
