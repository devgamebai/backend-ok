/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.xizach.server.GamePlayer;
import game.xizach.server.logic.GroupCard;
import game.xizach.server.sPlayerInfo;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SendKetQuaXiZach
extends BaseMsg {
    public GamePlayer[] gamePlayers = new GamePlayer[6];
    public List<Long> currentMoneyList = new ArrayList<Long>();
    public List<Long> winMoneyList = new ArrayList<Long>();
    public List<Boolean> needShowCard = new ArrayList<Boolean>();
    public List<Boolean> needUpdateXizach = new ArrayList<Boolean>();

    public SendKetQuaXiZach() {
        super((short)3134);
    }

    public void copyData(SendKetQuaXiZach msg) {
        this.gamePlayers = msg.gamePlayers;
        this.needShowCard = msg.needShowCard;
        this.needUpdateXizach = msg.needUpdateXizach;
        this.winMoneyList = msg.winMoneyList;
        this.currentMoneyList = msg.currentMoneyList;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBooleanArray(bf, this.convertBoolean(this.needShowCard));
        this.putBooleanArray(bf, this.convertBoolean(this.needUpdateXizach));
        for (int i = 0; i < this.gamePlayers.length; ++i) {
            GamePlayer gp = this.gamePlayers[i];
            if (!this.needShowCard.get(i).booleanValue()) continue;
            this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
        }
        this.putLongArray(bf, this.convert(this.winMoneyList));
        this.putLongArray(bf, this.convert(this.currentMoneyList));
        return this.packBuffer(bf);
    }

    public boolean[] convertBoolean(List<Boolean> list) {
        boolean[] d = new boolean[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            d[i] = list.get(i);
        }
        return d;
    }

    public long[] convert(List<Long> list) {
        long[] d = new long[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            d[i] = list.get(i);
        }
        return d;
    }
}

