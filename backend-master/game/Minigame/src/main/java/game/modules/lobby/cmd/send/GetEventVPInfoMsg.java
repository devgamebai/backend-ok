/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GetEventVPInfoMsg
extends BaseMsgEx {
    public byte status;
    public long time;

    public GetEventVPInfoMsg() {
        super(20039);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.status);
        bf.putLong(this.time);
        return this.packBuffer(bf);
    }
}

