/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.config.ConfigHandle
 *  bitzero.server.entities.User
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.UserResponse
 */
package game.utils;

import bitzero.server.config.ConfigHandle;
import bitzero.server.entities.User;
import bitzero.util.socialcontroller.bean.UserInfo;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.UserResponse;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.utils.ConfigGame;
import java.util.Random;

public class GameUtils {
    public static boolean dev_mod = ConfigHandle.instance().getLong("dev_mod") == 1L;
    public static boolean isCheat = ConfigHandle.instance().getLong("isCheat") == 1L;
    public static final String gameName = ConfigHandle.instance().get("games");

    public static boolean infoCheck(User user) {
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        if (pInfo == null) {
            return false;
        }
        return !PlayerInfo.getIsHold(pInfo.userId);
    }

    public static void setUserScore(int userId, UserScore score, boolean updateDatabase) {
    }

    public static UserInfo getUserInfo(String username, String sessionKey) {
        if (dev_mod) {
            UserInfo info = new UserInfo();
            int userId = 0;
            try {
                Integer v = new Integer(username);
                userId = v;
            }
            catch (Exception e) {
                Random rd = new Random();
                userId = Math.abs(rd.nextInt() % 100000);
            }
            info.setUsername("vp_" + userId);
            info.setUserId("" + userId);
            return info;
        }
        UserServiceImpl service = new UserServiceImpl();
        UserResponse res = service.checkSessionKey(username, sessionKey, Games.MINIGAME);
        if (res.getErrorCode() == "0") {
            res.getUser().getId();
            UserInfo info = new UserInfo();
            info.setUserId(String.valueOf(res.getUser().getId()));
            info.setUsername(res.getUser().getNickname());
            info.setStatus(String.valueOf(res.getUser().getDaily()));
            return info;
        }
        return null;
    }

    public static void sendAlert(String msg) {
    }

    public static void sendAlertAndCall(String msg) {
    }

    public static void sendSMSToUser(String username, String message) {
        if (ConfigGame.getIntValue("enable_alert_msg", 0) == 1) {
            try {
                AlertServiceImpl alert = new AlertServiceImpl();
                alert.sendSMS2User(username, message);
            }
            catch (Exception alert) {
                // empty catch block
            }
        }
    }

    public static boolean disablePlayMiniGame(User user) {
//        int daily;
//        String status = (String)user.getProperty((Object)"dai_ly");
//        return status != null && !status.isEmpty() && (daily = Integer.parseInt(status)) > 0;
        return false;
    }
}

