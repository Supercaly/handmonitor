package com.handmonitor.wear.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorlib.SensorWindowProducer
import com.handmonitor.sensorlib.asFlow
import com.handmonitor.wear.prediction.GesturePredictor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        private const val SAMPLING_PERIOD_MS = 20L
    }

    private val mServiceScope = CoroutineScope(Dispatchers.Default + Job())
    private val mProducer =
        SensorWindowProducer(
            this@SensorService,
            SAMPLING_PERIOD_MS,
            SAMPLING_WINDOW_SIZE
        )
    private val mGesturePredictor: GesturePredictor = GesturePredictor(this@SensorService)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started!")
        mServiceScope.launch {
            withContext(Dispatchers.Default) {
                mProducer.asFlow().onEach {
                    mGesturePredictor.onNewData(it)
                }.collect()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopped!")
        mServiceScope.cancel()
    }
}
