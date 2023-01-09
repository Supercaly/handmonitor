package com.handmonitor.recorder.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handmonitor.recorder.RecorderRepository
import com.handmonitor.recorder.RecorderService
import com.handmonitor.recorder.RecorderViewModel
import com.handmonitor.recorder.database.AppDatabase
import com.handmonitor.recorder.presentation.theme.HandMonitorTheme

class MainActivity : ComponentActivity() {
    private val mRecorderViewModel: RecorderViewModel by lazy {
        RecorderViewModel(
            mRecorderRepository,
            onStartService = {
                // Start service and bind to it
                val intent = Intent(this, RecorderService::class.java)
                intent.putExtra("action-type", it.name)
                this.startService(intent)
                this.bindService(intent, mRecorderViewModel.serviceConnection, 0)
            },
        )
    }
    private val mRecorderRepository: RecorderRepository by lazy {
        RecorderRepository(
            AppDatabase.getDatabase(
                this
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecorderApp(mRecorderViewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to service if started
        this.bindService(
            Intent(this, RecorderService::class.java),
            mRecorderViewModel.serviceConnection,
            0
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from service
        this.unbindService(mRecorderViewModel.serviceConnection)
    }
}

@Composable
fun RecorderApp(recorderViewModel: RecorderViewModel) {
    val showRecordingScreen by recorderViewModel.showRecordingScreen.collectAsState()
    val actionsTime by recorderViewModel.actionsTime.collectAsState()

    HandMonitorTheme {
        if (showRecordingScreen) {
            RecordingScreen(
                recorderViewModel.recordingTimeString,
                onStopRecording = { recorderViewModel.stopRecording() },
                onConfirm = { recorderViewModel.confirmSave() },
                onDiscard = { recorderViewModel.discardSave() }
            )
        } else {
            ActionsList(
                actionsTime,
                onActionSelected = { recorderViewModel.startRecording(it) }
            )
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    RecorderApp(
        viewModel(
            factory = RecorderViewModel.RecorderViewModelFactory(
                RecorderRepository(AppDatabase.getDatabase(LocalContext.current))
            ) {}
        )
    )
}
