package com.logiccuteguy;

import com.bitwig.extension.controller.api.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * OSC (Open Sound Control) controller for looper communication
 * Allows controlling looper via OSC messages
 */
public class OSCLooperController {
    private final ControllerHost host;
    private final Map<Integer, LooperGroup> looperGroups;
    private DatagramSocket oscSocket;
    private int oscPort = 9001;
    private String oscHost = "localhost";
    private volatile boolean running = true;
    
    public OSCLooperController(ControllerHost host, Map<Integer, LooperGroup> looperGroups) {
        this.host = host;
        this.looperGroups = looperGroups;
        initializeOSC();
    }
    
    /**
     * Initialize OSC socket for receiving messages
     */
    private void initializeOSC() {
        try {
            oscSocket = new DatagramSocket(oscPort);
            host.println("OSC Looper Controller initialized on port " + oscPort);
            startOSCListener();
        } catch (SocketException e) {
            host.println("Error initializing OSC socket: " + e.getMessage());
        }
    }
    
    /**
     * Start listening for OSC messages in a separate thread
     */
    private void startOSCListener() {
        Thread oscThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    oscSocket.receive(packet);
                    processOSCMessage(packet);
                } catch (IOException e) {
                    if (running) {
                        host.println("OSC receive error: " + e.getMessage());
                    }
                }
            }
        });
        oscThread.setDaemon(true);
        oscThread.start();
    }
    
    /**
     * Process incoming OSC messages
     * Message format: /looper/{groupId}/{command} {args}
     */
    private void processOSCMessage(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            
            // Simple OSC message parsing
            String[] parts = message.split(" ");
            String command = parts[0];
            
            if (command.startsWith("/looper/")) {
                String[] pathParts = command.split("/");
                
                if (pathParts.length >= 3) {
                    int groupId = Integer.parseInt(pathParts[2]);
                    String action = pathParts.length > 3 ? pathParts[3] : "";
                    
                    LooperGroup looper = looperGroups.get(groupId);
                    if (looper != null) {
                        handleOSCCommand(looper, action, parts);
                    }
                }
            }
        } catch (Exception e) {
            host.println("Error processing OSC message: " + e.getMessage());
        }
    }
    
    /**
     * Handle specific OSC commands
     * Supported commands:
     * - /looper/{id}/rec
     * - /looper/{id}/stop
     * - /looper/{id}/overdub (next overdub layer)
     * - /looper/{id}/clear
     * - /looper/{id}/launch {layer} {slot}
     */
    private void handleOSCCommand(LooperGroup looper, String action, String[] args) {
        try {
            switch (action) {
                case "rec":
                    looper.startRecording();
                    break;
                    
                case "stop":
                    looper.stopRecording();
                    break;
                    
                case "overdub":
                    looper.nextLayer();
                    break;
                    
                case "clear":
                    looper.stopAll();
                    break;
                    
                case "launch":
                    if (args.length > 2) {
                        int layer = Integer.parseInt(args[1]);
                        int slot = Integer.parseInt(args[2]);
                        looper.launchClip(layer, slot);
                    }
                    break;
                    
                default:
                    host.println("Unknown OSC command: " + action);
            }
        } catch (NumberFormatException e) {
            host.println("Invalid OSC command format: " + e.getMessage());
        }
    }
    
    /**
     * Send OSC message to external controller
     */
    public void sendOSCMessage(String address, Object... args) {
        try {
            StringBuilder message = new StringBuilder(address);
            for (Object arg : args) {
                message.append(" ").append(arg);
            }
            
            byte[] data = message.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(oscHost), oscPort);
            oscSocket.send(packet);
        } catch (IOException e) {
            host.println("Error sending OSC message: " + e.getMessage());
        }
    }
    
    /**
     * Set OSC server address and port
     */
    public void setOSCServer(String host, int port) {
        this.oscHost = host;
        this.oscPort = port;
    }
    
    /**
     * Shutdown OSC controller
     */
    public void shutdown() {
        running = false;
        if (oscSocket != null && !oscSocket.isClosed()) {
            oscSocket.close();
        }
    }
}

