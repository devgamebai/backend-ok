/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendChuyenGiaiDoan2
extends BaseMsg {
    public SendChuyenGiaiDoan2() {
        super((short)3124);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)20);
        return this.packBuffer(bf);
    }
}

