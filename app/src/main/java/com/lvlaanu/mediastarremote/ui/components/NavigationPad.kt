package com.lvlaanu.mediastarremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * The circular navigation ring with OK at its centre.
 *
 * Built as four arrow keys positioned around the edge of a drawn ring rather
 * than as true arc-shaped hit areas. Arc hit-testing looks more faithful in a
 * screenshot but is materially worse to use on glass, where a thumb wants a
 * generous rectangular target. The ring artwork carries the visual fidelity and
 * the square targets carry the ergonomics.
 */
@Composable
fun NavigationPad(
    isMapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        val arrowSize = diameter * 0.26f
        val okSize = diameter * 0.36f
        val edgeInset = diameter * 0.03f

        // The ring itself: a raised dish with a darker centre well.
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RemoteColors.KeyRaised,
                            RemoteColors.Key,
                            RemoteColors.BodyEdge,
                        ),
                    ),
                )
                .border(1.dp, RemoteColors.KeyBorder, CircleShape),
        )

        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowUp,
            modifier = Modifier
                .size(arrowSize)
                .align(Alignment.TopCenter)
                .padding(top = edgeInset),
            accent = RemoteColors.KeyRaised,
            mapped = isMapped(RemoteKey.UP),
            autoRepeat = true,
            contentDescription = "Up",
            onPress = { onKey(RemoteKey.UP) },
            onRepeat = { onRepeat(RemoteKey.UP) },
        )

        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowDown,
            modifier = Modifier
                .size(arrowSize)
                .align(Alignment.BottomCenter)
                .padding(bottom = edgeInset),
            accent = RemoteColors.KeyRaised,
            mapped = isMapped(RemoteKey.DOWN),
            autoRepeat = true,
            contentDescription = "Down",
            onPress = { onKey(RemoteKey.DOWN) },
            onRepeat = { onRepeat(RemoteKey.DOWN) },
        )

        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowLeft,
            modifier = Modifier
                .size(arrowSize)
                .align(Alignment.CenterStart)
                .padding(start = edgeInset),
            accent = RemoteColors.KeyRaised,
            mapped = isMapped(RemoteKey.LEFT),
            autoRepeat = true,
            contentDescription = "Left",
            onPress = { onKey(RemoteKey.LEFT) },
            onRepeat = { onRepeat(RemoteKey.LEFT) },
        )

        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardArrowRight,
            modifier = Modifier
                .size(arrowSize)
                .align(Alignment.CenterEnd)
                .padding(end = edgeInset),
            accent = RemoteColors.KeyRaised,
            mapped = isMapped(RemoteKey.RIGHT),
            autoRepeat = true,
            contentDescription = "Right",
            onPress = { onKey(RemoteKey.RIGHT) },
            onRepeat = { onRepeat(RemoteKey.RIGHT) },
        )

        RoundRemoteButton(
            label = "OK",
            modifier = Modifier.size(okSize),
            accent = RemoteColors.Key,
            pressedAccent = RemoteColors.KeyRaisedPressed,
            fontSize = 15.sp,
            mapped = isMapped(RemoteKey.OK),
            contentDescription = "OK",
            onPress = { onKey(RemoteKey.OK) },
        )
    }
}

/** Fills its parent, used to keep the pad square inside a row. */
@Composable
fun NavigationPadFill(
    isMapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) = NavigationPad(isMapped, onKey, onRepeat, Modifier.fillMaxSize())
