package com.lvlaanu.mediastarremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ui.components.DirectionPad
import com.lvlaanu.mediastarremote.ui.components.PillRemoteButton
import com.lvlaanu.mediastarremote.ui.components.RemoteButton
import com.lvlaanu.mediastarremote.ui.components.RoundRemoteButton
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * The handset itself, laid out to match the physical remote row for row.
 *
 * The whole body is one scrolling column capped at a remote-like width, and
 * every key sizes itself from row weights rather than fixed dp, so the
 * proportions hold from a compact phone up to a tablet.
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
 * The moulded black body. Capped at 360 dp so it keeps the tall, narrow
 * proportions of the real handset instead of stretching on a large screen.
 */
@Composable
private fun RemoteBody(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    0f to RemoteColors.BodyEdge,
                    0.06f to RemoteColors.Body,
                    1f to RemoteColors.Body,
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .border(1.dp, RemoteColors.KeyBorder.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
            .padding(horizontal = 18.dp)
            .padding(top = 26.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        PowerRow(mapped, onKey)
        ColourRow(mapped, onKey)
        FunctionRow(mapped, onKey)
        TransportRows(mapped, onKey, onRepeat)
        SatRow(mapped, onKey)
        NavigationBlock(mapped, onKey, onRepeat)
        UtilityBlock(mapped, onKey, onRepeat)
        NumericPad(mapped, onKey)
        BrandMark()
    }
}

// --------------------------------------------------------------- row models

/** One of the four coloured keys and the two faces it is drawn with. */
private data class ColourKey(val key: RemoteKey, val face: Color, val pressed: Color)

/** One transport key. [showLabel] draws the legend beside the glyph, as on Rec. */
private data class MediaKey(
    val key: RemoteKey,
    val icon: ImageVector,
    val showLabel: Boolean = false,
)

/** Height of the Sub/Fav/Recall/USB block, which the PAGE key spans. */
private val UTILITY_BLOCK_HEIGHT = 88.dp

// ------------------------------------------------------------------ key rows

/** Power on the left, mute on the right, both round. */
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
            modifier = Modifier.size(34.dp),
            accent = RemoteColors.Power,
            pressedAccent = RemoteColors.PowerPressed,
            iconSize = 17.dp,
            mapped = mapped(RemoteKey.POWER),
            contentDescription = "Power",
            onPress = { onKey(RemoteKey.POWER) },
        )
        RoundRemoteButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.VolumeOff,
            modifier = Modifier.size(34.dp),
            iconSize = 17.dp,
            mapped = mapped(RemoteKey.MUTE),
            contentDescription = "Mute",
            onPress = { onKey(RemoteKey.MUTE) },
        )
    }
}

/**
 * The four coloured keys. On this handset they carry the Audio, APP, Wifi and
 * Info functions rather than being a bare red/green/yellow/blue strip.
 */
@Composable
private fun ColourRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    val colours = listOf(
        ColourKey(RemoteKey.AUDIO, RemoteColors.KeyAudio, RemoteColors.KeyAudioPressed),
        ColourKey(RemoteKey.APP, RemoteColors.KeyApp, RemoteColors.KeyAppPressed),
        ColourKey(RemoteKey.WIFI, RemoteColors.KeyWifi, RemoteColors.KeyWifiPressed),
        ColourKey(RemoteKey.INFO, RemoteColors.KeyInfo, RemoteColors.KeyInfoPressed),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        colours.forEach { entry ->
            RemoteButton(
                label = entry.key.label,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(7.dp),
                accent = entry.face,
                pressedAccent = entry.pressed,
                fontSize = 10.sp,
                minHeight = 26.dp,
                mapped = mapped(entry.key),
                onPress = { onKey(entry.key) },
            )
        }
    }
}

/** EPG, Zoom, Meter, TXT. */
@Composable
private fun FunctionRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        listOf(RemoteKey.EPG, RemoteKey.ZOOM, RemoteKey.METER, RemoteKey.TXT).forEach { key ->
            RemoteButton(
                label = key.label,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(7.dp),
                fontSize = 10.sp,
                minHeight = 26.dp,
                mapped = mapped(key),
                onPress = { onKey(key) },
            )
        }
    }
}

/**
 * The two transport rows. Play, stop, pause and record on the first line,
 * track skip and scan on the second, exactly as on the handset.
 */
@Composable
private fun TransportRows(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    val rows = listOf(
        listOf(
            MediaKey(RemoteKey.PLAY, Icons.Filled.PlayArrow),
            MediaKey(RemoteKey.STOP, Icons.Filled.Stop),
            MediaKey(RemoteKey.PAUSE, Icons.Filled.Pause),
            MediaKey(RemoteKey.RECORD, Icons.Filled.FiberManualRecord, showLabel = true),
        ),
        listOf(
            MediaKey(RemoteKey.PREVIOUS, Icons.Filled.SkipPrevious),
            MediaKey(RemoteKey.NEXT, Icons.Filled.SkipNext),
            MediaKey(RemoteKey.REWIND, Icons.Filled.FastRewind),
            MediaKey(RemoteKey.FORWARD, Icons.Filled.FastForward),
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { entry ->
                    RemoteButton(
                        label = entry.key.label,
                        icon = entry.icon,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        fontSize = 9.sp,
                        minHeight = 26.dp,
                        iconSize = if (entry.showLabel) 10.dp else 16.dp,
                        iconTint = if (entry.key == RemoteKey.RECORD) {
                            RemoteColors.ColourRed
                        } else {
                            RemoteColors.LabelPrimary
                        },
                        labelWithIcon = entry.showLabel,
                        mapped = mapped(entry.key),
                        autoRepeat = entry.key.autoRepeat,
                        contentDescription = entry.key.label,
                        onPress = { onKey(entry.key) },
                        onRepeat = { onRepeat(entry.key) },
                    )
                }
            }
        }
    }
}

/** Sat at the far left, F1 at the far right, both small pills. */
@Composable
private fun SatRow(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillRemoteButton(
            label = RemoteKey.SAT.label,
            modifier = Modifier.width(44.dp),
            minHeight = 22.dp,
            mapped = mapped(RemoteKey.SAT),
            onPress = { onKey(RemoteKey.SAT) },
        )
        PillRemoteButton(
            label = RemoteKey.F1.label,
            modifier = Modifier.width(44.dp),
            minHeight = 22.dp,
            mapped = mapped(RemoteKey.F1),
            onPress = { onKey(RemoteKey.F1) },
        )
    }
}

/**
 * The direction cluster with Menu and Exit tucked into the bottom corners,
 * which is where they sit on the handset.
 */
@Composable
private fun NavigationBlock(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        DirectionPad(
            isMapped = mapped,
            onKey = onKey,
            onRepeat = onRepeat,
            modifier = Modifier.fillMaxWidth(0.68f),
        )
        PillRemoteButton(
            label = RemoteKey.MENU.label,
            modifier = Modifier.width(44.dp).align(Alignment.BottomStart),
            minHeight = 22.dp,
            mapped = mapped(RemoteKey.MENU),
            onPress = { onKey(RemoteKey.MENU) },
        )
        PillRemoteButton(
            label = RemoteKey.EXIT.label,
            modifier = Modifier.width(44.dp).align(Alignment.BottomEnd),
            minHeight = 22.dp,
            mapped = mapped(RemoteKey.EXIT),
            onPress = { onKey(RemoteKey.EXIT) },
        )
    }
}

/**
 * Sub and Recall down the left, Fav and USB as round keys in the middle, and
 * the tall two-way PAGE key spanning both rows on the right.
 */
@Composable
private fun UtilityBlock(
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    // A fixed block height rather than IntrinsicSize.Min: the PAGE key has to
    // span both rows, and a concrete height makes that exact at every size
    // without asking the layout for intrinsic measurements.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UTILITY_BLOCK_HEIGHT)
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            RemoteButton(
                label = RemoteKey.SUB.label,
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(7.dp),
                fontSize = 10.sp,
                minHeight = 0.dp,
                mapped = mapped(RemoteKey.SUB),
                onPress = { onKey(RemoteKey.SUB) },
            )
            RemoteButton(
                label = RemoteKey.RECALL.label,
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(7.dp),
                fontSize = 10.sp,
                minHeight = 0.dp,
                mapped = mapped(RemoteKey.RECALL),
                onPress = { onKey(RemoteKey.RECALL) },
            )
        }

        Column(
            modifier = Modifier.weight(0.85f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RoundRemoteButton(
                label = RemoteKey.FAV.label,
                modifier = Modifier.size(38.dp),
                fontSize = 10.sp,
                mapped = mapped(RemoteKey.FAV),
                onPress = { onKey(RemoteKey.FAV) },
            )
            RoundRemoteButton(
                label = RemoteKey.USB.label,
                modifier = Modifier.size(38.dp),
                fontSize = 10.sp,
                mapped = mapped(RemoteKey.USB),
                onPress = { onKey(RemoteKey.USB) },
            )
        }

        PageKey(
            modifier = Modifier.weight(0.85f).fillMaxHeight(),
            mapped = mapped,
            onKey = onKey,
            onRepeat = onRepeat,
        )
    }
}

/**
 * The tall PAGE key: one moulded body carrying two switches, page up at the
 * top and page down at the bottom, with the legend between them.
 */
@Composable
private fun PageKey(
    modifier: Modifier,
    mapped: (RemoteKey) -> Boolean,
    onKey: (RemoteKey) -> Unit,
    onRepeat: (RemoteKey) -> Unit,
) {
    Column(
        modifier = modifier
            .background(RemoteColors.Key, RoundedCornerShape(19.dp))
            .border(1.dp, RemoteColors.KeyBorder.copy(alpha = 0.7f), RoundedCornerShape(19.dp))
            .padding(3.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardDoubleArrowUp,
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            accent = RemoteColors.Key,
            pressedAccent = RemoteColors.KeyPressed,
            minHeight = 0.dp,
            iconSize = 16.dp,
            mapped = mapped(RemoteKey.PAGE_UP),
            autoRepeat = true,
            contentDescription = "Page up",
            onPress = { onKey(RemoteKey.PAGE_UP) },
            onRepeat = { onRepeat(RemoteKey.PAGE_UP) },
        )
        Text(
            text = "PAGE",
            color = RemoteColors.LabelSecondary,
            fontSize = 8.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(vertical = 1.dp),
        )
        RemoteButton(
            label = "",
            icon = Icons.Filled.KeyboardDoubleArrowDown,
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            accent = RemoteColors.Key,
            pressedAccent = RemoteColors.KeyPressed,
            minHeight = 0.dp,
            iconSize = 16.dp,
            mapped = mapped(RemoteKey.PAGE_DOWN),
            autoRepeat = true,
            contentDescription = "Page down",
            onPress = { onKey(RemoteKey.PAGE_DOWN) },
            onRepeat = { onRepeat(RemoteKey.PAGE_DOWN) },
        )
    }
}

/** Digits one to nine, then TV/R, zero and Timer. */
@Composable
private fun NumericPad(mapped: (RemoteKey) -> Boolean, onKey: (RemoteKey) -> Unit) {
    val rows = listOf(
        listOf(RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3),
        listOf(RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6),
        listOf(RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9),
        listOf(RemoteKey.TV_RADIO, RemoteKey.NUM_0, RemoteKey.TIMER),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEach { key ->
                    val isDigit = key.label.length == 1
                    RemoteButton(
                        label = key.label,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        fontSize = if (isDigit) 16.sp else 10.sp,
                        minHeight = 38.dp,
                        mapped = mapped(key),
                        onPress = { onKey(key) },
                    )
                }
            }
        }
    }
}

/** The wordmark printed at the foot of the handset. */
@Composable
private fun BrandMark() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mediastar",
            color = RemoteColors.Brand,
            fontSize = 19.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
        )
        // The small crescent that closes the wordmark on the handset.
        Spacer(modifier = Modifier.width(3.dp))
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(RemoteColors.Brand, CircleShape),
        )
    }
}

// ------------------------------------------------------------------- helpers

/** Kept so callers can add an evenly weighted text key to any row. */
@Suppress("unused")
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
