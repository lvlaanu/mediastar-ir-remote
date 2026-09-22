# MediaStar IR Remote

An Android replica of the MediaStar MS-MINI satellite handset that drives the receiver through
your phone's infrared blaster. Built for a POCO X6 Pro 5G, and works on any Android 7.0 or later
device with an IR emitter.

Kotlin, Jetpack Compose, no network access, one permission.

---

## What you need to know before you start

**The shipped code table is a guess, not a capture.** MediaStar has never published IR codes for
the MS-MINI family and no public database carries this handset. What is certain is the protocol
and carrier; what is not certain is which command byte each button sends. The app ships a
plausible starting table and a Discovery tool for finding the real one. See
[`docs/CODES.md`](docs/CODES.md) for the full research write-up.

**Android phones cannot receive infrared.** The public API, `ConsumerIrManager`, transmits only,
and the blaster has no receive diode behind it. No app on an unrooted phone can copy a code off
your physical handset. Learn Mode works around this by sweeping codes outward instead of reading
them in. This is covered in detail under [Learn Mode](#learn-mode) below.

---

## Opening the project in Android Studio on Windows

1. Install **Android Studio Ladybug (2024.2.1)** or newer. Anything that ships Android Gradle
   Plugin 8.7 support will do.
2. Clone or unzip this repository somewhere without spaces in the path.
3. **File → Open**, select the repository root (the folder holding `settings.gradle.kts`), and
   open it. Do not use *Import Project*.
4. Let the first Gradle sync finish. It downloads the Gradle 8.11.1 distribution, the Android
   Gradle Plugin and the Compose libraries, so it needs a working internet connection and takes
   a few minutes.
5. When Android Studio offers to install a missing SDK platform or build-tools, accept. The
   project targets **compileSdk 35** and **minSdk 24**.

No `local.properties` is committed. Android Studio writes one pointing at your SDK on first sync.

### Building an APK

From the IDE:

- Debug: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The output lands in
  `app/build/outputs/apk/debug/app-debug.apk`.
- Release: **Build → Generate Signed Bundle / APK**, choose APK, and create or select a keystore.

From a terminal in the project root:

```bat
gradlew.bat assembleDebug
gradlew.bat assembleRelease
gradlew.bat test
```

On macOS or Linux use `./gradlew` instead.

The debug build installs as a separate app (`applicationId` gets a `.debug` suffix), so you can
keep a release build installed side by side while you experiment.

### Troubleshooting the build

**`KeytoolException: ... Algorithm HmacPBE1.2.840.113549.1.5.14 not available`**

This is a JDK mismatch, not a problem with the code. The debug keystore in your home directory
was written by a JDK new enough to use PBMAC1, and the JDK running the build is too old to read
that format. It happens routinely when Android Studio signs with its bundled runtime and the
command line signs with whatever `JAVA_HOME` points at.

The project works around it already: `app/keystore/debug.keystore` is committed and the debug
build type is wired to use it. It is written with the legacy SHA-1 MAC that every JDK from 8
onward can read, so debug builds now sign identically on every machine. Those are Android's
standard public debug credentials, they protect nothing, and the release build type does not use
them.

If you would rather go back to the per-machine keystore, delete `app/keystore/debug.keystore` and
the signing config falls back to the one Android Studio generates. In that case also delete the
stale one so it gets rebuilt by your current JDK:

```bat
del "%USERPROFILE%\.android\debug.keystore"
```

**Gradle and Android Studio disagree about the JDK.** Check which one the command line uses with
`gradlew.bat -version`. To make both the same, point `JAVA_HOME` at Android Studio's bundled
runtime, normally `C:\Program Files\Android\Android Studio\jbr`.

### Installing on the phone

Enable **Developer options → USB debugging** on the POCO, plug it in, and either press Run in
Android Studio or:

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Using the app

The main screen is the handset. Press a key and it transmits. Holding an arrow, a page key, or
rewind and fast forward sends NEC repeat frames for as long as you hold, exactly as the physical
remote does.

A key whose label is dimmed has no code in the active profile. Press it and the status strip says
so rather than failing silently.

All 45 buttons of the handset are present. Note that this remote has no separate
red/green/yellow/blue strip: the four coloured keys are Audio, APP, Wifi and Info, printed in red,
green, amber and blue. They are modelled as one key each rather than as a colour and a function.

The strip at the top shows whether an IR emitter was found and which profile is active. The two
links lead to Learn Mode and Settings.

### First run checklist

1. Open **Settings** and confirm *Emitter: Present*. If it says otherwise, the device has no
   blaster and nothing else in the app will work.
2. Press **Send test IR frame**. Note that most modern phone cameras filter infrared out, so you
   usually cannot see the flash; check with an older camera, a selfie camera, or the receiver.
3. Point the phone at the receiver and press **Power** on the handset.
4. If the receiver reacts, the shipped guess happens to be right and you are done. If it does not,
   go to [finding your receiver's codes](#finding-your-receivers-codes).

---

## How the IR codes are stored

Every profile is a JSON document holding one signal per button, keyed by the button's enum name.
Profiles live in a DataStore file at
`/data/data/com.lvlaanu.mediastarremote/files/datastore/mediastar_profiles.preferences_pb`, which
is included in Android cloud backup and device-to-device transfer.

A signal is one of three shapes:

| `type` | Fields | Use |
|---|---|---|
| `nec` | `address`, `command`, `extended`, `carrierHz` | Normal case. Encoded to NEC timing at send time. |
| `raw` | `pattern` (microseconds), `carrierHz` | Any protocol at all, from a hardware capture. |
| `pronto` | `hex` | Pronto CCF from a database, decoded at send time. |

A stored profile looks like this:

```json
{
  "id": "user-learned",
  "name": "My MediaStar (learned)",
  "verified": true,
  "builtIn": false,
  "codes": {
    "POWER": { "type": "nec", "address": 0, "command": 12, "extended": false, "carrierHz": 38000 },
    "MUTE":  { "type": "pronto", "hex": "0000 006D 0022 0002 0155 00AA ..." }
  }
}
```

### Updating the codes

Three ways, in increasing order of effort:

- **In the app.** Learn Mode assigns codes one button at a time and writes them straight to the
  active profile. Nothing else is needed.
- **By editing JSON.** Settings → *Copy active profile to clipboard*, paste it somewhere, edit the
  numbers, and paste it back into the import box. Importing always creates a new profile, so you
  cannot destroy the one you have.
- **In source.** `DefaultProfiles.candidateCommands` in
  `app/src/main/java/com/lvlaanu/mediastarremote/data/DefaultProfiles.kt` is the shipped table. Edit
  it, rebuild, and clear the app's data so the new defaults are written.

Built-in profiles cannot be edited in place. Changing a key in one silently forks it into a user
copy, so the shipped table is always recoverable.

---

## Learn Mode

Learn Mode has two halves. Both write into whichever profile is active, so switch to
*My MediaStar (learned)* in Settings first if you want to keep the shipped guess intact.

### Discovery, for codes you do not have

1. Open **Learn**, and pick the button you are mapping from the chip list.
2. Point the phone at the receiver, somewhere you can see its front panel or the TV.
3. Press **Start sweep**. The app sends NEC frames one command at a time, stepping every 700 ms
   by default. The large readout shows what is currently on the air.
4. The moment the receiver reacts, press one of the three **Capture** buttons. Human reaction time
   means the code that worked is usually one or two behind what is on screen, which is why Capture
   offers the current value and the two before it.
5. Press the mapped button on the main screen to confirm, then repeat for the next button.

Once the first button works you know the address, and every other code on that receiver is on the
same address. Set the step delay lower and the rest go quickly.

**Sweep addresses too** extends the sweep past command `0xFF` into the next address. Leave it off
once you know the address, or you will drift off it.

### Import, for codes you already have

Paste a code and assign it to a button. Three formats are accepted:

| Format | Example |
|---|---|
| NEC hex | `00 0C`, `0x00 0x0C`, `000C` |
| Pronto CCF | `0000 006D 0022 0002 0155 00AA 0015 0015 ...` |
| Raw microseconds | `9000 4500 560 560 560 1690 ...` |

**Test send** fires the code without binding it, so you can check before committing. A malformed
code is rejected at paste time rather than failing later on a button press.

This is also the route for a code captured with real hardware. An Arduino with a TSOP38238
receiver and the IRremote library, a Broadlink hub, or a Flipper Zero will all read your physical
handset and print raw timings you can paste straight in.

---

## Profiles

Settings manages as many profiles as you like: one per receiver, or one per firmware revision.

- **Copy** duplicates a profile, including a built-in one, so you can experiment freely.
- **Delete** removes a user profile. Built-ins cannot be deleted.
- **Clear every code** empties a user profile without deleting it.
- **Candidate profile address** rebuilds the shipped table on a different NEC address. Try this
  before a full Discovery sweep: if the receiver uses a contiguous command block, the address is
  the only thing wrong, and stepping through addresses takes seconds.

---

## Project layout

```
app/src/main/java/com/lvlaanu/mediastarremote/
├── MainActivity.kt              Single activity, Compose navigation between the three screens
├── ir/                          Protocol engine. No Android UI dependencies.
│   ├── IrSignal.kt              NEC / raw / Pronto signal model, and the IrBurst hardware form
│   ├── NecEncoder.kt            NEC framing, repeat frames, hex parsing
│   ├── ProntoCodec.kt           Pronto CCF decode and encode
│   └── IrTransmitter.kt         ConsumerIrManager wrapper, capability probe, send queue
├── data/                        Buttons and persistence
│   ├── RemoteKey.kt             Every button on the handset
│   ├── CodeProfile.kt           Profile and store models
│   ├── DefaultProfiles.kt       The shipped candidate table
│   └── ProfileRepository.kt     DataStore persistence, import and export
└── ui/                          Compose layer
    ├── RemoteScreen.kt          The handset replica
    ├── LearnScreen.kt           Discovery and import
    ├── SettingsScreen.kt        Hardware check, profiles, backup, about
    ├── RemoteViewModel.kt       State, sweep loop, transmit dispatch
    ├── components/              Reusable key shapes and the direction pad
    └── theme/                   Colours and typography
```

The protocol engine has no dependency on the UI layer and no Android UI imports, so it can be
unit tested on the JVM and reused elsewhere. `app/src/test/` covers NEC framing, Pronto
round-tripping and profile serialisation. Run it with `gradlew.bat test`.

---

## Permissions

One permission, `android.permission.TRANSMIT_IR`. It is a normal permission granted at install
time, with no runtime prompt. The app requests no network, storage or location access, and makes
no network calls.

`android.hardware.consumerir` is declared with `required="false"` so the app still installs on a
phone without a blaster and reports the missing hardware instead of refusing to run. Set it to
`true` if you would rather the Play Store hid the app from those devices.

---

## Technical notes

**NEC timing.** 38 kHz carrier. 9000 us leader mark, 4500 us space, then address, inverted
address, command and inverted command, LSB first, each bit a 560 us mark followed by a 560 us
space for zero or 1690 us for one. A 560 us trailer mark closes the last bit, and the frame is
padded to a 110 ms slot. Extended NEC sends a 16-bit address in place of the address and its
complement.

**Repeat frames.** Holding a key sends the NEC ditto frame, 9000 us mark, 2250 us space, 560 us
mark, every 110 ms after a 420 ms initial delay. This is what makes a held arrow scroll smoothly
rather than stutter through full frames.

**The 110 ms pad matters.** Some Xiaomi and POCO IR drivers return from `transmit()` as soon as
the pattern is queued rather than when it finishes. Without the trailing idle, a fast double tap
can put two frames back to back with no gap and the receiver reads them as one malformed frame.

**Carrier negotiation.** On startup the app asks the driver which carrier ranges it supports.
Xiaomi devices typically report one wide range that covers 38 kHz comfortably. If a driver reports
ranges that exclude the requested frequency, the nearest supported one is used; if it reports
nothing at all, the request is passed through untouched, because the HAL usually still works and
refusing to transmit would be worse than trying.

**Transmit threading.** `ConsumerIrManager.transmit` blocks and is not safe to call from several
threads at once, so every burst goes through one dedicated thread fed by a conflated channel. If
you press faster than the hardware can send, intermediate presses are dropped rather than queued,
which is what you want from a remote.

---

## Licence

No licence chosen yet. Add one before publishing.
