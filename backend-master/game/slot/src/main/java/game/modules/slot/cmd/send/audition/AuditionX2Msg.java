/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class AuditionX2Msg
extends BaseMsg {
    public String ngayX2;

    public AuditionX2Msg() {
        super((short)2009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        return this.packBuffer(bf);
    }
}

