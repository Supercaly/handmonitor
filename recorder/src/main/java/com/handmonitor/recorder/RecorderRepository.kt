package com.handmonitor.recorder

import android.content.Context
import android.content.Intent
import com.handmonitor.recorder.data.Action

class RecorderRepository(private val mCtx: Context) {
    private val mIntent by lazy { Intent(mCtx, RecorderService::class.java) }
    private var mRecordedAction: Action.Type? = null

    fun startRecording(action: Action.Type) {
        mRecordedAction = action
        mCtx.startService(mIntent)
    }

    fun stopRecording() {
        mRecordedAction = null
        mCtx.stopService(mIntent)
    }
}
