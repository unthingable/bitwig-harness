package com.github.unthingable.harness;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class HarnessDefinition extends ControllerExtensionDefinition {

    private static final UUID ID = UUID.fromString("f1a2b3c4-d5e6-47f8-9a0b-1c2d3e4f5a6b");

    @Override
    public String getName() {
        return "Harness";
    }

    @Override
    public String getAuthor() {
        return "Unthingable";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public UUID getId() {
        return ID;
    }

    @Override
    public int getRequiredAPIVersion() {
        return 21;
    }

    @Override
    public String getHardwareVendor() {
        return "Unthingable";
    }

    @Override
    public String getHardwareModel() {
        return "Harness";
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }

    @Override
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        // No auto-detection
    }

    @Override
    public HarnessExtension createInstance(ControllerHost host) {
        return new HarnessExtension(this, host);
    }
}
