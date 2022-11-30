package com.handmonitor.wear.sensors

import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.Semaphore

@ExtendWith(MockKExtension::class)
class SensorsDataTest {
    @MockK
    private lateinit var mPutSemaphore: Semaphore

    @MockK
    private lateinit var mGetSemaphore: Semaphore

    private var mAccArray: FloatArray = FloatArray(128 * 3) { 0.0f }
    private var mGyrArray: FloatArray = FloatArray(128 * 3) { 0.0f }

    @InjectMockKs(injectImmutable = true, overrideValues = true)
    private var mData = SensorsData()

    @BeforeEach
    fun setup() {
        every { mGetSemaphore.acquire() } just Runs
        every { mGetSemaphore.release() } just Runs
        every { mPutSemaphore.acquire() } just Runs
        every { mPutSemaphore.release() } just Runs
    }

    @Test
    fun `putAcc appends new data`() {
        mData.putAcc(floatArrayOf(1.0f, 2.0f, 3.0f))
        val expected = FloatArray(128 * 3) { 0.0f }
        expected[0] = 1.0f
        expected[1] = 2.0f
        expected[2] = 3.0f
        assertThat(mAccArray).isEqualTo(expected)
    }

    @Test
    fun `putGyro appends new data`() {
        mData.putGyro(floatArrayOf(4.0f, 5.0f, 6.0f))
        val expected = FloatArray(128 * 3) { 0.0f }
        expected[0] = 4.0f
        expected[1] = 5.0f
        expected[2] = 6.0f
        assertThat(mGyrArray).isEqualTo(expected)
    }

    @Test
    fun `putAcc unlock consumer when data is ready`() {
        for (i in 0 until 128 * 3) {
            mData.putAcc(floatArrayOf(0.0f, 0.0f, 0.0f))
        }
        verifyOrder {
            mGetSemaphore.release()
            mPutSemaphore.acquire()
        }
    }

    @Test
    fun `putGyro unlock consumer when data is ready`() {
        for (i in 0 until 128 * 3) {
            mData.putGyro(floatArrayOf(0.0f, 0.0f, 0.0f))
        }
        verifyOrder {
            mGetSemaphore.release()
            mPutSemaphore.acquire()
        }
    }

    @Test
    fun `getData returns data`() {
        var data = mData.getData()
        assertThat(data.size).isEqualTo(128 * 6)
        assertThat(data).isEqualTo(FloatArray(128 * 6) { 0.0f })

        mAccArray[0] = 1.0f
        mAccArray[1] = 2.0f
        mAccArray[2] = 3.0f
        mGyrArray[0] = 4.0f
        mGyrArray[1] = 5.0f
        mGyrArray[2] = 6.0f
        data = mData.getData()
        assertThat(data[0]).isEqualTo(1.0f)
        assertThat(data[1]).isEqualTo(2.0f)
        assertThat(data[2]).isEqualTo(3.0f)
        assertThat(data[3]).isEqualTo(4.0f)
        assertThat(data[4]).isEqualTo(5.0f)
        assertThat(data[5]).isEqualTo(6.0f)
    }

    @Test
    fun `getData locks the consumer for new data`() {
        mData.getData()
        verifyOrder {
            mGetSemaphore.acquire()
            mPutSemaphore.release()
        }
    }
}
