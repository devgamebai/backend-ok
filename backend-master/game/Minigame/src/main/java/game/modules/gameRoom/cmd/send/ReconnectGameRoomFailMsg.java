/*
 * Decompiled with CFR 0.144.
 */
package game.modules.gameRoom.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ReconnectGameRoomFailMsg
extends BaseMsgEx {
    public ReconnectGameRoomFailMsg() {
        super(3002);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        return this.packBuffer(buffer);
    }
}

