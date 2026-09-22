package com.lvlaanu.mediastarremote.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lvlaanu.mediastarremote.ir.IrSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// The reified encodeToString/decodeFromString helpers live in the root
// kotlinx.serialization package on 1.7.x and do not resolve without these.
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mediastar_profiles")

/**
 * Persists code profiles as a single JSON blob in DataStore.
 *
 * JSON rather than a database because the whole point is that a profile should
 * be something the user can export, mail to themselves and paste back in. The
 * same [json] instance drives both persistence and the export/import feature,
 * so what is on disk is exactly what the export screen shows.
 */
class ProfileRepository(context: Context) {

    private val appContext = context.applicationContext

    val storeFlow: Flow<ProfileStore> = appContext.dataStore.data.map { prefs ->
        val raw = prefs[KEY_STORE]
        if (raw.isNullOrBlank()) {
            DefaultProfiles.initialStore()
        } else {
            runCatching { json.decodeFromString<ProfileStore>(raw) }
                .onFailure { Log.e(TAG, "Stored profiles were unreadable, falling back to defaults", it) }
                .getOrElse { DefaultProfiles.initialStore() }
                .ensureDefaults()
        }
    }

    suspend fun current(): ProfileStore = storeFlow.first()

    private suspend fun write(store: ProfileStore) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_STORE] = json.encodeToString(store)
        }
    }

    suspend fun update(transform: (ProfileStore) -> ProfileStore) {
        write(transform(current()))
    }

    suspend fun setActiveProfile(id: String) = update { store ->
        if (store.profiles.any { it.id == id }) store.copy(activeProfileId = id) else store
    }

    /**
     * Assigns a signal to a key in the named profile.
     *
     * Editing a built-in profile transparently forks it into a user copy, so
     * the shipped candidate table is never lost and the user is never blocked
     * from tweaking a single key.
     */
    suspend fun assign(profileId: String, key: RemoteKey, signal: IrSignal) = update { store ->
        val target = store.profiles.firstOrNull { it.id == profileId } ?: return@update store
        if (target.builtIn) {
            val fork = target.copy(
                id = "user-${System.currentTimeMillis()}",
                name = "${target.name} (edited)",
                description = "Forked from ${target.name}.",
                builtIn = false,
                verified = false,
            ).with(key, signal)
            store.copy(profiles = store.profiles + fork, activeProfileId = fork.id)
        } else {
            store.copy(profiles = store.profiles.map { if (it.id == profileId) it.with(key, signal) else it })
        }
    }

    suspend fun clearKey(profileId: String, key: RemoteKey) = update { store ->
        store.copy(profiles = store.profiles.map { if (it.id == profileId) it.without(key) else it })
    }

    suspend fun clearAllCodes(profileId: String) = update { store ->
        store.copy(
            profiles = store.profiles.map {
                if (it.id == profileId && !it.builtIn) it.copy(codes = emptyMap()) else it
            },
        )
    }

    suspend fun addProfile(profile: CodeProfile, makeActive: Boolean = true) = update { store ->
        val deduped = store.profiles.filterNot { it.id == profile.id }
        store.copy(
            profiles = deduped + profile,
            activeProfileId = if (makeActive) profile.id else store.activeProfileId,
        )
    }

    suspend fun duplicateProfile(profileId: String) = update { store ->
        val source = store.profiles.firstOrNull { it.id == profileId } ?: return@update store
        val copy = source.copy(
            id = "user-${System.currentTimeMillis()}",
            name = "${source.name} copy",
            builtIn = false,
        )
        store.copy(profiles = store.profiles + copy, activeProfileId = copy.id)
    }

    suspend fun renameProfile(profileId: String, name: String) = update { store ->
        store.copy(
            profiles = store.profiles.map {
                if (it.id == profileId) it.copy(name = name, builtIn = false) else it
            },
        )
    }

    suspend fun deleteProfile(profileId: String) = update { store ->
        val target = store.profiles.firstOrNull { it.id == profileId }
        if (target == null || target.builtIn) return@update store
        val remaining = store.profiles.filterNot { it.id == profileId }
        store.copy(
            profiles = remaining,
            activeProfileId = if (store.activeProfileId == profileId) {
                remaining.firstOrNull()?.id.orEmpty()
            } else {
                store.activeProfileId
            },
        ).ensureDefaults()
    }

    /** Rebuilds the candidate profile on a different NEC address. */
    suspend fun rebuildCandidate(address: Int) = update { store ->
        val rebuilt = DefaultProfiles.candidateNec(address)
        store.copy(
            profiles = store.profiles.map { if (it.id == DefaultProfiles.CANDIDATE_ID) rebuilt else it },
            activeProfileId = DefaultProfiles.CANDIDATE_ID,
        )
    }

    fun exportProfile(profile: CodeProfile): String = json.encodeToString(profile)

    fun importProfile(text: String): Result<CodeProfile> = runCatching {
        val parsed = json.decodeFromString<CodeProfile>(text)
        // Never let an import overwrite a built-in or collide with an existing id.
        parsed.copy(id = "user-${System.currentTimeMillis()}", builtIn = false)
    }

    /** Guarantees the two shipped profiles always exist and one is selected. */
    private fun ProfileStore.ensureDefaults(): ProfileStore {
        var profiles = this.profiles
        if (profiles.none { it.id == DefaultProfiles.CANDIDATE_ID }) {
            profiles = listOf(DefaultProfiles.candidateNec()) + profiles
        }
        if (profiles.none { it.id == DefaultProfiles.LEARNED_ID }) {
            profiles = profiles + DefaultProfiles.emptyLearned()
        }
        val active = if (profiles.any { it.id == activeProfileId }) {
            activeProfileId
        } else {
            profiles.first().id
        }
        return ProfileStore(profiles, active)
    }

    private companion object {
        const val TAG = "ProfileRepository"
        val KEY_STORE = stringPreferencesKey("profile_store_json")

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
            serializersModule = SerializersModule {
                polymorphic(IrSignal::class) {
                    subclass(IrSignal.Nec::class)
                    subclass(IrSignal.Raw::class)
                    subclass(IrSignal.Pronto::class)
                }
            }
        }
    }
}
