/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class BuyPotMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public static final byte NOT_ENOUGH_MONEY = 1;
    public static final byte BUY_LIMIT = 2;
    public String nickname;
    public long moneyBuy;
    public long rmMoneySell;

    public BuyPotMsg(String nickname) {
        super((short)3111);
        this.nickname = nickname;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        bf.putLong(this.moneyBuy);
        bf.putLong(this.rmMoneySell);
        return this.packBuffer(bf);
    }
}

