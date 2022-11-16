package com.handmonitor.wear.presentation

import java.util.concurrent.Semaphore

/**
 * A shared sensor data access.
 *
 * This class represents a memory object shared between two threads,
 * one that listens to real-time values from the device sensors, and
 * the other that uses the data.
 *
 * This class implements a sort of producer-consumer pattern, but the
 * data is produced continuously and it's consumed in windows of [SAMPLING_WINDOW_SIZE].
 */
class SensorsData {
    companion object {
        const val SAMPLING_WINDOW_SIZE = 100
        const val SENSORS_LIST_SIZE = SAMPLING_WINDOW_SIZE * 3
    }

    // Semaphores for thread synchronization
    private val mPutSemaphore: Semaphore = Semaphore(1)
    private val mGetSemaphore: Semaphore = Semaphore(0)

    // Internal data arrays and indexes
    private val mAccArray: FloatArray = FloatArray(SENSORS_LIST_SIZE) { 0.0f }
    private val mGyrArray: FloatArray = FloatArray(SENSORS_LIST_SIZE) { 0.0f }
    private var mAccIdx: Int = 0
    private var mGyrIdx: Int = 0

    /**
     * Adds a new [accelerometer][acc] sample to the collected data list.
     *
     * NOTE: This method is intended to be called only by one producer thread
     * at a time; using multiple producers will result in unpredictable behaviour.
     *
     * @param[acc] An accelerometer sample is a [FloatArray] with 3 value for the
     * three axes of the accelerometer sensor.
     *
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putAcc(acc: FloatArray) {
        // WARNING: this function assumes it's only called by one thread at a time.
        mAccArray[mAccIdx + 0] = acc[0]
        mAccArray[mAccIdx + 1] = acc[1]
        mAccArray[mAccIdx + 2] = acc[2]
        mAccIdx += 3
        checkArraysAreFull()
    }

    /**
     * Adds a new [gyroscope][gyro] sample to the collected data list.
     *
     * NOTE: This method is intended to be called only by one producer thread
     * at a time; using multiple producers will result in unpredictable behaviour.
     *
     * @param[gyro] A gyroscope sample is a [FloatArray] with 3 value for the
     * three axes of the gyroscope sensor.
     *
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putGyro(gyro: FloatArray) {
        // WARNING: this function assumes it's only called by one thread at a time.
        mGyrArray[mGyrIdx + 0] = gyro[0]
        mGyrArray[mGyrIdx + 1] = gyro[1]
        mGyrArray[mGyrIdx + 2] = gyro[2]
        mGyrIdx += 3
        checkArraysAreFull()
    }

    /**
     * Return the collected sensor data in a window of [SAMPLING_WINDOW_SIZE].
     *
     * This method will wait until the producer has filled the processing
     * window's list with sensors data.
     *
     * NOTE: This method is intended to be called only by one consumer thread
     * at a time; using multiple consumers will result in unpredictable behaviour.
     *
     * @return the collected data in a window of [SAMPLING_WINDOW_SIZE]
     * as a [FloatArray].
     *
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun getData(): FloatArray {
        // Wait for all data to be produced
        mGetSemaphore.acquire()
        // Copy data to a thread-safe buffer
        val buffer = mAccArray.clone()
        // Notify producer that we are done copying
        mPutSemaphore.release()
        return buffer
    }

    /**
     * Check if the internal arrays are full and notify the consumer.
     *
     * This method is called by [putAcc] and [putGyro] every time a new
     * data sample is added; if one of the internal sensor arrays is full
     * reset the indexes to zero, release one permission of the consumer
     * semaphore and wait for it to copy the data.
     *
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    private fun checkArraysAreFull() {
        if (mAccIdx >= SENSORS_LIST_SIZE ||
            mGyrIdx >= SENSORS_LIST_SIZE
        ) {
            mAccIdx = 0
            mGyrIdx = 0
            // Notify the consumer that the data is ready
            mGetSemaphore.release()
            // Wait for the consumer to copy the data
            mPutSemaphore.acquire()
        }
    }
}