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
    private final boolean[] mutes;
    private final boolean[] solos;
    private final boolean[] arms;
    private final double[] volumes;
    private int scrollPosition = 0;

    public TrackBankObserver(TrackBank trackBank, ClientManager clientManager, int bankSize) {
        this.clientManager = clientManager;
        this.bankSize = bankSize;
        this.names = new String[bankSize];
        this.positions = new int[bankSize];
        this.types = new String[bankSize];
        this.mutes = new boolean[bankSize];
        this.solos = new boolean[bankSize];
        this.arms = new boolean[bankSize];
        this.volumes = new double[bankSize];

        for (int i = 0; i < bankSize; i++) {
            names[i] = "";
            positions[i] = -1;
            types[i] = "";
            mutes[i] = false;
            solos[i] = false;
            arms[i] = false;
            volumes[i] = 0.0;
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

            track.mute().addValueObserver(mute -> {
                mutes[idx] = mute;
                broadcastTrack(idx);
            });

            track.solo().addValueObserver(solo -> {
                solos[idx] = solo;
                broadcastTrack(idx);
            });

            track.arm().addValueObserver(arm -> {
                arms[idx] = arm;
                broadcastTrack(idx);
            });

            track.volume().value().addValueObserver(volume -> {
                volumes[idx] = volume;
                broadcastTrack(idx);
            });

            track.name().markInterested();
            track.position().markInterested();
            track.trackType().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.arm().markInterested();
            track.volume().value().markInterested();
        }

        trackBank.scrollPosition().addValueObserver(pos -> {
            scrollPosition = pos;
            broadcastScrollPosition();
        });
        trackBank.scrollPosition().markInterested();
    }

    private void broadcastTrack(int bankIndex) {
        clientManager.broadcast("/state/track", bankIndex, names[bankIndex], positions[bankIndex], types[bankIndex],
                mutes[bankIndex] ? 1 : 0, solos[bankIndex] ? 1 : 0, arms[bankIndex] ? 1 : 0, volumes[bankIndex]);
    }

    private void broadcastScrollPosition() {
        clientManager.broadcast("/state/track_bank", scrollPosition);
    }

    public void sendSnapshot(OscConnection conn) {
        for (int i = 0; i < bankSize; i++) {
            clientManager.sendTo(conn, "/state/track", i, names[i], positions[i], types[i],
                    mutes[i] ? 1 : 0, solos[i] ? 1 : 0, arms[i] ? 1 : 0, volumes[i]);
        }
        clientManager.sendTo(conn, "/state/track_bank", scrollPosition);
    }
}
