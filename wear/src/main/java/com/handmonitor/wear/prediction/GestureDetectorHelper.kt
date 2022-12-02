package com.handmonitor.wear.prediction

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.handmonitor.wear.data.Label
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Helper for gesture detector.
 *
 * This class is an helper class for tflite.
 *
 * @constructor Creates an instance of [GestureDetectorHelper] with
 * given [Context] and model name.
 */
class GestureDetectorHelper {
    companion object {
        private const val TAG = "GestureDetectorHelper"

        /**
         * Loads a model with given [modelName] to a
         * [MappedByteBuffer].
         *
         * @param[ctx] A [Context] used to access assets folder.
         * @return A [MappedByteBuffer] with model data.
         */
        private fun loadModel(ctx: Context, modelName: String): MappedByteBuffer {
            val fd = ctx.assets.openFd(modelName)
            val stream = FileInputStream(fd.fileDescriptor)
            val channel = stream.channel
            val startOffset = fd.startOffset
            val declareLength = fd.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
        }

        /**
         * Returns the label from an output array.
         *
         * @param[out] The output arras as a [FloatBuffer].
         * @return The predicted [Label].
         */
        private fun getLabel(out: FloatBuffer): Label {
            var maxValue = out[0]
            var maxIdx = 0
            for (i in 1 until out.capacity()) {
                if (out[i] > maxValue) {
                    maxValue = out[i]
                    maxIdx = i
                }
            }
            return when (maxIdx) {
                1 -> Label.WASHING
                2 -> Label.RUBBING
                else -> Label.OTHER
            }
        }
    }

    private val mInterpreter: Interpreter

    /**
     * Constructs an instance of [GestureDetectorHelper] with
     * given [Context] and model name.
     *
     * @param[ctx] The [Context] to use.
     * @param[modelName] The name of the model to load.
     */
    constructor(ctx: Context, modelName: String) {
        // Load tflite model and init the interpreter
        val model = loadModel(ctx, modelName)
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
                Log.i(TAG, "Using GPU acceleration!")
            } else {
                Log.w(TAG, "GPU acceleration not supported on this device!")
            }
        }
        mInterpreter = Interpreter(model, options)

        Log.d(TAG, "loaded model '$modelName'")
        Log.d(TAG, "input")
        Log.d(TAG, "\tshape: ${mInterpreter.getInputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getInputTensor(0).dataType()}")
        Log.d(TAG, "output")
        Log.d(TAG, "\tshape: ${mInterpreter.getOutputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getOutputTensor(0).dataType()}")
    }

    /**
     * Constructs an instance of [GestureDetectorHelper] with
     * a pre-created [Interpreter].
     *
     * @param[interpreter] The pre-created [Interpreter] to use.
     */
    @VisibleForTesting
    constructor(interpreter: Interpreter) {
        mInterpreter = interpreter
    }

    /**
     * Predict a label from [input] data.
     *
     * @param[input] A [FloatArray] with raw accelerometer and
     * gyroscope data.
     * @return The predicted [Label].
     */
    fun predict(input: FloatArray): Label {
        val inputBuffer = FloatBuffer.wrap(input)
        val outputBuffer = FloatBuffer.allocate(6)

        mInterpreter.run(inputBuffer, outputBuffer)
        return getLabel(outputBuffer)
    }
}
