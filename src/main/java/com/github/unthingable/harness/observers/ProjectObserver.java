package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.Application;
import com.github.unthingable.harness.ClientManager;

import java.io.IOException;

public class ProjectObserver {

    private final ClientManager clientManager;
    private String projectName = "";

    public ProjectObserver(Application application, ClientManager clientManager) {
        this.clientManager = clientManager;

        application.projectName().markInterested();
        application.projectName().addValueObserver(name -> {
            projectName = name;
            clientManager.broadcast("/state/project", projectName);
        });
    }

    public void sendSnapshot(OscConnection conn) {
        try {
            conn.sendMessage("/state/project", projectName);
        } catch (IOException e) {
            // UDP send failure
        }
    }
}
