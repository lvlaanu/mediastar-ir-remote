package com.lvlaanu.mediastarremote.data

import com.lvlaanu.mediastarremote.ir.IrSignal
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the on-disk format. A profile written by one version has to survive
 * being read back by the next, because a user may have spent an hour in
 * Discovery building it.
 */
class ProfileSerializationTest {

    /** Mirrors the configuration in ProfileRepository. */
    private val json = Json {
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

    @Test
    fun `the whole store round trips`() {
        val store = DefaultProfiles.initialStore()
        val text = json.encodeToString(ProfileStore.serializer(), store)
        assertEquals(store, json.decodeFromString(ProfileStore.serializer(), text))
    }

    @Test
    fun `every signal kind round trips`() {
        listOf(
            IrSignal.Nec(address = 0x00, command = 0x0C),
            IrSignal.Nec(address = 0x1234, command = 0x0C, extended = true),
            IrSignal.Raw(pattern = listOf(9000, 4500, 560, 560)),
            IrSignal.Pronto("0000 006D 0002 0000 0155 00AA 0015 0015"),
        ).forEach { signal ->
            val text = json.encodeToString(IrSignal.serializer(), signal)
            assertEquals(signal, json.decodeFromString(IrSignal.serializer(), text))
        }
    }

    @Test
    fun `candidate profile maps every key with a distinct command`() {
        assertEquals(RemoteKey.entries.size, DefaultProfiles.candidateCommands.size)
        assertEquals(
            "two buttons share a command byte",
            DefaultProfiles.candidateCommands.size,
            DefaultProfiles.candidateCommands.values.toSet().size,
        )
        val profile = DefaultProfiles.candidateNec()
        assertEquals(RemoteKey.entries.size, profile.mappedCount)
        assertTrue(profile.unmappedKeys.isEmpty())
    }

    @Test
    fun `candidate commands all fit in one byte`() {
        assertTrue(DefaultProfiles.candidateCommands.values.all { it in 0..0xFF })
    }

    @Test
    fun `editing a built-in profile marks the result as user owned`() {
        val builtIn = DefaultProfiles.candidateNec()
        assertTrue(builtIn.builtIn)

        val edited = builtIn.with(RemoteKey.POWER, IrSignal.Nec(address = 0x04, command = 0x08))
        assertFalse(edited.builtIn)
        assertEquals(IrSignal.Nec(address = 0x04, command = 0x08), edited[RemoteKey.POWER])
        assertNull(edited.without(RemoteKey.POWER)[RemoteKey.POWER])
    }

    @Test
    fun `unknown keys in stored json are ignored rather than fatal`() {
        val text = """
            {
              "id": "user-1",
              "name": "From a newer build",
              "codes": {
                "POWER": { "type": "nec", "address": 0, "command": 12 },
                "SOME_FUTURE_KEY": { "type": "nec", "address": 0, "command": 99 }
              }
            }
        """.trimIndent()
        val profile = json.decodeFromString(CodeProfile.serializer(), text)
        assertEquals(IrSignal.Nec(address = 0x00, command = 0x0C), profile[RemoteKey.POWER])
        // The unknown key is kept in the map but never surfaces as a button.
        assertEquals(1, profile.mappedCount)
    }
}
