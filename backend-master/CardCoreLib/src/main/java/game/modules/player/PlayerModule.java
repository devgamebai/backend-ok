/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.player;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.player.cmd.rev.RevBangXepHang;
import game.modules.player.cmd.rev.RevPing;
import game.modules.player.cmd.rev.SendPong;
import game.modules.player.cmd.send.SendBangXepHang;
import game.modules.player.entities.MoneyRanking;
import game.modules.player.entities.NormalMoneyInfo;
import game.modules.player.entities.RankingTableNormal;
import game.modules.player.entities.RankingTableVip;
import game.modules.player.entities.VipMoneyInfo;

public class PlayerModule
extends BaseClientRequestHandler {
    private String x = "test";
    private int y = 1000;

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        User user;
        if (ibzevent.getType() == GameEventType.EVENT_ADD_SCORE) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            UserScore score = (UserScore)ibzevent.getParameter((IBZEventParam)GameEventParam.USER_SCORE);
            this.userAddEventScore(user, score);
        }
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDisconnected(user);
        }
    }

    private void userDisconnected(User user) {
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        NormalMoneyInfo nmInfo = NormalMoneyInfo.getInfo(user);
        VipMoneyInfo vInfo = VipMoneyInfo.getInfo(user);
        nmInfo.save();
        vInfo.save();
    }

    public void init() {
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.EVENT_ADD_SCORE, (IBZEventListener)this);
        RankingTableNormal.getIntansce();
        RankingTableVip.getIntansce();
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 1050: {
                this.onPingPong(user, dataCmd);
                break;
            }
            case 1001: {
                this.getRankingTable(user, dataCmd);
                break;
            }
        }
    }

    private void onPingPong(User user, DataCmd dataCmd) {
        RevPing cmd = new RevPing(dataCmd);
        SendPong msg = new SendPong();
        msg.v = cmd.v;
        this.send((BaseMsg)msg, user);
    }

    private void getRankingTable(User user, DataCmd dataCmd) {
        RevBangXepHang cmd = new RevBangXepHang(dataCmd);
        SendBangXepHang msg = new SendBangXepHang();
        if (cmd.type == 1) {
            msg.type = 1;
            msg.topDay = RankingTableVip.getIntansce().getTopDay();
            msg.topWeek = RankingTableVip.getIntansce().getTopWeek();
            msg.topAll = RankingTableVip.getIntansce().getTopAll();
            msg.topDayWin = RankingTableVip.getIntansce().getTopDayWin();
            msg.topWeekWin = RankingTableVip.getIntansce().getTopWeekWin();
            msg.topAllWin = RankingTableVip.getIntansce().getTopAllWin();
            this.send((BaseMsg)msg, user);
        }
        if (cmd.type == 0) {
            msg.type = 0;
            msg.topDay = RankingTableNormal.getIntansce().getTopDay();
            msg.topWeek = RankingTableNormal.getIntansce().getTopWeek();
            msg.topAll = RankingTableNormal.getIntansce().getTopAll();
            msg.topDayWin = RankingTableNormal.getIntansce().getTopDayWin();
            msg.topWeekWin = RankingTableNormal.getIntansce().getTopWeekWin();
            msg.topAllWin = RankingTableNormal.getIntansce().getTopAllWin();
            this.send((BaseMsg)msg, user);
        }
    }

    private void userAddEventScore(User user, UserScore sc) {
        MoneyRanking info;
        if (sc.moneyType == 1 && (info = VipMoneyInfo.getInfo(user)) != null) {
            info.addScore(sc);
            info.save();
            if (sc.money > 0) {
                RankingTableVip.getIntansce().addTopInfo(info, user.isBot());
            }
        }
        if (sc.moneyType == 0 && (info = NormalMoneyInfo.getInfo(user)) != null) {
            info.addScore(sc);
            info.save();
            if (sc.money > 0) {
                RankingTableNormal.getIntansce().addTopInfo(info, user.isBot());
            }
        }
    }
}

