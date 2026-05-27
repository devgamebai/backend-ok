/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  org.json.JSONObject
 */
package game.entities;

import bitzero.server.entities.User;
import bitzero.util.ExtensionUtility;
import bitzero.util.socialcontroller.bean.UserInfo;
import game.utils.DataUtils;
import game.utils.GameUtils;
import java.util.Random;
import org.json.JSONObject;

public class PlayerInfo {
    public static final String PLAYER_INFO = "PLAYER_INFO";
    public String nickName = "";
    public int userId = 0;
    public String avatarUrl = "";
    private static final boolean dependOnGame = false;

    public void setIsHold(boolean value) {
        ExtensionUtility.instance().setCache(this.nickName, "hold", (Object)value);
        if (!value) {
            ExtensionUtility.instance().removeKey(this.nickName, "hold");
        }
    }

    public static boolean getIsHold(String nickName) {
        Boolean isHold = (Boolean)ExtensionUtility.instance().getCache(nickName, "hold");
        return isHold == null ? false : isHold;
    }

    public static void setRoomId(String nickName, int roomId) {
        if (roomId > 0) {
            ExtensionUtility.instance().setCache(nickName, "roomId", (Object)roomId);
        }
    }

    public static int getHoldRoom(String nickName) {
        Integer roomId = (Integer)ExtensionUtility.instance().getCache(nickName, "roomId");
        return roomId == null ? 0 : roomId;
    }

    public static PlayerInfo copyFromDB(String nickName) {
        StringBuilder key = new StringBuilder(PlayerInfo.class.getSimpleName());
        key.append(nickName);
        PlayerInfo pInfo = (PlayerInfo)DataUtils.copyDataFromDB(key.toString(), PlayerInfo.class);
        return pInfo;
    }

    public static PlayerInfo getInfo(User user) {
        UserInfo info;
        PlayerInfo pInfo = (PlayerInfo)user.getProperty((Object)"PLAYER_INFO");
        if (pInfo == null) {
            pInfo = new PlayerInfo();
            pInfo.userId = user.getId();
            pInfo.nickName = user.getName();
            int avatar = new Random().nextInt(12);
            pInfo.avatarUrl = String.valueOf(avatar);
            user.setProperty((Object)"PLAYER_INFO", (Object)pInfo);
        }
        if ((info = (UserInfo)user.getProperty((Object)"user_info")) != null) {
            pInfo.avatarUrl = info.getHeadurl();
        }
        return pInfo;
    }

    public JSONObject toJSONObject() {
        return GameUtils.toJSONObject(this);
    }
}

