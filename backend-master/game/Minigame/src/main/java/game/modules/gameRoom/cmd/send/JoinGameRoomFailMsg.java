/*
 * Decompiled with CFR 0.144.
 */
package game.modules.gameRoom.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class JoinGameRoomFailMsg
extends BaseMsgEx {
    public static final byte INFO_ERROR = 1;
    public static final byte ROOM_ERROR = 2;
    public static final byte MONEY_ERROR = 3;
    public static final byte JOIN_ERROR = 4;

    public JoinGameRoomFailMsg() {
        super(3004);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        return this.packBuffer(buffer);
    }
}

