package com.lvlaanu.mediastarremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NecEncoderTest {

    @Test
    fun `standard frame has leader, 32 bits, trailer and pad`() {
        val burst = NecEncoder.encode(address = 0x00, command = 0x0C)
        // 2 leader + 64 bit halves + 1 trailer mark + 1 pad space
        assertEquals(68, burst.patternUs.size)
        assertEquals(9_000, burst.patternUs[0])
        assertEquals(4_500, burst.patternUs[1])
        assertEquals(38_000, burst.carrierHz)
    }

    @Test
    fun `frame occupies the full 110 ms slot`() {
        val burst = NecEncoder.encode(address = 0x00, command = 0x0C)
        assertEquals(NecEncoder.FRAME_PERIOD_US, burst.durationUs)
    }

    @Test
    fun `pattern length stays even so the framework accepts it`() {
        for (command in 0..0xFF) {
            val burst = NecEncoder.encode(address = 0x00, command = command)
            assertEquals(0, burst.patternUs.size % 2)
        }
    }

    @Test
    fun `address byte is followed by its complement, LSB first`() {
        val burst = NecEncoder.encode(address = 0x01, command = 0x00)
        // First payload bit is LSB of 0x01, which is 1, so a long space.
        assertEquals(560, burst.patternUs[2])
        assertEquals(1_690, burst.patternUs[3])
        // Second bit is 0, so a short space.
        assertEquals(560, burst.patternUs[5])
    }

    @Test
    fun `extended frame sends 16 bits of address with no complement`() {
        val standard = NecEncoder.encode(address = 0x00, command = 0x0C, extended = false)
        val extended = NecEncoder.encode(address = 0x1234, command = 0x0C, extended = true)
        assertEquals(standard.patternUs.size, extended.patternUs.size)
        // Low byte 0x34 first: LSB is 0, so a short space.
        assertEquals(560, extended.patternUs[3])
    }

    @Test
    fun `repeat frame is the short ditto burst`() {
        val repeat = NecEncoder.encodeRepeat()
        assertEquals(4, repeat.patternUs.size)
        assertEquals(9_000, repeat.patternUs[0])
        assertEquals(2_250, repeat.patternUs[1])
        assertEquals(560, repeat.patternUs[2])
        assertEquals(NecEncoder.FRAME_PERIOD_US, repeat.durationUs)
    }

    @Test
    fun `address and command parsing accepts the common spellings`() {
        val expected = 0x00 to 0x0C
        assertEquals(expected, NecEncoder.parseAddressCommand("00 0C"))
        assertEquals(expected, NecEncoder.parseAddressCommand("0x00 0x0C"))
        assertEquals(expected, NecEncoder.parseAddressCommand("00,0C"))
        assertEquals(expected, NecEncoder.parseAddressCommand("000C"))
    }

    @Test
    fun `address and command parsing rejects junk`() {
        assertNull(NecEncoder.parseAddressCommand(""))
        assertNull(NecEncoder.parseAddressCommand("hello"))
        assertNull(NecEncoder.parseAddressCommand("0C"))
        assertNull(NecEncoder.parseAddressCommand("00 1FF"))
    }

    @Test
    fun `every duration is positive`() {
        val burst = NecEncoder.encode(address = 0xFF, command = 0xFF)
        assertTrue(burst.patternUs.all { it > 0 })
    }
}
