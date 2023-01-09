package com.handmonitor.recorder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long
) {
//    val type: Action.Type
//        get() = Action.Type.valueOf(actionType)
}
