/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.thanden;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class ThanDenFreeDailyMsg
extends BaseMsg {
    public byte remain = 0;

    public ThanDenFreeDailyMsg() {
        super((short)19012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}

