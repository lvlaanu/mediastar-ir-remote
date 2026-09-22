package com.lvlaanu.mediastarremote.data

import com.lvlaanu.mediastarremote.ir.IrSignal
import kotlinx.serialization.Serializable

/**
 * A named set of IR codes, one per button.
 *
 * The map is keyed by [RemoteKey.name] rather than by the enum itself so a
 * profile exported today still loads after buttons are added or renamed in a
 * later version: unknown keys are ignored, missing keys simply show as unmapped.
 */
@Serializable
data class CodeProfile(
    val id: String,
    val name: String,
    /** Where the codes came from, shown verbatim in Settings. */
    val description: String = "",
    /** False for the shipped guesses, true once the user has verified them. */
    val verified: Boolean = false,
    /** Built-in profiles cannot be deleted, only copied. */
    val builtIn: Boolean = false,
    val codes: Map<String, IrSignal> = emptyMap(),
) {
    operator fun get(key: RemoteKey): IrSignal? = codes[key.name]

    fun with(key: RemoteKey, signal: IrSignal): CodeProfile =
        copy(codes = codes + (key.name to signal), builtIn = false)

    fun without(key: RemoteKey): CodeProfile =
        copy(codes = codes - key.name, builtIn = false)

    val mappedCount: Int get() = RemoteKey.entries.count { codes.containsKey(it.name) }

    val unmappedKeys: List<RemoteKey> get() = RemoteKey.entries.filter { !codes.containsKey(it.name) }
}

/** The whole persisted state: every profile plus which one is active. */
@Serializable
data class ProfileStore(
    val profiles: List<CodeProfile> = emptyList(),
    val activeProfileId: String = "",
) {
    val active: CodeProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
}
