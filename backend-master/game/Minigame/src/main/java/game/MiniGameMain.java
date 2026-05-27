/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  com.vinplay.vbee.common.config.VBeePath
 */
package game;

import bitzero.server.BitZeroServer;
import com.vinplay.vbee.common.config.VBeePath;

public class MiniGameMain {
    public static void main(String[] args) {
        VBeePath.initBasePath(MiniGameMain.class);
        boolean clusterMode = false;
        boolean useConsole = false;
        if (args.length > 0) {
            clusterMode = args[0].equalsIgnoreCase("cluster");
            useConsole = args.length > 1 && args[1].equalsIgnoreCase("console");
        }
        BitZeroServer bzServer = BitZeroServer.getInstance();
        bzServer.setClustered(clusterMode);
        if (useConsole) {
            bzServer.startDebugConsole();
        }
        bzServer.start();
    }
}

