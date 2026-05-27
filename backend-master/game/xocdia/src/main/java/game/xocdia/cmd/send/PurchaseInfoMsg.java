/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class PurchaseInfoMsg
extends BaseMsg {
    public long moneyEven;
    public long moneyOdd;

    public PurchaseInfoMsg() {
        super((short)3126);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyEven);
        bf.putLong(this.moneyOdd);
        return this.packBuffer(bf);
    }
}

