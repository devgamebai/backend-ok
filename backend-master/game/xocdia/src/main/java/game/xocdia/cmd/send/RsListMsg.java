/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;
import java.util.List;

public class RsListMsg
extends BaseMsg {
    public int totalEven;
    public int totalOdd;
    public List<Byte> rsList;

    public RsListMsg() {
        super((short)3121);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.totalEven);
        bf.putInt(this.totalOdd);
        bf.putInt(this.rsList.size());
        for (Byte rs : this.rsList) {
            bf.put(rs);
        }
        return this.packBuffer(bf);
    }
}

