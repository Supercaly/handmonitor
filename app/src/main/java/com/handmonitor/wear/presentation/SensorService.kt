package com.handmonitor.wear.presentation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

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
    private val mDo: SensorsConsumer = object : SensorsConsumer {
        override fun onNewData(data: FloatArray) {
            Log.d(TAG, "done: ciao ${data.size}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate: Service created!")
        mSensorsData = SensorsData()
        mSensorsListenerThread = SensorsListenerThread(this, mSensorsData)
        mSensorsConsumer = SensorsConsumerRn(mSensorsData, mDo)
        mConsumerThread = Thread(mSensorsConsumer, SensorsConsumerRn.threadName)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: Service started!")
        mSensorsListenerThread.startListening()
        mConsumerThread.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Service stopped!")
        mSensorsListenerThread.stopListening()
        mConsumerThread.interrupt()
    }
}