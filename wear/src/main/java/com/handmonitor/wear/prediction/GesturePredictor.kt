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
 * This class implements the [SensorsConsumer]'s method [onNewData] to receive
 * new sensor data collected by the system in windows. On every call a label is
 * predicted from this data and the set of labels creates hand events.
 *
 * An hand event is created when we receive a label of type WASHING or RUBBING
 * and is considered open until those labels are received. When the label OTHER
 * is received more than [MAX_N_DIFFERENT_LABELS] the event is considered closed
 * and it's stored to the database using [HandEventsRepository].
 *
 * @constructor Creates an instance of [GesturePredictor] with given [Context].
 */
class GesturePredictor : SensorsConsumer {
    companion object {
        const val MAX_N_DIFFERENT_LABELS = 3
        private const val TAG = "GesturePredictor"
    }

    private val mDetectorHelper: GestureDetectorHelper

    // Current open event stuff
    private var mEventOpenType: HandEventType = HandEventType.WASHING
    private var mCurrentEventStartTime: Long = 0L
    private var mCloseCnt: Int = 0
    private var mOtherLabelsCnt: Int = 0
    private var mWashingLabelsCnt: Int = 0
    private var mRubbingLabelsCnt: Int = 0

    /**
     * This value is set to true if there is a current hand event being
     * collected, otherwise it's set to false.
     */
    val hasOpenEvent: Boolean
        get() = (mWashingLabelsCnt != 0) or (mRubbingLabelsCnt != 0)

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

        // The OTHER label is associated with two different counters:
        // - mOtherLabelsCnt counts the number of OTHER labels obtained in the middle
        //  of the hand event (for example the case where we have some WASHING, then
        //  some OTHER and then more WASHING)
        // - mCloseCnt counts the number of consecutive OTHER labels and it's used to
        //  trigger the closing of the hand event
        // mCloseCnt is incremented every time we got a OTHER label, then if we got a
        // WASHING or RUBBING label we add the value of mCloseCnt to mOtherLabelsCnt
        // and reset mCloseCnt to zero.
        when (predictedLabel) {
            // The label is OTHER
            Label.OTHER -> {
                // There's an open event?
                if (hasOpenEvent) {
                    // Increment OTHER counter
                    mCloseCnt++
                    // Check if we need to close the current event
                    if (mCloseCnt >= MAX_N_DIFFERENT_LABELS) {
                        closeCurrentEvent()
                    }
                }
            }
            // The label is WASHING
            Label.WASHING -> {
                // Open a new event if there's not one
                if (!hasOpenEvent) {
                    openNewEvent()
                    mEventOpenType = HandEventType.WASHING
                }
                // Append a new WASHING label
                mWashingLabelsCnt++
                // We got some OTHER in the middle of the event
                if (mCloseCnt > 0) {
                    mOtherLabelsCnt += mCloseCnt
                    mCloseCnt = 0
                }
            }
            // The label is RUBBING
            Label.RUBBING -> {
                // Open a new event if there's not one
                if (!hasOpenEvent) {
                    openNewEvent()
                    mEventOpenType = HandEventType.RUBBING
                }
                // Append a new RUBBING label
                mRubbingLabelsCnt++
                // We got some OTHER in the middle of the event
                if (mCloseCnt > 0) {
                    mOtherLabelsCnt += mCloseCnt
                    mCloseCnt = 0
                }
            }
        }
    }

    /**
     * Open a new hand event.
     * @see [closeCurrentEvent]
     */
    private fun openNewEvent() {
        mCurrentEventStartTime = System.currentTimeMillis()
        mCloseCnt = 0
        mOtherLabelsCnt = 0
        mWashingLabelsCnt = 0
        mRubbingLabelsCnt = 0
    }

    /**
     * Returns the [HandEventType] of the current open event
     * based on the type of labels collected so far.
     *
     * This method is called when closing the currently
     * olen event.
     *
     * @see [closeCurrentEvent]
     */
    private fun getEventType(): HandEventType =
        if (mWashingLabelsCnt > mRubbingLabelsCnt)
            HandEventType.WASHING
        else if (mRubbingLabelsCnt > mWashingLabelsCnt)
            HandEventType.RUBBING
        else
            mEventOpenType

    /**
     * Close the currently open hand event.
     *
     * @see [openNewEvent]
     */
    private fun closeCurrentEvent() {
        println("$mWashingLabelsCnt $mRubbingLabelsCnt $mOtherLabelsCnt")
        val event = HandEvent(
            0,
            getEventType(),
            (mWashingLabelsCnt + mRubbingLabelsCnt + mOtherLabelsCnt),
            mCurrentEventStartTime,
            System.currentTimeMillis()
        )

        mCloseCnt = 0
        mOtherLabelsCnt = 0
        mWashingLabelsCnt = 0
        mRubbingLabelsCnt = 0

        Log.d(TAG, "closeCurrentEvent: New HandEvent produced $event")
    }
}