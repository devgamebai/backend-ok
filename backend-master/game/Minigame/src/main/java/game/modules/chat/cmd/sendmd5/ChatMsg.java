/*
 * Decompiled with CFR 0.144.
 */
package game.modules.chat.cmd.sendmd5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class ChatMsg
extends BaseMsgEx {
    public String nickname = "";
    public String mesasge = "";

    public ChatMsg() {
        super(MiniGameCMD.CHATMD5);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        this.putStr(bf, this.mesasge);
        return this.packBuffer(bf);
    }
}

