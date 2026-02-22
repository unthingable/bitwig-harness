package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.github.unthingable.harness.ClientManager;

public class TrackBankObserver {

    private final ClientManager clientManager;
    private final int bankSize;
    private final String[] names;
    private final int[] positions;
    private final String[] types;

    public TrackBankObserver(TrackBank trackBank, ClientManager clientManager, int bankSize) {
        this.clientManager = clientManager;
        this.bankSize = bankSize;
        this.names = new String[bankSize];
        this.positions = new int[bankSize];
        this.types = new String[bankSize];

        for (int i = 0; i < bankSize; i++) {
            names[i] = "";
            positions[i] = -1;
            types[i] = "";
        }

        for (int i = 0; i < bankSize; i++) {
            final int idx = i;
            Track track = trackBank.getItemAt(i);

            track.name().addValueObserver(name -> {
                names[idx] = name;
                broadcastTrack(idx);
            });

            track.position().addValueObserver(position -> {
                positions[idx] = position;
                broadcastTrack(idx);
            });

            track.trackType().addValueObserver(type -> {
                types[idx] = type;
                broadcastTrack(idx);
            });

            track.name().markInterested();
            track.position().markInterested();
            track.trackType().markInterested();
        }
    }

    private void broadcastTrack(int bankIndex) {
        clientManager.broadcast("/state/track", bankIndex, names[bankIndex], positions[bankIndex], types[bankIndex]);
    }

    public void sendSnapshot(OscConnection conn) {
        for (int i = 0; i < bankSize; i++) {
            clientManager.sendTo(conn, "/state/track", i, names[i], positions[i], types[i]);
        }
    }
}
