package com.handmonitor.wear.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorslib.SensorEventConsumerRn
import com.handmonitor.sensorslib.SensorEventProducer
import com.handmonitor.sensorslib.SensorSharedData
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

    // SensorReaderHelper collection stuff
    private val mSensorsData: SensorSharedData = SensorSharedData(SAMPLING_WINDOW_SIZE)
    private lateinit var mSensorsConsumer: SensorEventConsumerRn
    private lateinit var mSensorEventProducer: SensorEventProducer

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
        mCollectorThread = HandlerThread("SensorEventProducerThread").apply { start() }
        mSensorEventProducer = SensorEventProducer(
            this,
            mSensorsData,
            Handler(mCollectorThread.looper),
            SAMPLING_WINDOW_SIZE,
            SAMPLING_PERIOD_MS
        )
        mGesturePredictor = GesturePredictor(this)
        mSensorsConsumer = SensorEventConsumerRn(mSensorsData, mGesturePredictor)
        mConsumerThread = Thread(mSensorsConsumer, "SensorEventConsumerThread")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started!")
        mSensorEventProducer.startListening()
        mConsumerThread.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopped!")
        mSensorEventProducer.stopListening()
        mConsumerThread.interrupt()
        mCollectorThread.quit()
    }
}
