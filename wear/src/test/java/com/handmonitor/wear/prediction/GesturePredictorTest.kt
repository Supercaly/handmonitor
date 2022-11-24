package com.handmonitor.wear.prediction

import android.util.Log
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
        every { mDetectorHelper.predict(any()) } returns Label.OTHER
    }

    @Test
    fun `onNewData predicts a label`() {
        mPredictor.onNewData(floatArrayOf())

        verify {
            mDetectorHelper.predict(any())
        }
    }
}