/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ChatRoomMsg
extends BaseMsg {
    public int chair;
    public boolean isIcon;
    public String content;
    public String nickName;

    public ChatRoomMsg() {
        super((short)3008);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        this.putBoolean(bf, Boolean.valueOf(this.isIcon));
        this.putStr(bf, this.content);
        this.putStr(bf, this.nickName);
        return this.packBuffer(bf);
    }
}

