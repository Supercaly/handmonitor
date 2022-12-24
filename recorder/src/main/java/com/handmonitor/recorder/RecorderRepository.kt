package com.handmonitor.recorder

import android.content.Context
import android.content.Intent
import com.handmonitor.recorder.data.Action

class RecorderRepository(private val mCtx: Context) {
    private var mRecordedAction: Action.Type? = null

    fun startRecording(action: Action.Type) {
        mRecordedAction = action
    }

    fun stopRecording() {
        mRecordedAction = null
    }
}