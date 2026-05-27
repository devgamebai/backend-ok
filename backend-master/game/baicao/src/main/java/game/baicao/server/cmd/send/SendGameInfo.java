/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.common.business.Debug
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.baicao.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import game.baicao.server.GamePlayer;
import game.baicao.server.logic.GroupCard;
import game.baicao.server.sPlayerInfo;
import game.baicao.server.sResultInfo;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendGameInfo
extends BaseMsg {
    public byte chair;
    public byte chuongChair;
    public byte[] cards;
    public sResultInfo res;
    public boolean isAutoStart;
    public int gameState;
    public int gameAction;
    public int countdownTime;
    public int comissionRate;
    public int jackpotRate;
    public int moneyType;
    public long moneyBet;
    public int gameId;
    public int roomId;
    public boolean[] hasInfoAtChair = new boolean[8];
    public GamePlayer[] pInfos = new GamePlayer[8];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        bf.put(this.chuongChair);
        this.putByteArray(bf, this.cards);
        this.putIntArray(bf, this.res.cuocDanhBien);
        this.putIntArray(bf, this.res.cuocKeCua);
        bf.put((byte)this.gameState);
        this.putBoolean(bf, Boolean.valueOf(this.isAutoStart));
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        Debug.trace((Object[])new Object[]{"SendGameInfo createData gameState=,gameAction=,countdownTime=,", this.gameState, this.gameAction, this.countdownTime});
        bf.put((byte)this.moneyType);
        this.putLong(bf, this.moneyBet);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 8; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            bf.put((byte)gp.getPlayerStatus());
            this.putLong(bf, gp.gameMoneyInfo.currentMoney);
            bf.putInt(gp.spRes.cuocGa);
            bf.putInt(gp.spRes.cuocChuong);
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            this.putStr(bf, pInfo.nickName);
        }
        return this.packBuffer(bf);
    }

    public void initPrivateInfo(GamePlayer gp) {
        this.chair = (byte)gp.chair;
        this.res = gp.spRes;
        this.cards = gp.spInfo.handCards != null ? gp.spInfo.handCards.toByteArray() : new byte[0];
    }
}

