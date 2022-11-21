package com.handmonitor.wear.prediction

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

/**
 * Helper for gesture detector.
 *
 * This class is an helper class for tflite.
 *
 * @property[mModelName] Name of the model to use for inference.
 * @constructor Creates an instance of [GestureDetectorHelper] with
 * given [Context] and model name.
 */
class GestureDetectorHelper(
    ctx: Context,
    private val mModelName: String
) {
    companion object {
        private const val TAG = "GestureDetectorHelper"
    }

    private val mInterpreter: Interpreter

    init {
        // Load tflite model and init the interpreter
        val model = loadModel(ctx)
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
                Log.i(TAG, "init: Using GPU acceleration!")
            } else {
                Log.i(TAG, "init: GPU acceleration not supported on this device!")
            }
        }
        mInterpreter = Interpreter(model, options)

        Log.i(TAG, "init: loaded model '$mModelName'")
        Log.i(TAG, "init: input")
        Log.i(TAG, "init: \tshape: ${Arrays.toString(mInterpreter.getInputTensor(0).shape())}")
        Log.i(TAG, "init: \ttype: ${mInterpreter.getInputTensor(0).dataType()}")
        Log.i(TAG, "init: output")
        Log.i(TAG, "init: \tshape: ${Arrays.toString(mInterpreter.getOutputTensor(0).shape())}")
        Log.i(TAG, "init: \ttype: ${mInterpreter.getOutputTensor(0).dataType()}")
    }

    /**
     * Predict a label from [input] data.
     *
     * @param[input] A [FloatArray] with raw accelerometer and
     * gyroscope data.
     * @return The predicted label.
     */
    fun predict(input: FloatArray): Int {
        val inputBuffer = FloatBuffer.wrap(input)
        val outputBuffer = FloatBuffer.allocate(6)

        mInterpreter.run(inputBuffer, outputBuffer)
        return getLabel(inputBuffer)
    }

    /**
     * Loads a model with given [mModelName] to a
     * [MappedByteBuffer].
     *
     * @param[ctx] A [Context] used to access assets folder.
     * @return A [MappedByteBuffer] with model data.
     */
    private fun loadModel(ctx: Context): MappedByteBuffer {
        val fd = ctx.assets.openFd(mModelName)
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        val startOffset = fd.startOffset
        val declareLength = fd.declaredLength
        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
    }

    /**
     * Returns the label form an output array.
     *
     * @param[out] The output arras as a [FloatBuffer].
     * @return The predicted label.
     */
    private fun getLabel(out: FloatBuffer): Int {
        var maxValue = out[0]
        var maxIdx = 0
        for (i in 1 until out.capacity()) {
            if (out[i] > maxValue) {
                maxValue = out[i]
                maxIdx = i
            }
        }
        return maxIdx
    }
}