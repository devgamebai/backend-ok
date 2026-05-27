/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendUpdateOwnerRoom
extends BaseMsgEx {
    public int ownerChair;

    public SendUpdateOwnerRoom() {
        super(3117);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.ownerChair);
        return this.packBuffer(bf);
    }
}

