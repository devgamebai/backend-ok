/*
 * Decompiled with CFR 0.144.
 */
package game.modules.chat.cmd.sendmd5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class ChatInfoMsg
extends BaseMsgEx {
    public String msg;
    public byte minVipPointRequire;
    public long timeUnBan;
    public byte userType;

    public ChatInfoMsg() {
        super(MiniGameCMD.CHATMD5_LOG);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.msg);
        bf.put(this.minVipPointRequire);
        bf.putLong(this.timeUnBan);
        bf.put(this.userType);
        return this.packBuffer(bf);
    }
}

