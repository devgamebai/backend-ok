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
import game.xizach.server.sResultInfo;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SendEndGame
extends BaseMsg {
    public sResultInfo result;
    public int[] playerStatus = new int[6];
    public GamePlayer[] gamePlayers = new GamePlayer[6];
    public List<Long> tongKetThangThua = new ArrayList<Long>();
    public List<Long> currentMoneyList = new ArrayList<Long>();
    public List<Long> winMoneyList = new ArrayList<Long>();
    public List<Boolean> needShowWinLostMoney = new ArrayList<Boolean>();
    public byte[][] cards = new byte[6][];

    public SendEndGame() {
        super((short)3103);
    }

    public void copyData(SendEndGame msg) {
        this.result = msg.result;
        this.playerStatus = msg.playerStatus;
        this.gamePlayers = msg.gamePlayers;
        this.tongKetThangThua = msg.tongKetThangThua;
        this.currentMoneyList = msg.currentMoneyList;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putIntArray(bf, this.playerStatus);
        for (int i = 0; i < this.gamePlayers.length; ++i) {
            GamePlayer gp = this.gamePlayers[i];
            if (gp == null || !gp.isPlaying()) continue;
            this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
        }
        if (this.result != null) {
            bf.putLong(this.result.tongTienCuoiVan);
        } else {
            bf.putLong(0L);
        }
        this.putLongArray(bf, this.convert(this.tongKetThangThua));
        this.putLongArray(bf, this.convert(this.currentMoneyList));
        this.putBooleanArray(bf, this.convertBoolean(this.needShowWinLostMoney));
        bf.put((byte)12);
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

