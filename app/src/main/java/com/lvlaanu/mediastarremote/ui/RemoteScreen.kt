package com.lvlaanu.mediastarremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ui.components.NavigationPad
import com.lvlaanu.mediastarremote.ui.components.RemoteButton
import com.lvlaanu.mediastarremote.ui.components.RoundRemoteButton
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * The handset itself.
 *
 * The whole remote is laid out in a single scrolling column of equal-weight
 * rows, so it keeps its proportions from a compact phone up to a tablet. Widths
 * come from weights rather than fixed dp for the same reason.
 */
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onOpenSettings: () -> Unit,
    onOpenLearn: () -> Unit,
) {
    val store by viewModel.store.collectAsState()
    val status by viewModel.status.collectAsState()
    val lastSent by viewModel.lastSent.collectAsState()
    val profile = store.active

    val mapped: (RemoteKey) -> Boolean = { key -> profile?.get(key) != null }
    val press: (RemoteKey) -> Unit = viewModel::press
    val repeat: (RemoteKey) -> Unit = viewModel::repeat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RemoteColors.Backdrop),
    ) {
        StatusBar(
            hasEmitter = viewModel.capability.hasEmitter,
            profileName = profile?.name ?: "No profile",
            verified = profile?.verified == true,
            onOpenSettings = onOpenSettings,
            onOpenLearn = onOpenLearn,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            RemoteBody(mapped = mapped, onKey = press, onRepeat = repeat)
        }

        AnimatedVisibility(visible = status != null || lastSent != null) {
            FooterReadout(
                statusText = status?.text,
                statusIsError = status?.isError == true,
                lastSentText = lastSent?.let { "${it.first.label}  ${it.second}" },
            )
        }
    }
}

/** Header strip: IR availability, active profile, and the two screen links. */
@Composable
private fun StatusBar(
    hasEmitter: Boolean,
    profileName: String,
    verified: Boolean,
    onOpenSettings: () -> Unit,
    onOpenLearn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RemoteColors.Body)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (hasEmitter) RemoteColors.StatusOk else RemoteColors.StatusError,
                    shape = CircleShape,
                ),
        )
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = if (hasEmitter) "IR blaster ready" else "No IR emitter on this device",
                color = if (hasEmitter) RemoteColors.LabelSecondary else RemoteColors.StatusError,
                fontSize = 11.sp,
            )
            Text(
                text = profileName + if (verified) "" else "  ·  unverified",
                color = RemoteColors.LabelMuted,
                fontSize = 10.sp,
            )
        }
        IconButton(onClick = onOpenLearn) {
            Text("Learn", color = RemoteColors.LabelSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = RemoteColors.LabelSecondary,
            )
        }
    }
}

/** Transient readout under the handset. */
@Composable
private fun FooterReadout(statusText: String?, statusIsError: Boolean, lastSentText: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RemoteColors.Body)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (statusText != null) {
            Text(
                text = statusText,
                color = if (statusIsError) RemoteColors.StatusError else RemoteColors.LabelSecondary,
                fontSize = 11.sp,
            )
        }
        if (lastSentText != null) {
            Text(text = lastSentText, color = RemoteColors.LabelMuted, fontSize = 10.sp)
        }
    }
}

/**
 * The moulded black body, drawn as a rounded card with the key rows inside.
 * Capped at 420 dp so it stays remote-shaped on a tablet instead of stretching.
 */
@Composable
private fun RemoteBody(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(RemoteColors.BodyEdge, RemoteColors.Body, RemoteColors.Body),
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .border(1.dp, RemoteColors.KeyBorder.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PowerRow(mapped, onKey)
        ColourRow(mapped, onKey)
        FunctionRowOne(mapped, onKey)
        FunctionRowTwo(mapped, onKey)
        TransportRows(mapped, onKey, onRepeat)
        SatRow(mapped, onKey)
        NavigationBlock(mapped, onKey, onRepeat)
        UtilityRows(mapped, onKey, onRepeat)
        NumericPad(mapped, onKey)
        BrandMark()
    }
}

// ------------------------------------------------------------------ key rows

@Composable
private fun PowerRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.PowerSettingsNew,
            modifier = Modifier.size(52.dp),
            accent = RemoteColors.Power,
            pressedAccent = RemoteColors.PowerPressed,
            mapped = mapped(RemoteKey.POWER),
            contentDescription = "Power",
            onPress = { onKey(RemoteKey.POWER) },
        )
        RoundRemoteButton(
            label = "",
            icon = Icons.Filled.VolumeOff,
            modifier = Modifier.size(52.dp),
            mapped = mapped(RemoteKey.MUTE),
            contentDescription = "Mute",
            onPress = { onKey(RemoteKey.MUTE) },
        )
    }
}

@Composable
private fun ColourRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    val colours = listOf(
        RemoteKey.RED to RemoteColors.ColourRed,
        RemoteKey.GREEN to RemoteColors.ColourGreen,
        RemoteKey.YELLOW to RemoteColors.ColourYellow,
        RemoteKey.BLUE to RemoteColors.ColourBlue,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colours.forEach { (key, colour) ->
            RemoteButton(
                label = "",
                modifier = Modifier.weight(1f).height(20.dp),
                shape = RoundedCornerShape(6.dp),
                accent = colour,
                pressedAccent = colour,
                mapped = mapped(key),
                contentDescription = key.label,
                onPress = { onKey(key) },
            )
        }
    }
}

@Composable
private fun FunctionRowOne(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) =
    EvenRow(listOf(RemoteKey.AUDIO, RemoteKey.APP, RemoteKey.WIFI, RemoteKey.INFO), mapped, onKey)

@Composable
private fun FunctionRowTwo(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) =
    EvenRow(listOf(RemoteKey.EPG, RemoteKey.ZOOM, RemoteKey.METER, RemoteKey.TXT), mapped, onKey)

@Composable
private fun TransportRows(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    val topRow = listOf(
        RemoteKey.REWIND to Icons.Filled.FastRewind,
        RemoteKey.PLAY to Icons.Filled.PlayArrow,
        RemoteKey.PAUSE to Icons.Filled.Pause,
        RemoteKey.FORWARD to Icons.Filled.FastForward,
    )
    val bottomRow = listOf(
        RemoteKey.PREVIOUS to Icons.Filled.SkipPrevious,
        RemoteKey.STOP to Icons.Filled.Stop,
        RemoteKey.RECORD to Icons.Filled.FiberManualRecord,
        RemoteKey.NEXT to Icons.Filled.SkipNext,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(topRow, bottomRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (key, icon) ->
                    RemoteButton(
                        label = key.label,
                        icon = icon,
                        modifier = Modifier.weight(1f),
                        labelColor = if (key == RemoteKey.RECORD) {
                            RemoteColors.ColourRed
                        } else {
                            RemoteColors.LabelPrimary
                        },
                        mapped = mapped(key),
                        autoRepeat = key.autoRepeat,
                        contentDescription = key.label,
                        onPress = { onKey(key) },
                        onRepeat = { onRepeat(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SatRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyCell(RemoteKey.SAT, mapped, onKey)
        Box(modifier = Modifier.weight(1f))
        KeyCell(RemoteKey.F1, mapped, onKey)
    }
}

/** Menu and Exit flanking the navigation ring. */
@Composable
private fun NavigationBlock(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteButton(
            label = RemoteKey.MENU.label,
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
            mapped = mapped(RemoteKey.MENU),
            onPress = { onKey(RemoteKey.MENU) },
        )
        Box(modifier = Modifier.weight(2f).aspectRatio(1f)) {
            NavigationPad(
                isMapped = mapped,
                onKey = onKey,
                onRepeat = onRepeat,
                modifier = Modifier.fillMaxSize(),
            )
        }
        RemoteButton(
            label = RemoteKey.EXIT.label,
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
            mapped = mapped(RemoteKey.EXIT),
            onPress = { onKey(RemoteKey.EXIT) },
        )
    }
}

/** Sub / Fav / Recall / USB with the page arrows down the middle column. */
@Composable
private fun UtilityRows(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyCell(RemoteKey.SUB, mapped, onKey)
            RemoteButton(
                label = "PAGE ▲",
                modifier = Modifier.weight(1f),
                mapped = mapped(RemoteKey.PAGE_UP),
                autoRepeat = true,
                contentDescription = "Page up",
                onPress = { onKey(RemoteKey.PAGE_UP) },
                onRepeat = { onRepeat(RemoteKey.PAGE_UP) },
            )
            KeyCell(RemoteKey.FAV, mapped, onKey)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyCell(RemoteKey.RECALL, mapped, onKey)
            RemoteButton(
                label = "PAGE ▼",
                modifier = Modifier.weight(1f),
                mapped = mapped(RemoteKey.PAGE_DOWN),
                autoRepeat = true,
                contentDescription = "Page down",
                onPress = { onKey(RemoteKey.PAGE_DOWN) },
                onRepeat = { onRepeat(RemoteKey.PAGE_DOWN) },
            )
            KeyCell(RemoteKey.USB, mapped, onKey)
        }
    }
}

@Composable
private fun NumericPad(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    val rows = listOf(
        listOf(RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3),
        listOf(RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6),
        listOf(RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { key ->
                    RoundRemoteButton(
                        label = key.label,
                        modifier = Modifier.weight(1f),
                        mapped = mapped(key),
                        onPress = { onKey(key) },
                    )
                }
            }
        }
        // Bottom row: TV/R and Timer flank the zero, as on the handset.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundRemoteButton(
                label = RemoteKey.TV_RADIO.label,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                mapped = mapped(RemoteKey.TV_RADIO),
                onPress = { onKey(RemoteKey.TV_RADIO) },
            )
            RoundRemoteButton(
                label = RemoteKey.NUM_0.label,
                modifier = Modifier.weight(1f),
                mapped = mapped(RemoteKey.NUM_0),
                onPress = { onKey(RemoteKey.NUM_0) },
            )
            RoundRemoteButton(
                label = RemoteKey.TIMER.label,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                mapped = mapped(RemoteKey.TIMER),
                onPress = { onKey(RemoteKey.TIMER) },
            )
        }
    }
}

/** The wordmark printed at the foot of the handset. */
@Composable
private fun BrandMark() {
    Text(
        text = "Mediastar",
        color = RemoteColors.Brand,
        fontSize = 20.sp,
        fontWeight = FontWeight.Light,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}

// ------------------------------------------------------------------- helpers

/** One evenly weighted key in a row. */
@Composable
private fun RowScope.KeyCell(
    key: RemoteKey,
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
) {
    RemoteButton(
        label = key.label,
        modifier = Modifier.weight(1f),
        mapped = mapped(key),
        autoRepeat = key.autoRepeat,
        onPress = { onKey(key) },
    )
}

/** A row of equally sized text keys. */
@Composable
private fun EvenRow(
    keys: List<RemoteKey>,
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keys.forEach { key -> KeyCell(key, mapped, onKey) }
    }
}
