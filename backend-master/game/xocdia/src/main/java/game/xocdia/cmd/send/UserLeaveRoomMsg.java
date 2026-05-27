/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UserLeaveRoomMsg
extends BaseMsg {
    public String nickname;

    public UserLeaveRoomMsg(String nickname) {
        super((short)3104);
        this.nickname = nickname;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        return this.packBuffer(bf);
    }
}

