package com.handmonitor.recorder

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.handmonitor.recorder.data.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Class that implements a [ViewModel] for the main
 * screens of the application.
 *
 * This view model helps all the views obtain the data
 * they need and manages the star/stop of the recording
 * process.
 *
 * @param[recorderRepository] Instance of [RecorderRepository].
 * @param[onStartService] Callback called every time the
 * [RecorderService] needs to start.
 */
class RecorderViewModel(
    recorderRepository: RecorderRepository,
    private val recorderPreferences: RecorderPreferences,
    private val onStartService: (Action.Type) -> Unit
) : ViewModel() {
    companion object {
        private const val TAG = "RecorderViewModel"
    }

    /**
     * Factory class that helps construct an instance
     * of [RecorderViewModel].
     *
     * @param[repository] Instance of [RecorderRepository].
     * @param[onStartService] Callback called every time the
     * [RecorderService] needs to start.
     */
    class RecorderViewModelFactory(
        private val repository: RecorderRepository,
        private val recorderPreferences: RecorderPreferences,
        private val onStartService: (Action.Type) -> Unit
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecorderViewModel(repository, recorderPreferences, onStartService) as T
    }

    private var mRecorderBinder: RecorderService.RecorderBinder? = null
    private val mShowRecordingScreen = MutableStateFlow(mRecorderBinder != null)
    private var mCounterCoroutineJob: Job? = null
    private val mElapsedTimeString = MutableStateFlow("00:00")
    private val mRecordedAction = MutableSharedFlow<Action.Type>(1)
    private val mShowConflictToast = MutableSharedFlow<Unit>(0)

    /**
     * Implement a [ServiceConnection] used to bind to a [RecorderService].
     */
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected: ")
            mRecorderBinder = service as RecorderService.RecorderBinder
            mShowRecordingScreen.value = true
            if (mRecorderBinder?.recordedAction != null) {
                mRecordedAction.tryEmit(mRecorderBinder!!.recordedAction!!)
            }
            if (mCounterCoroutineJob != null) {
                mCounterCoroutineJob!!.cancel()
            }
            mCounterCoroutineJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    mRecorderBinder?.elapsedTime?.collect {
                        val m = ((it / 1_000) / 60).toInt()
                        val s = ((it / 1_000) % 60).toInt()
                        mElapsedTimeString.value = "%02d:%02d".format(m, s)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: ")
            mRecorderBinder = null
            mShowRecordingScreen.value = false
        }
    }

    /**
     * Parameter that let the view know if the recording
     * screen needs to be displayed on screen.
     */
    val showRecordingScreen: StateFlow<Boolean> =
        mShowRecordingScreen.asStateFlow()

    /**
     * A String representing the time elapsed since the start
     * of a recording.
     */
    val elapsedTimeString: StateFlow<String> = mElapsedTimeString.asStateFlow()

    val recordedAction: SharedFlow<Action.Type?> = mRecordedAction.asSharedFlow()

    /**
     * Object that maps each recorded action kind with it's duration.
     */
    val actionsTime: StateFlow<Map<Action.Type, Action.TimeRange>> =
        recorderRepository.actionsTime.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            mapOf(
                Action.Type.HandWash to Action.TimeRange(0L),
                Action.Type.HandRub to Action.TimeRange(0L),
                Action.Type.Eating to Action.TimeRange(0L),
                Action.Type.TeethBrush to Action.TimeRange(0L),
                Action.Type.FaceWash to Action.TimeRange(0L),
                Action.Type.Writing to Action.TimeRange(0L),
                Action.Type.Typing to Action.TimeRange(0L),
                Action.Type.Housework to Action.TimeRange(0L)
            )
        )

    /**
     * Shared flow used to mark if we need to display
     * a recording conflict toast.
     */
    val showConflictToast: SharedFlow<Unit> =
        mShowConflictToast.asSharedFlow()

    /**
     * Start the recording process of an action.
     *
     * @param[action] [Action.Type] to record.
     */
    fun startRecording(action: Action.Type) {
        if (recorderPreferences.isSomeoneRecording) {
            Log.w(TAG, "startRecording: Someone else is recording at the moment!")
            viewModelScope.launch { mShowConflictToast.emit(Unit) }
        } else {
            onStartService(action)
        }
    }

    /**
     * Stop recording the current action.
     */
    fun stopRecording() {
        mRecorderBinder?.stopRecording()
    }

    /**
     * The uses confirms that he wants to save the current
     * recorded action.
     */
    fun confirmSave() {
        mShowRecordingScreen.value = false
        mRecorderBinder?.saveRecordedData()
    }

    /**
     * The uses don't want to store the last recorded
     * action and it must be discarded.
     */
    fun discardSave() {
        mShowRecordingScreen.value = false
        mRecorderBinder?.discardRecordedData()
    }
}
