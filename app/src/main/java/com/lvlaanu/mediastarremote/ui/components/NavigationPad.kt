package com.lvlaanu.mediastarremote.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * The direction cluster: four oval arrow keys arranged in a diamond around a
 * round OK key, matching the handset.
 *
 * Positions are absolute fractions of the cluster's own width rather than
 * weights in a row, because the arrows are not the same shape as each other:
 * up and down are wide and flat, left and right are narrow and tall. Absolute
 * placement is the only way to keep that relationship at every screen size.
 *
 * Hit targets are rectangles, not arcs. Arcs photograph better but are
 * materially worse to hit with a thumb on glass, and the visual fidelity here
 * comes from the key shapes rather than from the hit geometry.
 */
@Composable
fun DirectionPad(
    isMapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Wider than tall, so the arrows sit close in around OK the way they do on
    // the handset rather than drifting to the corners of a square.
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1.25f),
        contentAlignment = Alignment.Center,
    ) {
        val span = maxWidth
        val armLong = span * 0.30f
        val armShort = span * 0.15f
        val okSize = span * 0.40f

        PillRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowUp,
            modifier = Modifier
                .width(armLong)
                .height(armShort)
                .align(Alignment.TopCenter),
            accent = RemoteColors.KeyRaised,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            minHeight = 0.dp,
            mapped = isMapped(RemoteKey.UP),
            autoRepeat = true,
            contentDescription = "Up",
            onPress = { onKey(RemoteKey.UP) },
            onRepeat = { onRepeat(RemoteKey.UP) },
        )

        PillRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowDown,
            modifier = Modifier
                .width(armLong)
                .height(armShort)
                .align(Alignment.BottomCenter),
            accent = RemoteColors.KeyRaised,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            minHeight = 0.dp,
            mapped = isMapped(RemoteKey.DOWN),
            autoRepeat = true,
            contentDescription = "Down",
            onPress = { onKey(RemoteKey.DOWN) },
            onRepeat = { onRepeat(RemoteKey.DOWN) },
        )

        PillRemoteButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            modifier = Modifier
                .width(armShort)
                .height(armLong)
                .align(Alignment.CenterStart),
            accent = RemoteColors.KeyRaised,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            minHeight = 0.dp,
            mapped = isMapped(RemoteKey.LEFT),
            autoRepeat = true,
            contentDescription = "Left",
            onPress = { onKey(RemoteKey.LEFT) },
            onRepeat = { onRepeat(RemoteKey.LEFT) },
        )

        PillRemoteButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = Modifier
                .width(armShort)
                .height(armLong)
                .align(Alignment.CenterEnd),
            accent = RemoteColors.KeyRaised,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            minHeight = 0.dp,
            mapped = isMapped(RemoteKey.RIGHT),
            autoRepeat = true,
            contentDescription = "Right",
            onPress = { onKey(RemoteKey.RIGHT) },
            onRepeat = { onRepeat(RemoteKey.RIGHT) },
        )

        RoundRemoteButton(
            label = "OK",
            modifier = Modifier.size(okSize),
            accent = RemoteColors.KeyRaised,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            fontSize = 15.sp,
            mapped = isMapped(RemoteKey.OK),
            contentDescription = "OK",
            onPress = { onKey(RemoteKey.OK) },
        )
    }
}

/** Convenience overload that fills the width it is given. */
@Composable
fun DirectionPadFill(
    isMapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) = DirectionPad(isMapped, onKey, onRepeat, Modifier.fillMaxWidth())
