package com.logiccuteguy;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class BitwigLooperPlaceExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("d231b35c-5c3e-4f52-a2a1-5b5be820c291");
   
   public BitwigLooperPlaceExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "BitwigLooperPlace";
   }
   
   @Override
   public String getAuthor()
   {
      return "LogicCuteGuy";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "LogicCuteGuy";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "BitwigLooperPlace";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 24;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 0;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
   }

   @Override
   public BitwigLooperPlaceExtension createInstance(final ControllerHost host)
   {
      return new BitwigLooperPlaceExtension(this, host);
   }
}
