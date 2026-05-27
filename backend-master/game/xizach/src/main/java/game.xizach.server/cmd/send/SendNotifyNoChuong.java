/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendNotifyNoChuong
extends BaseMsg {
    public SendNotifyNoChuong() {
        super((short)3132);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        return this.packBuffer(bf);
    }
}

