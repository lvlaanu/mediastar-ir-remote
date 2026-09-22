package com.lvlaanu.mediastarremote.ir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An infrared signal in one of the three forms this app can store.
 *
 * Everything eventually collapses to [IrBurst] (a carrier frequency plus an
 * alternating on/off pattern in microseconds), which is the only thing
 * `ConsumerIrManager.transmit` understands. Keeping the higher-level forms
 * around means the stored profile stays human-readable and editable.
 */
@Serializable
sealed interface IrSignal {

    /** Rendered form ready for the hardware. */
    fun toBurst(): IrBurst

    /**
     * The burst sent for an auto-repeat tick while a key is held down.
     * NEC has a dedicated 9 ms + 2.25 ms repeat frame; raw signals simply
     * resend themselves.
     */
    fun toRepeatBurst(): IrBurst = toBurst()

    /** Short human-readable description, used in the Learn/Settings screens. */
    fun describe(): String

    /**
     * Standard NEC: 8-bit address plus its bitwise complement, then 8-bit
     * command plus its complement. This is what the overwhelming majority of
     * low-cost DVB-S2 set-top boxes, MediaStar's included, actually use.
     */
    @Serializable
    @SerialName("nec")
    data class Nec(
        val address: Int,
        val command: Int,
        val extended: Boolean = false,
        val carrierHz: Int = NecEncoder.DEFAULT_CARRIER_HZ,
    ) : IrSignal {
        override fun toBurst(): IrBurst =
            NecEncoder.encode(address, command, extended, carrierHz)

        override fun toRepeatBurst(): IrBurst =
            NecEncoder.encodeRepeat(carrierHz)

        override fun describe(): String {
            val kind = if (extended) "NEC-ext" else "NEC"
            return "%s  addr=0x%02X  cmd=0x%02X".format(kind, address and 0xFFFF, command and 0xFF)
        }
    }

    /**
     * A raw alternating on/off pattern in microseconds. This is what Learn Mode
     * imports produce, and it can express any protocol at all.
     */
    @Serializable
    @SerialName("raw")
    data class Raw(
        val pattern: List<Int>,
        val carrierHz: Int = NecEncoder.DEFAULT_CARRIER_HZ,
    ) : IrSignal {
        override fun toBurst(): IrBurst = IrBurst(carrierHz, pattern.toIntArray())

        override fun describe(): String =
            "RAW  ${pattern.size} marks  @${carrierHz / 1000} kHz"
    }

    /**
     * Pronto (CCF) hex, the format used by RemoteCentral, JP1 and most
     * universal-remote databases. Stored verbatim so it round-trips on export.
     */
    @Serializable
    @SerialName("pronto")
    data class Pronto(val hex: String) : IrSignal {
        override fun toBurst(): IrBurst = ProntoCodec.decode(hex)

        override fun describe(): String {
            val words = hex.trim().split(Regex("\\s+")).size
            return "PRONTO  $words words"
        }
    }
}

/**
 * The hardware-level representation: a carrier frequency in Hz and an
 * alternating on/off duration list in microseconds, always starting with an
 * "on" mark and always of even length.
 */
data class IrBurst(val carrierHz: Int, val patternUs: IntArray) {

    /** Total airtime of this burst, used to pace the transmit queue. */
    val durationUs: Int get() = patternUs.sum()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrBurst) return false
        return carrierHz == other.carrierHz && patternUs.contentEquals(other.patternUs)
    }

    override fun hashCode(): Int = 31 * carrierHz + patternUs.contentHashCode()
}
