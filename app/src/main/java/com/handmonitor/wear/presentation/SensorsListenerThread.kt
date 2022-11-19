package com.handmonitor.wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * A listener for sensors values in a dedicate thread.
 *
 * This class listen periodically to samples produced by the
 * sensors in the Android device like accelerometer and gyroscope
 * in his own dedicated thread and pass them to a shared [SensorsData].
 *
 * @property[mSensorsData] The values produced are passed to a shared [SensorsData].
 * @constructor Crates an instance of [SensorsListenerThread] with given [Context]
 * and [SensorsData]; when constructed the internal thread is started immediately,
 * but the sensors data are collected only after a call to [startListening].
 */
class SensorsListenerThread(
    ctx: Context,
    private val mSensorsData: SensorsData
) : SensorEventListener {
    companion object {
        private const val TAG = "SensorsListenerThread"
        private const val SAMPLING_PERIOD_US = 20_000
        private const val MAX_LATENCY_US = 2_500_000
    }

    // Thread mechanics
    private val mHandler: Handler
    private val mThread: HandlerThread

    // Sensor manager and sensors
    private val mSensorManager: SensorManager
    private var mAccSensor: Sensor? = null
    private var mGyroSensor: Sensor? = null

    init {
        mSensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mAccSensor == null) {
            Log.e(TAG, "onCreate: Accelerometer sensor is not supported!")
        }

        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (mGyroSensor == null) {
            Log.e(TAG, "onCreate: Gyroscope sensor is not supported!")
        }

        mThread = HandlerThread(TAG).apply {
            start()
            mHandler = Handler(looper)
        }
    }

    /**
     * Start listening to sensors events.
     *
     * @see [stopListening].
     */
    fun startListening() {
        // TODO: Fail if the internal thread is not started
        mSensorManager.registerListener(
            this,
            mAccSensor,
            SAMPLING_PERIOD_US,
            MAX_LATENCY_US,
            mHandler
        )
        mSensorManager.registerListener(
            this,
            mGyroSensor,
            SAMPLING_PERIOD_US,
            MAX_LATENCY_US,
            mHandler
        )
    }

    /**
     * Stop listening to sensors events.
     *
     * This method stops listening to new sensors events
     * and interrupts the internal thread. This meas that
     * after calling this method successive calls to [startListening]
     * will fail.
     *
     * @see [startListening].
     */
    fun stopListening() {
        mSensorManager.unregisterListener(this)
        mThread.quitSafely()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> mSensorsData.putAcc(event.values)
            Sensor.TYPE_GYROSCOPE -> mSensorsData.putGyro(event.values)
            else -> {
                Log.w(TAG, "onSensorChanged: Unknown sensor type ${event?.sensor?.type}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG, "onAccuracyChanged: Changed accuracy of sensor '${sensor?.name}' to $accuracy")
    }
}