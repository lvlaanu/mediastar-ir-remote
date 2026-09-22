package com.lvlaanu.mediastarremote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.DefaultProfiles
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * Settings: hardware check, profile management, import and export, and the
 * about text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: RemoteViewModel, onBack: () -> Unit) {
    val store by viewModel.store.collectAsState()
    val status by viewModel.status.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = RemoteColors.Backdrop,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RemoteColors.Body,
                    titleContentColor = RemoteColors.LabelPrimary,
                    navigationIconContentColor = RemoteColors.LabelSecondary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            status?.let { message ->
                Text(
                    text = message.text,
                    color = if (message.isError) RemoteColors.StatusError else RemoteColors.StatusOk,
                    fontSize = 12.sp,
                )
            }

            HardwareCard(viewModel)
            ProfilesCard(viewModel, store.profiles, store.activeProfileId)
            CandidateAddressCard(viewModel)
            TransferCard(viewModel, context)
            AboutCard()
        }
    }
}

@Composable
private fun HardwareCard(viewModel: RemoteViewModel) {
    val capability = viewModel.capability
    SettingsCard("Infrared hardware") {
        LabelledValue(
            "Emitter",
            if (capability.hasEmitter) "Present" else "Not available on this device",
            ok = capability.hasEmitter,
        )
        LabelledValue(
            "Carrier ranges",
            when {
                capability.rangesUnknown -> "Not reported by the driver"
                capability.carrierRanges.isEmpty() -> "None"
                else -> capability.carrierRanges.joinToString(", ") {
                    "${it.first / 1000}-${it.last / 1000} kHz"
                }
            },
            ok = capability.supports(38_000),
        )
        Text(
            text = "The app transmits at 38 kHz. If the driver reports ranges that exclude it, " +
                "the nearest supported frequency is used instead.",
            color = RemoteColors.LabelMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Button(
            onClick = viewModel::transmitTestSignal,
            enabled = capability.hasEmitter,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send test IR frame")
        }
        Text(
            text = "A phone camera cannot see this: most modern sensors filter infrared out. " +
                "Check with a selfie camera, an older camera, or the receiver itself.",
            color = RemoteColors.LabelMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun ProfilesCard(
    viewModel: RemoteViewModel,
    profiles: List<com.lvlaanu.mediastarremote.data.CodeProfile>,
    activeId: String,
) {
    SettingsCard("Code profiles") {
        profiles.forEach { profile ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = profile.id == activeId,
                    onClick = { viewModel.selectProfile(profile.id) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = RemoteColors.LabelPrimary, fontSize = 13.sp)
                    val tags = buildList {
                        add("${profile.mappedCount} of ${RemoteKey.entries.size} keys")
                        if (profile.builtIn) add("built in")
                        if (!profile.verified) add("unverified")
                    }
                    Text(
                        text = tags.joinToString("  ·  "),
                        color = RemoteColors.LabelMuted,
                        fontSize = 10.sp,
                    )
                }
                OutlinedButton(onClick = { viewModel.duplicateProfile(profile.id) }) {
                    Text("Copy", fontSize = 11.sp)
                }
                if (!profile.builtIn) {
                    OutlinedButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
            if (profile.description.isNotBlank()) {
                Text(
                    text = profile.description,
                    color = RemoteColors.LabelMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                )
            }
        }

        val active = profiles.firstOrNull { it.id == activeId }
        if (active != null && !active.builtIn) {
            OutlinedButton(
                onClick = { viewModel.clearLearnedCodes(active.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear every code in '${active.name}'")
            }
        }
    }
}

/**
 * Rebuilds the shipped candidate table on a different NEC address.
 *
 * This is the cheapest thing to try before a full sweep: if the receiver uses
 * a contiguous command block, only the address is wrong, and stepping through
 * addresses takes seconds rather than minutes.
 */
@Composable
private fun CandidateAddressCard(viewModel: RemoteViewModel) {
    var addressText by remember { mutableStateOf("%02X".format(DefaultProfiles.DEFAULT_ADDRESS)) }

    SettingsCard("Candidate profile address") {
        Text(
            text = "The built-in profile is a guess, not a capture. If no button works, try " +
                "another NEC address here before running a full Discovery sweep.",
            color = RemoteColors.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it },
                label = { Text("Address (hex)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val address = addressText.removePrefix("0x").toIntOrNull(16)
                    if (address == null || address !in 0..0xFF) {
                        viewModel.postStatus("Enter a hex byte between 00 and FF", isError = true)
                    } else {
                        viewModel.rebuildCandidate(address)
                    }
                },
            ) {
                Text("Rebuild")
            }
        }
    }
}

@Composable
private fun TransferCard(viewModel: RemoteViewModel, context: Context) {
    var importText by remember { mutableStateOf("") }

    SettingsCard("Backup and transfer") {
        Text(
            text = "Profiles are plain JSON. Copy one out to back it up or share it, paste one " +
                "in to restore it.",
            color = RemoteColors.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Button(
            onClick = {
                val json = viewModel.exportActiveProfile()
                if (json.isBlank()) {
                    viewModel.postStatus("Nothing to export", isError = true)
                } else {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MediaStar profile", json))
                    viewModel.postStatus("Active profile copied to clipboard")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Copy active profile to clipboard")
        }

        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("Paste profile JSON", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
        )
        OutlinedButton(
            onClick = {
                viewModel.importProfile(importText)
                importText = ""
            },
            enabled = importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import profile")
        }
    }
}

@Composable
private fun AboutCard() {
    SettingsCard("About") {
        Text(
            text = "MediaStar IR Remote 1.0.0",
            color = RemoteColors.LabelPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "A replica of the MediaStar MS-MINI handset that drives your receiver " +
                "through the phone's infrared blaster.\n\n" +
                "Protocol: NEC, 38 kHz, 8-bit address and command, LSB first, with the " +
                "standard 9 ms leader and 110 ms frame slot. Held keys send NEC repeat frames.\n\n" +
                "MediaStar has never published its IR codes and no public database carries " +
                "this handset, so the shipped table is a starting guess. Learn Mode is how you " +
                "get the real ones.",
            color = RemoteColors.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

// ------------------------------------------------------------------- helpers

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RemoteColors.Body),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = RemoteColors.LabelPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RemoteColors.LabelMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = if (ok) RemoteColors.StatusOk else RemoteColors.StatusWarn,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
