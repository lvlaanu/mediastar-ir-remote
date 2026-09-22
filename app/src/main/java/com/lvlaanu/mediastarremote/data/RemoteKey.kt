package com.lvlaanu.mediastarremote.data

/**
 * Every physical button on the MediaStar handset, in the order it appears from
 * the top of the remote downwards.
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

    // Row 2 — the four coloured function keys
    RED("Red", KeyGroup.COLOUR),
    GREEN("Green", KeyGroup.COLOUR),
    YELLOW("Yellow", KeyGroup.COLOUR),
    BLUE("Blue", KeyGroup.COLOUR),

    // Row 3
    AUDIO("Audio", KeyGroup.FUNCTION),
    APP("APP", KeyGroup.FUNCTION),
    WIFI("Wifi", KeyGroup.FUNCTION),
    INFO("Info", KeyGroup.FUNCTION),

    // Row 4
    EPG("EPG", KeyGroup.FUNCTION),
    ZOOM("Zoom", KeyGroup.FUNCTION),
    METER("Meter", KeyGroup.FUNCTION),
    TXT("TXT", KeyGroup.FUNCTION),

    // Rows 5 and 6 — transport controls
    REWIND("Rewind", KeyGroup.MEDIA, autoRepeat = true),
    PLAY("Play", KeyGroup.MEDIA),
    PAUSE("Pause", KeyGroup.MEDIA),
    FORWARD("Forward", KeyGroup.MEDIA, autoRepeat = true),
    PREVIOUS("Previous", KeyGroup.MEDIA),
    STOP("Stop", KeyGroup.MEDIA),
    RECORD("Record", KeyGroup.MEDIA),
    NEXT("Next", KeyGroup.MEDIA),

    // Row 7
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

    // Row 9
    SUB("Sub", KeyGroup.FUNCTION),
    FAV("Fav", KeyGroup.FUNCTION),
    PAGE_UP("Page +", KeyGroup.FUNCTION, autoRepeat = true),
    PAGE_DOWN("Page -", KeyGroup.FUNCTION, autoRepeat = true),
    RECALL("Recall", KeyGroup.FUNCTION),
    USB("USB", KeyGroup.FUNCTION),

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
