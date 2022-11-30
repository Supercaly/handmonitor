package com.handmonitor.wear.prediction

import com.google.common.truth.Truth.assertThat
import com.handmonitor.wear.data.Label
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.tensorflow.lite.Interpreter
import java.nio.FloatBuffer

@ExtendWith(MockKExtension::class)
class GestureDetectorHelperTest {
    @MockK
    private lateinit var mInterpreter: Interpreter

    @Test
    fun `predict runs the inference and returns a label`() {
        val out = slot<FloatBuffer>()
        every { mInterpreter.run(any(), capture(out)) } answers {
            out.captured.array()[1] = 1.0f
        }

        val gdh = spyk(GestureDetectorHelper(mInterpreter), recordPrivateCalls = true)
        val fakeData = FloatArray(128 * 6) { 0.0f }
        val predictedLabel = gdh.predict(fakeData)

        verifyOrder {
            mInterpreter.run(any(), any())
        }
        assertThat(predictedLabel).isEqualTo(Label.WASHING)
    }
}
