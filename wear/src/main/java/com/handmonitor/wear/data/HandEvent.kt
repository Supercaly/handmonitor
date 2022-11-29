package com.handmonitor.wear.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Enum representing an hand event type.
 *
 * An hand event can be of two types:
 * - WASHING
 * - RUBBING
 */
enum class HandEventType {
    WASHING,
    RUBBING
}

/**
 * Class representing an hand event.
 *
 * An hand event is created when some labels of the same type
 * are obtained one after the other until a label until one or
 * more labels of type OTHER are received.
 *
 * This class is also an [Entity] for storing [HandEvent]s to
 * a local database.
 *
 * @param[id] ID of this hand event.
 * @param[type] Type of this hand event, [HandEventType].
 * @param[nSamples] Total number of samples than compose this hand event.
 * @param[startTime] Start time of the event in milliseconds since the epoch.
 * @param[endTime] End time of the event in milliseconds since the epoch.
 */
@Entity(tableName = "hand_events")
data class HandEvent(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val type: HandEventType,
    @ColumnInfo(name = "n_samples") val nSamples: Int,
    // TODO: Add a parameter that represents the number of seconds for each sample
    //  this is equal to the window size.
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long

    // TODO: Add conversion methods to return the duration in seconds and
    //  other useful things that can be derived form the data.
)