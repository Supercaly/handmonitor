package com.handmonitor.wear.prediction

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.handmonitor.wear.sensors.SensorsConsumer

/**
 * A gesture predictor.
 *
 * This class implements the [SensorsConsumer] interface using
 * the obtained sensors data to extract hand-washing and
 * hand-rubbing events.
 *
 * @constructor Creates an instance of [GesturePredictor] with given [Context].
 */
class GesturePredictor : SensorsConsumer {
    companion object {
        private const val TAG = "GesturePredictor"
    }

    private val mDetectorHelper: GestureDetectorHelper

    /**
     * Constructs an instance of [GesturePredictor] with given
     * [Context].
     */
    constructor(ctx: Context) {
        mDetectorHelper = GestureDetectorHelper(ctx, "model.tflite")
    }

    /**
     * Constructs an instance of [GesturePredictor] with
     * pre-existing [GestureDetectorHelper].
     */
    @VisibleForTesting
    constructor(detectorHelper: GestureDetectorHelper) {
        mDetectorHelper = detectorHelper
    }

    override fun onNewData(data: FloatArray) {
        val predictedLabel = mDetectorHelper.predict(data)
        // TODO: Finish implementing this method.
        Log.d(TAG, "onNewData: Predicted class '$predictedLabel'")
    }
}