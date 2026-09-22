package com.lvlaanu.mediastarremote.ir

import kotlin.math.roundToInt

/**
 * Pronto (CCF) hex converter.
 *
 * A learned Pronto code is a list of 4-digit hex words:
 *
 *   word 0 : format. 0000 = raw modulated (the only one this app transmits)
 *   word 1 : carrier, in units of 0.241246 us. carrierHz = 1_000_000 / (word1 * 0.241246)
 *   word 2 : burst-pair count of sequence 1 (the "once" sequence)
 *   word 3 : burst-pair count of sequence 2 (the "repeat" sequence)
 *   rest   : alternating mark/space durations, in carrier cycles
 *
 * Only format 0000 carries real timing. Format 0100 ("learned, unmodulated")
 * has no carrier and format 5000/5001 are database references that need a
 * lookup table this app does not ship, so those are rejected with a clear
 * message rather than silently mistransmitted.
 */
object ProntoCodec {

    /** The Pronto clock period, in microseconds. */
    private const val PRONTO_CLOCK_US = 0.241246

    class ProntoFormatException(message: String) : IllegalArgumentException(message)

    /**
     * Converts Pronto hex to a transmittable burst.
     *
     * When both an "once" and a "repeat" sequence are present the once
     * sequence is used, which is what a single button press should send.
     */
    fun decode(hex: String): IrBurst {
        val words = parseWords(hex)
        if (words.size < 4) throw ProntoFormatException("Pronto code needs at least 4 words")

        when (words[0]) {
            0x0000 -> Unit
            0x0100 -> throw ProntoFormatException(
                "Unmodulated Pronto (0100) has no carrier and cannot be sent by an IR blaster"
            )
            else -> throw ProntoFormatException(
                "Unsupported Pronto format 0x%04X, only 0000 (raw modulated) is supported".format(words[0])
            )
        }

        val carrierWord = words[1]
        if (carrierWord == 0) throw ProntoFormatException("Pronto carrier word is zero")
        val carrierHz = (1_000_000.0 / (carrierWord * PRONTO_CLOCK_US)).roundToInt()

        val oncePairs = words[2]
        val repeatPairs = words[3]
        val body = words.drop(4)

        val expected = (oncePairs + repeatPairs) * 2
        if (body.size < expected) {
            throw ProntoFormatException(
                "Pronto code is truncated: header declares $expected values, found ${body.size}"
            )
        }

        val chosen = if (oncePairs > 0) {
            body.take(oncePairs * 2)
        } else {
            body.drop(oncePairs * 2).take(repeatPairs * 2)
        }
        if (chosen.isEmpty()) throw ProntoFormatException("Pronto code contains no burst pairs")

        // Durations are carrier cycles; one cycle lasts carrierWord * clock us.
        val cycleUs = carrierWord * PRONTO_CLOCK_US
        val pattern = IntArray(chosen.size) { i ->
            (chosen[i] * cycleUs).roundToInt().coerceAtLeast(1)
        }
        return IrBurst(carrierHz, pattern)
    }

    /**
     * Renders a raw burst back to Pronto hex, so learned or swept codes can be
     * exported and shared with other remote apps and databases.
     */
    fun encode(burst: IrBurst): String {
        val carrierWord = (1_000_000.0 / (burst.carrierHz * PRONTO_CLOCK_US)).roundToInt()
        val cycleUs = carrierWord * PRONTO_CLOCK_US

        // Pronto pairs must be complete; drop a dangling trailing mark.
        val pairCount = burst.patternUs.size / 2
        val words = ArrayList<Int>(4 + pairCount * 2)
        words += 0x0000
        words += carrierWord
        words += pairCount
        words += 0x0000
        for (i in 0 until pairCount * 2) {
            words += (burst.patternUs[i] / cycleUs).roundToInt().coerceIn(1, 0xFFFF)
        }
        return words.joinToString(" ") { "%04X".format(it) }
    }

    private fun parseWords(hex: String): List<Int> {
        val tokens = hex.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
        return tokens.map { token ->
            token.toIntOrNull(16)
                ?: throw ProntoFormatException("'$token' is not a hex word")
        }
    }
}
