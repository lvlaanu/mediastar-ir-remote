package com.lvlaanu.mediastarremote.ir

/**
 * NEC protocol encoder.
 *
 * Timing is expressed in multiples of one 560 us logical unit, exactly as in
 * the original NEC/Renesas application notes:
 *
 *   Leader   : 9000 us mark, 4500 us space
 *   Logic "0": 560 us mark, 560 us space
 *   Logic "1": 560 us mark, 1690 us space
 *   Trailer  : 560 us mark
 *   Frame    : padded to 110 ms total
 *   Repeat   : 9000 us mark, 2250 us space, 560 us mark, padded to 110 ms
 *
 * Bit order is LSB first, and the payload is address, ~address, command,
 * ~command. In extended NEC the 16-bit address is sent verbatim with no
 * complement byte, which buys 8 more address bits at the cost of the
 * integrity check.
 */
object NecEncoder {

    const val DEFAULT_CARRIER_HZ = 38_000

    private const val UNIT_US = 560
    private const val LEADER_MARK_US = 9_000
    private const val LEADER_SPACE_US = 4_500
    private const val REPEAT_SPACE_US = 2_250
    private const val ONE_SPACE_US = 1_690

    /** NEC frames are defined to occupy a 110 ms slot including trailing idle. */
    const val FRAME_PERIOD_US = 110_000

    /**
     * Builds a complete NEC frame.
     *
     * @param address 8-bit address, or 16-bit when [extended] is true.
     * @param command 8-bit command.
     */
    fun encode(
        address: Int,
        command: Int,
        extended: Boolean = false,
        carrierHz: Int = DEFAULT_CARRIER_HZ,
    ): IrBurst {
        val pattern = ArrayList<Int>(68)
        pattern += LEADER_MARK_US
        pattern += LEADER_SPACE_US

        if (extended) {
            appendByte(pattern, address and 0xFF)
            appendByte(pattern, (address shr 8) and 0xFF)
        } else {
            val addr = address and 0xFF
            appendByte(pattern, addr)
            appendByte(pattern, addr.inv() and 0xFF)
        }

        val cmd = command and 0xFF
        appendByte(pattern, cmd)
        appendByte(pattern, cmd.inv() and 0xFF)

        // Trailing mark closes the last bit's space.
        pattern += UNIT_US
        padToFramePeriod(pattern)

        return IrBurst(carrierHz, pattern.toIntArray())
    }

    /**
     * The NEC "ditto" frame, sent every 110 ms while a key stays held. Using
     * this instead of resending the full frame is what makes volume and arrow
     * keys ramp smoothly on real receivers.
     */
    fun encodeRepeat(carrierHz: Int = DEFAULT_CARRIER_HZ): IrBurst {
        val pattern = arrayListOf(LEADER_MARK_US, REPEAT_SPACE_US, UNIT_US)
        padToFramePeriod(pattern)
        return IrBurst(carrierHz, pattern.toIntArray())
    }

    /** LSB-first byte, each bit as a mark/space pair. */
    private fun appendByte(out: MutableList<Int>, value: Int) {
        for (bit in 0 until 8) {
            out += UNIT_US
            out += if ((value shr bit) and 1 == 1) ONE_SPACE_US else UNIT_US
        }
    }

    /**
     * Pads the frame with a trailing space so the whole burst occupies the
     * full 110 ms NEC slot.
     *
     * This matters more than it looks: several Xiaomi/POCO IR HALs return from
     * `transmit()` as soon as the pattern is queued, so without the trailing
     * idle a fast double tap can put two frames back to back with no gap and
     * the receiver reads them as one malformed frame. The pattern length stays
     * even, which the framework requires.
     */
    private fun padToFramePeriod(pattern: MutableList<Int>) {
        val elapsed = pattern.sum()
        val tail = FRAME_PERIOD_US - elapsed
        pattern += if (tail > 0) tail else UNIT_US
    }

    /**
     * Parses "0x00 0x0C", "00 0C", "000C" or "0,12" into an address/command
     * pair. Returns null when the text is not a usable NEC pair.
     */
    fun parseAddressCommand(text: String): Pair<Int, Int>? {
        val tokens = text.trim()
            .split(Regex("[\\s,;:/]+"))
            .filter { it.isNotEmpty() }
            .map { it.removePrefix("0x").removePrefix("0X") }

        val values = when {
            tokens.size >= 2 -> tokens.take(2)
            tokens.size == 1 && tokens[0].length == 4 ->
                listOf(tokens[0].substring(0, 2), tokens[0].substring(2, 4))
            else -> return null
        }

        val address = values[0].toIntOrNull(16) ?: return null
        val command = values[1].toIntOrNull(16) ?: return null
        if (address !in 0..0xFFFF || command !in 0..0xFF) return null
        return address to command
    }
}
