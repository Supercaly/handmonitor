package com.handmonitor.wear.sensors

import android.util.Log
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorsConsumerTest {
    @MockK
    private lateinit var mSensorsData: SensorsData

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun `onNewData called when new data is ready`() {
        val mockData = floatArrayOf(0.0f, 1.1f, 2.2f, 3.3f)
        every { mSensorsData.getData() } returns mockData

        val impl = object : SensorsConsumer {
            override fun onNewData(data: FloatArray) {
                assertEquals(mockData, data)
                throw InterruptedException()
            }
        }
        val runnable = SensorsConsumerRn(mSensorsData, impl)
        runnable.run()

        verify {
            mSensorsData.getData()
        }
    }
}