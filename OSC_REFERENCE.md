# Bitwig Looper OSC Control Reference

This document describes the Open Sound Control (OSC) interface for the Bitwig Looper extension.

## Connection Details

- **Protocol:** OSC (Open Sound Control) over UDP
- **Default Port:** 9001
- **Host:** localhost (127.0.0.1)
- **Format:** `/looper/{id}/{command}` with optional arguments

## Commands

### Record Control

#### Start Recording
```
/looper/{id}/rec
```
- Starts recording on the active overdub layer
- Disables recording on the other layer
- Use with layer swapping for overdub functionality

**Example:**
```
/looper/0/rec
```

#### Stop Recording
```
/looper/{id}/stop
```
- Stops recording on the active layer
- Marks the layer as having recorded content
- Does NOT stop playback

**Example:**
```
/looper/0/stop
```

### Layer Control

#### Next Layer (Overdub Swap)
```
/looper/{id}/next
```
- Switches to the next recording layer (0 → 1, 1 → 0)
- Maintains playback on current layer while swapping record destination
- Used for overdub recording workflow

**Example:**
```
/looper/0/next
```

#### Clear All
```
/looper/{id}/clear
```
- Stops all playback and recording
- Does NOT clear recorded clips (they remain in the clip slots)
- Use this to silence the looper

**Example:**
```
/looper/0/clear
```

### Clip Launcher

#### Launch Clip
```
/looper/{id}/launch {layer} {slot}
```
- Launches a clip at the specified layer and slot
- `{layer}`: 0 or 1 (which overdub layer)
- `{slot}`: 0-7 (clip slot number, depending on looper configuration)

**Example:**
```
/looper/0/launch 0 0
```
Launches the clip at layer 0, slot 0

```
/looper/0/launch 1 3
```
Launches the clip at layer 1, slot 3

## RC-505 Workflow in Bitwig

The Bitwig Looper replicates the Roland RC-505 overdub workflow:

1. **Record First Layer:** `rec` → record on layer 0
2. **Stop Recording:** `stop` → mark layer 0 as recorded
3. **Switch Layer:** `next` → switch record to layer 1 (layer 0 continues playing)
4. **Overdub:** `rec` → record on layer 1 while layer 0 plays
5. **Stop Overdub:** `stop` → mark layer 1 as recorded
6. **Next Overdub:** `next` → switch record back to layer 0
7. **Clear:** `clear` → stop all playback

## Looper ID

Each looper group is assigned a unique ID:
- Created with: `create {name}` command (see Command Reference)
- IDs are auto-incremented starting from 0
- Use `list` command to see all looper IDs

## Multiple Loopers

You can control multiple looper groups by using different `{id}` values:

```
/looper/0/rec      # Start recording on looper 0
/looper/1/rec      # Start recording on looper 1
/looper/0/next     # Switch layers on looper 0
/looper/1/next     # Switch layers on looper 1
```

## External Controller Setup

To send OSC messages from external controllers:

1. **OSC Client Configuration:**
   - Send to: `127.0.0.1:9001`
   - Protocol: UDP
   - Message format: Text strings

2. **Controller Examples:**
   - Boss RC-505 (with OSC support)
   - iPad apps: TouchOSC, OSCsmith
   - Ableton Live: Max for Live with OSC
   - Custom scripts using Python python-osc or similar

## Implementation Notes

- The OSC controller runs on a separate thread to avoid blocking the Bitwig extension
- Messages are processed immediately upon receipt
- Invalid looper IDs are silently ignored
- The OSC server initializes automatically when the extension loads
- Port 9001 is hardcoded; modify `OSCLooperController.java` to change it

## Future Enhancement

To use Bitwig's native `com.bitwig.extension.api.opensoundcontrol` API in the future:
- The current implementation uses manual UDP sockets for compatibility
- Bitwig v24's native OSC API requires a specific callback signature (4 parameters)
- Once the exact API is documented, the controller can be refactored for better integration

## Troubleshooting

**OSC Messages Not Working:**
- Verify the looper ID is correct (`list` command shows all IDs)
- Check that port 9001 is not blocked by firewall
- Ensure OSC controller output on extension initialization (check Bitwig console)

**Port Already in Use:**
- Another application is using port 9001
- Change the port in `OSCLooperController.java` line 15: `oscPort = 9001;`
- Rebuild the extension

**Looper Not Found:**
- Ensure the looper was created with the `create {name}` command first
- Use the correct ID from the `list` command output
