package com.handmonitor.recorder

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.handmonitor.recorder.data.Action
import com.handmonitor.sensorlib.v2.SensorWindowProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.time.Duration

/**
 * Extension function that formats a [Float]
 * to 4 digits after the comma.
 */
fun Float.format() = String.format("%.4f", this)

/**
 * Implements a [Service] that records an action performed
 * by the user and stores it in the device for future use.
 */
class RecorderService : Service() {
    companion object {
        private const val TAG = "RecorderService"
        private const val SAMPLING_WINDOW_SIZE = 100
        private const val SAMPLING_PERIOD_MS = 20L
    }

    /**
     * Implements a [Binder] that helps the main application
     * connect to the [RecorderService].
     */
    inner class RecorderBinder : Binder() {
        /**
         * Returns the time in milliseconds elapsed since the start
         * of the current recording.
         */
        val recordingTime: StateFlow<Long> = mRecordingTime.asStateFlow()

        /**
         * Stop the recording of the current action.
         */
        fun stopRecording() = this@RecorderService.stopRecording()

        /**
         * Save the current recorded action to a local database.
         */
        fun saveRecordedData() = this@RecorderService.saveRecordedData()

        /**
         * Discard the current recorded action.
         */
        fun discardRecordedData() = this@RecorderService.discardRecordedData()
    }

    private lateinit var mBinder: RecorderBinder
    private lateinit var mSensorWindowProducer: SensorWindowProducer
    private var mRecorderStorer: RecorderStorer? = null
    private lateinit var mRecorderPreferences: RecorderPreferences

    private lateinit var mTickerCoroutineJob: Job
    private val mRecordingTime = MutableStateFlow(0L)

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")

        mSensorWindowProducer = SensorWindowProducer(
            this@RecorderService,
            SAMPLING_PERIOD_MS,
            SAMPLING_WINDOW_SIZE
        )
        mSensorWindowProducer.setOnNewWindowListener {
            Log.d(TAG, "onNewData: ")
            mRecorderStorer?.recordWindow(it)
        }
        mRecorderPreferences = RecorderPreferences(this)

        mBinder = RecorderBinder()
        mTickerCoroutineJob = CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    mRecordingTime.value += 1_000L
                    delay(1_000L)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: $intent")
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!mSensorWindowProducer.isListening) {
            val action = intent!!.getStringExtra("action-type")?.run {
                Action.Type.valueOf(this)
            }
            try {
                mRecorderStorer = RecorderStorer(this@RecorderService, action!!)
            } catch (ex: FileNotFoundException) {
                ex.printStackTrace()
                stopSelf()
                return START_NOT_STICKY
            }
            mSensorWindowProducer.startSensors()

            mRecorderPreferences.isSomeoneRecording = true
        }

        Log.d(TAG, "onStartCommand: recording ${mRecorderStorer?.action} action")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mTickerCoroutineJob.cancel()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopRecording: Stop recording ${mRecorderStorer?.action} action")
        mSensorWindowProducer.stopSensors()
        mRecorderStorer?.stopRecording()

        mRecorderPreferences.isSomeoneRecording = false
    }

    private fun saveRecordedData() {
        Log.d(TAG, "saveRecordedData: Saving ${mRecorderStorer?.action} action")
        runBlocking {
            mRecorderStorer?.saveRecording()
        }

        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<OtherActionRecorderWorker>()
                .setInitialDelay(Duration.ofMinutes(30))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofMinutes(5)
                )
                .build()
        )

        stopSelf()
    }

    private fun discardRecordedData() {
        Log.d(TAG, "discardRecordedData: Discarding ${mRecorderStorer?.action} action")
        stopSelf()
    }
}
