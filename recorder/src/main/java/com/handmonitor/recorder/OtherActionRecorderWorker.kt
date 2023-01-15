package com.handmonitor.recorder

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.handmonitor.recorder.data.Action
import com.handmonitor.sensorslib.SensorDataHandler
import com.handmonitor.sensorslib.SensorReaderHelper
import kotlinx.coroutines.runBlocking

class OtherActionRecorderWorker(
    ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {
    companion object {
        private const val TAG = "OtherActionRecorderWorker"
    }

    private val mDataHandler = object : SensorDataHandler {
        override fun onNewData(data: FloatArray) {
            Log.d(TAG, "onNewData:")
            mRecorderStorer.recordData(data)
        }
    }
    private val mSensorReaderHelper: SensorReaderHelper =
        SensorReaderHelper(
            applicationContext,
            mDataHandler,
            100,
            20
        )
    private val mRecorderStorer: RecorderStorer =
        RecorderStorer(
            applicationContext,
            Action.Type.Other
        )
    private val mRecorderPreferences: RecorderPreferences =
        RecorderPreferences(applicationContext)

    override fun doWork(): Result {
        Log.d(TAG, "doWork: record other action in background")

        if (mRecorderPreferences.isSomeoneRecording) {
            Log.w(TAG, "doWork: someone else is recording already")
            // TODO: Do a retry instead of a fail
            return Result.failure()
        }

        mRecorderPreferences.isSomeoneRecording = true
        mSensorReaderHelper.start()

        Thread.sleep(180_000)

        mSensorReaderHelper.stop()
        mRecorderStorer.stopRecording()
        runBlocking {
            mRecorderStorer.saveRecording()
        }

        mRecorderPreferences.isSomeoneRecording = false

        return Result.success()
    }
}
