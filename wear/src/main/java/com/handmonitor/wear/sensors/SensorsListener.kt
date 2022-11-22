package com.handmonitor.wear.sensors

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
 * The user must start and stop the separate thread by his own and then
 * create a [Handler] that will offload all the work. The internal Android
 * [SensorEventListener] will post a new message for each senor event so it's
 * suggested to use a [HandlerThread] for automatically manage those works.
 *
 * @property[mSensorsData] The values produced are passed to a shared [SensorsData].
 * @property[mHandler] The [Handler] for the separate [Thread].
 * @constructor Crates an instance of [SensorsListener] with given [Context],
 * [SensorsData] and [Handler].
 */
class SensorsListener(
    ctx: Context,
    private val mSensorsData: SensorsData,
    private val mHandler: Handler
) : SensorEventListener {
    companion object {
        const val threadName = "SensorsListenerThread"
        private const val TAG = "SensorsListener"
        private const val SAMPLING_PERIOD_US = 20_000

        // TODO: Extract max latency outside of this class and make it depend of the sampling size.
        private const val MAX_LATENCY_US = 2_500_000
    }

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
    }

    /**
     * Start listening to sensors events.
     *
     * @see [stopListening].
     */
    fun startListening() {
        if (mAccSensor != null) {
            mSensorManager.registerListener(
                this,
                mAccSensor,
                SAMPLING_PERIOD_US,
                MAX_LATENCY_US,
                mHandler
            )
        }
        if (mGyroSensor != null) {
            mSensorManager.registerListener(
                this,
                mGyroSensor,
                SAMPLING_PERIOD_US,
                MAX_LATENCY_US,
                mHandler
            )
        }
    }

    /**
     * Stop listening to sensors events.
     *
     * @see [startListening].
     */
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
        Log.i(TAG, "onAccuracyChanged: Changed accuracy of sensor '${sensor?.name}' to $accuracy")
    }
}