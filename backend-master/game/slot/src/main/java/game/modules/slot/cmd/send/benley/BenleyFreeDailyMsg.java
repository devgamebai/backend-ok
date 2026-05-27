/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.benley;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class BenleyFreeDailyMsg
extends BaseMsg {
    public byte remain = 0;

    public BenleyFreeDailyMsg() {
        super((short)4012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}

