package com.lvlaanu.mediastarremote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors
import kotlinx.coroutines.delay

/** How long a key must be held before auto-repeat starts. */
private const val REPEAT_DELAY_MS = 420L

/**
 * Interval between auto-repeat ticks. One NEC frame occupies 110 ms, so
 * repeating on that boundary matches what the physical handset puts on the
 * air and keeps the receiver's own repeat detection happy.
 */
private const val REPEAT_INTERVAL_MS = 110L

/**
 * The gesture and feedback behaviour shared by every key on the handset.
 *
 * Press handling is written against the raw pointer stream rather than
 * `combinedClickable` because a remote needs the signal to go out on *press*,
 * not on release, and because auto-repeat has to begin while the finger is
 * still down.
 */
@Composable
private fun Modifier.remoteKeyGestures(
    enabled: Boolean,
    autoRepeat: Boolean,
    onPress: () -> Unit,
    onRepeat: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val currentPress by rememberUpdatedState(onPress)
    val currentRepeat by rememberUpdatedState(onRepeat)
    var held by remember { mutableStateOf(false) }

    // Auto-repeat lives in its own effect so it cancels cleanly when the
    // finger lifts, the key is disabled, or the composable leaves the tree.
    if (autoRepeat) {
        LaunchedEffect(held) {
            if (!held) return@LaunchedEffect
            delay(REPEAT_DELAY_MS)
            while (true) {
                currentRepeat()
                delay(REPEAT_INTERVAL_MS)
            }
        }
    }

    return this.pointerInput(enabled, autoRepeat) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            held = true
            onPressedChange(true)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            currentPress()

            // Returns on release or when the gesture is cancelled by a scroll.
            waitForUpOrCancellation()

            held = false
            onPressedChange(false)
        }
    }
}

/**
 * A moulded rubber key.
 *
 * @param label text printed on the key, drawn only when [icon] is null.
 * @param accent fill colour, defaulting to the standard dark key.
 * @param mapped false when the active profile has no code for this key, which
 *   dims the label so missing codes are obvious at a glance.
 */
@Composable
fun RemoteButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(10.dp),
    accent: Color = RemoteColors.Key,
    pressedAccent: Color = RemoteColors.KeyPressed,
    labelColor: Color = RemoteColors.LabelPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    minHeight: Dp = 34.dp,
    enabled: Boolean = true,
    mapped: Boolean = true,
    autoRepeat: Boolean = false,
    /** Draws the label beside the icon rather than in place of it, as on Rec. */
    labelWithIcon: Boolean = false,
    iconSize: Dp = 18.dp,
    iconTint: Color = labelColor,
    contentDescription: String = label,
    onPress: () -> Unit,
    onRepeat: () -> Unit = onPress,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        label = "keyScale",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .scale(scale)
            .clip(shape)
            .background(
                // A soft top-to-bottom gradient reads as a moulded key rather
                // than a flat rectangle, which is what sells the replica.
                Brush.verticalGradient(
                    listOf(
                        (if (pressed) pressedAccent else accent).lighten(0.10f),
                        if (pressed) pressedAccent else accent,
                    ),
                ),
            )
            .border(BorderStroke(1.dp, RemoteColors.KeyBorder.copy(alpha = 0.7f)), shape)
            .remoteKeyGestures(
                enabled = enabled,
                autoRepeat = autoRepeat,
                onPress = onPress,
                onRepeat = onRepeat,
                onPressedChange = { pressed = it },
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        val contentAlpha = if (mapped) 1f else 0.38f
        val showLabel = icon == null || labelWithIcon

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = contentAlpha),
                    modifier = Modifier.size(iconSize),
                )
            }
            if (showLabel) {
                Text(
                    text = label,
                    color = labelColor.copy(alpha = contentAlpha),
                    fontSize = fontSize,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = if (icon != null) 0.dp else 4.dp),
                )
            }
        }
    }
}

/** A round key, used for power and for the digits. */
@Composable
fun RoundRemoteButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = RemoteColors.KeyRaised,
    pressedAccent: Color = RemoteColors.KeyRaisedPressed,
    labelColor: Color = RemoteColors.LabelPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    enabled: Boolean = true,
    mapped: Boolean = true,
    autoRepeat: Boolean = false,
    iconSize: Dp = 18.dp,
    iconTint: Color = labelColor,
    contentDescription: String = label,
    onPress: () -> Unit,
    onRepeat: () -> Unit = onPress,
) {
    RemoteButton(
        label = label,
        modifier = modifier.aspectRatio(1f),
        icon = icon,
        shape = CircleShape,
        accent = accent,
        pressedAccent = pressedAccent,
        labelColor = labelColor,
        fontSize = fontSize,
        enabled = enabled,
        mapped = mapped,
        autoRepeat = autoRepeat,
        iconSize = iconSize,
        iconTint = iconTint,
        contentDescription = contentDescription,
        onPress = onPress,
        onRepeat = onRepeat,
    )
}

/**
 * A pill: a rounded key whose corner radius always equals half its height, so
 * it reads as a moulded oval at any size. Used for the arrow keys, Menu, Exit,
 * Sat and F1, all of which are ovals on the physical handset.
 */
@Composable
fun PillRemoteButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = RemoteColors.Key,
    pressedAccent: Color = RemoteColors.KeyPressed,
    labelColor: Color = RemoteColors.LabelPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 10.sp,
    minHeight: Dp = 24.dp,
    mapped: Boolean = true,
    autoRepeat: Boolean = false,
    iconSize: Dp = 16.dp,
    contentDescription: String = label,
    onPress: () -> Unit,
    onRepeat: () -> Unit = onPress,
) {
    RemoteButton(
        label = label,
        modifier = modifier,
        icon = icon,
        shape = RoundedCornerShape(percent = 50),
        accent = accent,
        pressedAccent = pressedAccent,
        labelColor = labelColor,
        fontSize = fontSize,
        minHeight = minHeight,
        mapped = mapped,
        autoRepeat = autoRepeat,
        iconSize = iconSize,
        contentDescription = contentDescription,
        onPress = onPress,
        onRepeat = onRepeat,
    )
}

/** A caption printed on the remote body under a key. */
@Composable
fun KeyCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = RemoteColors.LabelMuted,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Vertical stack of a key and its caption, used where labels sit below keys. */
@Composable
fun CaptionedKey(
    caption: String,
    modifier: Modifier = Modifier,
    key: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        key()
        KeyCaption(caption)
    }
}

/** Lightens a colour toward white, for the moulded-key gradient. */
private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)
