/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUpdateOwnerRoom
extends BaseMsg {
    public int ownerChair;

    public SendUpdateOwnerRoom() {
        super((short)3117);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.ownerChair);
        return this.packBuffer(bf);
    }
}

