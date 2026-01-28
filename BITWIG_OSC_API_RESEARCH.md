# Bitwig Studio Extension API - OpenSoundControl (OSC) Support

## Overview

Bitwig Studio provides built-in support for OSC (Open Sound Control) protocol through the `com.bitwig.extension.api.opensoundcontrol` package. This document summarizes the available classes, methods, and best practices for implementing OSC communication in Bitwig extensions based on research of the Bitwig API and DrivenByMoss extension implementation.

---

## Core OSC Classes

### 1. **OscModule** - Main Entry Point
Located in: `com.bitwig.extension.api.opensoundcontrol`

**Purpose:** Provides access to OSC functionality from the ControllerHost

**Key Methods:**
```java
// Get OSC module from host
OscModule oscModule = host.getOscModule();

// Create address space for OSC communication
OscAddressSpace createAddressSpace()

// Connect to remote OSC server (as client)
OscConnection connectToUdpServer(String address, int port, OscAddressSpace space)

// Create local OSC server to receive messages
OscServer createUdpServer(OscAddressSpace addressSpace)
```

---

### 2. **OscServer** - Receiving OSC Messages
Represents a local OSC server that receives incoming messages

**Key Methods:**
```java
// Start listening on specified port
void start(int port) throws IOException

// Get the port the server is listening on
int getListeningPort()

// Stop the server
void stop()
```

**Example Usage:**
```java
OscModule oscModule = host.getOscModule();
OscAddressSpace addressSpace = oscModule.createAddressSpace();

// Register default message handler
addressSpace.registerDefaultMethod((source, message) -> {
    String address = message.getAddressPattern();
    Object[] values = message.getArguments();
    handleOSCMessage(address, values);
});

OscServer server = oscModule.createUdpServer(addressSpace);
server.start(9000);  // Listen on port 9000
```

---

### 3. **OscConnection** - Sending OSC Messages
Represents a connection to a remote OSC server where messages can be sent

**Key Methods:**
```java
// Send single OSC message
void sendMessage(String address, Object... values) throws IOException

// Start an OSC bundle
void startBundle()

// End an OSC bundle
void endBundle() throws IOException

// Close the connection
void close()
```

**Example Usage:**
```java
OscModule oscModule = host.getOscModule();
OscAddressSpace addressSpace = oscModule.createAddressSpace();

// Connect to remote server
OscConnection connection = oscModule.connectToUdpServer("localhost", 9001, addressSpace);

// Send single message
connection.sendMessage("/looper/1/launch", 0, 1);

// Send bundle of messages
connection.startBundle();
connection.sendMessage("/mixer/track/1/volume", 0.5);
connection.sendMessage("/mixer/track/2/volume", 0.75);
connection.endBundle();
```

---

### 4. **OscMessage** - Incoming Message Data
Represents a received OSC message

**Key Methods:**
```java
// Get the OSC address pattern
String getAddressPattern()

// Get all argument values
Object[] getArguments()

// Get number of arguments
int getArgumentCount()

// Get specific argument by index
Object getArgument(int index)
```

**Example Usage:**
```java
void handleOSCMessage(OscMessage message) {
    String address = message.getAddressPattern();
    Object[] args = message.getArguments();
    
    if (address.startsWith("/looper/")) {
        int looperId = Integer.parseInt(args[0]);
        String command = (String) args[1];
        // Process command...
    }
}
```

---

### 5. **OscAddressSpace** - Message Routing
Manages OSC address space and routes incoming messages to handlers

**Key Methods:**
```java
// Register handler for specific address
void registerMethod(String address, OscMessageHandler handler)

// Register default handler for unmatched addresses
void registerDefaultMethod(OscMessageHandler handler)

// Unregister a handler
void unregisterMethod(String address)
```

**Message Handler Interface:**
```java
@FunctionalInterface
interface OscMessageHandler {
    void handleMessage(OscMessageSource source, OscMessage message) throws IOException;
}
```

**Example Usage:**
```java
OscAddressSpace addressSpace = oscModule.createAddressSpace();

// Register specific handler for /looper commands
addressSpace.registerMethod("/looper/*", (source, message) -> {
    String address = message.getAddressPattern();
    Object[] args = message.getArguments();
    
    // Parse and handle looper commands
    handleLooperCommand(address, args);
});

// Register handler for track commands
addressSpace.registerMethod("/track/*/*", (source, message) -> {
    handleTrackCommand(message);
});

// Register catch-all handler
addressSpace.registerDefaultMethod((source, message) -> {
    handleUnknownCommand(message.getAddressPattern());
});
```

---

## ControllerHost Integration

### Get OSC Module from ControllerHost

```java
public class YourExtension implements ControllerExtension {
    private OscModule oscModule;
    private OscServer oscServer;
    private OscConnection oscClient;
    
    @Override
    public void init() {
        oscModule = getHost().getOscModule();
        
        // Create server to receive messages
        initializeOSCServer();
        
        // Create client to send messages
        initializeOSCClient();
    }
    
    private void initializeOSCServer() throws IOException {
        OscAddressSpace addressSpace = oscModule.createAddressSpace();
        addressSpace.registerDefaultMethod(this::handleIncomingOSCMessage);
        
        oscServer = oscModule.createUdpServer(addressSpace);
        oscServer.start(9000);  // Listen on port 9000
    }
    
    private void initializeOSCClient() throws IOException {
        OscAddressSpace addressSpace = oscModule.createAddressSpace();
        oscClient = oscModule.connectToUdpServer("localhost", 9001, addressSpace);
    }
}
```

---

## Registering OSC Message Handlers

### Method 1: Wildcard Patterns

```java
// Matches /looper/1/launch, /looper/2/launch, etc.
addressSpace.registerMethod("/looper/*/launch", (source, message) -> {
    Object[] args = message.getArguments();
    int looperId = Integer.parseInt(args[0].toString());
    int trackIndex = Integer.parseInt(args[1].toString());
    performLaunch(looperId, trackIndex);
});

// Matches /track/1/volume, /track/2/volume, etc.
addressSpace.registerMethod("/track/*/volume", (source, message) -> {
    int trackIndex = Integer.parseInt(message.getAddressPattern().split("/")[2]);
    double volume = ((Number) message.getArgument(0)).doubleValue();
    setTrackVolume(trackIndex, volume);
});
```

### Method 2: Nested Commands

```java
// Matches /mixer/track/1/pan, /mixer/track/1/solo, etc.
addressSpace.registerMethod("/mixer/track/*/*", (source, message) -> {
    String[] parts = message.getAddressPattern().split("/");
    int trackIndex = Integer.parseInt(parts[2]);
    String parameter = parts[3];  // "pan", "solo", etc.
    Object value = message.getArgument(0);
    
    switch(parameter) {
        case "pan":
            setTrackPan(trackIndex, ((Number) value).doubleValue());
            break;
        case "solo":
            setTrackSolo(trackIndex, (Boolean) value);
            break;
    }
});
```

### Method 3: Catch-All Handler

```java
addressSpace.registerDefaultMethod((source, message) -> {
    String address = message.getAddressPattern();
    Object[] values = message.getArguments();
    
    host.println("Received OSC: " + address);
    for (Object value : values) {
        host.println("  - " + value);
    }
});
```

---

## Sending OSC Messages

### Method 1: Direct Message

```java
// Send simple numeric value
oscClient.sendMessage("/looper/1/volume", 0.75);

// Send string value
oscClient.sendMessage("/looper/1/name", "MyLoop");

// Send multiple values
oscClient.sendMessage("/looper/1/launch", 0, 1);  // track 0, slot 1
```

### Method 2: Bundle (Multiple Messages)

```java
try {
    oscClient.startBundle();
    
    oscClient.sendMessage("/looper/1/mark", 0);
    oscClient.sendMessage("/looper/1/swap", true);
    oscClient.sendMessage("/looper/1/record", 1);
    
    oscClient.endBundle();
} catch (IOException e) {
    host.println("Error sending OSC bundle: " + e.getMessage());
}
```

### Method 3: With Type Checking

```java
public void sendOSCValue(OscConnection connection, String address, Object value) throws IOException {
    if (value instanceof Boolean) {
        connection.sendMessage(address, (Boolean) value);
    } else if (value instanceof Integer) {
        connection.sendMessage(address, (Integer) value);
    } else if (value instanceof Double) {
        connection.sendMessage(address, (Double) value);
    } else if (value instanceof String) {
        connection.sendMessage(address, (String) value);
    }
}
```

---

## Complete Implementation Example

```java
public class OSCLooperController {
    private final ControllerHost host;
    private final OscModule oscModule;
    private OscServer oscServer;
    private OscConnection oscClient;
    private final int RECEIVE_PORT = 9000;
    private final String SEND_HOST = "localhost";
    private final int SEND_PORT = 9001;
    
    public OSCLooperController(ControllerHost host) {
        this.host = host;
        this.oscModule = host.getOscModule();
    }
    
    public void initialize() throws IOException {
        // Initialize server to receive OSC messages
        initializeServer();
        
        // Initialize client to send OSC messages
        initializeClient();
        
        host.println("OSC Controller initialized on port " + RECEIVE_PORT);
    }
    
    private void initializeServer() throws IOException {
        OscAddressSpace addressSpace = oscModule.createAddressSpace();
        
        // Register handlers for various OSC addresses
        addressSpace.registerMethod("/looper/*/record", this::handleLooperRecord);
        addressSpace.registerMethod("/looper/*/stop", this::handleLooperStop);
        addressSpace.registerMethod("/looper/*/launch", this::handleLooperLaunch);
        
        // Catch-all handler
        addressSpace.registerDefaultMethod((source, message) -> {
            host.println("Unhandled OSC: " + message.getAddressPattern());
        });
        
        oscServer = oscModule.createUdpServer(addressSpace);
        oscServer.start(RECEIVE_PORT);
    }
    
    private void initializeClient() throws IOException {
        OscAddressSpace addressSpace = oscModule.createAddressSpace();
        oscClient = oscModule.connectToUdpServer(SEND_HOST, SEND_PORT, addressSpace);
    }
    
    private void handleLooperRecord(OscMessageSource source, OscMessage message) throws IOException {
        Object[] args = message.getArguments();
        if (args.length > 0) {
            int looperId = Integer.parseInt(args[0].toString());
            boolean recordEnabled = (Boolean) args[1];
            
            // Perform record action
            host.println("Looper " + looperId + " record: " + recordEnabled);
            
            // Send feedback
            oscClient.sendMessage("/looper/" + looperId + "/status", 
                recordEnabled ? "recording" : "idle");
        }
    }
    
    private void handleLooperStop(OscMessageSource source, OscMessage message) throws IOException {
        Object[] args = message.getArguments();
        if (args.length > 0) {
            int looperId = Integer.parseInt(args[0].toString());
            host.println("Looper " + looperId + " stopped");
        }
    }
    
    private void handleLooperLaunch(OscMessageSource source, OscMessage message) throws IOException {
        Object[] args = message.getArguments();
        if (args.length >= 2) {
            int looperId = Integer.parseInt(args[0].toString());
            int slot = Integer.parseInt(args[1].toString());
            host.println("Launching looper " + looperId + " slot " + slot);
        }
    }
    
    public void sendFeedback(String address, Object value) throws IOException {
        if (oscClient != null) {
            oscClient.sendMessage(address, value);
        }
    }
    
    public void shutdown() throws IOException {
        if (oscServer != null) {
            oscServer.stop();
        }
        if (oscClient != null) {
            oscClient.close();
        }
    }
}
```

---

## OSC Message Format

### Address Patterns
- Use forward slashes to separate hierarchical levels: `/device/parameter`
- Use wildcards for dynamic values: `/looper/*/launch`
- Can use multiple levels: `/mixer/track/1/send/2/volume`

### Value Types
Bitwig OSC supports common types:
- **Boolean**: true/false
- **Integer**: Whole numbers
- **Float/Double**: Decimal numbers
- **String**: Text values

### Example OSC Commands
```
/looper/1/record              # Start recording on looper 1
/looper/1/stop                # Stop recording on looper 1
/looper/1/launch 0 1          # Launch track 0, slot 1 on looper 1
/looper/1/volume 0.75         # Set looper 1 volume to 0.75
/track/1/pan -0.5             # Pan track 1 left
/mixer/master/volume 1.0      # Set master volume to 1.0
```

---

## Best Practices

### 1. **Error Handling**
```java
try {
    oscClient.sendMessage("/looper/1/launch", 0);
} catch (IOException e) {
    host.println("Failed to send OSC message: " + e.getMessage());
    // Handle gracefully - reconnect or retry
}
```

### 2. **Thread Safety**
OSC operations may be asynchronous. Consider using synchronized blocks if accessed from multiple threads:
```java
private final Object oscLock = new Object();

public void sendSafeOSC(String address, Object value) throws IOException {
    synchronized(oscLock) {
        if (oscClient != null) {
            oscClient.sendMessage(address, value);
        }
    }
}
```

### 3. **Batching Messages**
Use bundles for multiple related messages to reduce network overhead:
```java
try {
    oscClient.startBundle();
    // Add multiple messages
    for (int i = 0; i < tracks.length; i++) {
        oscClient.sendMessage("/track/" + i + "/volume", volumes[i]);
    }
    oscClient.endBundle();
} catch (IOException e) {
    host.error("Failed to send OSC bundle", e);
}
```

### 4. **Configuration**
Store OSC settings (host, port) in extension configuration:
```java
public class OSCConfiguration {
    public int receivePort = 9000;
    public String sendHost = "localhost";
    public int sendPort = 9001;
}
```

### 5. **Graceful Shutdown**
Always close connections on extension exit:
```java
@Override
public void exit() {
    try {
        oscController.shutdown();
    } catch (IOException e) {
        host.error("Error closing OSC connections", e);
    }
}
```

---

## References

- **Bitwig API Version**: 24+
- **OSC Specification**: http://opensoundcontrol.org/
- **Implementation Example**: DrivenByMoss project (git-moss/DrivenByMoss)
- **Maven Dependency**:
  ```xml
  <dependency>
      <groupId>com.bitwig</groupId>
      <artifactId>extension-api</artifactId>
      <version>24</version>
  </dependency>
  ```

---

## Integration with BitwigLooperPlace

The current `OSCLooperController` in the project uses UDP sockets directly. For better integration with Bitwig's official OSC API:

1. **Replace UDP implementation** with `OscModule` API
2. **Use native address space routing** instead of manual parsing
3. **Leverage Bitwig's message bundling** for efficiency
4. **Access host.getOscModule()** from ControllerHost

This would provide:
- Better compatibility with Bitwig Studio
- Automatic message serialization/deserialization
- Built-in UDP handling and error recovery
- Future compatibility with protocol updates
