package com.handmonitor.recorder

import com.handmonitor.recorder.data.Action
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecorderRepository {
    companion object {
        private const val TAG = "RecorderRepository"
    }

    private val mActionsTime = MutableStateFlow(
        mutableMapOf(
            Action.Type.HandWash to Action.TimeRange(0.0f, 10.0f),
            Action.Type.HandRub to Action.TimeRange(0.0f, 10.0f),
            Action.Type.Eat to Action.TimeRange(0.0f, 10.0f),
            Action.Type.TeethBrush to Action.TimeRange(0.0f, 10.0f),
            Action.Type.FaceWash to Action.TimeRange(0.0f, 10.0f),
            Action.Type.Write to Action.TimeRange(0.0f, 10.0f),
            Action.Type.Type to Action.TimeRange(0.0f, 10.0f),
            Action.Type.Housework to Action.TimeRange(0.0f, 10.0f),
        )
    )
    val actionsTime: StateFlow<Map<Action.Type, Action.TimeRange>> =
        mActionsTime.asStateFlow()
}
