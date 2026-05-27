/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class GetRevenueMsg
extends BaseMsg {
    public long moneyBoss;
    public static final byte SUCCESS = 0;
    public static final byte MONEY_ERROR = 1;

    public GetRevenueMsg() {
        super((short)3134);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyBoss);
        return this.packBuffer(bf);
    }
}

