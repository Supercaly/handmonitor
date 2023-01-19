package com.handmonitor.mltest

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorslib.SensorReaderHelper

class TestService : Service() {
    companion object {
        private const val TAG = "TestService"
    }

    private lateinit var mDataHandler: TestDataHandler
    private lateinit var mSensorReaderHelper: SensorReaderHelper

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        mDataHandler = TestDataHandler(
            this
        ) {
            stopSelf()
        }
        mSensorReaderHelper = SensorReaderHelper(
            this,
            mDataHandler,
            100,
            20
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        if (!mSensorReaderHelper.isStarted) {
            mSensorReaderHelper.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mSensorReaderHelper.stop()
    }
}
