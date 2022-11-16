package com.handmonitor.wear.presentation

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log

class SensorService : Service() {
    companion object {
        private const val TAG = "SensorService"
    }

    private lateinit var mSensorListener: SensorListener
    private lateinit var mPredictionRunnable: PredictionRunnable
    private lateinit var mSensorsData: SensorsData

    private var mCollectionThread = HandlerThread("SensorsCollectionThread")
    private lateinit var mPredictionThread: Thread
    private lateinit var mCollectionThHd: Handler

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate: Service created!")

        mSensorsData = SensorsData()

        mCollectionThread.apply {
            start()
            mCollectionThHd = Handler(looper)
        }
        mSensorListener = SensorListener(this, mCollectionThHd, mSensorsData)
        mPredictionRunnable = PredictionRunnable(mSensorsData)
        mPredictionThread = Thread(mPredictionRunnable, "SensorsPredictionThread")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: Service started!")
        mSensorListener.startListening()
        mPredictionThread.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Service stopped!")
        mSensorListener.stopListening()
        mCollectionThread.quit()
        mPredictionThread.interrupt()
    }
}