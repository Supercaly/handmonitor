package com.handmonitor.recorder

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker.Result.Retry
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.handmonitor.recorder.data.Action
import com.handmonitor.sensorlib.v2.SensorWindowProducer
import kotlinx.coroutines.runBlocking

class OtherActionRecorderWorker(
    ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {
    companion object {
        private const val TAG = "OtherActionRecorderWorker"
    }

    private val mSensorWindowProducer: SensorWindowProducer =
        SensorWindowProducer(
            applicationContext,
            20L,
            100
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

        mSensorWindowProducer.setOnNewWindowListener {
            Log.d(TAG, "onNewData:")
            mRecordingStorer.recordWindow(it)
        }

        if (mRecorderPreferences.isSomeoneRecording) {
            Log.w(TAG, "doWork: someone else is recording already")
            return if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Retry.failure()
            }
        }

        mRecorderPreferences.isSomeoneRecording = true
        mSensorWindowProducer.startSensors()

        Thread.sleep(60_000)

        mSensorWindowProducer.stopSensors()
        mRecordingStorer.stopRecording()
        runBlocking {
            mRecordingStorer.saveRecording()
        }

        mRecorderPreferences.isSomeoneRecording = false

        return Result.success()
    }
}
