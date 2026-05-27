/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class BetMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public static final byte NOT_ENOUGH_MONEY = 1;
    public static final byte LIMIT_POT = 2;
    public String nickname;
    public long betMoney;
    public byte potId;
    public long potMoney;
    public long currentMoney;

    public BetMsg(String nickname) {
        super((short)3106);
        this.nickname = nickname;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        bf.putLong(this.betMoney);
        bf.put(this.potId);
        bf.putLong(this.potMoney);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

