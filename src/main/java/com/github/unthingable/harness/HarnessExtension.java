package com.github.unthingable.harness;

import com.bitwig.extension.api.opensoundcontrol.OscAddressSpace;
import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.unthingable.harness.observers.ClipMatrixObserver;
import com.github.unthingable.harness.observers.CursorTrackObserver;
import com.github.unthingable.harness.observers.DeviceObserver;
import com.github.unthingable.harness.observers.ProjectObserver;
import com.github.unthingable.harness.observers.RemoteControlsObserver;
import com.github.unthingable.harness.observers.TrackBankObserver;
import com.github.unthingable.harness.observers.TransportObserver;

public class HarnessExtension extends ControllerExtension {

    private static final int OSC_PORT = 9000;
    private static final int CLIENT_PORT_START = 9001;
    private static final int CLIENT_PORT_END = 9016;
    private static final int BANK_SIZE = 8;
    private static final int SCENE_COUNT = 8;
    private static final int REMOTE_CONTROL_COUNT = 8;

    protected HarnessExtension(HarnessDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        ControllerHost host = getHost();
        OscModule oscModule = host.getOscModule();
        OscAddressSpace addressSpace = oscModule.createAddressSpace();

        // Pre-allocate client connection pool (ports 9001–9016)
        OscAddressSpace clientAddressSpace = oscModule.createAddressSpace();
        Map<Integer, OscConnection> connectionPool = new HashMap<>();
        for (int port = CLIENT_PORT_START; port <= CLIENT_PORT_END; port++) {
            connectionPool.put(port, oscModule.connectToUdpServer("::ffff:127.0.0.1", port, clientAddressSpace));
        }
        host.println("Pre-allocated " + connectionPool.size() + " client connections");

        // Client manager
        ClientManager clientManager = new ClientManager(connectionPool);

        // MIDI ports
        MidiIn midiIn = host.getMidiInPort(0);
        MidiOut midiOut = host.getMidiOutPort(0);
        MidiProxy midiProxy = new MidiProxy(midiIn, midiOut, clientManager);

        // Bitwig API objects
        var application = host.createApplication();
        Transport transport = host.createTransport();
        CursorTrack cursorTrack = host.createCursorTrack("harness-cursor", "Harness Cursor", 0, 0, true);
        CursorDevice cursorDevice = cursorTrack.createCursorDevice("harness-device", "Harness Device", 0,
                CursorDeviceFollowMode.FOLLOW_SELECTION);
        CursorRemoteControlsPage remoteControls = cursorDevice.createCursorRemoteControlsPage(REMOTE_CONTROL_COUNT);
        TrackBank trackBank = host.createMainTrackBank(BANK_SIZE, 0, SCENE_COUNT);
        trackBank.followCursorTrack(cursorTrack);

        // Observers
        ProjectObserver projectObserver = new ProjectObserver(application, clientManager);
        TransportObserver transportObserver = new TransportObserver(transport, clientManager);
        CursorTrackObserver cursorTrackObserver = new CursorTrackObserver(cursorTrack, clientManager);
        DeviceObserver deviceObserver = new DeviceObserver(cursorDevice, clientManager);
        RemoteControlsObserver remoteControlsObserver = new RemoteControlsObserver(remoteControls, clientManager, REMOTE_CONTROL_COUNT);
        TrackBankObserver trackBankObserver = new TrackBankObserver(trackBank, clientManager, BANK_SIZE);
        ClipMatrixObserver clipMatrixObserver = new ClipMatrixObserver(trackBank, clientManager, BANK_SIZE, SCENE_COUNT);

        // Register snapshot providers
        clientManager.addSnapshotProvider(projectObserver::sendSnapshot);
        clientManager.addSnapshotProvider(transportObserver::sendSnapshot);
        clientManager.addSnapshotProvider(cursorTrackObserver::sendSnapshot);
        clientManager.addSnapshotProvider(deviceObserver::sendSnapshot);
        clientManager.addSnapshotProvider(remoteControlsObserver::sendSnapshot);
        clientManager.addSnapshotProvider(trackBankObserver::sendSnapshot);
        clientManager.addSnapshotProvider(clipMatrixObserver::sendSnapshot);

        // Connection management
        addressSpace.registerMethod("/connect", ",i", "Connect client", (source, message) -> {
            int port = intArg(message.getArguments(), 0);
            if (clientManager.register(port)) {
                host.println("Client connected on port " + port);
            } else {
                host.errorln("Client port " + port + " not in pre-allocated range "
                        + CLIENT_PORT_START + "–" + CLIENT_PORT_END);
            }
        });

        addressSpace.registerMethod("/disconnect", ",i", "Disconnect client", (source, message) -> {
            int port = intArg(message.getArguments(), 0);
            clientManager.unregister(port);
            host.println("Client disconnected from port " + port);
        });

        // MIDI proxy
        addressSpace.registerMethod("/midi/send", ",iiii", "Send MIDI to virtual port", (source, message) -> {
            var args = message.getArguments();
            midiProxy.sendMidi(intArg(args, 0), intArg(args, 1), intArg(args, 2), intArg(args, 3));
        });

        addressSpace.registerMethod("/midi/sysex/send", ",s", "Send sysex to virtual port", (source, message) -> {
            midiProxy.sendSysex(message.getArguments().get(0).toString());
        });

        // Transport controls
        addressSpace.registerMethod("/transport/play", ",", "Play", (source, message) -> transport.play());
        addressSpace.registerMethod("/transport/stop", ",", "Stop", (source, message) -> transport.stop());
        addressSpace.registerMethod("/transport/record", ",", "Record", (source, message) -> transport.record());

        // Track selection
        addressSpace.registerMethod("/track/select", ",i", "Select track by index", (source, message) -> {
            int index = intArg(message.getArguments(), 0);
            cursorTrack.selectFirst();
            for (int i = 0; i < index; i++) {
                cursorTrack.selectNext();
            }
        });

        // Device selection
        addressSpace.registerMethod("/device/select", ",i", "Select device by index", (source, message) -> {
            int index = intArg(message.getArguments(), 0);
            cursorDevice.selectFirst();
            for (int i = 0; i < index; i++) {
                cursorDevice.selectNext();
            }
        });

        // Remote controls
        addressSpace.registerMethod("/remote_control/page/next", ",", "Next remote controls page",
                (source, message) -> remoteControls.selectNextPage(false));

        addressSpace.registerMethod("/remote_control/page/prev", ",", "Previous remote controls page",
                (source, message) -> remoteControls.selectPreviousPage(false));

        addressSpace.registerMethod("/remote_control/page/select", ",i", "Select remote controls page", (source, message) -> {
            remoteControls.selectedPageIndex().set(intArg(message.getArguments(), 0));
        });

        addressSpace.registerMethod("/remote_control/set", ",if", "Set remote control value", (source, message) -> {
            var args = message.getArguments();
            int index = intArg(args, 0);
            if (index >= 0 && index < REMOTE_CONTROL_COUNT) {
                remoteControls.getParameter(index).value().set(doubleArg(args, 1));
            }
        });

        // Track bank
        addressSpace.registerMethod("/track/bank/scroll", ",i", "Scroll track bank", (source, message) -> {
            trackBank.scrollPosition().set(intArg(message.getArguments(), 0));
        });

        // Clip launcher
        addressSpace.registerMethod("/clip/launch", ",ii", "Launch clip", (source, message) -> {
            var args = message.getArguments();
            int trackIndex = intArg(args, 0);
            int sceneIndex = intArg(args, 1);
            if (trackIndex >= 0 && trackIndex < BANK_SIZE && sceneIndex >= 0 && sceneIndex < SCENE_COUNT) {
                trackBank.getItemAt(trackIndex).clipLauncherSlotBank().launch(sceneIndex);
            }
        });

        addressSpace.registerMethod("/clip/create", ",ii", "Create empty clip", (source, message) -> {
            var args = message.getArguments();
            int trackIndex = intArg(args, 0);
            int sceneIndex = intArg(args, 1);
            if (trackIndex >= 0 && trackIndex < BANK_SIZE && sceneIndex >= 0 && sceneIndex < SCENE_COUNT) {
                trackBank.getItemAt(trackIndex).clipLauncherSlotBank().createEmptyClip(sceneIndex, 4);
            }
        });

        addressSpace.registerMethod("/scene/launch", ",i", "Launch scene", (source, message) -> {
            int sceneIndex = intArg(message.getArguments(), 0);
            if (sceneIndex >= 0 && sceneIndex < SCENE_COUNT) {
                trackBank.sceneBank().launchScene(sceneIndex);
            }
        });

        // Application actions
        addressSpace.registerMethod("/undo", ",", "Undo", (source, message) -> application.undo());

        // Start OSC server
        oscModule.createUdpServer(OSC_PORT, addressSpace);

        host.println("Harness extension initialized on port " + OSC_PORT);
    }

    @Override
    public void exit() {
        getHost().println("Harness extension exiting");
    }

    @Override
    public void flush() {
        // State pushed via observers — nothing to flush
    }

    private static int intArg(List<?> args, int index) {
        return args.get(index) instanceof Number n ? n.intValue() : 0;
    }

    private static double doubleArg(List<?> args, int index) {
        return args.get(index) instanceof Number n ? n.doubleValue() : 0.0;
    }
}
