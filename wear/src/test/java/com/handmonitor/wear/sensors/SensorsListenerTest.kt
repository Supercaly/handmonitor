package com.handmonitor.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorsListenerTest {
    @MockK
    private lateinit var mContext: Context

    @MockK(relaxed = true)
    private lateinit var mSensorsData: SensorsData

    @MockK
    private lateinit var mSensorManager: SensorManager

    @MockK
    private lateinit var mAcc: Sensor

    @MockK
    private lateinit var mGyr: Sensor

    @MockK
    private lateinit var mHandler: Handler

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        every { mAcc.type } returns Sensor.TYPE_ACCELEROMETER
        every { mGyr.type } returns Sensor.TYPE_GYROSCOPE
    }

    @Test
    fun `init when all sensors are supported`() {
        SensorsListener(mContext, mSensorsData, mHandler)
        verifyAll {
            mContext.getSystemService(Context.SENSOR_SERVICE)
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        }
    }

    @Test
    fun `init when some sensor is not supported`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns null
        SensorsListener(mContext, mSensorsData, mHandler)
        verifyAll {
            mContext.getSystemService(Context.SENSOR_SERVICE)
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        }
    }

    @Test
    fun `startListening starts listening to sensors`() {
        SensorsListener(mContext, mSensorsData, mHandler).startListening()
        verify(exactly = 2) {
            mSensorManager.registerListener(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `startListening starts listening to supported sensors`() {
        every { mSensorManager.getDefaultSensor(any()) } returns null
        SensorsListener(mContext, mSensorsData, mHandler).startListening()
        verify(inverse = true) {
            mSensorManager.registerListener(any(), any(), any(), any(), mHandler)
        }
    }

    @Test
    fun `stopListening stops listening to sensors`() {
        val l = SensorsListener(mContext, mSensorsData, mHandler)
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } returns Unit
        l.stopListening()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
        }
    }
}
