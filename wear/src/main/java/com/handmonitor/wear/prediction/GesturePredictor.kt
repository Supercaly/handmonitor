package com.handmonitor.wear.prediction

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.data.Label
import com.handmonitor.wear.sensors.SensorsConsumer

/**
 * A gesture predictor.
 *
 * This class implements the [SensorsConsumer] interface using
 * the obtained sensors data to extract hand-washing and
 * hand-rubbing events.
 *
 * @constructor Creates an instance of [GesturePredictor] with given [Context].
 */
class GesturePredictor : SensorsConsumer {
    companion object {
        const val MAX_N_DIFFERENT_LABELS = 3
        private const val TAG = "GesturePredictor"
    }

    private val mDetectorHelper: GestureDetectorHelper

    // TODO: Replace this list with data saved on a database
    private val mEvents: MutableList<HandEvent> = mutableListOf()

    // Current open event stuff
    private var mCurrentEventType: HandEventType? = null
    private var mNumDiffEvents: Int = 0
    private var mSamplesNum: Int = 0
    private var mEventStartTime: Long = 0L

    /**
     * Current open event type.
     * This is [HandEventType] or null.
     */
    val openEventType: HandEventType?
        get() = mCurrentEventType

    /**
     * Last closed event.
     * This is an instance of [HandEvent] or null in case
     * no event was ever closed.
     */
    val lastEvent: HandEvent?
        get() = mEvents.getOrNull(0)

    /**
     * Constructs an instance of [GesturePredictor] with given
     * [Context].
     */
    constructor(ctx: Context) {
        mDetectorHelper = GestureDetectorHelper(ctx, "model.tflite")
    }

    /**
     * Constructs an instance of [GesturePredictor] with
     * pre-existing [GestureDetectorHelper].
     */
    @VisibleForTesting
    constructor(detectorHelper: GestureDetectorHelper) {
        mDetectorHelper = detectorHelper
    }

    override fun onNewData(data: FloatArray) {
        // Predict the label using ML
        val predictedLabel = mDetectorHelper.predict(data)
        Log.d(TAG, "onNewData: Predicted label '$predictedLabel'")

        when (predictedLabel) {
            // The label is OTHER
            Label.OTHER -> {
                // There's an open event?
                if (mCurrentEventType != null) {
                    // Increment counter of different labels
                    mNumDiffEvents++
                    // If i got more than n different labels close the current event
                    if (mNumDiffEvents > MAX_N_DIFFERENT_LABELS) {
                        closeCurrentEvent()
                    }
                }
            }
            // The label is WASHING
            Label.WASHING -> {
                // Check current open event type
                when (mCurrentEventType) {
                    HandEventType.WASHING -> {
                        // Append to the current wash event
                        mSamplesNum++
                    }
                    HandEventType.RUBBING -> {
                        // Increment number of different labels
                        mNumDiffEvents++
                        // If i got more than n different labels close the current event
                        if (mNumDiffEvents > MAX_N_DIFFERENT_LABELS) {
                            closeCurrentEvent()
                        }
                    }
                    null -> {
                        // Open a new wash event
                        openNewEvent(HandEventType.WASHING)
                    }
                }
            }
            // The label is RUBBING
            Label.RUBBING -> {
                // Check current open event type
                when (mCurrentEventType) {
                    HandEventType.WASHING -> {
                        // Increment number of different labels
                        mNumDiffEvents++
                        // If i got more than n different labels close the current event
                        if (mNumDiffEvents > MAX_N_DIFFERENT_LABELS) {
                            closeCurrentEvent()
                        }
                    }
                    HandEventType.RUBBING -> {
                        // Append to the current rub event
                        mSamplesNum++
                    }
                    null -> {
                        // Open a new rub event
                        openNewEvent(HandEventType.RUBBING)
                    }
                }
            }
        }
    }

    /**
     * Open a new event.
     *
     * Create a new hand event appending all new labels
     * of the same type, until the event it's closed.
     *
     * @param[eventType] Type of hand event to create.
     * @see [closeCurrentEvent]
     */
    private fun openNewEvent(eventType: HandEventType) {
        mNumDiffEvents = 0
        mEventStartTime = System.currentTimeMillis()
        mSamplesNum = 1
        mCurrentEventType = eventType
    }

    /**
     * Close the currently open hand event.
     *
     * @see [openNewEvent]
     */
    private fun closeCurrentEvent() {
        // FIXME: HandEvent has int duration, but times are floats
        // TODO: Store the new event in a local database
        mEvents.add(
            HandEvent(
                openEventType!!,
                mSamplesNum * 2,
                mEventStartTime
            )
        )
        mCurrentEventType = null
        mNumDiffEvents = 0
        mEventStartTime = 0L
        mSamplesNum = 0
    }
}