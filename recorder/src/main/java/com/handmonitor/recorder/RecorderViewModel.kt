package com.handmonitor.recorder

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.handmonitor.recorder.data.Action
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecorderViewModel(
    recorderRepository: RecorderRepository,
    private val onStartService: (Action.Type) -> Unit
) : ViewModel() {
    companion object {
        private const val TAG = "RecorderViewModel"
    }

    class RecorderViewModelFactory(
        private val repo: RecorderRepository,
        private val onStartService: (Action.Type) -> Unit
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecorderViewModel(repo, onStartService) as T
    }

    private var mJob: Job? = null

    private var mRecorderBinder: RecorderService.RecorderBinder? = null
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected: ")
            mRecorderBinder = service as RecorderService.RecorderBinder
            mShowRecordingScreen.value = true
            if (mJob != null) {
                mJob!!.cancel()
            }
            mJob = viewModelScope.launch {
                mRecorderBinder!!.service.recordingTime.collect {
                    mRecordingTime.value = it
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: ")
            mRecorderBinder = null
        }
    }

    private val mShowRecordingScreen =
        MutableStateFlow(mRecorderBinder != null)
    val showRecordingScreen: StateFlow<Boolean> =
        mShowRecordingScreen.asStateFlow()

    private val mRecordingTime = MutableStateFlow(1000L)
    val recordingTimeString: StateFlow<String> =
        mRecordingTime.map {
            val m = ((it / 1_000) / 60).toInt()
            val s = ((it / 1_000) % 60).toInt()
            "%02d:%02d".format(m, s)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            "00:00"
        )

    val actionsTime: StateFlow<Map<Action.Type, Action.TimeRange>> =
        recorderRepository.actionsTime

    fun startRecording(action: Action.Type) {
        mShowRecordingScreen.value = true
        onStartService(action)
    }

    fun stopRecording() {
        mRecorderBinder?.service?.stopRecording()
    }

    fun confirmSave() {
        mShowRecordingScreen.value = false
        mRecorderBinder?.service?.saveRecordedData()
    }

    fun discardSave() {
        mShowRecordingScreen.value = false
        mRecorderBinder?.service?.discardRecordedData()
    }
}
