package com.handmonitor.wear.presentation

import android.content.Context
import android.util.Log

class GesturePredictor(ctx: Context) : SensorsConsumer {
    companion object {
        private const val TAG = "GesturePredictor"
    }

    private val mDetectorHelper: GestureDetectorHelper = GestureDetectorHelper(ctx, "model.tflite")

    override fun onNewData(data: FloatArray) {
        val predictedLabel = mDetectorHelper.predict(data)
        Log.d(TAG, "onNewData: Predicted class '$predictedLabel'")
    }
}