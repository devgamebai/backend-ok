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

public class SendKetQuaSoBai
extends BaseMsg {
    public GamePlayer[] gamePlayers = new GamePlayer[6];
    public long chair1Money;
    public long chair2Money;
    public long chair1Balance;
    public long chair2Balance;
    public GamePlayer gp1;
    public GamePlayer gp2;
    public boolean hasCard1;
    public boolean hasCard2;
    public boolean isXiZach;
    public long curWinPlayerMoney;
    public long curLostPlayerMoney;

    public SendKetQuaSoBai() {
        super((short)3126);
    }

    public byte[] createData() {
        byte[] c;
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.gp1.chair);
        bf.put((byte)this.gp2.chair);
        this.putLong(bf, this.chair1Money);
        this.putLong(bf, this.chair2Money);
        this.putLong(bf, this.chair1Balance);
        this.putLong(bf, this.chair2Balance);
        this.putBoolean(bf, Boolean.valueOf(this.hasCard1));
        this.putBoolean(bf, Boolean.valueOf(this.hasCard2));
        if (this.gp1 != null) {
            this.putByteArray(bf, this.gp1.spInfo.handCards.toByteArray());
        } else {
            c = new byte[]{};
            this.putByteArray(bf, c);
        }
        if (this.gp2 != null) {
            this.putByteArray(bf, this.gp2.spInfo.handCards.toByteArray());
        } else {
            c = new byte[]{};
            this.putByteArray(bf, c);
        }
        this.putBoolean(bf, Boolean.valueOf(this.isXiZach));
        return this.packBuffer(bf);
    }
}

