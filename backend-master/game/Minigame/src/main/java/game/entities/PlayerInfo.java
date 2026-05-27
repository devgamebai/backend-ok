/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.ExtensionUtility
 */
package game.entities;

import bitzero.server.entities.User;
import bitzero.util.ExtensionUtility;
import game.utils.DataUtils;

public class PlayerInfo {
    public static final String PLAYER_INFO = "PLAYER_INFO";
    public String nickName = "";
    public int userId = 0;
    public String displayName = "";
    public String avatarUrl = "";
    private static final boolean dependOnGame = false;

    public void setIsHold(boolean value) {
        ExtensionUtility.instance().setCache(this.userId, "hold", value);
        if (!value) {
            ExtensionUtility.instance().removeKey(this.userId, "hold");
        }
    }

    public static boolean getIsHold(int userId) {
        if (userId < 0) {
            return false;
        }
        Boolean isHold = (Boolean)ExtensionUtility.instance().getCache(userId, "hold");
        return isHold == null ? false : isHold;
    }

    public static void setRoomId(int uId, int roomId) {
        if (roomId > 0) {
            ExtensionUtility.instance().setCache(uId, "roomId", roomId);
        }
    }

    public static int getHoldRoom(int uId) {
        Integer roomId = (Integer)ExtensionUtility.instance().getCache(uId, "roomId");
        return roomId == null ? 0 : roomId;
    }

    public static PlayerInfo copyFromDB(int userId) {
        StringBuilder key = new StringBuilder(PlayerInfo.class.getName());
        key.append(userId);
        PlayerInfo pInfo = (PlayerInfo)DataUtils.copyDataFromDB(key.toString(), PlayerInfo.class);
        return pInfo;
    }

    public void save() {
        StringBuilder key = new StringBuilder(this.getClass().getName());
        key.append(this.userId);
        DataUtils.saveToDB(key.toString(), this, this.getClass());
    }

    public static PlayerInfo getInfo(User user) {
        PlayerInfo pInfo = (PlayerInfo)user.getProperty(PLAYER_INFO);
        if (pInfo == null) {
            pInfo = PlayerInfo.copyFromDB(user.getId());
            if (pInfo == null) {
                pInfo = new PlayerInfo();
                pInfo.userId = user.getId();
                pInfo.nickName = user.getName();
            }
            user.setProperty(PLAYER_INFO, pInfo);
        }
        return pInfo;
    }
}

