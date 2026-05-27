/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 */
package game.modules.gameRoom.entities;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.utils.GameUtils;
import java.text.ParseException;

public class ListGameMoneyInfo {
    public static final String LIST_GAME_MONEY_INFO = "ListGameMoneyInfo";
    private static ListGameMoneyInfo ins = null;

    public static ListGameMoneyInfo instance() {
        if (ins == null) {
            ins = new ListGameMoneyInfo();
            ins.init();
        }
        return ins;
    }

    public synchronized void removeGameMoneyInfo(GameMoneyInfo info, int roomId) {
        info.restoreMoney(roomId);
    }

    private ListGameMoneyInfo() {
    }

    private void init() {
        MoneyInGameServiceImpl moneyService = new MoneyInGameServiceImpl();
        try {
            Debug.trace((Object)("restoreFreeze start: " + GameUtils.gameName));
            moneyService.restoreFreeze("*", GameUtils.gameName, "*", "*");
            Debug.trace((Object)("restoreFreeze done: " + GameUtils.gameName));
        }
        catch (ParseException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }
}

