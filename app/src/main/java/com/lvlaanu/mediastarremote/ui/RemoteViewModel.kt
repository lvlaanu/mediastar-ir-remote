package com.lvlaanu.mediastarremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lvlaanu.mediastarremote.data.DefaultProfiles
import com.lvlaanu.mediastarremote.data.ProfileRepository
import com.lvlaanu.mediastarremote.data.ProfileStore
import com.lvlaanu.mediastarremote.data.RemoteKey
import com.lvlaanu.mediastarremote.ir.IrCapability
import com.lvlaanu.mediastarremote.ir.IrSignal
import com.lvlaanu.mediastarremote.ir.IrTransmitter
import com.lvlaanu.mediastarremote.ir.NecEncoder
import com.lvlaanu.mediastarremote.ir.ProntoCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Transient message shown in the status strip. */
data class StatusMessage(val text: String, val isError: Boolean = false)

/** State of the Discovery sweep in Learn Mode. */
data class DiscoveryState(
    val running: Boolean = false,
    val address: Int = DefaultProfiles.DEFAULT_ADDRESS,
    val command: Int = 0,
    val targetKey: RemoteKey? = null,
    val sweepAddresses: Boolean = false,
    val intervalMs: Long = 700L,
)

/**
 * Single source of truth for the remote screen.
 *
 * The IR engine, the profile store and the sweep loop all live here so the
 * composables stay declarative and hold no state of their own beyond press
 * animation.
 */
class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val transmitter = IrTransmitter(app)
    private val repository = ProfileRepository(app)

    val capability: IrCapability = transmitter.capability

    private val _status = MutableStateFlow<StatusMessage?>(null)
    val status: StateFlow<StatusMessage?> = _status.asStateFlow()

    private val _discovery = MutableStateFlow(DiscoveryState())
    val discovery: StateFlow<DiscoveryState> = _discovery.asStateFlow()

    /** The most recently transmitted key, used for the on-screen readout. */
    private val _lastSent = MutableStateFlow<Pair<RemoteKey, String>?>(null)
    val lastSent: StateFlow<Pair<RemoteKey, String>?> = _lastSent.asStateFlow()

    val store: StateFlow<ProfileStore> = repository.storeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DefaultProfiles.initialStore(),
    )

    private var sweepJob: Job? = null
    private var statusJob: Job? = null

    init {
        viewModelScope.launch {
            transmitter.lastError.collect { error ->
                if (error != null) {
                    postStatus(error, isError = true)
                    transmitter.clearError()
                }
            }
        }
    }

    // ---------------------------------------------------------------- sending

    /** Sends the code bound to [key] in the active profile. */
    fun press(key: RemoteKey) {
        val profile = store.value.active
        val signal = profile?.get(key)
        if (signal == null) {
            postStatus("${key.label} has no code in this profile", isError = true)
            return
        }
        if (transmitter.send(signal.toBurst())) {
            _lastSent.value = key to signal.describe()
        }
    }

    /**
     * Auto-repeat tick. NEC sends its short ditto frame here, which is what a
     * real handset does and what makes a held arrow key scroll smoothly instead
     * of stuttering through full frames.
     */
    fun repeat(key: RemoteKey) {
        val signal = store.value.active?.get(key) ?: return
        transmitter.send(signal.toRepeatBurst())
    }

    /** Fires an arbitrary signal, used by the test button and by Discovery. */
    fun sendRaw(signal: IrSignal) {
        transmitter.send(signal.toBurst())
    }

    fun isMapped(key: RemoteKey): Boolean = store.value.active?.get(key) != null

    // --------------------------------------------------------------- profiles

    fun selectProfile(id: String) = viewModelScope.launch {
        repository.setActiveProfile(id)
        postStatus("Profile switched")
    }

    fun duplicateProfile(id: String) = viewModelScope.launch {
        repository.duplicateProfile(id)
        postStatus("Profile duplicated")
    }

    fun renameProfile(id: String, name: String) = viewModelScope.launch {
        repository.renameProfile(id, name)
    }

    fun deleteProfile(id: String) = viewModelScope.launch {
        repository.deleteProfile(id)
        postStatus("Profile deleted")
    }

    fun clearLearnedCodes(id: String) = viewModelScope.launch {
        repository.clearAllCodes(id)
        postStatus("Codes cleared")
    }

    /** Re-issues the built-in candidate table on a different NEC address. */
    fun rebuildCandidate(address: Int) = viewModelScope.launch {
        repository.rebuildCandidate(address)
        postStatus("Candidate profile rebuilt on address 0x%02X".format(address))
    }

    fun exportActiveProfile(): String =
        store.value.active?.let { repository.exportProfile(it) }.orEmpty()

    fun importProfile(text: String) = viewModelScope.launch {
        repository.importProfile(text)
            .onSuccess {
                repository.addProfile(it)
                postStatus("Imported '${it.name}'")
            }
            .onFailure { postStatus("Import failed: ${it.message}", isError = true) }
    }

    // ------------------------------------------------------------- assignment

    fun assign(key: RemoteKey, signal: IrSignal) = viewModelScope.launch {
        val profileId = store.value.active?.id ?: return@launch
        repository.assign(profileId, key, signal)
        postStatus("${key.label} assigned: ${signal.describe()}")
    }

    fun clearKey(key: RemoteKey) = viewModelScope.launch {
        val profileId = store.value.active?.id ?: return@launch
        repository.clearKey(profileId, key)
        postStatus("${key.label} cleared")
    }

    /**
     * Parses one of the three accepted text formats and assigns it.
     *
     * Accepts NEC "addr cmd" hex pairs, Pronto CCF hex, and raw microsecond
     * lists. The format is detected from the shape of the input so the user
     * does not have to pick one from a menu.
     */
    fun assignFromText(key: RemoteKey, text: String, format: CodeFormat) {
        val parsed = parseSignal(text, format)
        parsed
            .onSuccess { assign(key, it) }
            .onFailure { postStatus(it.message ?: "Could not parse code", isError = true) }
    }

    fun parseSignal(text: String, format: CodeFormat): Result<IrSignal> = runCatching {
        when (format) {
            CodeFormat.NEC -> {
                val (address, command) = NecEncoder.parseAddressCommand(text)
                    ?: throw IllegalArgumentException("Expected two hex bytes, for example '00 0C'")
                IrSignal.Nec(address = address, command = command)
            }

            CodeFormat.PRONTO -> {
                val signal = IrSignal.Pronto(text.trim())
                // Decode eagerly so a malformed code is rejected at paste time
                // rather than silently failing later on a button press.
                signal.toBurst()
                signal
            }

            CodeFormat.RAW -> {
                val values = text.trim()
                    .split(Regex("[\\s,;]+"))
                    .filter { it.isNotEmpty() }
                    .map {
                        it.toIntOrNull()
                            ?: throw IllegalArgumentException("'$it' is not a microsecond value")
                    }
                if (values.size < 2) throw IllegalArgumentException("Need at least one on/off pair")
                if (values.any { it <= 0 }) throw IllegalArgumentException("Durations must be positive")
                IrSignal.Raw(pattern = values)
            }
        }
    }

    /** Renders any stored signal to Pronto hex for sharing. */
    fun toPronto(signal: IrSignal): String = ProntoCodec.encode(signal.toBurst())

    // -------------------------------------------------------------- discovery

    /**
     * Starts the Discovery sweep.
     *
     * Android's public API can only transmit, never receive, so the phone
     * cannot copy a code off the physical handset by itself. The sweep is the
     * practical substitute: it walks the NEC command space a step at a time
     * while the user watches the receiver, and binds whichever code produced a
     * visible reaction.
     */
    fun startDiscovery(targetKey: RemoteKey?, startCommand: Int, address: Int, sweepAddresses: Boolean) {
        sweepJob?.cancel()
        _discovery.value = DiscoveryState(
            running = true,
            address = address,
            command = startCommand,
            targetKey = targetKey,
            sweepAddresses = sweepAddresses,
        )
        sweepJob = viewModelScope.launch {
            while (true) {
                val state = _discovery.value
                if (!state.running) break
                sendRaw(IrSignal.Nec(address = state.address, command = state.command))
                delay(state.intervalMs)
                advanceDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        sweepJob?.cancel()
        sweepJob = null
        _discovery.value = _discovery.value.copy(running = false)
    }

    /** Re-sends the current sweep code without advancing, for confirmation. */
    fun resendDiscovery() {
        val state = _discovery.value
        sendRaw(IrSignal.Nec(address = state.address, command = state.command))
    }

    fun stepDiscovery(delta: Int) {
        val state = _discovery.value
        _discovery.value = state.copy(command = (state.command + delta) and 0xFF)
    }

    fun setDiscoveryInterval(ms: Long) {
        _discovery.value = _discovery.value.copy(intervalMs = ms.coerceIn(200L, 3_000L))
    }

    fun setDiscoveryAddress(address: Int) {
        _discovery.value = _discovery.value.copy(address = address and 0xFF)
    }

    fun setDiscoveryCommand(command: Int) {
        _discovery.value = _discovery.value.copy(command = command and 0xFF)
    }

    fun setDiscoveryTarget(key: RemoteKey?) {
        _discovery.value = _discovery.value.copy(targetKey = key)
    }

    /**
     * Binds the code the sweep just sent. Called when the user sees the
     * receiver react, so the code of interest is the previous one rather than
     * whatever the loop has moved on to.
     */
    fun captureCurrentDiscovery(offset: Int = 0) {
        val state = _discovery.value
        val key = state.targetKey
        if (key == null) {
            postStatus("Choose which button to assign first", isError = true)
            return
        }
        stopDiscovery()
        val command = (state.command + offset) and 0xFF
        assign(key, IrSignal.Nec(address = state.address, command = command))
    }

    private fun advanceDiscovery() {
        val state = _discovery.value
        val nextCommand = state.command + 1
        _discovery.value = if (nextCommand > 0xFF) {
            if (state.sweepAddresses) {
                state.copy(command = 0, address = (state.address + 1) and 0xFF)
            } else {
                state.copy(command = 0)
            }
        } else {
            state.copy(command = nextCommand)
        }
    }

    // ----------------------------------------------------------------- status

    /** Sends a recognisable NEC frame so the user can check the emitter works. */
    fun transmitTestSignal() {
        val ok = transmitter.send(IrSignal.Nec(address = 0x00, command = 0x00).toBurst())
        if (ok) postStatus("Test frame sent, NEC addr 0x00 cmd 0x00")
    }

    fun postStatus(text: String, isError: Boolean = false) {
        statusJob?.cancel()
        _status.value = StatusMessage(text, isError)
        statusJob = viewModelScope.launch {
            delay(if (isError) 3_500 else 2_000)
            _status.value = null
        }
    }

    override fun onCleared() {
        sweepJob?.cancel()
        transmitter.shutdown()
        super.onCleared()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return RemoteViewModel(app) as T
            }
        }
    }
}

/** The three text formats Learn Mode can import. */
enum class CodeFormat(val label: String, val hint: String) {
    NEC("NEC hex", "Address and command, e.g. 00 0C"),
    PRONTO("Pronto CCF", "0000 006D 0022 0002 0155 00AA ..."),
    RAW("Raw microseconds", "9000 4500 560 560 560 1690 ..."),
}
