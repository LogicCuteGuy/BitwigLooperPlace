# BitwigLooperPlace - Quick Reference

## Command Syntax

### Create Looper
```
create {name}
create MyLooper         # Creates new looper group
```

### Track Control
```
mark {id} {track}       # Mark track for overdub (0 or 1)
unmark {id} {track}     # Remove overdub mark
record {id} {track} {true|false}    # Enable/disable record
```

### Clip Control
```
launch {id} {track} {slot}   # Launch clip at slot
stop {id} {track}             # Stop track playback
```

### Audio Routing
```
swap {id}               # Toggle active recording track
```

### Info
```
list                    # Show all looper groups
```

---

## Workflow Cheat Sheet

### Single Track Loop (Basic)
```
create Loop1            # Create group
mark 0 0               # Mark track 0
record 0 0 true       # Start recording
                      # Play audio...
record 0 0 false      # Stop recording
launch 0 0 0          # Play clip
```

### Overdub Pattern (Two Tracks)
```
create Loop2           # Create group
mark 0 0              # Mark track 0
record 0 0 true      # Record baseline
record 0 0 false     # Stop

swap 0                # Switch to track 1
record 0 1 true      # Add overdub
record 0 1 false     # Stop

swap 0                # Back to track 0 if needed
launch 0 0 0         # Launch base clip
```

### Continuous Overdub Loop
```
create LoopOver
mark 0 0
record 0 0 true
                      # Record and play
record 0 0 false
                      # Now add overdub without stopping
swap 0                # Switch track (track 1 now records)
record 0 1 true
                      # Add new layer while track 0 plays
record 0 1 false
                      # Continue swapping and adding layers
swap 0
record 0 0 true
```

---

## OSC Quick Reference

### Basic OSC Format
```
/looper/{groupId}/{command} {args}
```

### Common OSC Sequences

#### Launch and Play
```
/looper/0/mark 0
/looper/0/record 0 true
/looper/0/record 0 false
/looper/0/launch 0 0
```

#### Swap and Overdub
```
/looper/0/swap
/looper/0/record 0 true
/looper/0/record 0 false
/looper/0/swap
```

#### Stop All
```
/looper/0/stop 0
/looper/0/stop 1
```

---

## Track Index Reference

- **Track 0** = First audio track in looper
- **Track 1** = Second audio track in looper
- **Group ID** = Looper group number (0, 1, 2, etc.)
- **Slot** = Clip launcher slot number (0+)

---

## Recording Tips

1. **Always mark before recording** - Ensures track is prepared
2. **Swap for clean transitions** - No audio pops or clicks
3. **Stop recording before switching** - Cleanest audio boundaries
4. **Monitor clip slots** - Launch correct clip when ready

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Recording not working | Check input device is selected in Bitwig |
| Clip won't launch | Ensure clip exists at that slot |
| Swap not working | Verify group ID is correct |
| OSC no response | Check port 9001 is open |
| No groups showing | Use `create` to make a new one |

---

## Hot Tips

- Use meaningful names: `create Drums`, `create Bass`, `create Vocals`
- OSC allows remote control from hardware controllers
- Multiple looper groups can run simultaneously
- Track marks persist during the session
- All commands are real-time, no latency

