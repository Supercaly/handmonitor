package com.handmonitor.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.handmonitor.recorder.data.Action
import com.handmonitor.sensorlib.v1.SensorDataHandler
import com.handmonitor.sensorlib.v1.SensorReaderHelper
import com.handmonitor.sensorlib.v2.SensorWindow
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
 * Implements a [Service] that records an action performed
 * by the user and stores it in the device for future use.
 */
class RecorderService : Service() {
    companion object {
        private const val TAG = "RecorderService"
        private const val SAMPLING_WINDOW_SIZE = 100
        private const val SAMPLING_PERIOD_MS = 20L
        private const val OTHER_ACTION_DELAY_TIME_M = 30L

        private const val NOTIFICATION_CHANNEL_ID = "sensor_channel"
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
        val elapsedTime: StateFlow<Long> = mElapsedTime.asStateFlow()

        /**
         * Returns the action being recorded or null.
         */
        val recordedAction: Action.Type?
            get() = mRecordingStorer?.action

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

    private lateinit var mRecorderBinder: RecorderBinder
    private lateinit var mSensorReader: SensorReaderHelper
    private var mRecordingStorer: RecordingStorer? = null
    private lateinit var mRecorderPreferences: RecorderPreferences

    private lateinit var mTickerCoroutineJob: Job
    private val mElapsedTime = MutableStateFlow(0L)

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")

        mSensorReader = SensorReaderHelper(
            this@RecorderService,
            object : SensorDataHandler {
                override fun onNewData(data: FloatArray) {
                    Log.d(TAG, "onNewData: ")
                    mRecordingStorer?.recordWindow(SensorWindow.fromArray(data))
                }
            },
            SAMPLING_WINDOW_SIZE,
            SAMPLING_PERIOD_MS.toInt()
        )
        mRecorderPreferences = RecorderPreferences(this)

        mRecorderBinder = RecorderBinder()
        mTickerCoroutineJob = CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    mElapsedTime.value += 1_000L
                    delay(1_000L)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: $intent")
        return mRecorderBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(5, getNotification())

        if (!mSensorReader.isStarted) {
            val action = intent!!.getStringExtra("action-type")?.run {
                Action.Type.valueOf(this)
            }
            try {
                mRecordingStorer = RecordingStorer(this@RecorderService, action!!)
            } catch (ex: FileNotFoundException) {
                ex.printStackTrace()
                stopSelf()
                return START_NOT_STICKY
            }
            mSensorReader.start()

            mRecorderPreferences.isSomeoneRecording = true
        }

        Log.d(TAG, "onStartCommand: recording ${mRecordingStorer?.action} action")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mTickerCoroutineJob.cancel()
        mRecorderPreferences.isSomeoneRecording = false

        if (mSensorReader.isStarted) {
            Log.e(
                TAG,
                "onDestroy: We are quitting the service, but the sensors are still reading... stopping them"
            )
            mSensorReader.stop()
            mRecordingStorer?.stopRecording()
            runBlocking { mRecordingStorer?.saveRecording() }
        }
    }

    private fun stopRecording() {
        Log.d(TAG, "stopRecording: Stop recording ${mRecordingStorer?.action} action")
        mSensorReader.stop()
        mRecordingStorer?.stopRecording()
    }

    private fun saveRecordedData() {
        Log.d(TAG, "saveRecordedData: Saving ${mRecordingStorer?.action} action")
        runBlocking {
            mRecordingStorer?.saveRecording()
        }

        Log.i(TAG, "saveRecordedData: Enqueue other action recorder worker")
        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<OtherActionRecorderWorker>()
                .setInitialDelay(Duration.ofMinutes(OTHER_ACTION_DELAY_TIME_M))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofMinutes(5)
                )
                .build()
        )

        stopSelf()
    }

    private fun discardRecordedData() {
        Log.d(TAG, "discardRecordedData: Discarding ${mRecordingStorer?.action} action")
        stopSelf()
    }

    private fun getNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Sensor Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Sensor Service channel"
            it.enableLights(true)
            it
        }
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sensor Service")
            .setContentText("This service is used to collect sensor events")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker")
            .build()
    }
}
