package com.handmonitor.recorder

import com.handmonitor.recorder.data.Action
import com.handmonitor.recorder.database.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

/**
 * This class is a repository that has the purpose
 * to help retrieve the stored information about
 * each recorded action.
 */
class RecorderRepository(database: AppDatabase) {
    /**
     * [Map] object mapping each action to it's [Action.TimeRange].
     *
     * This parameter is a [Flow] of maps that are automatically
     * updated every time the underlying data changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val actionsTime: Flow<Map<Action.Type, Action.TimeRange>> =
        combine(
            database.recordingDao().getRecordingsForAction(Action.Type.HandWash.name)
                .mapLatest {
                    it.fold(0L) { acc, rec ->
                        acc + rec.durationMs
                    }
                },
            database.recordingDao().getRecordingsForAction(Action.Type.HandRub.name).mapLatest {
                it.fold(0L) { acc, rec ->
                    acc + rec.durationMs
                }
            },
            database.recordingDao().getRecordingsForAction(Action.Type.Eat.name).mapLatest {
                it.fold(0L) { acc, rec ->
                    acc + rec.durationMs
                }
            },
            database.recordingDao().getRecordingsForAction(Action.Type.TeethBrush.name)
                .mapLatest {
                    it.fold(0L) { acc, rec ->
                        acc + rec.durationMs
                    }
                },
            database.recordingDao().getRecordingsForAction(Action.Type.FaceWash.name)
                .mapLatest {
                    it.fold(0L) { acc, rec ->
                        acc + rec.durationMs
                    }
                },
            database.recordingDao().getRecordingsForAction(Action.Type.Write.name).mapLatest {
                it.fold(0L) { acc, rec ->
                    acc + rec.durationMs
                }
            },
            database.recordingDao().getRecordingsForAction(Action.Type.Type.name).mapLatest {
                it.fold(0L) { acc, rec ->
                    acc + rec.durationMs
                }
            },
            database.recordingDao().getRecordingsForAction(Action.Type.Housework.name)
                .mapLatest {
                    it.fold(0L) { acc, rec ->
                        acc + rec.durationMs
                    }
                }
        ) { array ->
            mapOf(
                Action.Type.HandWash to Action.TimeRange(array[0]),
                Action.Type.HandRub to Action.TimeRange(array[1]),
                Action.Type.Eat to Action.TimeRange(array[2]),
                Action.Type.TeethBrush to Action.TimeRange(array[3]),
                Action.Type.FaceWash to Action.TimeRange(array[4]),
                Action.Type.Write to Action.TimeRange(array[5]),
                Action.Type.Type to Action.TimeRange(array[6]),
                Action.Type.Housework to Action.TimeRange(array[7])
            )
        }
}
