/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GetVQVipMsg
extends BaseMsgEx {
    public short remainCount;

    public GetVQVipMsg() {
        super(20043);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort(this.remainCount);
        return this.packBuffer(bf);
    }
}

