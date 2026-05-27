/*
 * Decompiled with CFR 0.152.
 */
package game.ConfigFake;

import java.util.Random;

public class ConfigFakeTLMN {
    public static final int NUMBER_ROOM_TLMN = 4;
    public static final int NUMBER_ROOM_TLMN_SOLO = 2;
    public static int[][] tlmnRoomFakeConfig = new int[][]{{25, 30}, {20, 25}, {10, 15}, {5, 7}, {1, 2}};
    public static int[][] tlmnSLRoomFakeConfig = new int[][]{{30, 40}, {20, 30}, {10, 20}, {10, 15}, {2, 4}};
    private static ConfigFakeTLMN _instance = null;
    private static final Object lock = new Object();
    public int[] currentRoomTLMN = new int[tlmnRoomFakeConfig.length];
    public int[] currentRoomTLMN_SL = null;
    private static Random rd = new Random();

    private ConfigFakeTLMN() {
        int i;
        for (i = 0; i < this.currentRoomTLMN.length; ++i) {
            this.currentRoomTLMN[i] = ConfigFakeTLMN.randomBetween(tlmnRoomFakeConfig[i][0], tlmnRoomFakeConfig[i][1]);
        }
        this.currentRoomTLMN_SL = new int[tlmnSLRoomFakeConfig.length];
        for (i = 0; i < this.currentRoomTLMN_SL.length; ++i) {
            this.currentRoomTLMN_SL[i] = ConfigFakeTLMN.randomBetween(tlmnSLRoomFakeConfig[i][0], tlmnSLRoomFakeConfig[i][1]);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ConfigFakeTLMN getInstance() {
        if (_instance == null) {
            Object object = lock;
            synchronized (object) {
                if (_instance == null) {
                    _instance = new ConfigFakeTLMN();
                }
            }
        }
        return _instance;
    }

    public int[] getCurrentFakeRoomTLMN() {
        for (int i = 0; i < this.currentRoomTLMN.length; ++i) {
            int random = ConfigFakeTLMN.randomBetween(0, 3);
            if (random == 1 && this.currentRoomTLMN[i] + random < tlmnRoomFakeConfig[i][1]) {
                int n = i;
                this.currentRoomTLMN[n] = this.currentRoomTLMN[n] + random;
            }
            if (random != 0 || this.currentRoomTLMN[i] - random <= tlmnRoomFakeConfig[i][0]) continue;
            int n = i;
            this.currentRoomTLMN[n] = this.currentRoomTLMN[n] - random;
        }
        return this.currentRoomTLMN;
    }

    public int[] getCurrentFakeRoomTLMNSL() {
        for (int i = 0; i < this.currentRoomTLMN_SL.length; ++i) {
            int random = ConfigFakeTLMN.randomBetween(0, 2);
            if (random == 1 && this.currentRoomTLMN_SL[i] + random < tlmnSLRoomFakeConfig[i][1]) {
                int n = i;
                this.currentRoomTLMN_SL[n] = this.currentRoomTLMN_SL[n] + random;
            }
            if (random != 0 || this.currentRoomTLMN_SL[i] - random <= tlmnSLRoomFakeConfig[i][0]) continue;
            int n = i;
            this.currentRoomTLMN_SL[n] = this.currentRoomTLMN_SL[n] - random;
        }
        return this.currentRoomTLMN;
    }

    public static int randomBetween(int a, int b) {
        return a + rd.nextInt(b - a);
    }
}

