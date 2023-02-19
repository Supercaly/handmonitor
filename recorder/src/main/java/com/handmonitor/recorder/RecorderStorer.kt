package com.handmonitor.recorder

import android.content.Context
import com.handmonitor.recorder.data.Action
import com.handmonitor.recorder.data.Recording
import com.handmonitor.recorder.database.AppDatabase
import com.handmonitor.sensorlib.SensorWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.OutputStreamWriter
import java.util.UUID

class RecorderStorer
@Throws(FileNotFoundException::class)
constructor(
    private val context: Context,
    val action: Action.Type
) {
    companion object {
        private const val FILE_EXT = "txt"
    }

    private var mFileName: String = "${UUID.randomUUID()}.$FILE_EXT"
    private var mFileStream: OutputStreamWriter =
        OutputStreamWriter(context.openFileOutput(mFileName, Context.MODE_PRIVATE))

    private var mStartTime: Long = System.currentTimeMillis()
    private var mRecordingDuration: Long = 0L

    fun recordWindow(window: SensorWindow) {
        val data = window.buffer
        for (i in 0 until data.capacity() step 6) {
            mFileStream.write(
                "\"${action.ordinal}\"," + "${data[i + 0].format()}," +
                    "${data[i + 1].format()}," + "${data[i + 2].format()}," +
                    "${data[i + 3].format()}," + "${data[i + 4].format()}," +
                    "${data[i + 5].format()}\n"
            )
        }
    }

    fun stopRecording() {
        mFileStream.close()
        mRecordingDuration = System.currentTimeMillis() - mStartTime
    }

    suspend fun saveRecording() {
        withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).recordingDao()
                .addRecording(
                    Recording(
                        0,
                        action.name,
                        mFileName,
                        mRecordingDuration
                    )
                )
        }
    }
}
