package com.handmonitor.recorder.data

/**
 * Represent an action.
 */
class Action {
    /**
     * Represent the type of action the can be recorded
     * by the app.
     */
    enum class Type {
        Other,
        HandWash,
        HandRub,
        Eating,
        TeethBrush,
        FaceWash,
        Writing,
        Typing,
        Housework
    }

    /**
     * This class represent a time range of an action.
     *
     * An action recording has a fixed maximum duration and
     * the current recorded duration.
     *
     * @property[remaining] Current recorded duration of this action type.
     * @property[total] Total duration of this action type.
     */
    class TimeRange(val remaining: Long, val total: Long = 600_000L)
}
