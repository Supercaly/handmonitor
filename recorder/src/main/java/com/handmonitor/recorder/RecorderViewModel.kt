package com.handmonitor.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.handmonitor.recorder.data.Action
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecorderViewModel(
    private val mRecorderRepository: RecorderRepository
) : ViewModel() {
    class RecorderViewModelFactory(private val repo: RecorderRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecorderViewModel(repo) as T
    }

    private val mIsRecording = MutableStateFlow(false)
    val isRecording = mIsRecording.asStateFlow()

    fun startRecording(action: Action.Type) {
        mIsRecording.value = true
        mRecorderRepository.startRecording(action)
    }

    fun stopRecording() {
        mIsRecording.value = false
        mRecorderRepository.stopRecording()
    }
}
