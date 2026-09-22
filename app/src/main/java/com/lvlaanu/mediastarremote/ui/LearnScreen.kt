package com.lvlaanu.mediastarremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ir.NecEncoder
import com.lvlaanu.mediastarremote.ui.theme.RemoteColors

/**
 * Learn Mode.
 *
 * ## Why this is not an IR capture screen
 *
 * Android's public IR API is `ConsumerIrManager`, and it transmits only. There
 * is no framework call to receive, and the POCO X6 Pro's blaster is an emitter
 * with no receive diode behind it. No app on an unrooted phone can read a code
 * off a physical handset, and any app claiming otherwise is either shipping a
 * code database or driving external hardware.
 *
 * So this screen offers the two things that genuinely do work:
 *
 *  - **Discovery** walks the NEC command space, sending one code at a time
 *    while you watch the receiver. When the box reacts you bind that code to a
 *    button. This is how you reverse-engineer an undocumented receiver with a
 *    transmit-only phone, and it is what actually gets a working profile.
 *  - **Import** takes a code you already have, in NEC hex, Pronto CCF or raw
 *    microseconds, from a database, a LIRC config, or a hardware IR reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(viewModel: RemoteViewModel, onBack: () -> Unit) {
    val discovery by viewModel.discovery.collectAsState()
    val store by viewModel.store.collectAsState()
    val status by viewModel.status.collectAsState()
    val profile = store.active

    Scaffold(
        containerColor = RemoteColors.Backdrop,
        topBar = {
            TopAppBar(
                title = { Text("Learn Mode", fontSize = 18.sp) },
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

            Text(
                text = "Writing into: ${profile?.name ?: "no profile"}  " +
                    "(${profile?.mappedCount ?: 0} of ${RemoteKey.entries.size} keys mapped)",
                color = RemoteColors.LabelSecondary,
                fontSize = 12.sp,
            )

            CaptureExplainer()
            DiscoverySection(viewModel, discovery)
            ImportSection(viewModel, discovery.targetKey)
        }
    }
}

/** States the capture limitation plainly rather than burying it. */
@Composable
private fun CaptureExplainer() {
    SectionCard(title = "Before you start") {
        Text(
            text = "Android phones can only transmit infrared, never receive it. " +
                "The POCO X6 Pro's blaster has no receive diode and the framework has no " +
                "receive API, so no app can copy a code straight off your physical handset.\n\n" +
                "Discovery below solves the same problem the other way round: it sends codes " +
                "one at a time while you watch the receiver, and you bind whichever one it " +
                "answers. Import handles codes you already have in writing.",
            color = RemoteColors.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverySection(viewModel: RemoteViewModel, discovery: DiscoveryState) {
    var addressText by remember { mutableStateOf("%02X".format(discovery.address)) }
    var sweepAddresses by remember { mutableStateOf(false) }

    SectionCard(title = "Discovery sweep") {
        Text(
            text = "1. Pick the button you want to map.  2. Point the phone at the receiver and " +
                "start the sweep.  3. The moment the receiver reacts, press Capture.",
            color = RemoteColors.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Text("Target button", color = RemoteColors.LabelMuted, fontSize = 11.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RemoteKey.entries.forEach { key ->
                FilterChip(
                    selected = discovery.targetKey == key,
                    onClick = { viewModel.setDiscoveryTarget(key) },
                    label = { Text(key.label, fontSize = 10.sp) },
                )
            }
        }

        // Live readout of the code currently on the air.
        Text(
            text = "NEC  addr 0x%02X   cmd 0x%02X".format(discovery.address, discovery.command),
            color = RemoteColors.LabelPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = { text ->
                    addressText = text
                    text.toIntOrNull(16)?.let(viewModel::setDiscoveryAddress)
                },
                label = { Text("Address (hex)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = sweepAddresses,
                onClick = { sweepAddresses = !sweepAddresses },
                label = { Text("Sweep addresses too", fontSize = 10.sp) },
            )
        }

        Text(
            text = "Step delay: ${discovery.intervalMs} ms",
            color = RemoteColors.LabelMuted,
            fontSize = 11.sp,
        )
        Slider(
            value = discovery.intervalMs.toFloat(),
            onValueChange = { viewModel.setDiscoveryInterval(it.toLong()) },
            valueRange = 300f..2000f,
            steps = 16,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (discovery.running) {
                Button(onClick = viewModel::stopDiscovery, modifier = Modifier.weight(1f)) {
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.startDiscovery(
                            targetKey = discovery.targetKey,
                            startCommand = discovery.command,
                            address = discovery.address,
                            sweepAddresses = sweepAddresses,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start sweep")
                }
            }
            OutlinedButton(onClick = viewModel::resendDiscovery, modifier = Modifier.weight(1f)) {
                Text("Resend")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.stepDiscovery(-1) }, modifier = Modifier.weight(1f)) {
                Text("- 1")
            }
            OutlinedButton(onClick = { viewModel.stepDiscovery(1) }, modifier = Modifier.weight(1f)) {
                Text("+ 1")
            }
        }

        Text(
            text = "Reaction time means the code that worked is usually one or two behind the " +
                "one on screen. Capture offers all three.",
            color = RemoteColors.LabelMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, -1, -2).forEach { offset ->
                Button(
                    onClick = { viewModel.captureCurrentDiscovery(offset) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Capture 0x%02X".format((discovery.command + offset) and 0xFF),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportSection(viewModel: RemoteViewModel, targetKey: RemoteKey?) {
    var format by remember { mutableStateOf(CodeFormat.NEC) }
    var text by remember { mutableStateOf("") }

    SectionCard(title = "Import a known code") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CodeFormat.entries.forEach { candidate ->
                FilterChip(
                    selected = format == candidate,
                    onClick = { format = candidate },
                    label = { Text(candidate.label, fontSize = 10.sp) },
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(format.label, fontSize = 11.sp) },
            placeholder = { Text(format.hint, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6,
        )

        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = targetKey?.let { "Assigning to ${it.label}" }
                        ?: "Pick a target button above",
                    fontSize = 10.sp,
                )
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.parseSignal(text, format)
                        .onSuccess {
                            viewModel.sendRaw(it)
                            viewModel.postStatus("Sent: ${it.describe()}")
                        }
                        .onFailure {
                            viewModel.postStatus(it.message ?: "Parse failed", isError = true)
                        }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Test send")
            }
            Button(
                onClick = {
                    val key = targetKey
                    if (key == null) {
                        viewModel.postStatus("Pick a target button first", isError = true)
                    } else {
                        viewModel.assignFromText(key, text, format)
                    }
                },
                enabled = targetKey != null && text.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Assign")
            }
        }

        if (format == CodeFormat.NEC) {
            Text(
                text = "NEC frames are sent at ${NecEncoder.DEFAULT_CARRIER_HZ / 1000} kHz with a " +
                    "9 ms leader and a 110 ms frame slot, matching the original NEC timing spec.",
                color = RemoteColors.LabelMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/** Consistent card wrapper for the sections on this screen. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
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
                style = MaterialTheme.typography.labelLarge,
            )
            content()
        }
    }
}
