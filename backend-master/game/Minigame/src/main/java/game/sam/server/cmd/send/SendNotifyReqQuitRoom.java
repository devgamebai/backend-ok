/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendNotifyReqQuitRoom
extends BaseMsgEx {
    public byte chair;
    public boolean reqQuitRoom;

    public SendNotifyReqQuitRoom() {
        super(3111);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBoolean(bf, Boolean.valueOf(this.reqQuitRoom));
        return this.packBuffer(bf);
    }
}

