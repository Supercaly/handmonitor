package com.handmonitor.wear.data

/**
 * A label of a gesture.
 *
 * This enum represents one of the possible values
 * a gesture event can take. Those values are:
 * - WASHING an hand-washing event
 * - RUBBING an hand-rubbing event
 * - OTHER a different event we don't care about
 */
enum class Label {
    OTHER,
    WASHING,
    RUBBING
}
