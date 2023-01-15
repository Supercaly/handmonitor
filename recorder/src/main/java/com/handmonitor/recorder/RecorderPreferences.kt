package com.handmonitor.recorder

import android.content.Context
import android.content.SharedPreferences

class RecorderPreferences(context: Context) {
    companion object {
        private const val PREFERENCE_FILE_NAME = "recording_info"
        private const val IS_SOMEONE_RECORDING_KEY = "is-someone-recording"
    }

    private val mSharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

    var isSomeoneRecording: Boolean
        get() = mSharedPreferences
            .getBoolean(IS_SOMEONE_RECORDING_KEY, false)
        set(value) {
            val editor = mSharedPreferences.edit()
            editor.putBoolean(IS_SOMEONE_RECORDING_KEY, value)
            editor.apply()
        }
}
