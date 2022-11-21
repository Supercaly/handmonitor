package com.handmonitor.wear.sensors

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
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
    }

    private lateinit var mSensorsData: SensorsData
    private lateinit var mSensorsListenerThread: SensorsListenerThread
    private lateinit var mConsumerThread: Thread
    private lateinit var mSensorsConsumer: SensorsConsumerRn
    private lateinit var mGesturePredictor: GesturePredictor

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Service created!")
        mSensorsData = SensorsData()
        mSensorsListenerThread = SensorsListenerThread(this, mSensorsData)
        mGesturePredictor = GesturePredictor(this)
        mSensorsConsumer = SensorsConsumerRn(mSensorsData, mGesturePredictor)
        mConsumerThread = Thread(mSensorsConsumer, SensorsConsumerRn.threadName)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started!")
        mSensorsListenerThread.startListening()
        mConsumerThread.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopped!")
        mSensorsListenerThread.stopListening()
        mConsumerThread.interrupt()
    }
}