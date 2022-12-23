package com.handmonitor.recorder.data

class Action {
    enum class Type {
        HandWash,
        HandRub,
        Eat,
        TeethBrush,
        FaceWash,
        Write,
        Type,
        Housework
    }

    class TimeRange(val remaining: Int, val total: Int)
}
