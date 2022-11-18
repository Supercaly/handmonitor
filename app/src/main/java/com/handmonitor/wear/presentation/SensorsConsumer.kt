package com.handmonitor.wear.presentation

import android.util.Log

/**
 * The [SensorsConsumer] interface should be implemented by any class
 * that want to do something with the data received from the sensors.
 *
 * @see[SensorsData]
 * @see[SensorsConsumerRn]
 * @see[SensorsListenerThread]
 */
interface SensorsConsumer {
    /**
     * This method is called every time a new list of sensors events
     * is produced; the class implementing the [SensorsConsumer] interface
     * will do his work in this method.
     *
     * @param[data] List of sensors values.
     */
    fun onNewData(data: FloatArray)
}

/**
 * A consumer of sensors values in a dedicated thread.
 *
 * This class waits for [SensorsData] to produce a full list of
 * sensors values and then calls [SensorsConsumer] interface with it. All the work
 * is done in a dedicated thread that the user needs to spawn.
 *
 * Example usage:
 * ```
 * // implement the SensorsConsumer interface with user's logic
 * val impl: Do = object : Do {
 *      override fun done(data: FloatArray) {
 *          // do something with data
 *      }
 * }
 * // create a SensorsConsumerRn
 * val c: SensorsConsumerRn = SensorsConsumerRn(data, impl)
 * // create a new Thread preferably using [threadName] as the name of thread
 * val t: Thread = Thread(c, SensorsConsumerRn.threadName)
 * // later start the thread using
 * t.start()
 * // and stop it using
 * t.interrupt()
 * ```
 *
 * @property[mData] A shared [SensorsData] that contains the values needed.
 * @property[mConsumer] A class that implements [SensorsConsumer] to do something with the
 * collected data.
 */
class SensorsConsumerRn(
    private val mData: SensorsData,
    private val mConsumer: SensorsConsumer
) : Runnable {
    companion object {
        private const val TAG = "SensorsConsumerRunnable"
        const val threadName = "SensorsConsumerThread"
    }

    override fun run() {
        var time = System.currentTimeMillis()
        while (!Thread.currentThread().isInterrupted) {
            try {
                mConsumer.onNewData(mData.getData())
                Log.i(
                    TAG, "run: ${Thread.currentThread().name}: " +
                            "Got data in ${System.currentTimeMillis() - time}"
                )
                time = System.currentTimeMillis()
            } catch (ie: InterruptedException) {
                ie.printStackTrace()
                Thread.currentThread().interrupt()
            }
        }
    }
}