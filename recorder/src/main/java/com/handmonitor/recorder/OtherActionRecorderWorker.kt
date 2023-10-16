package com.handmonitor.recorder

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker.Result.Retry
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.handmonitor.recorder.data.Action
import com.handmonitor.sensorlib.v1.SensorDataHandler
import com.handmonitor.sensorlib.v1.SensorReaderHelper
import com.handmonitor.sensorlib.v2.SensorWindow
import kotlinx.coroutines.runBlocking

class OtherActionRecorderWorker(
    ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {
    companion object {
        private const val TAG = "OtherActionRecorderWorker"
    }

    private val mSensorRecorder: SensorReaderHelper =
        SensorReaderHelper(
            applicationContext,
            object : SensorDataHandler {
                override fun onNewData(data: FloatArray) {
                    Log.d(TAG, "onNewData:")
                    mRecordingStorer.recordWindow(SensorWindow.fromArray(data))
                }
            },
            100,
            20,
        )
    private val mRecordingStorer: RecordingStorer =
        RecordingStorer(
            applicationContext,
            Action.Type.Other
        )
    private val mRecorderPreferences: RecorderPreferences =
        RecorderPreferences(applicationContext)

    override fun doWork(): Result {
        Log.d(TAG, "doWork: record other action in background")

        if (mRecorderPreferences.isSomeoneRecording) {
            Log.w(TAG, "doWork: someone else is recording already")
            return if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Retry.failure()
            }
        }

        mRecorderPreferences.isSomeoneRecording = true
        mSensorRecorder.start()

        Thread.sleep(60_000)

        mSensorRecorder.stop()
        mRecordingStorer.stopRecording()
        runBlocking {
            mRecordingStorer.saveRecording()
        }

        mRecorderPreferences.isSomeoneRecording = false

        return Result.success()
    }
}
