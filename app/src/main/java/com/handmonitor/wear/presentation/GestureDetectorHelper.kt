package com.handmonitor.wear.presentation

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class GestureDetectorHelper(
    ctx: Context,
    private val mModelName: String
) {
    companion object {
        private const val TAG = "GestureDetectorHelper"
    }

    private val mInterpreter: Interpreter

    init {
        val model = loadModel(ctx)
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                // TODO: Manage GPU support
                val delegateOptions = compatList.bestOptionsForThisDevice
                //this.addDelegate(GpuDelegate(delegateOptions))
                Log.e(TAG, "Using GPU acceleration!")
            } else {
                Log.e(TAG, "GPU Delegate not supported on this device!")
            }
        }
        mInterpreter = Interpreter(model, options)
    }

    private fun loadModel(ctx: Context): MappedByteBuffer {
        val fd = ctx.assets.openFd(mModelName)
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        val startOffset = fd.startOffset
        val declareLength = fd.declaredLength
        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
    }

    fun predict(input: FloatArray): Int {
        // TODO: Fix this code
        Log.d(
            TAG,
            "predict: input shape= ${Arrays.toString(mInterpreter.getInputTensor(0).shape())}"
        )
        Log.d(TAG, "predict: input type= ${mInterpreter.getInputTensor(0).dataType()}")
        Log.d(
            TAG,
            "predict: output shape= ${Arrays.toString(mInterpreter.getOutputTensor(0).shape())}"
        )
        Log.d(TAG, "predict: output type= ${mInterpreter.getOutputTensor(0).dataType()}")

        val inp = FloatArray(128*6) {0.0f}
        for (i in input.indices) {
            inp[i] = input[i]
        }
        val inputBuffer = FloatBuffer.wrap(inp)
        val outputBuffer = FloatBuffer.allocate(6)

        // run inference
        mInterpreter.run(inputBuffer, outputBuffer)

        // convert output buffer to label
        return getLabel(inputBuffer)
    }

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