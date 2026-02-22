package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.TrackBank;
import com.github.unthingable.harness.ClientManager;

public class ClipMatrixObserver {

    private final ClientManager clientManager;
    private final int numTracks;
    private final int numScenes;
    private final boolean[][] hasContent;
    private final boolean[][] isPlaying;
    private final boolean[][] isRecording;

    public ClipMatrixObserver(TrackBank trackBank, ClientManager clientManager, int numTracks, int numScenes) {
        this.clientManager = clientManager;
        this.numTracks = numTracks;
        this.numScenes = numScenes;
        this.hasContent = new boolean[numTracks][numScenes];
        this.isPlaying = new boolean[numTracks][numScenes];
        this.isRecording = new boolean[numTracks][numScenes];

        for (int t = 0; t < numTracks; t++) {
            final int trackIdx = t;
            ClipLauncherSlotBank slotBank = trackBank.getItemAt(t).clipLauncherSlotBank();

            slotBank.addHasContentObserver((slotIdx, value) -> {
                if (slotIdx < numScenes) {
                    hasContent[trackIdx][slotIdx] = value;
                    broadcastClip(trackIdx, slotIdx);
                }
            });

            slotBank.addIsPlayingObserver((slotIdx, value) -> {
                if (slotIdx < numScenes) {
                    isPlaying[trackIdx][slotIdx] = value;
                    broadcastClip(trackIdx, slotIdx);
                }
            });

            slotBank.addIsRecordingObserver((slotIdx, value) -> {
                if (slotIdx < numScenes) {
                    isRecording[trackIdx][slotIdx] = value;
                    broadcastClip(trackIdx, slotIdx);
                }
            });
        }
    }

    private void broadcastClip(int trackIdx, int sceneIdx) {
        clientManager.broadcast("/state/clip",
                trackIdx, sceneIdx,
                hasContent[trackIdx][sceneIdx] ? 1 : 0,
                isPlaying[trackIdx][sceneIdx] ? 1 : 0,
                isRecording[trackIdx][sceneIdx] ? 1 : 0);
    }

    public void sendSnapshot(OscConnection conn) {
        for (int t = 0; t < numTracks; t++) {
            for (int s = 0; s < numScenes; s++) {
                if (hasContent[t][s]) {
                    clientManager.sendTo(conn, "/state/clip",
                            t, s, 1,
                            isPlaying[t][s] ? 1 : 0,
                            isRecording[t][s] ? 1 : 0);
                }
            }
        }
    }
}
