package com.lvlaanu.mediastarremote.data

/**
 * Every physical button on the MediaStar handset, in the order it appears from
 * the top of the remote downwards.
 *
 * Note on the coloured keys: the handset has no separate red/green/yellow/blue
 * strip. The four coloured keys **are** Audio, APP, Wifi and Info, printed in
 * red, green, amber and blue respectively, which is the usual arrangement on
 * this class of receiver. They are modelled as one key each rather than as a
 * colour and a function.
 *
 * [label] is what is printed on the key, [group] drives the visual section it
 * is drawn in, and [autoRepeat] marks the keys that should keep sending while
 * held down.
 */
enum class RemoteKey(
    val label: String,
    val group: KeyGroup,
    val autoRepeat: Boolean = false,
) {
    // Row 1 — power and mute
    POWER("Power", KeyGroup.POWER),
    MUTE("Mute", KeyGroup.POWER),

    // Row 2 — the four coloured keys
    AUDIO("Audio", KeyGroup.COLOUR),
    APP("APP", KeyGroup.COLOUR),
    WIFI("Wifi", KeyGroup.COLOUR),
    INFO("Info", KeyGroup.COLOUR),

    // Row 3
    EPG("EPG", KeyGroup.FUNCTION),
    ZOOM("Zoom", KeyGroup.FUNCTION),
    METER("Meter", KeyGroup.FUNCTION),
    TXT("TXT", KeyGroup.FUNCTION),

    // Row 4 — transport, first line
    PLAY("Play", KeyGroup.MEDIA),
    STOP("Stop", KeyGroup.MEDIA),
    PAUSE("Pause", KeyGroup.MEDIA),
    RECORD("Rec", KeyGroup.MEDIA),

    // Row 5 — transport, second line
    PREVIOUS("Previous", KeyGroup.MEDIA),
    NEXT("Next", KeyGroup.MEDIA),
    REWIND("Rewind", KeyGroup.MEDIA, autoRepeat = true),
    FORWARD("Forward", KeyGroup.MEDIA, autoRepeat = true),

    // Row 6
    SAT("Sat", KeyGroup.FUNCTION),
    F1("F1", KeyGroup.FUNCTION),

    // Navigation cluster. Left/right double as volume and up/down as channel
    // on most MediaStar firmware, which is why they auto-repeat.
    UP("Up", KeyGroup.NAVIGATION, autoRepeat = true),
    DOWN("Down", KeyGroup.NAVIGATION, autoRepeat = true),
    LEFT("Left", KeyGroup.NAVIGATION, autoRepeat = true),
    RIGHT("Right", KeyGroup.NAVIGATION, autoRepeat = true),
    OK("OK", KeyGroup.NAVIGATION),
    MENU("Menu", KeyGroup.NAVIGATION),
    EXIT("Exit", KeyGroup.NAVIGATION),

    // Rows 8 and 9, with the tall two-way PAGE key on the right
    SUB("Sub", KeyGroup.FUNCTION),
    FAV("Fav", KeyGroup.FUNCTION),
    RECALL("Recall", KeyGroup.FUNCTION),
    USB("USB", KeyGroup.FUNCTION),
    PAGE_UP("Page up", KeyGroup.FUNCTION, autoRepeat = true),
    PAGE_DOWN("Page down", KeyGroup.FUNCTION, autoRepeat = true),

    // Numeric keypad
    NUM_1("1", KeyGroup.NUMERIC),
    NUM_2("2", KeyGroup.NUMERIC),
    NUM_3("3", KeyGroup.NUMERIC),
    NUM_4("4", KeyGroup.NUMERIC),
    NUM_5("5", KeyGroup.NUMERIC),
    NUM_6("6", KeyGroup.NUMERIC),
    NUM_7("7", KeyGroup.NUMERIC),
    NUM_8("8", KeyGroup.NUMERIC),
    NUM_9("9", KeyGroup.NUMERIC),
    NUM_0("0", KeyGroup.NUMERIC),

    // Bottom row, flanking the zero
    TV_RADIO("TV/R", KeyGroup.NUMERIC),
    TIMER("Timer", KeyGroup.NUMERIC),
    ;

    companion object {
        fun fromNameOrNull(name: String): RemoteKey? = entries.firstOrNull { it.name == name }
    }
}

/** Visual grouping, used only by the UI layer for styling. */
enum class KeyGroup {
    POWER,
    COLOUR,
    FUNCTION,
    MEDIA,
    NAVIGATION,
    NUMERIC,
}
