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

public class VipMoneyInfo
extends MoneyRanking {
    public static final String VIP_DATA_INFO = "VIP_DATA_INFO";

    public static VipMoneyInfo copyFromDB(String nickName) {
        StringBuilder key = new StringBuilder(VipMoneyInfo.class.getSimpleName());
        key.append(nickName);
        key.append(GameUtils.gameName);
        VipMoneyInfo info = (VipMoneyInfo)DataUtils.copyDataFromDB(key.toString(), VipMoneyInfo.class);
        return info;
    }

    public static VipMoneyInfo getInfo(User user) {
        if (user == null) {
            return null;
        }
        VipMoneyInfo info = (VipMoneyInfo)user.getProperty((Object)"VIP_DATA_INFO");
        if (info == null) {
            info = VipMoneyInfo.copyFromDB(user.getName());
            if (info == null) {
                PlayerInfo pInfo = PlayerInfo.getInfo(user);
                if (pInfo == null) {
                    return null;
                }
                info = new VipMoneyInfo();
                info.nickName = pInfo.nickName;
                info.avatar = pInfo.avatarUrl;
                info.save();
            }
            user.setProperty((Object)"VIP_DATA_INFO", (Object)info);
        }
        return info;
    }

    public JSONObject toJSONObject() {
        return GameUtils.toJSONObject(this);
    }
}

