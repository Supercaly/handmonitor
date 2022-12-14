package com.handmonitor.sensorslib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorsListenerTest {
    companion object {
        private const val WINDOW_DUR_MS = 2_000
        private const val SAMPLING_P_MS = 20
        private const val TIMESTAMP_NS = 314159265358979000L
        private const val MS_TO_NS = 1_000_000
    }

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
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0
        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager

        every { mAcc.type } returns Sensor.TYPE_ACCELEROMETER
        every { mGyr.type } returns Sensor.TYPE_GYROSCOPE
    }

    @Test
    fun `startListening starts supported sensors`() {
        // Start listening with all sensors supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true

        var listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }

        // Start listening with only accelerometer supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns null
        listener = SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
        }

        // Start listening with only gyroscope supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        listener = SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }

    @Test
    fun `startListening skips multiple calls without stopListening`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }

        listener.startListening()
        verify(atMost = 1) {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }

    @Test
    fun `stopListening stops listening to sensors`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.stopListening()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
        }
    }

    @Test
    fun `startListening, stopListening and then startListening again`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)

        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
        listener.stopListening()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
        }
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }

    @Test
    fun `onSensorChanged appends new data from the correct sensor`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)

        val accEvent = mockSensorEvent(mAcc)
        listener.onSensorChanged(accEvent)
        verify {
            mSensorsData.putAcc(any())
        }

        val gyrEvent = mockSensorEvent(mGyr)
        listener.onSensorChanged(gyrEvent)
        verify {
            mSensorsData.putGyro(any())
        }
    }

    @Test
    fun `onSensorChanged discards events faster than the sampling rate`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        var timestamp = TIMESTAMP_NS

        // Send one accelerometer event to simulate the previous ones
        var accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify { mSensorsData.putAcc(any()) }

        // Accelerometer event faster than the sampling rate is discarded
        timestamp = TIMESTAMP_NS + 5_000_000
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(atMost = 1) { mSensorsData.putAcc(any()) }

        // Accelerometer event slightly faster than the sampling rate is discarded
        timestamp = TIMESTAMP_NS + ((SAMPLING_P_MS * MS_TO_NS) - (2 * MS_TO_NS) - 1)
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(atMost = 1) { mSensorsData.putAcc(any()) }

        // Send one gyroscope event to simulate the previous ones
        timestamp = TIMESTAMP_NS
        var gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify { mSensorsData.putGyro(any()) }

        // Gyroscope events faster than the sampling rate is discarded
        timestamp = TIMESTAMP_NS + 5_000_000
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(atMost = 1) { mSensorsData.putGyro(any()) }

        // Gyroscope event slightly faster than the sampling rate is discarded
        timestamp = TIMESTAMP_NS + ((SAMPLING_P_MS * MS_TO_NS) - (2 * MS_TO_NS) - 1)
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(atMost = 1) { mSensorsData.putGyro(any()) }
    }

    @Test
    fun `onSensorChanged accepts events in range of sampling rate`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        var timestamp = TIMESTAMP_NS

        // Send one accelerometer event to simulate the previous ones
        var accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify { mSensorsData.putAcc(any()) }

        // Accelerometer event on time is accepted
        timestamp = TIMESTAMP_NS + 20_000_000
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(exactly = 2) { mSensorsData.putAcc(any()) }

        // Accelerometer event in range is accepted
        timestamp += 20_000_000 + (2 * MS_TO_NS)
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(exactly = 3) { mSensorsData.putAcc(any()) }

        // Accelerometer event in range is accepted
        timestamp += 20_000_000 - (2 * MS_TO_NS)
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(exactly = 4) { mSensorsData.putAcc(any()) }

        // Send one gyroscope event to simulate the previous ones
        timestamp = TIMESTAMP_NS
        var gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify { mSensorsData.putGyro(any()) }

        // Accelerometer event on time is accepted
        timestamp = TIMESTAMP_NS + 20_000_000
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(exactly = 2) { mSensorsData.putGyro(any()) }

        // Accelerometer event in range is accepted
        timestamp += 20_000_000 + (2 * MS_TO_NS)
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(exactly = 3) { mSensorsData.putGyro(any()) }

        // Accelerometer event in range is accepted
        timestamp += 20_000_000 - (2 * MS_TO_NS)
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(exactly = 4) { mSensorsData.putGyro(any()) }
    }

    @Test
    fun `onSensorChanged accepts with hesitation events later than the sampling rate`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        var timestamp = TIMESTAMP_NS

        // Send one accelerometer event to simulate the previous ones
        var accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify { mSensorsData.putAcc(any()) }

        // Accelerometer event late is accepted
        timestamp = TIMESTAMP_NS + 25_000_000
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(exactly = 2) { mSensorsData.putAcc(any()) }
        verify(exactly = 1) { Log.d(any(), any()) }

        // Accelerometer event slightly later than the sampling rate is accepted
        timestamp += (SAMPLING_P_MS * MS_TO_NS) + (2 * MS_TO_NS) + 1
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(exactly = 3) { mSensorsData.putAcc(any()) }
        verify(exactly = 2) { Log.d(any(), any()) }

        // Send one gyroscope event to simulate the previous ones
        timestamp = TIMESTAMP_NS
        var gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify { mSensorsData.putGyro(any()) }

        // Gyroscope event late is accepted
        timestamp = TIMESTAMP_NS + 25_000_000
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(exactly = 2) { mSensorsData.putGyro(any()) }
        verify(exactly = 3) { Log.d(any(), any()) }

        // Gyroscope event slightly later than the sampling rate is accepted
        timestamp += (SAMPLING_P_MS * MS_TO_NS) + (2 * MS_TO_NS) + 1
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(exactly = 3) { mSensorsData.putGyro(any()) }
        verify(exactly = 4) { Log.d(any(), any()) }
    }

    @Test
    fun `onSensorChanged discards events at the same time or earlier`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        var timestamp = TIMESTAMP_NS

        // Send one accelerometer event to simulate the previous ones
        var accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify { mSensorsData.putAcc(any()) }

        // Accelerometer event at the same time than the last is discarded
        timestamp = TIMESTAMP_NS
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(atMost = 1) { mSensorsData.putAcc(any()) }

        // Accelerometer event at a time before the last is discarded
        timestamp = TIMESTAMP_NS - 5_000_000
        accEvent = mockSensorEvent(mAcc, timestamp = timestamp)
        listener.onSensorChanged(accEvent)
        verify(atMost = 1) { mSensorsData.putAcc(any()) }
        verify(exactly = 1) { Log.wtf(any(), any<String>()) }

        // Send one gyroscope event to simulate the previous ones
        timestamp = TIMESTAMP_NS
        var gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify { mSensorsData.putGyro(any()) }

        // Gyroscope event at the same time than the last is discarded
        timestamp = TIMESTAMP_NS
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(atMost = 1) { mSensorsData.putGyro(any()) }

        // Gyroscope event at a time before the last is discarded
        timestamp = TIMESTAMP_NS - 5_000_000
        gyroEvent = mockSensorEvent(mGyr, timestamp = timestamp)
        listener.onSensorChanged(gyroEvent)
        verify(atMost = 1) { mSensorsData.putGyro(any()) }
        verify(exactly = 2) { Log.wtf(any(), any<String>()) }
    }

    // Inspired by this StackOverflow post
    // https://stackoverflow.com/questions/2806976/how-can-i-unit-test-an-android-activity-that-acts-on-accelerometer
    // https://source.chromium.org/chromium/chromium/src/+/main:services/device/generic_sensor/android/junit/src/org/chromium/device/sensors/PlatformSensorAndProviderTest.java
    private fun mockSensorEvent(
        sensor: Sensor? = null,
        accuracy: Int = 0,
        timestamp: Long = 0L,
        values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    ): SensorEvent {
        val eventConstructor = SensorEvent::class.java.getDeclaredConstructor()
        eventConstructor.isAccessible = true
        val sensorEvent = eventConstructor.newInstance()

        sensorEvent.sensor = sensor
        sensorEvent.accuracy = accuracy
        sensorEvent.timestamp = timestamp

        val valuesField = sensorEvent.javaClass.getField("values")
        valuesField.isAccessible = true
        valuesField.set(sensorEvent, values)

        return sensorEvent
    }
}
