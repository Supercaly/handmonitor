package com.handmonitor.wear.data

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
 */
@Entity(tableName = "hand_events")
data class HandEvent(
    val id: Int,
    val type: HandEventType,
    val nSamples: Int,
    // TODO: Add a parameter that represents the number of seconds for each sample
    //  this is equal to the window size.
    val startTime: Long,
    val endTime: Long

    // TODO: Add conversion methods to return the duration in seconds and
    //  other useful things that can be derived form the data.
)