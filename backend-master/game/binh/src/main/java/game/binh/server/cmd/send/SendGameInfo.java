/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.binh.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.binh.server.GamePlayer;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.KetQuaSoBai;
import game.binh.server.sPlayerInfo;
import game.binh.server.sResultInfo;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendGameInfo
extends BaseMsg {
    public int moneyType;
    public int chair;
    public int gameState;
    public int gameAction;
    public int countdownTime;
    public long moneyBet;
    public int gameId;
    public int roomId;
    public int rule;
    public boolean[] hasInfoAtChair = new boolean[4];
    public GamePlayer[] pInfos = new GamePlayer[4];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.gameState);
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        this.putLong(bf, this.moneyBet);
        bf.put((byte)this.moneyType);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        bf.put((byte)this.rule);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 4; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            if (this.gameState == 1) {
                if (i == this.chair) {
                    if (gp.spInfo.handCards != null) {
                        this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
                    } else {
                        this.putByteArray(bf, new byte[13]);
                    }
                }
            } else if (this.gameState == 2) {
                this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
                bf.put((byte)gp.kiemTraMauBinh(this.rule));
                this.putLong(bf, gp.spRes.getResultWithPlayer((int)gp.chair).moneyCommon);
            }
            this.putBoolean(bf, Boolean.valueOf(gp.sochi));
            bf.put((byte)gp.getPlayerStatus());
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            bf.putInt(pInfo.userId);
            this.putStr(bf, pInfo.nickName);
            this.putLong(bf, gp.gameMoneyInfo.currentMoney);
        }
        return this.packBuffer(bf);
    }
}

