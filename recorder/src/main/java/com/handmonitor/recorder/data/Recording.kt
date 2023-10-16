package com.handmonitor.recorder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represent a recording of an action stored on
 * the device.
 */
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long
)
