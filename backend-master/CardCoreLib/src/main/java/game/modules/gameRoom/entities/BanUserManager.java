/*
 * Decompiled with CFR 0_116.
 */
package game.modules.gameRoom.entities;

import game.xocdia.conf.XocDiaConfig;
import game.xocdia.conf.XocDiaGameUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BanUserManager {
    private static BanUserManager banMgr = null;
    private Map<Integer, Map<String, BanUserModel>> banList = new ConcurrentHashMap<Integer, Map<String, BanUserModel>>();

    public static BanUserManager instance() {
        if (banMgr == null) {
            banMgr = new BanUserManager();
        }
        return banMgr;
    }

    private BanUserManager() {
        this.init();
    }

    private void init() {
        this.banList = new ConcurrentHashMap<Integer, Map<String, BanUserModel>>();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeBanList(int roomId) {
        Map<Integer, Map<String, BanUserModel>> map = this.banList;
        synchronized (map) {
            if (this.banList.containsKey(roomId)) {
                this.banList.remove(roomId);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void banUser(int roomId, String nickname) {
        Map<Integer, Map<String, BanUserModel>> map = this.banList;
        synchronized (map) {
            Map banRoom = new HashMap<String, BanUserModel>();
            if (this.banList.containsKey(roomId)) {
                banRoom = this.banList.get(roomId);
            }
            banRoom.put(nickname, new BanUserModel(nickname, System.currentTimeMillis()));
            this.banList.put(roomId, banRoom);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isBan(int roomId, String nickname) {
        Map<Integer, Map<String, BanUserModel>> map = this.banList;
        synchronized (map) {
            if (this.banList.containsKey(roomId) && this.banList.get(roomId).containsKey(nickname)) {
                if (XocDiaGameUtils.isXocDia()) {
                    long timeBlock = this.banList.get((Object)Integer.valueOf((int)roomId)).get((Object)nickname).timeBlock;
                    if (System.currentTimeMillis() - timeBlock <= (long)(XocDiaConfig.timeBlockUser * 3600000)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public class BanUserModel {
        public String nickname;
        public long timeBlock;

        public BanUserModel(String nickname, long timeBlock) {
            this.nickname = nickname;
            this.timeBlock = timeBlock;
        }
    }

}

