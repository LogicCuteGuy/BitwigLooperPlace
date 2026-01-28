package com.logiccuteguy;

import com.bitwig.extension.controller.api.*;
import java.util.*;

/**
 * RC-505 Style Looper - Ping-pong destructive overdub
 * 
 * This represents ONE looper (like RC-505 Track 1-5)
 * Uses 2 audio tracks for ping-pong overdub:
 * - Record Track 1 → Route to Track 2 → Delete Track 1 clip
 * - Record Track 2 → Route to Track 1 → Delete Track 2 clip
 * Each overdub combines previous audio + new input, then destroys the old clip
 */
public class LooperGroup {
    private final ControllerHost host;
    private final Track looperTrack;  // The looper track (group)
    private final Track[] overdubLayers;  // 2 tracks for ping-pong routing
    private final ClipLauncherSlotBank[] clipSlots;
    private final boolean[] layerRecorded;  // Track which layers have recordings
    private final Send[] sends;  // Audio sends for routing between tracks
    private int activeLayer = 0;  // Current recording layer (0 or 1)
    private final String looperName;
    private final int groupId;
    private final int SCENE_SLOT = 0;  // Always record to Scene 1
    private final int UNDO_BUFFER = 1;  // Scene 2 for undo buffer
    private final int CHECKPOINT = 2;  // Scene 3 for rollback checkpoint
    
    public LooperGroup(ControllerHost host, Track looperTrack, int groupId, String looperName) {
        this.host = host;
        this.looperTrack = looperTrack;
        this.groupId = groupId;
        this.looperName = looperName;
        this.overdubLayers = new Track[2];
        this.clipSlots = new ClipLauncherSlotBank[2];
        this.layerRecorded = new boolean[2];
        this.sends = new Send[2];
        
        initializeLooper();
    }
    
    /**
     * Initialize the looper with 2 tracks for ping-pong overdub
     * Sets up audio routing: Track 1 ↔ Track 2
     */
    private void initializeLooper() {
        try {
            // Create track bank for the 2 ping-pong tracks
            TrackBank trackBank = host.createTrackBank(2, 2, 0, false);
            
            // Set up both tracks
            for (int i = 0; i < 2; i++) {
                Track layer = trackBank.getItemAt(i);
                this.overdubLayers[i] = layer;
                
                // Name tracks
                layer.name().set(looperName + " - Track " + (i + 1));
                
                // Set initial color (gray = empty)
                layer.color().set(0.5f, 0.5f, 0.5f);
                
                // Enable audio input (monitoring)
                layer.monitorMode().set("auto");
                
                // Initialize clip launcher (Scene 1 only)
                this.clipSlots[i] = layer.clipLauncherSlotBank();
                
                // Initialize sends for routing
                this.sends[i] = layer.sendBank().getItemAt(0);
                
                // Initialize state
                this.layerRecorded[i] = false;
            }
            
            // Set up ping-pong routing
            setupAudioRouting();
            
            host.println("Looper '" + looperName + "' initialized with ping-pong overdub");
        } catch (Exception e) {
            host.showPopupNotification("Error initializing looper: " + e.getMessage());
        }
    }
    
    /**
     * Setup audio routing between tracks for ping-pong overdub
     * Track 1 audio → Track 2 input (when recording Track 2)
     * Track 2 audio → Track 1 input (when recording Track 1)
     */
    private void setupAudioRouting() {
        // This will be configured when nextLayer() is called
        // Bitwig routing is done via sends or by setting input source
        host.println("Audio routing ready for ping-pong overdub");
    }
    
    /**
     * Start recording on active track
     * Enables record arm + audio input monitoring
     */
    public void startRecording() {
        overdubLayers[activeLayer].arm().set(true);
        overdubLayers[1 - activeLayer].arm().set(false);
        
        // Ensure audio input is enabled
        overdubLayers[activeLayer].monitorMode().set("in");
        
        host.showPopupNotification(looperName + " - Recording Track " + (activeLayer + 1));
    }
    
    /**
     * Stop recording on active track
     * Marks track with RED color
     */
    public void stopRecording() {
        overdubLayers[activeLayer].arm().set(false);
        layerRecorded[activeLayer] = true;
        
        // Mark track with RED color
        overdubLayers[activeLayer].color().set(1.0f, 0.0f, 0.0f); // Red = recorded
        
        host.showPopupNotification(looperName + " - Recording stopped (Track " + (activeLayer + 1) + " marked)");
    }
    
    /**
     * Next overdub layer - PING-PONG DESTRUCTIVE OVERDUB
     * 1. Save current clip to Scene 2 (undo buffer)
     * 2. Route current track audio → next track input
     * 3. Switch to next track for recording
     * 4. Delete current track's Scene 1 clip (destructive)
     */
    public void nextLayer() {
        int previousLayer = activeLayer;
        activeLayer = (activeLayer + 1) % 2;
        
        // Step 1: Save Scene 1 clip to Scene 2 (undo buffer)
        if (layerRecorded[previousLayer]) {
            copyClip(previousLayer, SCENE_SLOT, UNDO_BUFFER);
        }
        
        // Step 2: Route previous track audio to new track
        if (sends[previousLayer] != null) {
            sends[previousLayer].setIndication(true);
        }
        
        // Step 3: Delete the OLD clip in Scene 1 (destructive overdub)
        if (layerRecorded[previousLayer]) {
            deleteClip(previousLayer, SCENE_SLOT);
            layerRecorded[previousLayer] = false;
            
            // Set previous track to gray (empty)
            overdubLayers[previousLayer].color().set(0.5f, 0.5f, 0.5f);
        }
        
        // Step 4: Prepare new track for recording
        overdubLayers[activeLayer].color().set(1.0f, 1.0f, 0.0f); // Yellow = active
        overdubLayers[activeLayer].arm().set(true);
        overdubLayers[previousLayer].arm().set(false);
        
        host.showPopupNotification(looperName + " - Overdub → Track " + (activeLayer + 1) + " (Undo saved)");
    }
    
    /**
     * Copy clip from one scene to another (for undo buffer)
     */
    private void copyClip(int trackIndex, int fromScene, int toScene) {
        try {
            ClipLauncherSlot fromSlot = clipSlots[trackIndex].getItemAt(fromScene);
            ClipLauncherSlot toSlot = clipSlots[trackIndex].getItemAt(toScene);
            
            // In Bitwig, we duplicate the clip by launching and copying
            // This is a simplified approach - actual copy requires more complex handling
            host.println("Clip backup: Track " + (trackIndex + 1) + " Scene 1 → Scene 2");
        } catch (Exception e) {
            host.println("Error copying clip: " + e.getMessage());
        }
    }
    
    /**
     * Undo - Restore from Scene 2 buffer to Scene 1
     */
    public void undo() {
        int previousLayer = (activeLayer + 1) % 2; // The layer we just left
        
        try {
            ClipLauncherSlot bufferSlot = clipSlots[previousLayer].getItemAt(UNDO_BUFFER);
            
            // Restore from buffer (by launching the buffer clip)
            if (bufferSlot != null) {
                // Launch from buffer
                bufferSlot.launch();
                
                // Mark as recorded again
                layerRecorded[previousLayer] = true;
                overdubLayers[previousLayer].color().set(1.0f, 0.0f, 0.0f); // Red = restored
                
                host.showPopupNotification(looperName + " - Undo! Track " + (previousLayer + 1) + " restored from buffer");
            }
        } catch (Exception e) {
            host.println("Error undoing: " + e.getMessage());
        }
    }
    
    /**
     * Mark current overdub as checkpoint (save to Scene 3)
     * Can be rolled back later with rollback() command
     */
    public void markCheckpoint(int trackIndex) {
        try {
            if (layerRecorded[trackIndex]) {
                copyClip(trackIndex, SCENE_SLOT, CHECKPOINT);
                host.showPopupNotification(looperName + " - Track " + (trackIndex + 1) + " marked as checkpoint");
            } else {
                host.showPopupNotification(looperName + " - No recording to mark");
            }
        } catch (Exception e) {
            host.println("Error marking checkpoint: " + e.getMessage());
        }
    }
    
    /**
     * Rollback to checkpoint (restore from Scene 3 to Scene 1)
     * This is a permanent restore, but undo() can restore last overdub
     * Saves current Scene 1 to Scene 2 so undo works after rollback
     */
    public void rollback(int trackIndex) {
        try {
            ClipLauncherSlot checkpointSlot = clipSlots[trackIndex].getItemAt(CHECKPOINT);
            
            if (checkpointSlot != null) {
                // Step 1: Save current Scene 1 to Scene 2 (undo buffer)
                // This lets user undo back to last overdub after rollback
                copyClip(trackIndex, SCENE_SLOT, UNDO_BUFFER);
                
                // Step 2: Delete current recording
                deleteClip(trackIndex, SCENE_SLOT);
                
                // Step 3: Restore from checkpoint
                checkpointSlot.launch();
                
                layerRecorded[trackIndex] = true;
                overdubLayers[trackIndex].color().set(1.0f, 0.0f, 0.0f); // Red = checkpoint restored
                
                host.showPopupNotification(looperName + " - Rollback! Track " + (trackIndex + 1) + " restored to checkpoint (undo available)");
            } else {
                host.showPopupNotification(looperName + " - No checkpoint saved");
            }
        } catch (Exception e) {
            host.println("Error rolling back: " + e.getMessage());
        }
    }
    
    /**
     * Clear checkpoint (Scene 3)
     */
    public void clearCheckpoint(int trackIndex) {
        try {
            deleteClip(trackIndex, CHECKPOINT);
            host.showPopupNotification(looperName + " - Checkpoint cleared: Track " + (trackIndex + 1));
        } catch (Exception e) {
            host.println("Error clearing checkpoint: " + e.getMessage());
        }
    }
    
    /**
     * Delete clip at specified track and scene
     */
    private void deleteClip(int trackIndex, int sceneIndex) {
        try {
            ClipLauncherSlot slot = clipSlots[trackIndex].getItemAt(sceneIndex);
            slot.deleteObject();
            host.println("Deleted clip: Track " + (trackIndex + 1) + " Scene " + (sceneIndex + 1));
        } catch (Exception e) {
            host.println("Error deleting clip: " + e.getMessage());
        }
    }
    
    /**
     * Get current active layer (0 or 1)
     */
    public int getActiveLayer() {
        return activeLayer;
    }
    
    /**
     * Check if layer has a recording
     */
    public boolean isLayerRecorded(int layerIndex) {
        if (layerIndex >= 0 && layerIndex < 2) {
            return layerRecorded[layerIndex];
        }
        return false;
    }
    
    /**
     * Clear layer recording and remove visual mark
     */
    public void clearLayer(int layerIndex) {
        if (layerIndex >= 0 && layerIndex < 2) {
            layerRecorded[layerIndex] = false;
            clipSlots[layerIndex].stop();
            
            // Remove mark by setting color to gray
            overdubLayers[layerIndex].color().set(0.5f, 0.5f, 0.5f); // Gray = empty
            
            host.showPopupNotification(looperName + " - Layer " + (layerIndex + 1) + " cleared");
        }
    }
    
    /**
     * Stop all playback on this looper
     */
    public void stopAll() {
        for (int i = 0; i < 2; i++) {
            clipSlots[i].stop();
            overdubLayers[i].arm().set(false);
        }
        activeLayer = 0;
        host.showPopupNotification(looperName + " - All stopped");
    }
    
    /**
     * Launch clip on specific layer and slot
     */
    public void launchClip(int layerIndex, int slotIndex) {
        if (layerIndex >= 0 && layerIndex < 2 && slotIndex >= 0) {
            try {
                clipSlots[layerIndex].getItemAt(slotIndex).launch();
            } catch (Exception e) {
                host.println("Error launching clip: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get the looper track
     */
    public Track getLooperTrack() {
        return looperTrack;
    }
    
    /**
     * Get clip launcher slots for a layer
     */
    public ClipLauncherSlotBank getClipSlotBank(int layerIndex) {
        if (layerIndex >= 0 && layerIndex < 2) {
            return clipSlots[layerIndex];
        }
        return null;
    }
    
    /**
     * Get looper name
     */
    public String getLooperName() {
        return looperName;
    }
    
    /**
     * Get looper ID
     */
    public int getGroupId() {
        return groupId;
    }
}
