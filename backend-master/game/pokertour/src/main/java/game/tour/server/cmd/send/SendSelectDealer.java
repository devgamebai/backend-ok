/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tour.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.tour.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendSelectDealer
extends BaseMsg {
    public int dealer;
    public int smallBlind;
    public int bigBlind;
    public boolean[] hasInfoAtChair = new boolean[6];
    public GamePlayer[] gamePlayers = new GamePlayer[6];
    public int gameId;
    public boolean isCheat = false;
    public long[] moneyArray = new long[6];
    public boolean[] requireBigBlinds = new boolean[6];
    public int smallBlindMoney = 0;

    public SendSelectDealer() {
        super((short)3100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.dealer);
        bf.put((byte)this.smallBlind);
        bf.put((byte)this.bigBlind);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 6; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            bf.put((byte)this.gamePlayers[i].getPlayerStatus());
        }
        bf.putInt(this.gameId);
        this.putBoolean(bf, Boolean.valueOf(this.isCheat));
        this.putLongArray(bf, this.moneyArray);
        this.putBooleanArray(bf, this.requireBigBlinds);
        bf.putInt(this.smallBlindMoney);
        return this.packBuffer(bf);
    }
}

