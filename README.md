# bitwig-harness

A development harness for Bitwig Studio controller extensions. Send MIDI to Bitwig, observe state changes, iterate — without hardware.

## Components

- **midi-bridge** — CLI tool that sends MIDI messages to Bitwig via a virtual MIDI port
- **observer** — Bitwig extension that watches DAW state and writes changes to `/tmp/bitwig-observer.jsonl`

## Prerequisites

- Java 21
- Maven
- Bitwig Studio 5.3+ (Extension API 21)
- A virtual MIDI port (see setup below)

## Build

```bash
mvn install
```

This builds both modules and copies the observer extension to your Bitwig Extensions folder.

## Virtual MIDI Port Setup

The midi-bridge sends MIDI through a virtual port that Bitwig receives as input. Each OS has its own mechanism — this is a one-time setup.

### macOS — IAC Driver

1. Open **Audio MIDI Setup** (in /Applications/Utilities)
2. Show MIDI Studio (Window > Show MIDI Studio, or Cmd+2)
3. Double-click **IAC Driver**
4. Check **Device is online**
5. In the Ports table, click **+** to add a port named `Bitwig Harness`
6. Click Apply

Bitwig will now see "IAC Driver Bitwig Harness" as a MIDI input.

### Linux — ALSA Virtual Port

Create a virtual MIDI port using `snd-virmidi` or `aconnect`:

```bash
# Load the virtual MIDI kernel module (if not already loaded)
sudo modprobe snd-virmidi

# List available ports
aconnect -l

# The midi-bridge will connect to the virtual port automatically
```

Alternatively, use a persistent virtual port with a udev rule or systemd service.

### Windows — loopMIDI

1. Download and install [loopMIDI](https://www.tobias-erichsen.de/software/loopmidi.html) by Tobias Erichsen
2. Run loopMIDI, create a new port named `Bitwig Harness`
3. The port persists across reboots (loopMIDI runs as a tray application)

## Usage

### Sending MIDI

```bash
# List available MIDI output ports
java -jar midi-bridge/target/midi-bridge-*.jar list-ports

# Send a CC message
java -jar midi-bridge/target/midi-bridge-*.jar send --cc 10 --value 127 --port "Bitwig Harness"
```

### Observing State

The observer extension writes state changes to `/tmp/bitwig-observer.jsonl` in JSONL format (one JSON object per line):

```bash
# Stream all state changes
tail -f /tmp/bitwig-observer.jsonl

# Filter for transport changes
tail -f /tmp/bitwig-observer.jsonl | jq 'select(.type=="transport")'

# Get the last few updates
tail -5 /tmp/bitwig-observer.jsonl | jq .
```

### Example Workflow

```bash
# 1. Send "play" via MIDI
java -jar midi-bridge/target/midi-bridge-*.jar send --cc 10 --value 127

# 2. Check that transport started
sleep 0.2
tail -5 /tmp/bitwig-observer.jsonl | jq 'select(.type=="transport")'
# => {"ts":"...","type":"transport","state":"playing"}
```

## Configuring Bitwig

1. Build and install: `mvn install`
2. Restart Bitwig Studio
3. Go to Settings > Controllers
4. The Bitwig Observer extension should appear — enable it
5. For testing a specific controller extension: configure its MIDI input to use the virtual port
