# BitwigLooperPlace - Advanced Looper Extension

A powerful Bitwig Studio extension for creating multi-track looper groups with clip launcher control, OSC support, and audio routing swap for seamless overdub recording.

## Features

### 1. **Looper Groups** 
- Create multiple independent looper groups
- Each group contains 2 synchronized audio tracks
- Dynamic track management via commands

### 2. **Clip Launcher Control**
- Control clips and clips slots via the extension
- Launch, stop, and manage clips programmatically
- Full clip launcher slot bank integration

### 3. **Audio Routing Swap**
- Swap active recording track without stopping playback
- One track records while the other plays
- Perfect for overdub recording with continuous looping

### 4. **Persistent Overdub Marking**
- Mark tracks for overdub before recording
- Marks persist until explicitly removed
- Internal state management for reliable tracking

### 5. **OSC Control**
- Remote control via Open Sound Control (OSC)
- Listen on port 9001 by default
- Command-based interface for all operations

### 6. **Command Interface**
- Easy-to-use text command system
- Support for MIDI, OSC, and programmatic control
- Extensible architecture for custom commands

---

## Installation

### Prerequisites
- Bitwig Studio 12.0 or later
- Java 8+
- Maven 3.6+

### Build & Install

```bash
cd /home/logic/Code/BitwigLooperPlace
mvn clean package
```

The compiled extension will be in `target/bitwiglooperplace-0.1.jar`

Install to your Bitwig extensions directory:
```bash
cp target/bitwiglooperplace-0.1.jar ~/.config/Bitwig\ Studio/Extensions/
```

Then enable in Bitwig Studio → Preferences → Extensions → BitwigLooperPlace

---

## Usage Guide

### Creating a Looper Group

**Command:** `create {name}`

Creates a new looper group with 2 audio tracks.

```
create MyLooper
```

This creates:
- Group track named "MyLooper Group"
- Track 1: "MyLooper - Track 1"
- Track 2: "MyLooper - Track 2"

### Marking Tracks for Overdub

**Command:** `mark {groupId} {track}`

Mark a track (0 or 1) for overdub recording. The mark persists until explicitly removed.

```
mark 0 0        // Mark group 0, track 0 for overdub
mark 0 1        // Mark group 0, track 1 for overdub
```

### Launching Clips

**Command:** `launch {groupId} {track} {slot}`

Launch a clip on a specific track and slot.

```
launch 0 0 0    // Group 0, Track 0, Slot 0
launch 0 1 5    // Group 0, Track 1, Slot 5
```

### Swapping Audio Routing

**Command:** `swap {groupId}`

Toggle record enable between the two tracks. Active track switches for overdub recording while the other continues playing.

```
swap 0          // Swap audio routing on group 0
```

After swap:
- Track 1 record enabled → Record arm goes to Track 1
- Track 0 record disabled → Record arm leaves Track 0
- Next swap: Track 0 record enabled, Track 1 disabled

### Setting Record State

**Command:** `record {groupId} {track} {true|false}`

Enable or disable recording on a specific track.

```
record 0 0 true     // Enable recording on group 0, track 0
record 0 1 false    // Disable recording on group 0, track 1
```

### Stopping Playback

**Command:** `stop {groupId} {track}`

Stop clip playback on a track.

```
stop 0 0    // Stop group 0, track 0
stop 0 1    // Stop group 0, track 1
```

### Listing Looper Groups

**Command:** `list`

Shows all active looper groups.

```
list
```

---

## OSC Control

### Overview
The OSC controller listens on **localhost:9001** by default.

### OSC Command Format

```
/looper/{groupId}/{command} {arguments}
```

### Supported OSC Commands

#### Mark Track for Overdub
```
/looper/0/mark 0        // Mark group 0, track 0
/looper/0/mark 1        // Mark group 0, track 1
```

#### Unmark Track
```
/looper/0/unmark 0      // Unmark group 0, track 0
```

#### Launch Clip
```
/looper/0/launch 0 5    // Group 0, Track 0, Slot 5
/looper/1/launch 1 3    // Group 1, Track 1, Slot 3
```

#### Stop Track
```
/looper/0/stop 0        // Stop group 0, track 0
```

#### Swap Audio Routing
```
/looper/0/swap          // Swap routing on group 0
```

#### Set Record State
```
/looper/0/record 0 true     // Enable record on group 0, track 0
/looper/0/record 1 false    // Disable record on group 0, track 1
```

### Example OSC Client (Python)

```python
import socket

def send_osc(command, *args):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    message = f"{command} " + " ".join(str(a) for a in args)
    sock.sendto(message.encode(), ("localhost", 9001))
    sock.close()

# Create looper group
send_osc("/looper/0/mark", 0)

# Launch clip
send_osc("/looper/0/launch", 0, 0)

# Swap routing
send_osc("/looper/0/swap")
```

---

## Workflow Example: Overdub Recording

### Scenario: Building a Loop

1. **Create Looper Group**
   ```
   create LoopA
   ```

2. **Record First Track**
   - Mark Track 0 for overdub: `mark 0 0`
   - Enable recording: `record 0 0 true`
   - Play audio into the looper
   - Clip launches automatically

3. **Add Overdub on Track 1**
   - Swap routing: `swap 0`
   - Mark Track 1: `mark 0 1` (auto-marked on record enable)
   - Record enable still active on Track 1
   - Add new performance (vocals, instruments, etc.)
   - Both tracks now loop simultaneously

4. **Switch Back to Track 0 for Another Overdub**
   - Swap routing: `swap 0` (returns to Track 0)
   - Continue recording new layers

5. **Stop Recording**
   - Disable recording: `record 0 0 false`
   - Loops continue playing
   - Clean transition, no pops or clicks

---

## Architecture

### Core Classes

#### `LooperGroup.java`
Manages a single looper group with 2 audio tracks.

**Key Methods:**
- `markTrackForOverdub(int trackIndex)` - Mark track for overdub
- `unmarkTrackForOverdub(int trackIndex)` - Remove overdub mark
- `swapAudioRouting()` - Toggle record enable between tracks
- `launchClip(int trackIndex, int slotIndex)` - Launch clip
- `stopTrack(int trackIndex)` - Stop playback
- `setTrackRecordEnabled(int trackIndex, boolean enabled)` - Set record state

#### `LooperCommandManager.java`
Manages command interface and looper creation.

**Key Methods:**
- `createLooperGroup(String groupName)` - Create new looper
- `executeCommand(String commandLine)` - Execute text commands
- `listLooperGroups()` - List all active loopers

#### `OSCLooperController.java`
Handles OSC message reception and routing.

**Key Methods:**
- `processOSCMessage(DatagramPacket packet)` - Parse OSC messages
- `handleOSCCommand(LooperGroup looper, String action, String[] args)` - Execute OSC commands
- `sendOSCMessage(String address, Object... args)` - Send OSC responses

#### `BitwigLooperPlaceExtension.java`
Main extension class that integrates all components.

---

## Performance Notes

- Each looper group uses 2 audio tracks
- Overdub state is persisted in memory
- Audio routing changes are real-time with no latency
- OSC messages are processed asynchronously to avoid blocking
- Supports up to 16 looper groups simultaneously

---

## Troubleshooting

### Looper Not Creating
- Check Bitwig console for error messages
- Verify track bank has available space
- Ensure audio tracks are not locked

### OSC Commands Not Working
- Verify OSC controller is running (check logs)
- Check port 9001 is not in use: `netstat -tulpn | grep 9001`
- Ensure message format is correct: `/looper/{id}/{command}`

### Recording Issues
- Make sure input routing is correct in Bitwig
- Verify audio input device is selected
- Check track record arm status

### Clip Launch Not Working
- Ensure clip exists in the slot
- Verify clip launcher mode is set to "trigger"
- Check clip length is appropriate

---

## Customization

### Changing OSC Port
Edit [OSCLooperController.java](OSCLooperController.java) line 20:
```java
private int oscPort = 9001;  // Change this value
```

### Extending Commands
Add new commands in `LooperCommandManager.executeCommand()`:
```java
case "mycommand":
    // Your implementation
    break;
```

### Adding New Track Features
Extend `LooperGroup.java` with additional methods:
```java
public void myCustomMethod(int trackIndex) {
    // Implementation
}
```

---

## Future Enhancements

- [ ] MIDI controller integration
- [ ] Undo/redo functionality
- [ ] Clip recording directly to looper
- [ ] Volume/pan per track
- [ ] Tempo sync modes
- [ ] Audio effects routing
- [ ] Preset saving/loading
- [ ] Extended track groups (3+ tracks)

---

## Support

For issues, feature requests, or contributions:
- Check Bitwig console logs for detailed error messages
- Review extension compatibility with your Bitwig version
- Verify all dependencies are correctly installed

---

## License

BitwigLooperPlace Extension
Copyright (c) 2026 LogicCuteGuy

---

## Version History

### v0.1 (2026-01-28)
- Initial release
- 2-track looper groups with swappable audio routing
- OSC control support
- Command interface for looper management
- Persistent overdub marking
