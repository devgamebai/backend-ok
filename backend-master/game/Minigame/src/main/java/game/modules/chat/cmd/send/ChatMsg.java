/*
 * Decompiled with CFR 0.144.
 */
package game.modules.chat.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ChatMsg
extends BaseMsgEx {
    public String nickname = "";
    public String mesasge = "";

    public ChatMsg() {
        super(18000);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        this.putStr(bf, this.mesasge);
        return this.packBuffer(bf);
    }
}

