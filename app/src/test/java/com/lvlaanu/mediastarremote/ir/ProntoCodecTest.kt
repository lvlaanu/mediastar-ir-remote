package com.lvlaanu.mediastarremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProntoCodecTest {

    /** A textbook NEC frame in Pronto form: carrier 0x006D is ~38.4 kHz. */
    private val necPronto =
        "0000 006D 0022 0002 0155 00AA 0015 0015 0015 0015 0015 0040 0015 0015 " +
            "0015 0015 0015 0015 0015 0015 0015 0015 0015 0040 0015 0040 0015 0015 " +
            "0015 0040 0015 0040 0015 0040 0015 0040 0015 0040 0015 0015 0015 0015 " +
            "0015 0015 0015 0040 0015 0015 0015 0015 0015 0015 0015 0015 0015 0040 " +
            "0015 0040 0015 0040 0015 0015 0015 0040 0015 0040 0015 0040 0015 0040 " +
            "0015 0015 0015 0015 0015 0603 0155 0055 0015 0E42"

    @Test
    fun `decodes carrier close to 38 kHz`() {
        val burst = ProntoCodec.decode(necPronto)
        assertTrue(
            "carrier was ${burst.carrierHz}",
            abs(burst.carrierHz - 38_400) < 500,
        )
    }

    @Test
    fun `decodes the once sequence and gets a plausible NEC leader`() {
        val burst = ProntoCodec.decode(necPronto)
        // 0x0155 units at ~26 us per unit is roughly the 9 ms NEC leader.
        assertTrue("leader was ${burst.patternUs[0]}", burst.patternUs[0] in 8_500..9_500)
        assertTrue("space was ${burst.patternUs[1]}", burst.patternUs[1] in 4_200..4_800)
        // 0x0022 = 34 burst pairs.
        assertEquals(68, burst.patternUs.size)
    }

    @Test
    fun `round trips through encode`() {
        val original = NecEncoder.encode(address = 0x00, command = 0x0C)
        val hex = ProntoCodec.encode(original)
        val decoded = ProntoCodec.decode(hex)

        assertEquals(original.patternUs.size, decoded.patternUs.size)
        original.patternUs.forEachIndexed { index, expected ->
            val actual = decoded.patternUs[index]
            // Quantising to carrier cycles loses a little precision.
            assertTrue(
                "index $index expected ~$expected but was $actual",
                abs(actual - expected) <= expected / 50 + 30,
            )
        }
    }

    @Test
    fun `rejects unmodulated codes rather than mistransmitting them`() {
        assertThrows(ProntoCodec.ProntoFormatException::class.java) {
            ProntoCodec.decode("0100 0000 0001 0000 0155 00AA")
        }
    }

    @Test
    fun `rejects truncated codes`() {
        assertThrows(ProntoCodec.ProntoFormatException::class.java) {
            ProntoCodec.decode("0000 006D 0022 0000 0155 00AA")
        }
    }

    @Test
    fun `rejects non hex input`() {
        assertThrows(ProntoCodec.ProntoFormatException::class.java) {
            ProntoCodec.decode("hello world")
        }
    }
}
