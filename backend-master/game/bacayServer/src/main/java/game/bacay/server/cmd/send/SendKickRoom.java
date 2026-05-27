/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendKickRoom
extends BaseMsg {
    public static final int ERROR_MONEY = 1;
    public static final int ERROR_BAO_TRI = 2;
    public byte reason;

    public SendKickRoom() {
        super((short)3120);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.reason);
        return this.packBuffer(bf);
    }
}

