package com.handmonitor.wear.presentation

import android.util.Log

class PredictionRunnable(
    private val mSensorsData: SensorsData
) : Runnable {
    companion object {
        private const val TAG = "PredictionRunnable"
    }

    override fun run() {
        var time = System.currentTimeMillis()
        while (!Thread.currentThread().isInterrupted) {
            try {
                mSensorsData.getData()
                Log.i(TAG, "run: Hello from runnable ${System.currentTimeMillis() - time}")
                time = System.currentTimeMillis()
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}