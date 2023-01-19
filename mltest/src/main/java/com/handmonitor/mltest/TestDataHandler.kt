package com.handmonitor.mltest

import android.content.Context
import android.util.Log
import com.handmonitor.sensorslib.SensorDataHandler
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TestDataHandler(
    context: Context,
    private val onFinish: () -> Unit
) : SensorDataHandler {
    companion object {
        private const val TAG = "TestDataHandler"
        private const val MODEL_NAME = "conv1d_step0_100_512_16.tflite"
//        private const val MODEL_NAME = "lstm_step0_100_128_16.tflite"
//        private const val MODEL_NAME = "conv1d_lstm_step0_100_256_16.tflite"
//        private const val MODEL_NAME = "conv1d_BI_lstm_step0_100_1024_16.tflite"
        private const val OUTPUT_SIZE = 3
        private const val MAX_REPETITION = 100
    }

    private var mCurrentRepetition = 0
    private val mTimes: LongArray = LongArray(MAX_REPETITION)
    private val mInterpreter: Interpreter

    init {
        val fd = context.assets.openFd(MODEL_NAME)
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        val startOffset = fd.startOffset
        val declareLength = fd.declaredLength
        val model = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)

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

        Log.d(TAG, "loaded model '$MODEL_NAME'")
        Log.d(TAG, "input")
        Log.d(TAG, "\tshape: ${mInterpreter.getInputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getInputTensor(0).dataType()}")
        Log.d(TAG, "output")
        Log.d(TAG, "\tshape: ${mInterpreter.getOutputTensor(0).shape().contentToString()}")
        Log.d(TAG, "\ttype: ${mInterpreter.getOutputTensor(0).dataType()}")
    }

    override fun onNewData(data: FloatArray) {
        if (mCurrentRepetition < MAX_REPETITION) {
            val startTimeNs = System.nanoTime()

            val inputBuffer = FloatBuffer.wrap(data)
            val outputBuffer = FloatBuffer.allocate(OUTPUT_SIZE)
            mInterpreter.run(inputBuffer, outputBuffer)
            var maxValue = outputBuffer[0]
            var label = 0
            for (i in 1 until outputBuffer.capacity()) {
                if (outputBuffer[i] > maxValue) {
                    maxValue = outputBuffer[i]
                    label = i
                }
            }
            Log.d(TAG, "onNewData: $mCurrentRepetition: Predicted label $label")
            mTimes[mCurrentRepetition] = System.nanoTime() - startTimeNs
//            Log.d(TAG, "onNewData: ${mTimes[mCurrentRepetition]}")
        } else {
            val mean = mTimes.fold(0L) { a, v -> a + v } / MAX_REPETITION.toFloat()
            val std =
                sqrt(mTimes.fold(0.0) { a, v -> a + v * v - mean * mean } / MAX_REPETITION - 1)
            Log.d(TAG, "onNewData: mean: $mean, std: $std")
            onFinish()
        }
        mCurrentRepetition++
    }
}
