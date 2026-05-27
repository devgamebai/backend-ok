/*
 * Decompiled with CFR 0_116.
 */
package game.xocdia.conf;

public class XocDiaRoomBotModel {
    private int maxBotInRoom;
    private long time;

    public XocDiaRoomBotModel(int maxBotInRoom, long time) {
        this.maxBotInRoom = maxBotInRoom;
        this.time = time;
    }

    public int getMaxBotInRoom() {
        return this.maxBotInRoom;
    }

    public void setMaxBotInRoom(int maxBotInRoom) {
        this.maxBotInRoom = maxBotInRoom;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

