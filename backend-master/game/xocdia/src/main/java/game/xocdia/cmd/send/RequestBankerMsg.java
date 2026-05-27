/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RequestBankerMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public static final byte NOT_ENOUGH_MONEY = 1;
    public long moneyRequire;

    public RequestBankerMsg() {
        super((short)3113);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyRequire);
        return this.packBuffer(bf);
    }
}

