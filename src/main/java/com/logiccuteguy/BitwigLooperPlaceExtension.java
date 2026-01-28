package com.logiccuteguy;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import java.util.*;

/**
 * BitwigLooperPlace - Advanced looper controller with OSC support
 * Features:
 * - Clip launcher control
 * - OSC (Open Sound Control) integration
 * - Multi-track looper groups with swappable audio routing
 * - Persistent overdub marking
 * - Command interface for looper management
 */
public class BitwigLooperPlaceExtension extends ControllerExtension
{
   private LooperCommandManager looperCommandManager;
   private OSCLooperController oscLooperController;
   private TrackBank mainTrackBank;
   private final Map<Integer, LooperGroup> looperGroups = new HashMap<>();
   
   protected BitwigLooperPlaceExtension(final BitwigLooperPlaceExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      
      try {
         // Initialize track bank for looper management
         mainTrackBank = host.createTrackBank(16, 0, 0, false);
         
         // Initialize command manager for looper creation
         looperCommandManager = new LooperCommandManager(host, mainTrackBank);
         
         // Initialize OSC controller for remote control
         oscLooperController = new OSCLooperController(host, looperGroups);
         
         host.showPopupNotification("BitwigLooperPlace Initialized - Ready to Loop!");
         host.println("=== BitwigLooperPlace Extension Loaded ===");
         host.println("Commands available:");
         host.println("  - create {name}: Create new looper group");
         host.println("  - mark {groupId} {track}: Mark track for overdub");
         host.println("  - swap {groupId}: Swap audio routing");
         host.println("  - launch {groupId} {track} {slot}: Launch clip");
         host.println("  - record {groupId} {track} {true|false}: Set record");
         host.println("OSC port: 9001");
         
      } catch (Exception e) {
         host.showPopupNotification("Error initializing extension: " + e.getMessage());
         host.println("Initialization error: " + e.getMessage());
      }
   }
   
   /**
    * Execute looper commands (can be called from MIDI, OSC, etc.)
    */
   public void executeLooperCommand(String command) {
      try {
         looperCommandManager.executeCommand(command);
      } catch (Exception e) {
         getHost().println("Error executing command: " + e.getMessage());
      }
   }
   
   /**
    * Get looper group by ID
    */
   public LooperGroup getLooperGroup(int groupId) {
      return looperGroups.get(groupId);
   }
   
   /**
    * Get all looper groups
    */
   public Map<Integer, LooperGroup> getAllLooperGroups() {
      return new HashMap<>(looperGroups);
   }
   
   /**
    * Get OSC controller for advanced control
    */
   public OSCLooperController getOSCController() {
      return oscLooperController;
   }

   @Override
   public void exit()
   {
      // Cleanup OSC controller
      if (oscLooperController != null) {
         oscLooperController.shutdown();
      }
      
      getHost().showPopupNotification("BitwigLooperPlace Exited");
      getHost().println("Extension shutdown complete");
   }

   @Override
   public void flush()
   {
      // Update any real-time values here
      // This is called frequently for smooth control updates
   }
}
