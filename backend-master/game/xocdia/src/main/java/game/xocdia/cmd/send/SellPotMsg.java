/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SellPotMsg
extends BaseMsg {
    public byte action;
    public long moneySell;

    public SellPotMsg() {
        super((short)3110);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.action);
        bf.putLong(this.moneySell);
        return this.packBuffer(bf);
    }
}

