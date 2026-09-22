package com.lvlaanu.mediastarremote.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette taken from the physical handset: a near-black soft-touch body, a
 * slightly lighter moulded keypad, and saturated accents on the power and
 * colour keys.
 */
object RemoteColors {
    /** Body of the remote. */
    val Body = Color(0xFF121214)
    val BodyEdge = Color(0xFF232327)

    /** Screen background behind the remote. */
    val Backdrop = Color(0xFF07070A)

    /** Standard moulded key, and its pressed state. */
    val Key = Color(0xFF2A2A2F)
    val KeyPressed = Color(0xFF45454D)
    val KeyBorder = Color(0xFF3C3C44)

    /** Slightly raised keys: navigation ring, OK, keypad digits. */
    val KeyRaised = Color(0xFF34343B)
    val KeyRaisedPressed = Color(0xFF50505A)

    val Power = Color(0xFFD32F2F)
    val PowerPressed = Color(0xFFFF5252)

    val ColourRed = Color(0xFFE53935)
    val ColourGreen = Color(0xFF43A047)
    val ColourYellow = Color(0xFFFDD835)
    val ColourBlue = Color(0xFF1E88E5)

    /**
     * The four coloured keys, sampled from the handset. On this remote the
     * coloured keys are Audio, APP, Wifi and Info rather than a separate
     * red/green/yellow/blue strip.
     */
    val KeyAudio = Color(0xFFBE3A2B)
    val KeyAudioPressed = Color(0xFFE05243)
    val KeyApp = Color(0xFF4C9A3F)
    val KeyAppPressed = Color(0xFF6DBE5F)
    val KeyWifi = Color(0xFFDE8A1C)
    val KeyWifiPressed = Color(0xFFF5A73C)
    val KeyInfo = Color(0xFF2C6FB5)
    val KeyInfoPressed = Color(0xFF4A90D9)

    val LabelPrimary = Color(0xFFF2F2F4)
    val LabelSecondary = Color(0xFFA8A8B2)
    val LabelMuted = Color(0xFF6E6E78)

    /** Brand wordmark at the foot of the remote. */
    val Brand = Color(0xFFE8E8EC)

    val StatusOk = Color(0xFF4CAF50)
    val StatusWarn = Color(0xFFFFB300)
    val StatusError = Color(0xFFE53935)

    /** Ring shown briefly on a key that has no code assigned. */
    val Unmapped = Color(0xFF8A6D00)
}
