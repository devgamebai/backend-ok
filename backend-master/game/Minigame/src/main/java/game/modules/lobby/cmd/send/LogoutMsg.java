/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class LogoutMsg
extends BaseMsgEx {
    public String username;

    public LogoutMsg() {
        super(20071);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.username);
        return this.packBuffer(bf);
    }
}

