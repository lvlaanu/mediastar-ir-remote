package com.lvlaanu.mediastarremote.data

import com.lvlaanu.mediastarremote.data.RemoteKey.*
import com.lvlaanu.mediastarremote.ir.IrSignal

/**
 * The profiles the app ships with.
 *
 * ## Why there is no authoritative MediaStar code table
 *
 * MediaStar has never published IR codes for the MS-MINI family, and as of
 * this writing none of the public databases carry the handset: irdb has no
 * MediaStar manufacturer folder, RemoteCentral's only "Media Star" entry is an
 * unrelated Pinnacle PC tuner, and the LIRC remotes database has no matching
 * config. What *is* well established is the carrier and the protocol: these
 * receivers are built on the GX6605S / 1506-series chipsets, whose reference
 * designs use plain 8-bit NEC at 38 kHz with a user-editable keymap. Vendors
 * flashing those images pick their own command bytes, which is exactly why no
 * single published table would be right for every box anyway.
 *
 * So [candidateNec] below is a **starting guess, not a capture**. It uses the
 * most common arrangement for these boxes, a contiguous command block on
 * address 0x00 with the digits at their face value. Try it first, and if the
 * receiver ignores it use Discovery in Learn Mode to find the real address and
 * commands. Discovery writes its hits straight into [emptyLearned].
 */
object DefaultProfiles {

    const val CANDIDATE_ID = "builtin-nec-candidate-a"
    const val LEARNED_ID = "user-learned"

    /** The address Discovery starts from, and the one [candidateNec] uses. */
    const val DEFAULT_ADDRESS = 0x00

    /**
     * Command byte per button for the candidate profile.
     *
     * Digits sit at their face value (0 -> 0x00, 1 -> 0x01 ... 9 -> 0x09) and
     * every other key follows contiguously from 0x0A. Keeping the block
     * contiguous is deliberate: if one key from this profile turns out to work,
     * the rest are very likely to be nearby, which makes a sweep converge fast.
     */
    val candidateCommands: Map<RemoteKey, Int> = mapOf(
        NUM_0 to 0x00,
        NUM_1 to 0x01,
        NUM_2 to 0x02,
        NUM_3 to 0x03,
        NUM_4 to 0x04,
        NUM_5 to 0x05,
        NUM_6 to 0x06,
        NUM_7 to 0x07,
        NUM_8 to 0x08,
        NUM_9 to 0x09,

        POWER to 0x0A,
        MUTE to 0x0B,

        RED to 0x0C,
        GREEN to 0x0D,
        YELLOW to 0x0E,
        BLUE to 0x0F,

        AUDIO to 0x10,
        APP to 0x11,
        WIFI to 0x12,
        INFO to 0x13,

        EPG to 0x14,
        ZOOM to 0x15,
        METER to 0x16,
        TXT to 0x17,

        REWIND to 0x18,
        PLAY to 0x19,
        PAUSE to 0x1A,
        FORWARD to 0x1B,
        PREVIOUS to 0x1C,
        STOP to 0x1D,
        RECORD to 0x1E,
        NEXT to 0x1F,

        SAT to 0x20,
        F1 to 0x21,

        UP to 0x22,
        DOWN to 0x23,
        LEFT to 0x24,
        RIGHT to 0x25,
        OK to 0x26,
        MENU to 0x27,
        EXIT to 0x28,

        SUB to 0x29,
        FAV to 0x2A,
        PAGE_UP to 0x2B,
        PAGE_DOWN to 0x2C,
        RECALL to 0x2D,
        USB to 0x2E,

        TV_RADIO to 0x2F,
        TIMER to 0x30,
    )

    /** Builds the candidate profile on a given NEC address. */
    fun candidateNec(address: Int = DEFAULT_ADDRESS): CodeProfile = CodeProfile(
        id = CANDIDATE_ID,
        name = "MediaStar NEC candidate (0x%02X)".format(address),
        description = "Unverified starting guess: NEC, 38 kHz, address 0x%02X, contiguous commands. "
            .format(address) +
            "Use Learn Mode > Discovery to find your receiver's real codes.",
        verified = false,
        builtIn = true,
        codes = candidateCommands.entries.associate { (key, command) ->
            key.name to IrSignal.Nec(address = address, command = command)
        },
    )

    /** The profile Discovery and manual imports write into. */
    fun emptyLearned(): CodeProfile = CodeProfile(
        id = LEARNED_ID,
        name = "My MediaStar (learned)",
        description = "Codes you captured with Discovery or imported by hand.",
        verified = true,
        builtIn = false,
        codes = emptyMap(),
    )

    fun initialStore(): ProfileStore = ProfileStore(
        profiles = listOf(candidateNec(), emptyLearned()),
        activeProfileId = CANDIDATE_ID,
    )
}
