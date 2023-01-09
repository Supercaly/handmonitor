package com.handmonitor.wear.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorslib.SensorReaderHelper
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
    }

    private val mGesturePredictor: GesturePredictor = GesturePredictor(this)
    private val mSensorReaderHelper: SensorReaderHelper = SensorReaderHelper(
        this,
        mGesturePredictor,
        SAMPLING_WINDOW_SIZE,
        SAMPLING_PERIOD_MS
    )

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started!")
        mSensorReaderHelper.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopped!")
        mSensorReaderHelper.stop()
    }
}
