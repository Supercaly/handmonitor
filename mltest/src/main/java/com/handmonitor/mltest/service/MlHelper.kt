package com.handmonitor.mltest.service

import android.content.Context
import android.util.Log
import com.handmonitor.sensorlib.v2.SensorWindow
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MlHelper(context: Context, name: String) {
    companion object {
        private const val TAG = "MlHelper"
        private const val OUTPUT_SIZE = 3

        fun loadModel(context: Context, name: String): MappedByteBuffer {
            val fd = context.assets.openFd(name)
            val stream = FileInputStream(fd.fileDescriptor)
            val channel = stream.channel
            val startOffset = fd.startOffset
            val declareLength = fd.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
        }
    }

    private val mInterpreter: Interpreter

    init {
        val model = loadModel(context, name)
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

        Log.d(TAG, "loaded model '$name'")
        Log.d(TAG, "input")
        Log.d(TAG, "\tshape: ${mInterpreter.getInputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getInputTensor(0).dataType()}")
        Log.d(TAG, "output")
        Log.d(TAG, "\tshape: ${mInterpreter.getOutputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getOutputTensor(0).dataType()}")
    }

    fun inference(window: SensorWindow): Int {
        val outputBuffer = FloatBuffer.allocate(OUTPUT_SIZE)
        mInterpreter.run(window.buffer, outputBuffer)
        var maxValue = outputBuffer[0]
        var label = 0
        for (i in 1 until outputBuffer.capacity()) {
            if (outputBuffer[i] > maxValue) {
                maxValue = outputBuffer[i]
                label = i
            }
        }
        return label
    }
}
