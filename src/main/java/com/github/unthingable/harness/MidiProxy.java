package com.github.unthingable.harness;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public class MidiProxy {

    private final MidiOut midiOut;
    private final ClientManager clientManager;

    public MidiProxy(MidiIn midiIn, MidiOut midiOut, ClientManager clientManager) {
        this.midiOut = midiOut;
        this.clientManager = clientManager;

        midiIn.setMidiCallback((statusByte, data1, data2) -> {
            int channel = statusByte & 0x0F;
            int status = statusByte & 0xF0;
            clientManager.broadcast("/midi/in", channel, status, data1, data2);
        });

        midiIn.setSysexCallback(hex -> clientManager.broadcast("/midi/sysex/in", hex));
    }

    public void sendMidi(int channel, int status, int data1, int data2) {
        midiOut.sendMidi(status | channel, data1, data2);
    }

    public void sendSysex(String hex) {
        midiOut.sendSysex(hex);
    }
}
