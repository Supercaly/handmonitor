package com.handmonitor.wear.sensors

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertArrayEquals(expected, mAccArray)
    }

    @Test
    fun `putGyro appends new data`() {
        mData.putGyro(floatArrayOf(4.0f, 5.0f, 6.0f))
        val expected = FloatArray(128 * 3) { 0.0f }
        expected[0] = 4.0f
        expected[1] = 5.0f
        expected[2] = 6.0f
        assertArrayEquals(expected, mGyrArray)
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
        assertEquals(data.size, 128 * 6)
        assertArrayEquals(FloatArray(128 * 6) { 0.0f }, data)

        mAccArray[0] = 1.0f
        mAccArray[1] = 2.0f
        mAccArray[2] = 3.0f
        mGyrArray[0] = 4.0f
        mGyrArray[1] = 5.0f
        mGyrArray[2] = 6.0f
        data = mData.getData()
        assertEquals(1.0f, data[0])
        assertEquals(2.0f, data[1])
        assertEquals(3.0f, data[2])
        assertEquals(4.0f, data[3])
        assertEquals(5.0f, data[4])
        assertEquals(6.0f, data[5])
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