/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendNotifyReqQuitRoom
extends BaseMsg {
    public byte chair;
    public boolean reqQuitRoom;

    public SendNotifyReqQuitRoom() {
        super((short)3111);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBoolean(bf, Boolean.valueOf(this.reqQuitRoom));
        return this.packBuffer(bf);
    }
}

