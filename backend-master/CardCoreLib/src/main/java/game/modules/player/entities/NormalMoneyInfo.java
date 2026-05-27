/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  org.json.JSONObject
 */
package game.modules.player.entities;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.modules.player.entities.MoneyRanking;
import game.utils.DataUtils;
import game.utils.GameUtils;
import org.json.JSONObject;

public class NormalMoneyInfo
extends MoneyRanking {
    public static final String NORMAL_DATA_INFO = "NORMAL_DATA_INFO";

    public static NormalMoneyInfo copyFromDB(String nickName) {
        StringBuilder key = new StringBuilder(NormalMoneyInfo.class.getSimpleName());
        key.append(nickName);
        key.append(GameUtils.gameName);
        NormalMoneyInfo info = (NormalMoneyInfo)DataUtils.copyDataFromDB(key.toString(), NormalMoneyInfo.class);
        return info;
    }

    public static NormalMoneyInfo getInfo(User user) {
        if (user == null) {
            return null;
        }
        NormalMoneyInfo info = (NormalMoneyInfo)user.getProperty((Object)"NORMAL_DATA_INFO");
        if (info == null) {
            info = NormalMoneyInfo.copyFromDB(user.getName());
            if (info == null) {
                PlayerInfo pInfo = PlayerInfo.getInfo(user);
                if (pInfo == null) {
                    return null;
                }
                info = new NormalMoneyInfo();
                info.nickName = pInfo.nickName;
                info.avatar = pInfo.avatarUrl;
                info.save();
            }
            user.setProperty((Object)"NORMAL_DATA_INFO", (Object)info);
        }
        return info;
    }

    public JSONObject toJSONObject() {
        return GameUtils.toJSONObject(this);
    }
}

