package com.handmonitor.mltest.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorslib.SensorWindowProducer
import com.handmonitor.sensorslib.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class TestService : Service() {
    companion object {
        private const val TAG = "TestService"
        private const val MAX_REPETITION = 20

        val models = listOf(
            "conv1d_lstm_step1_100_256_512.tflite",
            "conv1d_lstm_step1_100_256_1024.tflite",
            "conv1d_step1_100_256_1024.tflite",
            "conv1d_step1_100_1024_256.tflite",
            "lstm_step1_100_64_256.tflite",
            "lstm_step1_100_512_1024.tflite",
            // Add paths to models to test here
        )
    }

    private val mServiceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        mServiceScope.launch {
            withContext(Dispatchers.Default) {
                models.forEach { model ->
                    val mTimes = mutableListOf<Long>()
                    val helper = MlHelper(this@TestService, model)
                    var t = 0L
                    SensorWindowProducer(this@TestService, 20L, 100).asFlow()
                        .take(MAX_REPETITION)
                        .onEach {
                            val startTimeNs = System.nanoTime()
                            val label = helper.inference(it)
                            val time = System.nanoTime() - startTimeNs
                            mTimes.add(time)
                            Log.d(
                                TAG,
                                "onNewData: ${0} Predicted label $label: $time"
                            )
                            println("${System.currentTimeMillis() - t} ${it.hashCode()}")
                            t = System.currentTimeMillis()
                        }.onCompletion {
                            val mean =
                                mTimes.fold(0L) { a, v -> a + v } / MAX_REPETITION.toFloat()
                            val std =
                                sqrt(mTimes.fold(0.0) { a, v -> a + v * v - mean * mean } / MAX_REPETITION - 1)
                            Log.i(
                                TAG,
                                "onNewData: model $model"
                            )
                            Log.i(
                                TAG,
                                "onNewData: mean: ${mean / 1e6}ms std: ${std / 1e6}ms"
                            )
                        }.collect()
                }
            }
        }
        return START_STICKY
    }

/*
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        mServiceScope.launch {
            withContext(Dispatchers.Default) {
                mProducer.asFlow().take(MAX_REPETITION).withIndex().onEach {
                    val startTimeNs = System.nanoTime()
                    val label = mMlHelper.inference(it.value)
                    val time = System.nanoTime() - startTimeNs
                    mTimes.add(time)
                    Log.d(
                        TAG, "onNewData: ${it.index} Predicted label $label: $time"
                    )
                }.onCompletion {
                    val mean =
                        mTimes.fold(0L) { a, v -> a + v } / MAX_REPETITION.toFloat()
                    val std =
                        sqrt(mTimes.fold(0.0) { a, v -> a + v * v - mean * mean } / MAX_REPETITION - 1)
                    Log.i(
                        TAG, "onNewData: model $MODEL_NAME"
                    )
                    Log.i(
                        TAG, "onNewData: mean: ${mean / 1e6}ms std: ${std / 1e6}ms"
                    )
                    stopSelf()
                }.collect()
            }

        }
        return START_STICKY
    }
*/

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mServiceScope.cancel()
    }
}
