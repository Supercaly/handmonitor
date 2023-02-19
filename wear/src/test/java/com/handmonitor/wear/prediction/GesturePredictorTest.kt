package com.handmonitor.wear.prediction

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.SensorWindow
import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.data.Label
import com.handmonitor.wear.repository.HandEventsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.FloatBuffer

@ExtendWith(MockKExtension::class)
class GesturePredictorTest {
    @MockK
    private lateinit var mDetectorHelper: GestureDetectorHelper

    @MockK
    private lateinit var mHandEventsRepository: HandEventsRepository
    private lateinit var mPredictor: GesturePredictor
    private val mEvents = mutableListOf<HandEvent>()
    private val mMockWindow = SensorWindow(FloatBuffer.allocate(0))

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        mPredictor = GesturePredictor(mDetectorHelper, mHandEventsRepository)
        coEvery { mHandEventsRepository.addNewEvent(capture(mEvents)) } just Runs
    }

    @Test
    fun `onNewData predicts a label`() {
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(mMockWindow)
        verify {
            mDetectorHelper.predict(any())
        }
    }

    @Test
    fun `onNewData skips OTHER label when no event is open`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..5) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isEmpty()
    }

    @Test
    fun `onNewData keeps the event open with less than N OTHER labels`() {
        // Send some WASHING label to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()

        // Send 1 (less than N) OTHER labels
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(mMockWindow)
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()
    }

    @Test
    fun `onNewData closes the event after N OTHER labels`() {
        // Send some WASHING label to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()

        // Send more than N OTHER labels
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..6) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[0].nSamples).isEqualTo(6)
    }

    @Test
    fun `onNewData opens a new event after receiving WASHING label`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isEmpty()
        // Send a WASHING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(mMockWindow)
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()
    }

    @Test
    fun `onNewData opens a new event after receiving RUBBING label`() {
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isEmpty()
        // Send a RUBBING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        mPredictor.onNewData(mMockWindow)
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()
    }

    @Test
    fun `onNewData appends labels to an open event`() {
        // Send one WASHING label to open a new event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(mMockWindow)
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isTrue()
        assertThat(mEvents).isEmpty()

        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }
        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[0].nSamples).isEqualTo(11)
    }

    @Test
    fun `onNewData produces a WASHING event when it gets all WASING labels`() {
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[0].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces a RUBBING event when it gets all RUBBING labels`() {
        // Send some RUBBING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..9) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(mEvents[0].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces a WASHING event when WASHING labels are the majority`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more WASHING labels than RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..9) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[0].nSamples).isEqualTo(15)
    }

    @Test
    fun `onNewData produces a RUBBING event when RUBBING labels are the majority`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..9) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send less WASHING labels than RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(mEvents[0].nSamples).isEqualTo(15)
    }

    @Test
    fun `onNewData produces an event of type of the first label when there's the same number of WAHING and RUBBING labels`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send same WASHING labels as RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.RUBBING)
        assertThat(mEvents[0].nSamples).isEqualTo(10)

        // Send some WASHING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send same RUBBING labels as RUBBING
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(2)
        assertThat(mEvents[1].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[1].nSamples).isEqualTo(10)
    }

    @Test
    fun `onNewData produces an event even when there are some OTHER labels (less than N)`() {
        // Send some RUBBING to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..4) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send less than N OTHER labels to keep the event open
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(mMockWindow)
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send less than N OTHER labels to keep the event open
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(mMockWindow)
        mPredictor.onNewData(mMockWindow)
        // Send some WASHING labels to simulate an event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..5) {
            mPredictor.onNewData(mMockWindow)
        }
        // Send more than N OTHER labels to close the current event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(mMockWindow)
        }

        assertThat(mPredictor.hasOpenEvent).isFalse()
        assertThat(mEvents).isNotEmpty()
        assertThat(mEvents).hasSize(1)
        assertThat(mEvents[0].type).isEqualTo(HandEventType.WASHING)
        assertThat(mEvents[0].nSamples).isEqualTo(20)
    }
}
