/*
 * Decompiled with CFR 0.152.
 */
package game.ConfigFake;

import game.ConfigFake.RoomFakeConfig;

public class UpdateFakeRoomTimer
implements Runnable {
    @Override
    public void run() {
        RoomFakeConfig.getInstance().changeConfigFakeRoom();
    }
}

