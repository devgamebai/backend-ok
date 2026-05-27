/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.xocdia.entities.GamePot;
import java.nio.ByteBuffer;
import java.util.Vector;

public class StartGameMsg
extends BaseMsg {
    public String banker;
    public int gameId;
    public long moneyBanker;
    public Vector<GamePot> potList;

    public StartGameMsg(String banker) {
        super((short)3117);
        this.banker = banker;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.banker);
        bf.putInt(this.gameId);
        bf.putLong(this.moneyBanker);
        for (GamePot pot : this.potList) {
            bf.put(pot.id);
            this.putBoolean(bf, Boolean.valueOf(pot.isLock));
        }
        return this.packBuffer(bf);
    }
}

