# MediaStar IR codes: what is known, what is not

Research notes behind the code table this app ships. Read this before assuming a button "should"
work.

---

## Summary

| Question | Answer | Confidence |
|---|---|---|
| Carrier frequency | 38 kHz | High |
| Protocol | NEC, 8-bit address and command, LSB first | High |
| Address byte | Unknown, `0x00` is the most common on this class of receiver | Low |
| Command byte per button | Unknown | None |

The protocol is settled. The specific bytes are not, and the app is built around that gap rather
than pretending it does not exist.

---

## Why no published table exists

I searched the places a MediaStar table would be if it existed:

- **irdb**, the largest crowd-sourced database of IR codes, has no MediaStar manufacturer folder.
  The manufacturer list runs straight past where it would sit alphabetically.
- **RemoteCentral's** hex code database has one "Media Star" entry, under Pinnacle. It is a PC TV
  tuner card from the 2000s and has nothing to do with this receiver.
- **The LIRC remotes database** has no matching config. A MediaStar entry would have to be created
  with `irrecord` against real hardware.
- **JP1 / RemoteMaster** forums discuss NEC entry generally, with no MediaStar device code.
- Vendor documentation does not publish codes. MediaStar's manuals cover the on-screen menus only.

This is not surprising. MediaStar is a regional brand for receivers built on reference designs
rather than a manufacturer with a protocol to document.

---

## Why NEC at 38 kHz is nevertheless a safe assumption

MS-MINI class receivers are built on the **GX6605S** and **1506-series** chipsets. Two things
follow from that:

1. Those reference designs use plain NEC at 38 kHz for the IR front end. The receiver modules
   fitted to these boards are 38 kHz parts.
2. The key mapping is **firmware data, not silicon**. These boxes carry an editable keymap, and
   the community tools that reflash them, such as the widely circulated "GX6605S Hello Merger",
   exist specifically to rewrite the remote keymap and front-panel key assignments.

Point 2 is the important one. Every vendor flashing one of these images picks its own command
bytes, and a vendor can change them between firmware revisions of the same model. Even a
genuine capture from one MS-MINI 790 Super would not be guaranteed to match another one. This is
why the app treats the code table as user data rather than as a constant, and why Discovery is
the headline feature rather than a fallback.

---

## The shipped candidate table

`DefaultProfiles.candidateCommands` uses NEC address `0x00` with a contiguous command block:

| Buttons | Commands |
|---|---|
| Digits 0 to 9 | `0x00` to `0x09` at face value |
| Power, Mute | `0x0A`, `0x0B` |
| Audio, APP, Wifi, Info (the coloured keys) | `0x0C` to `0x0F` |
| EPG, Zoom, Meter, TXT | `0x10` to `0x13` |
| Play, Stop, Pause, Rec | `0x14` to `0x17` |
| Previous, Next, Rewind, Forward | `0x18` to `0x1B` |
| Sat, F1 | `0x1C`, `0x1D` |
| Up, Down, Left, Right, OK | `0x1E` to `0x22` |
| Menu, Exit | `0x23`, `0x24` |
| Sub, Fav, Recall, USB | `0x25` to `0x28` |
| Page up, Page down | `0x29`, `0x2A` |
| TV/R, Timer | `0x2B`, `0x2C` |

There is no separate red/green/yellow/blue strip on this handset. The four
coloured keys **are** Audio, APP, Wifi and Info, printed in red, green, amber
and blue, which is the usual arrangement on this class of receiver. They are
modelled as one key each, so the app has 45 buttons in total.

Contiguity is deliberate. If any one key from this table turns out to work, the rest are very
likely to be nearby, and a Discovery sweep converges in seconds instead of minutes. If none work,
nothing has been lost: a wrong NEC frame is simply ignored by the receiver.

**Do not cite this table as MediaStar's codes.** It is a search starting point.

---

## Finding your receiver's codes

In rough order of effort:

1. **Try other addresses first.** Settings → *Candidate profile address* rebuilds the whole table
   on a different NEC address. If the receiver uses a contiguous block, the address is the only
   thing wrong. Worth trying `0x00`, `0x01`, `0x02`, `0x04`, `0x08`, `0x40`, `0x80` before
   anything else.
2. **Run a Discovery sweep.** Learn Mode, one button, watch the receiver. Once the first code
   lands you know the address and the rest follow quickly.
3. **Capture with hardware.** An Arduino or ESP32 with a TSOP38238 receiver and the
   `Arduino-IRremote` library prints the address and command of any button you press on the
   physical handset, in seconds, with no guessing. A Flipper Zero or a Broadlink hub does the
   same. Paste the result into Learn Mode → Import. If you have access to the physical remote and
   ten minutes of hardware time, this is by far the best route.
4. **Contribute what you find.** A confirmed MediaStar table would be worth submitting to irdb,
   since there is currently nothing there.

---

## Recording what you find

Once you have a working profile, Settings → *Copy active profile to clipboard* gives you the
whole thing as JSON. Keep a copy outside the phone. The app's DataStore file is included in
Android backup, but a text copy costs nothing and survives a factory reset.

If you want the codes in a portable form, the app can render any stored signal to Pronto CCF
through `RemoteViewModel.toPronto`, which is the format every other remote app and database
understands.

---

## Sources

- [irdb infrared remote control code database](https://github.com/probonopd/irdb)
- [RemoteCentral NEC protocol hex code database](https://www.remotecentral.com/cgi-bin/codes/nec/)
- [The lircd.conf file format](https://www.lirc.org/html/configure.html)
- [LIRC configuration guide](https://lirc.sourceforge.net/lirc.org/html/configuration-guide.html)
- [Vishay: Data formats for IR remote control](https://www.vishay.com/docs/80071/dataform.pdf)
- [Renesas AN-1184: Remote control IR receiver and decoder](https://www.renesas.com/en/document/apn/1184-remote-control-ir-receiver-decoder)
- [GX6605S keymap adjustment tooling](https://www.receiveroption.com/2021/01/gx6605s-hello-merger-v11-tool-for.html)
- [Arduino-IRremote, for hardware capture](https://github.com/Arduino-IRremote/Arduino-IRremote)
- [ConsumerIrManager API reference](https://developer.android.com/reference/android/hardware/ConsumerIrManager)
