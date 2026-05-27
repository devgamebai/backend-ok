/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.lieng.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendBuyIn
extends BaseMsg {
    public static final byte ERROR_BUYIN = 1;
    public int chair;
    public long buyInMoney;

    public SendBuyIn() {
        super((short)3102);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.putLong(this.buyInMoney);
        return this.packBuffer(bf);
    }
}

