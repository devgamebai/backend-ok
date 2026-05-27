/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RegisLeaveRoomMsg
extends BaseMsg {
    public boolean reqLeaveRoom;
    public String nickname;

    public RegisLeaveRoomMsg(String nickname) {
        super((short)3100);
        this.nickname = nickname;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.reqLeaveRoom));
        this.putStr(bf, this.nickname);
        return this.packBuffer(bf);
    }
}

