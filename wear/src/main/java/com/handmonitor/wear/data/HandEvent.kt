package com.handmonitor.wear.data

enum class HandEventType {
    WASHING,
    RUBBING
}

data class HandEvent(
    val type: HandEventType,
    val duration: Int,
    val startTime: Long
)