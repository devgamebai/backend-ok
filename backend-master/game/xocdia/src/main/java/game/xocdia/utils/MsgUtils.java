/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  game.utils.GameUtils
 *  game.xocdia.conf.XocDiaConfig
 */
package game.xocdia.utils;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaConfig;
import game.xocdia.entities.GamePlayer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MsgUtils {
    public static void send(BaseMsg msg, User user, boolean revMsg) {
        try {
            if (user != null && !user.isBot() && user.isConnected() && revMsg) {
                ExtensionUtility.getExtension().send(msg, user);
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    public static void sendToPlayingUser(BaseMsg msg, Map<String, GamePlayer> playerList) {
        for (Map.Entry<String, GamePlayer> entry : playerList.entrySet()) {
            GamePlayer gp = entry.getValue();
            if (!gp.isPlaying()) continue;
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
    }

    public static void sendToSittingUser(BaseMsg msg, Map<String, GamePlayer> playerList) {
        for (Map.Entry<String, GamePlayer> entry : playerList.entrySet()) {
            GamePlayer gp = entry.getValue();
            if (!gp.isSitting()) continue;
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
    }

    public static void sendToSittingUserExceptMe(BaseMsg msg, User user, Map<String, GamePlayer> playerList) {
        for (Map.Entry<String, GamePlayer> entry : playerList.entrySet()) {
            GamePlayer gp = entry.getValue();
            if (user.getName().equalsIgnoreCase(entry.getKey()) || !gp.isSitting()) continue;
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
    }

    public static void sendToRoom(BaseMsg msg, Map<String, GamePlayer> playerList) {
        for (Map.Entry<String, GamePlayer> entry : playerList.entrySet()) {
            GamePlayer gp = entry.getValue();
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
    }

    public static void sendExceptMe(BaseMsg msg, User user, Map<String, GamePlayer> playerList) {
        for (Map.Entry<String, GamePlayer> entry : playerList.entrySet()) {
            if (user.getName().equalsIgnoreCase(entry.getKey())) continue;
            GamePlayer gp = entry.getValue();
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
    }

    public static void alertServer(String content, boolean isMaintain, boolean alertAdmin) {
        try {
            Debug.trace((Object)(VinPlayUtils.getCurrentDateTime() + " ===ERROR=== " + content));
            if (isMaintain) {
                Debug.trace((Object)(VinPlayUtils.getCurrentDateTime() + " ===MAINTAIN===" + content));
                GameUtils.isMainTain = true;
            }
            if (alertAdmin) {
                MsgUtils.alertAdmin(content);
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    public static void alertAdmin(String content) {
        try {
            AlertServiceImpl alertSer = new AlertServiceImpl();
            alertSer.sendSMS2List(XocDiaConfig.mobileAdmin, content, false);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    public static void alertBoss(String content, String nickname) {
        try {
            UserServiceImpl userSer = new UserServiceImpl();
            UserModel model = userSer.getUserByNickName(nickname);
            if (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity()) {
                AlertServiceImpl alertSer = new AlertServiceImpl();
                alertSer.sendSMS2One(model.getMobile(), content, false);
                alertSer.sendSMS2List(XocDiaConfig.mobileAdmin, content, false);
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }
}

