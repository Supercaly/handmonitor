package com.handmonitor.wear.sensors

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
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

    // Sensors collection stuff
    private lateinit var mSensorsData: SensorsData
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
        mSensorsData = SensorsData()
        mCollectorThread = HandlerThread(SensorsListener.threadName).apply { start() }
        mSensorsListener = SensorsListener(
            this,
            mSensorsData,
            Handler(mCollectorThread.looper)
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
