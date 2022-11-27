package com.handmonitor.wear.prediction

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.data.Label
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class GesturePredictorTest {
    @MockK
    private lateinit var mDetectorHelper: GestureDetectorHelper
    private lateinit var mPredictor: GesturePredictor

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        mPredictor = GesturePredictor(mDetectorHelper)
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
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..5) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.lastEvent).isNull()
        assertThat(mPredictor.openEventType).isNull()
    }

    @Test
    fun `onNewData closes an event after n OTHER labels`() {
        // Send some WASHING labels to simulate a washing event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)

        // Send more than N OTHER labels to close the washing event
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNull()
        assertThat(mPredictor.lastEvent).isNotNull()
        assertThat(mPredictor.lastEvent!!.type).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent!!.duration).isEqualTo(4 * 2)
        assertThat(mPredictor.lastEvent!!.startTime).isGreaterThan(0L)
    }

    @Test
    fun `onNewData keeps event open with less than n OTHER labels`() {
        // Send some WASHING labels to simulate a washing event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)

        // Send 1 (less than N) OTHER labels to keep the washing event open
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData opens new wash event`() {
        // Send a WASHING labels to open a new washing event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData keeps wash event open until receives WASHING labels`() {
        // Send a WASHING label to open a new washing event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send some WASHING labels to simulate a washing event in process
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData keeps rub event open with less than n WASHING labels`() {
        // Send some RUBBING labels to simulate a rub event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send 1 (less than N) WASHING labels to keep the rub event open
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData closes rub event with more than n WASHING labels`() {
        // Send some RUBBING labels to simulate a rub event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send more than N WASHING labels to close the open rub event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNull()
        assertThat(mPredictor.lastEvent).isNotNull()
        assertThat(mPredictor.lastEvent!!.type).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent!!.duration).isEqualTo(4 * 2)
        assertThat(mPredictor.lastEvent!!.startTime).isGreaterThan(0L)
    }

    @Test
    fun `onNewData opens new rub event`() {
        // Send a RUBBING labels to open a new rub event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData keeps rub event open until receives RUBBING labels`() {
        // Send a RUBBING label to open a new rub event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send some RUBBING labels to simulate a rub event in process
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.RUBBING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData keeps wash event open with less than n RUBBING labels`() {
        // Send some WASHING labels to simulate a wash event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send 1 (less than N) RUBBING labels to keep the wash event open
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        mPredictor.onNewData(floatArrayOf())
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()
    }

    @Test
    fun `onNewData closes wash event with more than n RUBBING labels`() {
        // Send some WASHING labels to simulate a wash event
        every { mDetectorHelper.predict(any()) } returns Label.WASHING
        for (i in 0..3) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNotNull()
        assertThat(mPredictor.openEventType).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent).isNull()

        // Send more than N RUBBING labels to close the open wash event
        every { mDetectorHelper.predict(any()) } returns Label.RUBBING
        for (i in 0..GesturePredictor.MAX_N_DIFFERENT_LABELS) {
            mPredictor.onNewData(floatArrayOf())
        }
        assertThat(mPredictor.openEventType).isNull()
        assertThat(mPredictor.lastEvent).isNotNull()
        assertThat(mPredictor.lastEvent!!.type).isEqualTo(HandEventType.WASHING)
        assertThat(mPredictor.lastEvent!!.duration).isEqualTo(4 * 2)
        assertThat(mPredictor.lastEvent!!.startTime).isGreaterThan(0L)
    }
}