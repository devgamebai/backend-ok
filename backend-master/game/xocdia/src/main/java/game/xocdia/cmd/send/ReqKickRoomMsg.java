/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ReqKickRoomMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public static final byte KICK = 1;
    public static final byte REGIS_KICK = 2;
    public static final byte DESTROY_KICK = 3;
    public byte reason;

    public ReqKickRoomMsg() {
        super((short)3132);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.reason);
        return this.packBuffer(bf);
    }
}

