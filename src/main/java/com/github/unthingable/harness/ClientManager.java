package com.github.unthingable.harness;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClientManager {

    private final Map<Integer, OscConnection> connectionPool;
    private final Set<Integer> activePorts = new HashSet<>();
    private final List<Consumer<OscConnection>> snapshotProviders = new ArrayList<>();

    public ClientManager(Map<Integer, OscConnection> connectionPool) {
        this.connectionPool = connectionPool;
    }

    public boolean register(int port) {
        OscConnection conn = connectionPool.get(port);
        if (conn == null) {
            return false;
        }
        activePorts.add(port);
        sendSnapshot(conn);
        return true;
    }

    public void unregister(int port) {
        activePorts.remove(port);
    }

    public void broadcast(String address, Object... args) {
        for (int port : activePorts) {
            OscConnection conn = connectionPool.get(port);
            if (conn != null) {
                try {
                    conn.sendMessage(address, args);
                } catch (IOException e) {
                    // UDP send failure — log and continue
                }
            }
        }
    }

    public void addSnapshotProvider(Consumer<OscConnection> provider) {
        snapshotProviders.add(provider);
    }

    private void sendSnapshot(OscConnection conn) {
        for (Consumer<OscConnection> provider : snapshotProviders) {
            provider.accept(conn);
        }
    }
}
