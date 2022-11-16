package com.handmonitor.wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log

class SensorListener(
    ctx: Context,
    private val mHandler: Handler,
    private val mSensorsData: SensorsData
) : SensorEventListener {
    companion object {
        private const val TAG = "SensorListener"
        private const val SAMPLING_PERIOD_US = 20_000
        private const val MAX_LATENCY_US = 2_000_000
    }

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
    }

    fun startListening() {
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

    fun stopListening() {
        mSensorManager.unregisterListener(this)
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
        Log.d(TAG, "onAccuracyChanged: Changed accuracy of sensor '${sensor?.name}' to $accuracy")
    }
}