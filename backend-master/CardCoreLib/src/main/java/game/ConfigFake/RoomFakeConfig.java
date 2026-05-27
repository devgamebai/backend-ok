/*
 * Decompiled with CFR 0.152.
 */
package game.ConfigFake;

import game.ConfigFake.ConfigFakeTLMN;

public class RoomFakeConfig {
    public int[] fakeRoomTLMN = new int[5];
    public int[] fakeRoomTLMNSolo = new int[5];
    public int[] fakeRoomSam = new int[5];
    public int[] fakeRoomSamSL = new int[5];
    private static Object lock = new Object();
    public static RoomFakeConfig _instance = null;

    private RoomFakeConfig() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static RoomFakeConfig getInstance() {
        if (_instance == null) {
            Object object = lock;
            synchronized (object) {
                if (_instance == null) {
                    _instance = new RoomFakeConfig();
                }
            }
        }
        return _instance;
    }

    public void changeConfigFakeRoom() {
        this.fakeRoomTLMN = ConfigFakeTLMN.getInstance().getCurrentFakeRoomTLMN();
        this.fakeRoomTLMNSolo = ConfigFakeTLMN.getInstance().getCurrentFakeRoomTLMNSL();
    }

    public static int getIndexWithBet(long bet) {
        if (bet == 100L) {
            return 0;
        }
        if (bet == 500L) {
            return 1;
        }
        if (bet == 1000L) {
            return 2;
        }
        if (bet == 2000L) {
            return 3;
        }
        if (bet == 3000L) {
            return 4;
        }
        return -1;
    }

    public int getBonusNumberPlayerTLMN(long bet) {
        int index = RoomFakeConfig.getIndexWithBet(bet);
        if (index < 0) {
            return 0;
        }
        return this.fakeRoomTLMN[index] * 4;
    }

    public int getBonusNumberPlayerTLMNSL(long bet) {
        int index = RoomFakeConfig.getIndexWithBet(bet);
        if (index < 0) {
            return 0;
        }
        return this.fakeRoomTLMNSolo[index] * 2;
    }
}

