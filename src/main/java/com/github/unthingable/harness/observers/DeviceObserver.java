package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.CursorDevice;
import com.github.unthingable.harness.ClientManager;

import java.io.IOException;

public class DeviceObserver {

    private final ClientManager clientManager;
    private String currentName = "";
    private int currentIndex = -1;

    public DeviceObserver(CursorDevice cursorDevice, ClientManager clientManager) {
        this.clientManager = clientManager;

        cursorDevice.name().addValueObserver(name -> {
            currentName = name;
            clientManager.broadcast("/state/device", currentName, currentIndex);
        });

        cursorDevice.position().addValueObserver(position -> {
            currentIndex = position;
            clientManager.broadcast("/state/device", currentName, currentIndex);
        });

        cursorDevice.name().markInterested();
        cursorDevice.position().markInterested();
    }

    public void sendSnapshot(OscConnection conn) {
        try {
            conn.sendMessage("/state/device", currentName, currentIndex);
        } catch (IOException e) {
            // UDP send failure
        }
    }
}
