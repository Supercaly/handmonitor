package com.handmonitor.recorder.presentation

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
import com.handmonitor.recorder.RecorderViewModel
import com.handmonitor.recorder.presentation.theme.HandMonitorTheme

class MainActivity : ComponentActivity() {
    private val mRecorderRepository by lazy { RecorderRepository(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecorderApp(
                viewModel(
                    factory = RecorderViewModel
                        .RecorderViewModelFactory(mRecorderRepository)
                )
            )
        }
    }
}

@Composable
fun RecorderApp(recorderViewModel: RecorderViewModel) {
    val isRecording by recorderViewModel.isRecording.collectAsState()
    HandMonitorTheme {
        if (!isRecording) {
            ActionsList(
                onActionSelected = { action ->
                    recorderViewModel.startRecording(action)
                }
            )
        } else {
            RecordingScreen(
                onStopRecording = {
                    recorderViewModel.stopRecording()
                }
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
                RecorderRepository(LocalContext.current)
            )
        )
    )
}
