/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RequestDestroyRoomMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public boolean reqDestroyRoom;

    public RequestDestroyRoomMsg() {
        super((short)3133);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.reqDestroyRoom));
        return this.packBuffer(bf);
    }
}

