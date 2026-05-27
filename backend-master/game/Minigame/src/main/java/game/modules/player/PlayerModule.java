/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.player;

import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import game.entities.NormalMoneyInfo;
import game.entities.UserScore;
import game.entities.VipMoneyInfo;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.player.cmd.rev.RevBangXepHang;
import game.modules.player.cmd.send.SendBangXepHang;
import game.modules.player.entities.RankingTableNormal;
import game.modules.player.entities.RankingTableVip;

public class PlayerModule
extends BaseClientRequestHandler {
    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == GameEventType.EVENT_ADD_SCORE) {
            User user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            UserScore score = (UserScore)ibzevent.getParameter((IBZEventParam)GameEventParam.USER_SCORE);
            this.userAddEventScore(user, score);
        }
    }

    public void init() {
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 1001: {
                this.getRankingTable(user, dataCmd);
            }
        }
    }

    private void getRankingTable(User user, DataCmd dataCmd) {
        RevBangXepHang cmd = new RevBangXepHang(dataCmd);
        SendBangXepHang msg = new SendBangXepHang();
        if (cmd.type == 1) {
            msg.type = 1;
            msg.top = RankingTableVip.getIntansce().getTopInfoWinToday();
            this.send((BaseMsg)msg, user);
        }
        if (cmd.type == 0) {
            msg.type = 0;
            msg.top = RankingTableNormal.getIntansce().getTopInfoWinToday();
            this.send((BaseMsg)msg, user);
        }
    }

    private void userAddEventScore(User user, UserScore score) {
        VipMoneyInfo info;
        NormalMoneyInfo info2;
        if (score.moneyType == 1 && (info = VipMoneyInfo.getInfo(user)) != null) {
            info.addScore(score);
            info.save();
        }
        if (score.moneyType == 0 && (info2 = NormalMoneyInfo.getInfo(user)) != null) {
            info2.addScore(score);
            info2.save();
        }
    }
}

