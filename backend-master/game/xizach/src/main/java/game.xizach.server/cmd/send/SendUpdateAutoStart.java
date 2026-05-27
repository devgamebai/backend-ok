/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUpdateAutoStart
extends BaseMsg {
    public boolean isAutoStart;
    public byte autoStartTime;
    public byte chuongChair;

    public SendUpdateAutoStart() {
        super((short)3107);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.isAutoStart));
        bf.put(this.autoStartTime);
        bf.put(this.chuongChair);
        return this.packBuffer(bf);
    }
}

