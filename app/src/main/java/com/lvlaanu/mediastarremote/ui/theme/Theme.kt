package com.lvlaanu.mediastarremote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The remote is a black object, so the app is dark in both system themes on
 * purpose. Dynamic colour is deliberately not used: recolouring the handset to
 * match a wallpaper would defeat the point of a visual replica.
 */
private val RemoteColorScheme = darkColorScheme(
    primary = RemoteColors.ColourBlue,
    onPrimary = RemoteColors.LabelPrimary,
    secondary = RemoteColors.KeyRaised,
    onSecondary = RemoteColors.LabelPrimary,
    error = RemoteColors.StatusError,
    background = RemoteColors.Backdrop,
    onBackground = RemoteColors.LabelPrimary,
    surface = RemoteColors.Body,
    onSurface = RemoteColors.LabelPrimary,
    surfaceVariant = RemoteColors.Key,
    onSurfaceVariant = RemoteColors.LabelSecondary,
    outline = RemoteColors.KeyBorder,
)

private val RemoteTypography = Typography(
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
)

@Composable
fun MediaStarTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RemoteColorScheme,
        typography = RemoteTypography,
        content = content,
    )
}
