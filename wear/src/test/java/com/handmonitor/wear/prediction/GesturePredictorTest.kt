package com.handmonitor.wear.prediction

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.handmonitor.wear.repository.HandEventsRepository
import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.data.Label
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class GesturePredictorTest {
    @MockK
    private lateinit var mDetectorHelper: GestureDetectorHelper

    @MockK
    private lateinit var mHandEventsRepository: HandEventsRepository
    private lateinit var mPredictor: GesturePredictor
    private val events = mutableListOf<HandEvent>()

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        mPredictor = GesturePredictor(mDetectorHelper, mHandEventsRepository)
        coEvery { mHandEventsRepository.addNewEvent(capture(events)) } just Runs
    }

    @Test
    fun `onNewData predicts a label`() {
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(floatArrayOf())
        verify {
            mDetectorHelper.predict(any())
        }
    }

    @Test
    fun `onNewData skips OTHER label when no event is open`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isEmpty()
    }

    @Test
    fun `onNewData keeps the event open with less than N OTHER labels`() {
        // Send some WASHING label to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()

        // Send 1 (less than N) OTHER labels
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()
    }

    @Test
    fun `onNewData closes the event after N OTHER labels`() {
        // Send some WASHING label to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()

        // Send more than N OTHER labels
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..6) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[0].nSamples).isEqualTo(6)
    }

    @Test
    fun `onNewData opens a new event after receiving WASHING label`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isEmpty()
        // Send a WASHING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()
    }

    @Test
    fun `onNewData opens a new event after receiving RUBBING label`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isEmpty()
        // Send a RUBBING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()
    }

    @Test
    fun `onNewData appends labels to an open event`() {
        // Send one WASHING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(floatArrayOf())
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(events).isEmpty()

        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[0].nSamples).isEqualTo(11)
    }

    @Test
    fun `onNewData produces a WASHING event when it gets all WASING labels`() {
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[0].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces a RUBBING event when it gets all RUBBING labels`() {
        // Send some RUBBING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..9) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(events[0].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces a WASHING event when WASHING labels are the majority`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more WASHING labels than RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[0].nSamples).isEqualTo(15)
    }

    @Test
    fun `onNewData produces a RUBBING event when RUBBING labels are the majority`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..9) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send less WASHING labels than RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(events[0].nSamples).isEqualTo(15)
    }

    @Test
    fun `onNewData produces an event of type of the first label when there's the same number of WAHING and RUBBING labels`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send same WASHING labels as RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(events[0].nSamples).isEqualTo(10)

        // Send some WASHING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send same RUBBING labels as RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(2)
        assertThat(events[1].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[1].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces an event even when there are some OTHER labels (less than N)`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send less than N OTHER labels to keep the event open
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(floatArrayOf())
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send less than N OTHER labels to keep the event open
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(floatArrayOf())
        mPredictor.onNewData(floatArrayOf())
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(events).isNotEmpty()
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(events[0].nSamples).isEqualTo(20)
    }
}