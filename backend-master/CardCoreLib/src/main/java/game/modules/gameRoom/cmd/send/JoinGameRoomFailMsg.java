/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class JoinGameRoomFailMsg
extends BaseMsg {
    public static final byte INFO_ERROR = 1;
    public static final byte ROOM_ERROR = 2;
    public static final byte MONEY_ERROR = 3;
    public static final byte JOIN_ERROR = 4;
    public static final byte JOIN_TIME_ERROR = 5;
    public static final byte SYSTEM_MAINTAIN_ERROR = 6;
    public static final byte ROOM_NOT_FOUND = 7;
    public static final byte WRONG_PASSWORD = 8;
    public static final byte ROOM_FULL = 9;
    public static final byte BAN_BY_BOSS = 10;
    public static final byte INFO_CREATE_ERROR = 11;
    public static final byte LEVEL_ERROR = 12;
    public static final byte FULL_BOARD_ERROR = 13;
    public static final byte LOCK_CREATE_ROOM = 14;

    public JoinGameRoomFailMsg() {
        super((short)3004);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        return this.packBuffer(buffer);
    }
}

