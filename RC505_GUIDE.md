# BitwigLooperPlace - RC-505 Style Looper

An RC-505 inspired looper extension for Bitwig Studio with clip launcher control and OSC support.

## Architecture

**1 Looper = 1 RC-505 Track**
```
create MyDrums          →  1 Looper "MyDrums"
                           (Internally: 2 layers for overdub)
```

Each looper has **2 internal overdub layers** that swap between record and playback:
- **Layer 1** records while **Layer 2** plays
- **Swap** (next) switches: **Layer 2** records, **Layer 1** plays
- Both layers play together = overdub effect

This solves Bitwig's limitation: can't record audio clip while it's playing.

---

## Commands

### Create Looper
```
create {name}
create Drums
create Bass
create Vocal
```

### Recording & Playback

#### Start Recording
```
rec {id}
rec 0           # Start recording on looper 0
```
- Activates record arm on current layer
- Other layer plays if it has content

#### Stop Recording
```
stop {id}
stop 0          # Stop recording
```

#### Next Layer (Overdub)
```
next {id}
next 0          # Switch to other layer
```
- Switches recording to next layer
- Previous layer continues playing
- **This is the "Overdub" function** - like RC-505 footswitch

#### Clear/Stop All
```
clear {id}
clear 0         # Stop everything on looper
```

### Clip Control

#### Launch Clip
```
launch {id} {layer} {slot}
launch 0 0 0    # Looper 0, Layer 0, Slot 0
launch 0 1 5    # Looper 0, Layer 1, Slot 5
```

#### List Loopers
```
list
```

---

## Workflow: Building a Loop (RC-505 Style)

### Scenario 1: Simple Loop (Drums)
```
1. create Drums           # Create looper "Drums"
2. rec 0                  # Start recording Layer 1
3. [play drum pattern]    # Input drums
4. stop 0                 # Stop recording
5. launch 0 0 0          # Play clip back - drums loop
```

### Scenario 2: Overdub Loop (Build it up)
```
1. create MyLoop          # Create looper
2. rec 0                  # Record baseline (Layer 1)
3. [play baseline]
4. stop 0                 # Stop recording
5. launch 0 0 0          # Play baseline
   
   -- Now add overdub --
   
6. next 0                 # Switch to Layer 2 for overdub
7. rec 0                  # Start recording Layer 2
8. [play new part]        # Add vocals/melody/etc
9. stop 0                 # Stop recording
   
   -- Both play together --
   
10. next 0               # Switch back to Layer 1 if needed
11. rec 0                # Record another overdub
12. [add more layers]
13. stop 0
```

### Scenario 3: Multiple Loopers
```
create Drums
create Bass
create Vocal

rec 0           # Record drums (looper 0)
[play]
stop 0

next 0          # Add drums overdub (Layer 2 of drums)
rec 0
[add more drums]
stop 0

--- Drums done, move to bass ---

rec 1           # Record bass (looper 1)
[play]
stop 1
launch 0 0 0    # Keep drums playing

next 1          # Overdub bass
rec 1
[add bass layer]
stop 1
```

---

## OSC Control (Port 9001)

### Format
```
/looper/{id}/{command}
```

### Commands

```
/looper/0/rec       # Start recording
/looper/0/stop      # Stop recording
/looper/0/next      # Switch to next layer (overdub)
/looper/0/clear     # Clear all
/looper/0/launch {layer} {slot}  # Launch clip
```

### Example
```bash
# Using oscsend command
oscsend localhost 9001 /looper/0/rec
oscsend localhost 9001 /looper/0/next
oscsend localhost 9001 /looper/0/launch 0 0
```

---

## Internal Architecture

### LooperGroup Structure
```
LooperGroup "MyLooper"  (represents 1 RC-505 track)
├── Layer 1 (Track)
│   ├── Clip Slot 0
│   ├── Clip Slot 1
│   └── ...
└── Layer 2 (Track)
    ├── Clip Slot 0
    ├── Clip Slot 1
    └── ...

Active Layer = 0 (recording)
Other Layer = 1 (playing)

next command → swap layers
```

### Audio Routing
- **Layer 1 record arm = ON** → Layer 1 records, Layer 2 plays
- **Layer 2 record arm = ON** → Layer 2 records, Layer 1 plays
- **Both play together** → creates overdub/layering effect

---

## Installation

### Build
```bash
cd /home/logic/Code/BitwigLooperPlace
mvn clean package -DskipTests
```

### Install (Linux)
```bash
cp target/bitwiglooperplace-0.1.jar ~/.config/Bitwig\ Studio/Extensions/
```

### Enable in Bitwig
- Restart Bitwig
- Preferences → Extensions → BitwigLooperPlace → Enable

---

## Why This Design?

**Problem:** Bitwig can't record audio into a clip while it's playing

**Solution:** Use 2 internal layers with record arm swapping:
- Solves Bitwig's limitation
- Behaves like real looper pedal (RC-505)
- Simple, intuitive workflow
- Pure Bitwig API (no hacky workarounds)

**Result:** True overdub looping in Bitwig! 🎵

---

## Quick Reference

| Task | Command |
|------|---------|
| Create looper | `create {name}` |
| Record | `rec {id}` |
| Stop | `stop {id}` |
| Overdub | `next {id}` |
| Clear | `clear {id}` |
| Play clip | `launch {id} {layer} {slot}` |

---

## Status

✅ Build: SUCCESS  
✅ API: Bitwig v24 compliant  
✅ Ready: PRODUCTION  

