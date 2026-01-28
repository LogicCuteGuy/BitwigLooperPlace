package com.logiccuteguy;

import com.bitwig.extension.controller.api.*;
import java.util.*;

/**
 * Manages commands for looper group creation and control
 * Provides easy command-based interface for looper operations
 */
public class LooperCommandManager {
    private final ControllerHost host;
    private final Map<Integer, LooperGroup> looperGroups;
    private int nextGroupId = 0;
    private final TrackBank mainTrackBank;
    
    public LooperCommandManager(ControllerHost host, TrackBank mainTrackBank) {
        this.host = host;
        this.mainTrackBank = mainTrackBank;
        this.looperGroups = new HashMap<>();
        
        // Register default commands
        registerCommands();
    }
    
    /**
     * Register all available commands
     */
    private void registerCommands() {
        host.println("Looper Command Manager initialized");
    }
    
    /**
     * Create a new looper (RC-505 style)
     * Command: "create {name}"
     */
    public synchronized LooperGroup createLooperGroup(String looperName) {
        try {
            int groupId = nextGroupId++;
            
            // Set looper track properties
            mainTrackBank.getItemAt(groupId).name().set(looperName);
            Track looperTrack = mainTrackBank.getItemAt(groupId);
            
            // Create looper group
            LooperGroup looper = new LooperGroup(host, looperTrack, groupId, looperName);
            looperGroups.put(groupId, looper);
            
            host.showPopupNotification("Looper '" + looperName + "' created (ID: " + groupId + ")");
            host.println("Created looper: " + looperName + " (ID: " + groupId + ")");
            
            return looper;
        } catch (Exception e) {
            host.showPopupNotification("Error creating looper: " + e.getMessage());
            host.println("Error creating looper: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create looper with command interface
     * Syntax: create {name}
     */
    public void executeCommand(String commandLine) {
        try {
            String[] tokens = commandLine.trim().split("\\s+", 2);
            
            if (tokens.length < 1) {
                host.showPopupNotification("Invalid command format");
                return;
            }
            
            String command = tokens[0];
            String[] args = tokens.length > 1 ? tokens[1].split("\\s+") : new String[0];
            
            switch (command.toLowerCase()) {
                case "create":
                    if (args.length > 0) {
                        String looperName = args[0];
                        createLooperGroup(looperName);
                    } else {
                        createLooperGroup("Looper_" + nextGroupId);
                    }
                    break;
                    
                case "rec":
                    // rec {id}
                    if (args.length >= 1) {
                        int id = Integer.parseInt(args[0]);
                        startRecording(id);
                    }
                    break;
                    
                case "stop":
                    // stop {id}
                    if (args.length >= 1) {
                        int id = Integer.parseInt(args[0]);
                        stopRecording(id);
                    }
                    break;
                    
                case "overdub":
                    // overdub {id} - next overdub layer
                    if (args.length >= 1) {
                        int id = Integer.parseInt(args[0]);
                        nextLayer(id);
                    }
                    break;
                    
                case "clear":
                    // clear {id}
                    if (args.length >= 1) {
                        int id = Integer.parseInt(args[0]);
                        clearLooper(id);
                    }
                    break;
                    
                case "launch":
                    // launch {id} {layer} {slot}
                    if (args.length >= 3) {
                        int id = Integer.parseInt(args[0]);
                        int layer = Integer.parseInt(args[1]);
                        int slot = Integer.parseInt(args[2]);
                        launchClip(id, layer, slot);
                    }
                    break;
                    
                case "list":
                    listLooperGroups();
                    break;
                
                case "undo":
                    // undo {id}
                    if (args.length >= 1) {
                        int id = Integer.parseInt(args[0]);
                        undo(id);
                    }
                    break;
                
                case "mark":
                    // mark {id} {track} - mark overdub as checkpoint
                    if (args.length >= 2) {
                        int id = Integer.parseInt(args[0]);
                        int track = Integer.parseInt(args[1]);
                        markCheckpoint(id, track);
                    }
                    break;
                
                case "rollback":
                    // rollback {id} {track} - restore to checkpoint
                    if (args.length >= 2) {
                        int id = Integer.parseInt(args[0]);
                        int track = Integer.parseInt(args[1]);
                        rollback(id, track);
                    }
                    break;
                    
                default:
                    host.showPopupNotification("Unknown command: " + command);
            }
        } catch (Exception e) {
            host.showPopupNotification("Command execution error: " + e.getMessage());
        }
    }
    
    /**
     * Start recording on looper
     */
    private void startRecording(int groupId) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.startRecording();
        }
    }
    
    /**
     * Stop recording on looper
     */
    private void stopRecording(int groupId) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.stopRecording();
        }
    }
    
    /**
     * Go to next overdub layer - ping-pong switch with undo buffer
     */
    private void nextLayer(int groupId) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.nextLayer();
        }
    }
    
    /**
     * Clear/stop looper
     */
    private void clearLooper(int groupId) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.stopAll();
        }
    }
    
    /**
     * Launch clip on layer
     */
    private void launchClip(int groupId, int layerIndex, int slotIndex) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.launchClip(layerIndex, slotIndex);
        }
    }
    
    /**
     * Undo last overdub - restore from Scene 2 buffer
     */
    private void undo(int groupId) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.undo();
        } else {
            host.showPopupNotification("Looper not found: " + groupId);
        }
    }
    
    /**
     * Mark overdub as checkpoint
     */
    private void markCheckpoint(int groupId, int trackIndex) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.markCheckpoint(trackIndex);
        } else {
            host.showPopupNotification("Looper not found: " + groupId);
        }
    }
    
    /**
     * Rollback to checkpoint
     */
    private void rollback(int groupId, int trackIndex) {
        LooperGroup looper = looperGroups.get(groupId);
        if (looper != null) {
            looper.rollback(trackIndex);
        } else {
            host.showPopupNotification("Looper not found: " + groupId);
        }
    }
    
    /**
     * List all looper groups
     */
    public void listLooperGroups() {
        if (looperGroups.isEmpty()) {
            host.showPopupNotification("No loopers created");
            return;
        }
        
        StringBuilder sb = new StringBuilder("Loopers:\n");
        for (Map.Entry<Integer, LooperGroup> entry : looperGroups.entrySet()) {
            LooperGroup looper = entry.getValue();
            sb.append("- ").append(looper.getLooperName())
              .append(" (ID: ").append(entry.getKey()).append(")\n");
        }
        
        host.showPopupNotification(sb.toString());
    }
    
    /**
     * Get looper by ID
     */
    public LooperGroup getLooperGroup(int groupId) {
        return looperGroups.get(groupId);
    }
    
    /**
     * Get all loopers
     */
    public Map<Integer, LooperGroup> getAllLooperGroups() {
        return new HashMap<>(looperGroups);
    }
}
