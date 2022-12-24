package com.handmonitor.recorder

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class RecorderService : Service() {
    companion object {
        private const val TAG = "RecorderService"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
    }
}