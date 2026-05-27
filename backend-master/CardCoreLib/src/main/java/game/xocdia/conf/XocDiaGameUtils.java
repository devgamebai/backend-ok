/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.gamebai.entities.XocDiaBoss
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.cache.UserMoneyModel
 */
package game.xocdia.conf;

import bitzero.util.common.business.Debug;
import com.vinplay.gamebai.entities.XocDiaBoss;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import game.modules.gameRoom.entities.BossManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaConfig;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class XocDiaGameUtils {
    public static long getFundByName(String roomName, int roomId, int rule) {
        try {
            if (GameUtils.gameName.equalsIgnoreCase("XocDia") && rule == 2) {
                XocDiaBoss boss = BossManager.instance().getBoss(roomId);
                return boss.getFundInitial();
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return 0;
    }

    public static int isCanCreateBoss(GameRoomSetting setting, long moneyRequire, String nickname) {
        UserServiceImpl ser;
        UserCacheModel user;
        //UserMoneyModel user;
        int res = 11;
        if (setting.rule == 2 && setting.moneyType == 1 && moneyRequire >= Math.round((double)setting.moneyBet * XocDiaConfig.fundVipMinRegis) && (user = (ser = new UserServiceImpl()).getMoneyUser(nickname)) != null) {
            int numBoardCanCreate = 0;
            for (Map.Entry<Integer, Integer> entry : XocDiaConfig.mapNumBoardBoss.entrySet()) {
                if (user.getVippointSave() <= entry.getKey()) continue;
                numBoardCanCreate = entry.getValue();
                break;
            }
            res = numBoardCanCreate > 0 ? (BossManager.instance().getNumBoard(nickname) < numBoardCanCreate ? 0 : 13) : 12;
        }
        return res;
    }

    public static boolean isXocDia() {
        return GameUtils.gameName.equals("XocDia");
    }

    public static int getRuleJoin() {
        Random rd = new Random();
        double i = rd.nextDouble() * 100.0;
        if (i < XocDiaConfig.ratioJoinGlobal) {
            return 1;
        }
        if (i < XocDiaConfig.ratioJoinGlobal + XocDiaConfig.ratioJoinVip) {
            return 2;
        }
        return 0;
    }
}

