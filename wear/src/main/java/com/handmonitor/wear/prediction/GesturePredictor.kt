package com.handmonitor.wear.prediction

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.handmonitor.sensorslib.SensorWindow
import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.data.Label
import com.handmonitor.wear.database.AppDatabase
import com.handmonitor.wear.prediction.GesturePredictor.Companion.MAX_N_DIFFERENT_LABELS
import com.handmonitor.wear.repository.HandEventsRepository
import kotlinx.coroutines.runBlocking

/**
 * A gesture predictor.
 *
 * This class receives [SensorWindow]s and predicts a label from this data
 * creating an hand event.
 *
 * An hand event is created when we receive a label of type WASHING or RUBBING
 * and is considered open until those labels are received. When the label OTHER
 * is received more than [MAX_N_DIFFERENT_LABELS] the event is considered closed
 * and it's stored to the database using [HandEventsRepository].
 *
 * @constructor Creates an instance of [GesturePredictor] with given [Context].
 */
// TODO: Rework this class to better integrate with the Flow API.
class GesturePredictor {
    companion object {
        const val MAX_N_DIFFERENT_LABELS = 3
        private const val TAG = "GesturePredictor"
    }

    private val mDetectorHelper: GestureDetectorHelper
    private val mHandEventsRepository: HandEventsRepository
    private var mLastDataTime: Long = System.currentTimeMillis()

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
        mHandEventsRepository = HandEventsRepository(AppDatabase.getDatabase(ctx))
    }

    /**
     * Constructs an instance of [GesturePredictor] with
     * pre-existing [GestureDetectorHelper].
     */
    @VisibleForTesting
    constructor(detectorHelper: GestureDetectorHelper, handEventsRepository: HandEventsRepository) {
        mDetectorHelper = detectorHelper
        mHandEventsRepository = handEventsRepository
    }

    fun onNewData(data: SensorWindow) {
        // Predict the label using ML
        val predictedLabel = mDetectorHelper.predict(data.buffer)
        val time = System.currentTimeMillis() - mLastDataTime
        mLastDataTime = System.currentTimeMillis()
        Log.d(TAG, "onNewData: Predicted label '$predictedLabel' in '${time}ms'")

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
        if (mWashingLabelsCnt > mRubbingLabelsCnt) {
            HandEventType.WASHING
        } else if (mRubbingLabelsCnt > mWashingLabelsCnt) {
            HandEventType.RUBBING
        } else {
            mEventOpenType
        }

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
        // FIXME(#5): Using runBlocking will stop current thread waiting for the repository
        //  to call addNewEvent on a coroutine. This could be a valid solution since we are
        //  not on the main thread, but remember that the current thread will not ask for new
        //  data until this call is finished (that can take more than 2 seconds).
        runBlocking {
            mHandEventsRepository.addNewEvent(event)
        }
    }
}
