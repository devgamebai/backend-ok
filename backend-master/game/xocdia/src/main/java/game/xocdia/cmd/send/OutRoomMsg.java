/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class OutRoomMsg
extends BaseMsg {
    public static final byte NO_LEAVE = 0;
    public static final byte ERROR_MONEY = 1;
    public static final byte SYSTEM_MAINTAIN = 2;
    public static final byte REQ_LEAVE_ROOM = 3;
    public static final byte DISCONNECT = 4;
    public static final byte NO_PLAY = 5;
    public static final byte DESTROY_ROOM = 6;
    public static final byte REQ_KICK_ROOM = 7;
    public byte reason;

    public OutRoomMsg() {
        super((short)3120);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.reason);
        return this.packBuffer(bf);
    }
}

