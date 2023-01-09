package com.handmonitor.recorder.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.handmonitor.recorder.data.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    fun addRecording(recording: Recording)

    @Query("SELECT * FROM recordings")
    fun getRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE action_type == :action")
    fun getRecordingsForAction(action: String): Flow<List<Recording>>
}
