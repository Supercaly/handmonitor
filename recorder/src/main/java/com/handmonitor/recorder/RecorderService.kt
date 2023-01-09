package com.handmonitor.recorder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.handmonitor.recorder.data.Action
import com.handmonitor.recorder.data.Recording
import com.handmonitor.recorder.database.AppDatabase
import com.handmonitor.sensorslib.SensorDataHandler
import com.handmonitor.sensorslib.SensorReaderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.OutputStreamWriter
import java.util.UUID

fun Float.format() = String.format("%.4f", this)

class RecorderService : Service() {
    companion object {
        private const val TAG = "RecorderService"
        private const val SAMPLING_WINDOW_SIZE = 100
        private const val SAMPLING_PERIOD_MS = 20
    }

    inner class RecorderBinder : Binder() {
        val service
            get() = this@RecorderService
    }

    private val mBinder: RecorderBinder = RecorderBinder()
    private val mActionStore = object : SensorDataHandler {
        override fun onNewData(data: FloatArray) {
            Log.d(TAG, "onNewData: ")
            for (i in data.indices step 6) {
                mFileStream?.write(
                    "\"${mCurrentRecordedAction?.ordinal}\"," +
                        "${data[i + 0].format()}," +
                        "${data[i + 1].format()}," +
                        "${data[i + 2].format()}," +
                        "${data[i + 3].format()}," +
                        "${data[i + 4].format()}," +
                        "${data[i + 5].format()}\n"
                )
            }
        }
    }
    private val mSensorReaderHelper: SensorReaderHelper = SensorReaderHelper(
        this,
        mActionStore,
        SAMPLING_WINDOW_SIZE,
        SAMPLING_PERIOD_MS
    )

    private var mCurrentRecordedAction: Action.Type? = null
    private var mRecordingStartTimeMs: Long = 0L
    private var mCurrentRecordingDuration: Long = 0L
    private var mFileName: String = ""
    private var mFileStream: OutputStreamWriter? = null

    private lateinit var mTickerJob: Job
    private val mRecordingTime = MutableStateFlow(0L)
    val recordingTime = mRecordingTime.asStateFlow()

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: $intent")
        return mBinder
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")

        mFileName = "${UUID.randomUUID()}.txt"
        try {
            mFileStream = OutputStreamWriter(this.openFileOutput(mFileName, Context.MODE_PRIVATE))
        } catch (ex: FileNotFoundException) {
            ex.printStackTrace()
        }

        mSensorReaderHelper.start()
        mRecordingStartTimeMs = System.currentTimeMillis()
        mCurrentRecordingDuration = 0L

        mTickerJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                mRecordingTime.value += 1_000L
                delay(1_000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!.getStringExtra("action-type")?.also {
            mCurrentRecordedAction = Action.Type.valueOf(it)
        }
        Log.d(TAG, "onStartCommand: $mCurrentRecordedAction")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mTickerJob.cancel()
    }

    fun stopRecording() {
        Log.d(TAG, "stopRecording: ")
        mCurrentRecordingDuration = System.currentTimeMillis() - mRecordingStartTimeMs
        mSensorReaderHelper.stop()
        mFileStream?.close()
    }

    fun saveRecordedData() {
        Log.d(TAG, "saveRecordedData: ")

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@RecorderService).recordingDao()
                .addRecording(
                    Recording(
                        0,
                        mCurrentRecordedAction!!.name,
                        mFileName,
                        mCurrentRecordingDuration
                    )
                )
        }
        stopSelf()
    }

    fun discardRecordedData() {
        Log.d(TAG, "discardRecordedData: ")
        stopSelf()
    }
}
