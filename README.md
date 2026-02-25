# bitwig-harness

A development harness for Bitwig Studio controller extensions. Observe DAW state, send/receive MIDI, control transport and devices — all over OSC, without hardware.

## What It Does

A single Bitwig extension with two complementary uses:

- **Testing harness** (primary) — operate and test controller extensions with programmatically controlled hardware emulations, especially suited for agentic development
- **OSC controller** — even without an extension under test, the harness provides basic direct DAW control and live state observation over OSC. Useful for scripting, automation, or building custom control surfaces.

The harness provides:

- **MIDI proxy** — bidirectional MIDI forwarding between an agent and the extension under test, via a virtual MIDI port
- **State observer** — watches DAW state (transport, tracks, devices, remote controls, clips) and pushes changes over OSC
- **Command interface** — direct DAW control (transport, track/device selection, clip launching, etc.) via OSC

### Testing methodology and rationale

Typical controller extension development cycle: develop (write code), verify (load extension, press buttons on hardware, observe what happens), repeat.

This harness allows an external **driver** (not included):
1. drive the extension (by sending MIDI)
2. observe the controller state (by receiving MIDI from extension)
3. observe the state of the project and Bitwig, monitor extension's effects

In other words, programmatic verification/QA. 

The **driver** can be a programmatically controlled emulator, overseen by AI agent who understands controller's MIDI assignments. Point your agent at this repo and test away.

## Prerequisites

- Java 21
- Maven
- Bitwig Studio 5.3+ (Extension API 21)
- A virtual MIDI port for the MIDI proxy (see setup below)
- (optional) Python 3 (for included OSC tools) or `oscsend` / `oscdump` from [liblo](https://github.com/radarsat1/liblo)

## Build

```bash
mvn install
```

Builds the extension and copies it to your Bitwig Extensions folder (`~/Documents/Bitwig Studio/Extensions/`).

## Included Tools

`tools/` contains zero-dependency Python scripts that replace liblo's `oscsend`/`oscdump`:

- **`oscsend.py`** — send OSC messages from the command line
- **`osclisten.py`** — listen for OSC messages (dual-stack IPv4+IPv6, which matters because Bitwig sends from IPv6)

```bash
# Listen for state updates
./tools/osclisten.py 9001

# Send a command
./tools/oscsend.py localhost 9000 /connect i 9001
./tools/oscsend.py localhost 9000 /transport/play
```

No installation needed — just Python 3. The quick start examples below use liblo's `oscsend`/`oscdump` syntax, but the included tools work the same way.

## Quick Start

```bash
# Terminal 1: Listen for state updates
oscdump 9001

# Terminal 2: Connect and send commands
oscsend localhost 9000 /connect i 9001
oscsend localhost 9000 /transport/play
oscsend localhost 9000 /track/select i 2
oscsend localhost 9000 /midi/send i 0 i 144 i 60 i 127   # Note On C4 on ch0
oscsend localhost 9000 /midi/sysex/send s "f07e7f0601f7"  # Sysex identity request
oscsend localhost 9000 /remote_control/set i 0 f 0.5      # Set first param to 50%
oscsend localhost 9000 /clip/launch i 0 i 0                # Launch clip at track 0, scene 0
oscsend localhost 9000 /disconnect i 9001
```

## Connection Protocol

1. Driver sends `/connect` with a reply port — harness immediately sends a full state snapshot
2. Harness pushes state changes to all registered clients as they occur
3. Driver sends `/disconnect` when done

Multiple clients can connect simultaneously (ports 9001–9016).

## How the MIDI Proxy Works

This is a convenience feature, the driver can communicate over MIDI without sending actual MIDI.

Bitwig controller extensions receive MIDI from MIDI ports. The harness can act as a man-in-the-middle: both the harness and the extension under test connect to the same virtual MIDI port, allowing the driver to send and receive MIDI over OSC.

```
Driver (OSC)                      Bitwig Studio
───────────────                  ──────────────────────────────────────
                                 ┌──────────────┐   ┌────────────────┐
 /midi/send ────────► Harness ──►│ Virtual MIDI │──►│ Extension      │
                      Extension  │ Port         │   │ under test     │
 /midi/in   ◄──────── Harness ◄──│             ◄│───│                │
                                 └──────────────┘   └────────────────┘
```

**Sending MIDI to the extension under test:**
Driver sends `/midi/send` → harness writes to its MIDI out port → virtual port carries it → extension under test reads from its MIDI in port.

**Receiving MIDI from the extension under test:**
Extension writes to its MIDI out port → virtual port → harness reads from its MIDI in port → broadcasts `/midi/in` to all connected drivers.

Both extensions see the same virtual port, but from opposite sides. The harness's MIDI out is the extension's MIDI in, and vice versa.

> If you only need DAW control and state observation (no MIDI proxy), skip the virtual MIDI port setup entirely — the harness works fine without it.

## OSC Address Space

### Client Lifecycle

| Address | Args | Direction | Description |
|---------|------|-----------|-------------|
| `/connect` | `<port:i>` | driver → harness | Register reply port; triggers full state snapshot |
| `/disconnect` | `<port:i>` | driver → harness | Unregister reply port |

### MIDI Proxy

| Address | Args | Direction | Description |
|---------|------|-----------|-------------|
| `/midi/send` | `<channel:i> <status:i> <data1:i> <data2:i>` | driver → harness | Send MIDI to extension under test via virtual port |
| `/midi/in` | `<channel:i> <status:i> <data1:i> <data2:i>` | harness → driver | MIDI received from extension under test |
| `/midi/sysex/send` | `<hex:s>` | driver → harness | Send sysex to extension under test via virtual port |
| `/midi/sysex/in` | `<hex:s>` | harness → driver | Sysex received from extension under test |

### DAW Control

| Address | Args | Direction | Description |
|---------|------|-----------|-------------|
| `/transport/play` | — | driver → harness | Start playback |
| `/transport/stop` | — | driver → harness | Stop playback |
| `/transport/record` | — | driver → harness | Start recording |
| `/track/select` | `<index:i>` | driver → harness | Select track by index |
| `/track/bank/scroll` | `<position:i>` | driver → harness | Scroll track bank to position |
| `/device/select` | `<index:i>` | driver → harness | Select device by index |
| `/remote_control/page/next` | — | driver → harness | Next remote controls page |
| `/remote_control/page/prev` | — | driver → harness | Previous remote controls page |
| `/remote_control/page/select` | `<index:i>` | driver → harness | Select remote controls page by index |
| `/remote_control/set` | `<index:i> <value:f>` | driver → harness | Set parameter value (0.0–1.0) |
| `/clip/launch` | `<track:i> <scene:i>` | driver → harness | Launch clip at position |
| `/clip/create` | `<track:i> <scene:i>` | driver → harness | Create empty clip (4 bars) |
| `/scene/launch` | `<scene:i>` | driver → harness | Launch scene |
| `/undo` | — | driver → harness | Undo last action |

### State Updates (pushed on change)

| Address | Args | Description |
|---------|------|-------------|
| `/state/transport` | `<state:s>` (playing/stopped/recording) | Transport state |
| `/state/cursor_track` | `<index:i> <name:s>` | Selected track |
| `/state/device` | `<name:s> <index:i>` | Selected device |
| `/state/track` | `<bank_index:i> <name:s> <position:i> <type:s> <mute:i> <solo:i> <arm:i> <volume:f>` | Track in bank (8 tracks) |
| `/state/track_bank` | `<scroll_position:i>` | Track bank scroll offset |
| `/state/remote_control/page` | `<name:s> <index:i> <count:i>` | Current remote controls page |
| `/state/remote_control/param` | `<index:i> <name:s> <value:f>` | Remote control parameter (8 params) |
| `/state/clip` | `<track:i> <scene:i> <has_content:i> <is_playing:i> <is_recording:i>` | Clip slot state (8×8 grid) |

## Virtual MIDI Port Setup

A virtual MIDI port to communicate between the driver and the extension under test. This is a one-time setup per OS.

### macOS — IAC Driver

1. Open **Audio MIDI Setup** (in /Applications/Utilities)
2. Show MIDI Studio (Window > Show MIDI Studio, or Cmd+2)
3. Double-click **IAC Driver**
4. Check **Device is online**
5. In the Ports table, click **+** to add a port named `Bitwig Harness`
6. Click Apply

### Linux — ALSA Virtual Port

```bash
# Load the virtual MIDI kernel module
sudo modprobe snd-virmidi

# List available ports
aconnect -l
```

### Windows — loopMIDI

1. Download and install [loopMIDI](https://www.tobias-erichsen.de/software/loopmidi.html)
2. Create a new port named `Bitwig Harness`

## Configuring Bitwig

### Basic setup (DAW control + state observation)

1. Build and install: `mvn install`
2. Restart Bitwig Studio (or reload extensions)
3. Go to Settings > Controllers
4. Add the **Harness** controller

This is enough for transport control, track/device selection, remote controls, clip launching, and state observation.

### MIDI proxy setup (for testing controller extensions)

After completing basic setup, wire the MIDI proxy to a virtual MIDI port:

5. In the Harness controller settings, set **MIDI In** and **MIDI Out** to the virtual MIDI port (e.g. "IAC Driver Bitwig Harness")
6. In the extension under test's controller settings, set its **MIDI In** to the same virtual MIDI port

The key: both extensions share the same virtual port. The harness's MIDI out becomes the extension's MIDI in. When the extension sends MIDI out, the harness picks it up on its MIDI in and forwards it to drivers as `/midi/in`.

## Agent Reference

Practical tips for AI agents and scripts working with the harness.

**Bitwig logs** — extension `println`/`errorln` output goes here:
- macOS: `~/Library/Logs/Bitwig Studio/BitwigStudio*.log`
- Linux: `~/.BitwigStudio/log/BitwigStudio*.log`
- Windows: `%LOCALAPPDATA%\Bitwig Studio\log\BitwigStudio*.log`

**Reload extension** — Settings > Controllers > disable/enable the Harness, or restart Bitwig.

**Snapshot on connect** — `/connect` triggers a full state dump immediately. No need to poll or wait for the next change.

**State is async** — after sending a command (e.g. `/transport/play`), allow a brief delay before checking for the corresponding state update. Bitwig processes commands asynchronously.

**Typical test loop:**
1. `oscsend localhost 9000 /connect i 9001` — connect and receive snapshot
2. Send commands (transport, MIDI, etc.)
3. Observe `/state/*` messages on your reply port
4. Assert expected state
5. `oscsend localhost 9000 /disconnect i 9001` — clean up
