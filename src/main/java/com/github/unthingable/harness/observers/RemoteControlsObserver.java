package com.github.unthingable.harness.observers;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;
import com.github.unthingable.harness.ClientManager;

public class RemoteControlsObserver {

    private final ClientManager clientManager;
    private final CursorRemoteControlsPage page;
    private final int paramCount;

    private String pageName = "";
    private int pageIndex = -1;
    private int pageCount = 0;
    private final String[] paramNames;
    private final double[] paramValues;

    public RemoteControlsObserver(CursorRemoteControlsPage page, ClientManager clientManager, int paramCount) {
        this.page = page;
        this.clientManager = clientManager;
        this.paramCount = paramCount;
        this.paramNames = new String[paramCount];
        this.paramValues = new double[paramCount];

        for (int i = 0; i < paramCount; i++) {
            paramNames[i] = "";
            paramValues[i] = 0.0;
        }

        page.getName().addValueObserver(name -> {
            pageName = name;
            broadcastPage();
        });

        page.selectedPageIndex().addValueObserver(index -> {
            pageIndex = index;
            broadcastPage();
        });

        page.pageCount().addValueObserver(count -> {
            pageCount = count;
            broadcastPage();
        });

        page.getName().markInterested();
        page.selectedPageIndex().markInterested();
        page.pageCount().markInterested();

        for (int i = 0; i < paramCount; i++) {
            final int idx = i;
            RemoteControl param = page.getParameter(i);

            param.name().addValueObserver(name -> {
                paramNames[idx] = name;
                clientManager.broadcast("/state/remote_control/param", idx, paramNames[idx], (float) paramValues[idx]);
            });

            param.value().addValueObserver(value -> {
                paramValues[idx] = value;
                clientManager.broadcast("/state/remote_control/param", idx, paramNames[idx], (float) paramValues[idx]);
            });

            param.name().markInterested();
            param.value().markInterested();
        }
    }

    private void broadcastPage() {
        clientManager.broadcast("/state/remote_control/page", pageName, pageIndex, pageCount);
    }

    public void sendSnapshot(OscConnection conn) {
        clientManager.sendTo(conn, "/state/remote_control/page", pageName, pageIndex, pageCount);
        for (int i = 0; i < paramCount; i++) {
            clientManager.sendTo(conn, "/state/remote_control/param", i, paramNames[i], (float) paramValues[i]);
        }
    }
}
