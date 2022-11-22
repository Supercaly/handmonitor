package com.handmonitor.wear

import android.util.Log
import com.handmonitor.wear.sensors.SensorsConsumer
import com.handmonitor.wear.sensors.SensorsConsumerRn
import com.handmonitor.wear.sensors.SensorsData
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
    private val mMockData = floatArrayOf(0.0f, 1.1f, 2.2f, 3.3f)

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun verify_onNewData_called() {
        every { mSensorsData.getData() } returns floatArrayOf()
        val impl = object : SensorsConsumer {
            override fun onNewData(data: FloatArray) {
                throw InterruptedException()
            }
        }
        val runnable = SensorsConsumerRn(mSensorsData, impl)
        runnable.run()
        verify {
            mSensorsData.getData()
        }
    }

    @Test
    fun verify_onNewData_calledWithData() {
        every { mSensorsData.getData() } returns mMockData
        val impl = object : SensorsConsumer {
            override fun onNewData(data: FloatArray) {
                assertEquals(data, mMockData)
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