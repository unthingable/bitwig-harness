package com.github.unthingable.harness;

import com.bitwig.extension.api.opensoundcontrol.OscAddressSpace;
import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Transport;

import java.util.HashMap;
import java.util.Map;

import com.github.unthingable.harness.observers.CursorTrackObserver;
import com.github.unthingable.harness.observers.DeviceObserver;
import com.github.unthingable.harness.observers.TransportObserver;

public class HarnessExtension extends ControllerExtension {

    private static final int OSC_PORT = 9000;
    private static final int CLIENT_PORT_START = 9001;
    private static final int CLIENT_PORT_END = 9016;

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

        // Bitwig API objects
        Transport transport = host.createTransport();
        CursorTrack cursorTrack = host.createCursorTrack("harness-cursor", "Harness Cursor", 0, 0, true);
        CursorDevice cursorDevice = cursorTrack.createCursorDevice("harness-device", "Harness Device", 0,
                CursorDeviceFollowMode.FOLLOW_SELECTION);

        // Observers
        TransportObserver transportObserver = new TransportObserver(transport, clientManager);
        CursorTrackObserver cursorTrackObserver = new CursorTrackObserver(cursorTrack, clientManager);
        DeviceObserver deviceObserver = new DeviceObserver(cursorDevice, clientManager);

        // Register snapshot providers
        clientManager.addSnapshotProvider(transportObserver::sendSnapshot);
        clientManager.addSnapshotProvider(cursorTrackObserver::sendSnapshot);
        clientManager.addSnapshotProvider(deviceObserver::sendSnapshot);

        // Connection management
        addressSpace.registerMethod("/connect", ",i", "Connect client", (source, message) -> {
            int port = message.getArguments().get(0) instanceof Number n ? n.intValue() : 0;
            if (clientManager.register(port)) {
                host.println("Client connected on port " + port);
            } else {
                host.errorln("Client port " + port + " not in pre-allocated range "
                        + CLIENT_PORT_START + "–" + CLIENT_PORT_END);
            }
        });

        addressSpace.registerMethod("/disconnect", ",i", "Disconnect client", (source, message) -> {
            int port = message.getArguments().get(0) instanceof Number n ? n.intValue() : 0;
            clientManager.unregister(port);
            host.println("Client disconnected from port " + port);
        });

        // Transport controls
        addressSpace.registerMethod("/transport/play", ",", "Play", (source, message) -> {
            transport.play();
        });

        addressSpace.registerMethod("/transport/stop", ",", "Stop", (source, message) -> {
            transport.stop();
        });

        addressSpace.registerMethod("/transport/record", ",", "Record", (source, message) -> {
            transport.record();
        });

        // Track selection
        addressSpace.registerMethod("/track/select", ",i", "Select track by index", (source, message) -> {
            int index = message.getArguments().get(0) instanceof Number n ? n.intValue() : 0;
            cursorTrack.selectFirst();
            for (int i = 0; i < index; i++) {
                cursorTrack.selectNext();
            }
        });

        // Device selection
        addressSpace.registerMethod("/device/select", ",i", "Select device by index", (source, message) -> {
            int index = message.getArguments().get(0) instanceof Number n ? n.intValue() : 0;
            cursorDevice.selectFirst();
            for (int i = 0; i < index; i++) {
                cursorDevice.selectNext();
            }
        });

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
}
