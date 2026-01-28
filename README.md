# BitwigLooperPlace

A professional Bitwig Studio extension that provides advanced looper functionality with clip launcher control, audio routing swap, OSC support, and persistent overdub marking.

## 🎯 Features

- **Looper Groups**: Create and manage multiple independent 2-track looper systems
- **Clip Launcher Control**: Launch and stop clips with full ClipLauncherSlotBank integration
- **Audio Routing Swap**: Seamlessly toggle recording tracks without audio artifacts
- **Persistent Overdub Marking**: Track state management with internal persistence
- **OSC Control**: Open Sound Control interface on port 9001
- **Command Interface**: Text-based command system supporting 8 distinct operations

## 📋 Requirements

- **Bitwig Studio**: 12.0 or newer
- **Java**: 8 or newer
- **Build Tool**: Maven 3.6+

## 🏗️ Project Structure

```
src/main/java/com/logiccuteguy/
├── BitwigLooperPlaceExtension.java              (Main extension integrator)
├── BitwigLooperPlaceExtensionDefinition.java    (Extension metadata)
├── LooperGroup.java                             (2-track looper system)
├── LooperCommandManager.java                    (Command parsing engine)
└── OSCLooperController.java                     (OSC interface)
```

## 🔧 Building

```bash
mvn clean install
```

This generates the `.bwextension` file in `target/BitwigLooperPlace.bwextension`.

## 📦 Technical Stack

- **Language**: Java 8
- **Framework**: Bitwig Extension API v24
- **Build System**: Maven
- **Protocol**: OSC (Open Sound Control)
- **Size**: ~15 KB (compiled)

## 🚀 Usage

### Installation

Copy `target/BitwigLooperPlace.bwextension` to your Bitwig extensions directory.

### Commands

The extension supports the following command format: `/looper/{id}/{command}`

Supported commands:
- `create` - Create a new looper group
- `mark` - Mark a track for overdub
- `unmark` - Remove overdub mark
- `launch` - Launch a clip from a clip slot
- `stop` - Stop clip playback
- `swap` - Toggle recording track
- `record` - Start recording
- `list` - List all looper groups

### OSC Interface

Default listening address: `localhost:9001`

Example OSC messages:
```
/looper/0/create
/looper/0/launch 0
/looper/0/stop 0
/looper/0/swap
```

## 📄 License

This project is provided as-is for use with Bitwig Studio.

## 🔗 References

- [Bitwig Studio](https://www.bitwig.com)
- [Bitwig Extension API Documentation](https://www.bitwig.com/community/extensions)
- [OSC Specification](http://opensoundcontrol.org/)
