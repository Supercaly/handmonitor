package com.handmonitor.wear.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorslib.SensorsConsumerRn
import com.handmonitor.sensorslib.SensorsData
import com.handmonitor.sensorslib.SensorsListener
import com.handmonitor.wear.prediction.GesturePredictor

/**
 * Android Service to process sensor data.
 *
 * This Service has the task to collect the real-time data from
 * the accelerometer and gyroscope inside the Android device and
 * process them to obtain hand-washing and hand-rubbing related data.
 */
class SensorService : Service() {
    companion object {
        private const val TAG = "SensorService"
        private const val SAMPLING_WINDOW_SIZE = 128
        private const val SAMPLING_PERIOD_MS = 20
        private const val SAMPLING_WINDOW_DURATION_MS = 2_500
    }

    // Sensors collection stuff
    private val mSensorsData: SensorsData = SensorsData(SAMPLING_WINDOW_SIZE)
    private lateinit var mSensorsConsumer: SensorsConsumerRn
    private lateinit var mSensorsListener: SensorsListener

    // External Threads
    private lateinit var mCollectorThread: HandlerThread
    private lateinit var mConsumerThread: Thread

    // Inference stuff
    private lateinit var mGesturePredictor: GesturePredictor

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Service created!")
        mCollectorThread = HandlerThread(SensorsListener.threadName).apply { start() }
        mSensorsListener = SensorsListener(
            this,
            mSensorsData,
            Handler(mCollectorThread.looper),
            SAMPLING_WINDOW_DURATION_MS,
            SAMPLING_PERIOD_MS
        )
        mGesturePredictor = GesturePredictor(this)
        mSensorsConsumer = SensorsConsumerRn(mSensorsData, mGesturePredictor)
        mConsumerThread = Thread(mSensorsConsumer, SensorsConsumerRn.threadName)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started!")
        mSensorsListener.startListening()
        mConsumerThread.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopped!")
        mSensorsListener.stopListening()
        mConsumerThread.interrupt()
        mCollectorThread.quit()
    }
}
