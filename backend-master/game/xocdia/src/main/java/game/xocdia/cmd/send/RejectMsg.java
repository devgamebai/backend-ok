/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RejectMsg
extends BaseMsg {
    public static final byte CAN_TAT = 1;
    public static final byte TRA_LAI = 2;
    public byte action;
    public long money;

    public RejectMsg() {
        super((short)3127);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.action);
        bf.putLong(this.money);
        return this.packBuffer(bf);
    }
}

