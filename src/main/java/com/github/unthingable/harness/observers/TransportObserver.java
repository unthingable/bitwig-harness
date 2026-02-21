package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.Transport;
import com.github.unthingable.harness.ClientManager;

import java.io.IOException;

public class TransportObserver {

    private final ClientManager clientManager;
    private String currentState = "stopped";

    public TransportObserver(Transport transport, ClientManager clientManager) {
        this.clientManager = clientManager;

        transport.isPlaying().addValueObserver(isPlaying -> {
            updateState(isPlaying, transport.isArrangerRecordEnabled().get());
        });

        transport.isArrangerRecordEnabled().addValueObserver(isRecording -> {
            updateState(transport.isPlaying().get(), isRecording);
        });

        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
    }

    private void updateState(boolean isPlaying, boolean isRecording) {
        if (isRecording && isPlaying) {
            currentState = "recording";
        } else if (isPlaying) {
            currentState = "playing";
        } else {
            currentState = "stopped";
        }
        clientManager.broadcast("/state/transport", currentState);
    }

    public void sendSnapshot(OscConnection conn) {
        try {
            conn.sendMessage("/state/transport", currentState);
        } catch (IOException e) {
            // UDP send failure
        }
    }
}
