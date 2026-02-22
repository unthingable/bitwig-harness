package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.CursorTrack;
import com.github.unthingable.harness.ClientManager;

public class CursorTrackObserver {

    private final ClientManager clientManager;
    private int currentIndex = -1;
    private String currentName = "";

    public CursorTrackObserver(CursorTrack cursorTrack, ClientManager clientManager) {
        this.clientManager = clientManager;

        cursorTrack.name().addValueObserver(name -> {
            currentName = name;
            clientManager.broadcast("/state/cursor_track", currentIndex, currentName);
        });

        cursorTrack.position().addValueObserver(position -> {
            currentIndex = position;
            clientManager.broadcast("/state/cursor_track", currentIndex, currentName);
        });

        cursorTrack.name().markInterested();
        cursorTrack.position().markInterested();
    }

    public void sendSnapshot(OscConnection conn) {
        clientManager.sendTo(conn, "/state/cursor_track", currentIndex, currentName);
    }
}
