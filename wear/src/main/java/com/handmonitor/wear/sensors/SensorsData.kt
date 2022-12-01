package com.handmonitor.wear.sensors

import com.handmonitor.wear.sensors.SensorsData.Companion.SAMPLING_WINDOW_SIZE
import com.handmonitor.wear.sensors.SensorsDataImpl.Companion.WINDOW_DATA_SIZE
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock

/**
 * Represents a single sample of data obtained from a sensor.
 *
 * This type of data is a [Triple] of [Float]s values representing
 * the sensors' x, y, z values.
 */
data class SensorSample(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        fun fromArray(array: FloatArray): SensorSample =
            SensorSample(array[0], array[1], array[2])
    }
}

/**
 * A shared access to sensors data.
 *
 * This class represents a memory object shared between two threads,
 * one produces real-time values from the device sensors, and
 * the other that consumes them.
 *
 * This class implements a sort of producer-consumer pattern, but the
 * data is produced continuously and it's consumed in windows of [SAMPLING_WINDOW_SIZE].
 */
class SensorsData {
    companion object {
        // TODO: Extract the sampling window size outside this class to be more customizable.
        const val SAMPLING_WINDOW_SIZE = 128
    }

    // Semaphores and locks for thread synchronization
    private val mProducerLock: ReentrantLock = ReentrantLock()
    private val mProducerSemaphore: Semaphore = Semaphore(1)
    private val mConsumerSemaphore: Semaphore = Semaphore(0)

    // Internal data arrays and indexes
    private val mSensorsDataImpl: SensorsDataImpl = SensorsDataImpl()

    /**
     * Adds a new accelerometer [SensorSample] to the window.
     *
     * @param[acc] A new accelerometer [SensorSample].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putAcc(acc: SensorSample) {
        mProducerLock.lock()
        val full = mSensorsDataImpl.appendAcc(acc)
        if (full) {
            // Notify the consumer that the data is ready
            mConsumerSemaphore.release()
            // Wait for the consumer to copy the data
            mProducerSemaphore.acquire()
        }
        mProducerLock.unlock()
    }

    /**
     * Adds a new gyroscope [SensorSample] to the window.
     *
     * @param[gyro] A new gyroscope [SensorSample].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putGyro(gyro: SensorSample) {
        mProducerLock.lock()
        val full = mSensorsDataImpl.appendGyro(gyro)
        if (full) {
            // Notify the consumer that the data is ready
            mConsumerSemaphore.release()
            // Wait for the consumer to copy the data
            mProducerSemaphore.acquire()
        }
        mProducerLock.unlock()
    }

    /**
     * Wait until a window with sensors data is produced and return it.
     *
     * This method will wait until the producer has filled the processing
     * window with sensors data.
     *
     * @return the window as a [FloatArray] of size [SAMPLING_WINDOW_SIZE].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun getData(): FloatArray {
        // Wait for all data to be produced
        mConsumerSemaphore.acquire()
        // Copy data to a thread-safe buffer
        val buffer = mSensorsDataImpl.window.clone()
        // Notify producer that we are done copying
        mProducerSemaphore.release()
        return buffer
    }
}

/**
 * Internal implementation of sensors data.
 *
 * This class implements methods to append accelerometer and gyroscope
 * [SensorSample]s filling a window of size [WINDOW_DATA_SIZE].
 */
internal class SensorsDataImpl {
    companion object {
        const val WINDOW_DATA_SIZE = SAMPLING_WINDOW_SIZE * 6
    }

    private val mWindowData: FloatArray = FloatArray(WINDOW_DATA_SIZE) { 0.0f }
    private var mAccIdx: Int = 0
    private var mGyroIdx: Int = 0

    /**
     * The window of data created by accelerometer and gyroscope samples
     * represented as a 1D [FloatArray].
     */
    val window: FloatArray
        get() = mWindowData

    /**
     * Appends a new accelerometer [SensorSample].
     * @param[acc] The new accelerometer [SensorSample].
     * @return true if the underlying window is full, false otherwise.
     */
    fun appendAcc(acc: SensorSample): Boolean {
        mWindowData[mAccIdx + 0] = acc.x
        mWindowData[mAccIdx + 1] = acc.y
        mWindowData[mAccIdx + 2] = acc.z
        mAccIdx += 6

        if (mAccIdx >= WINDOW_DATA_SIZE || mGyroIdx >= WINDOW_DATA_SIZE) {
            mAccIdx = 0
            mGyroIdx = 0
            return true
        }
        return false
    }

    /**
     * Appends a new gyroscope [SensorSample].
     * @param[gyro] The new gyroscope [SensorSample].
     * @return true if the underlying window is full, false otherwise.
     */
    fun appendGyro(gyro: SensorSample): Boolean {
        mWindowData[mGyroIdx + 3] = gyro.x
        mWindowData[mGyroIdx + 4] = gyro.y
        mWindowData[mGyroIdx + 5] = gyro.z
        mGyroIdx += 6

        if (mAccIdx >= WINDOW_DATA_SIZE || mGyroIdx >= WINDOW_DATA_SIZE) {
            mAccIdx = 0
            mGyroIdx = 0
            return true
        }
        return false
    }
}
