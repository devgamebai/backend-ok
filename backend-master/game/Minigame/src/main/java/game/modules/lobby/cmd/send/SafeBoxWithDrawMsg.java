/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SafeBoxWithDrawMsg
extends BaseMsgEx {
    public String message;
    public long currentMoney;

    public SafeBoxWithDrawMsg() {
        super(20312);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.message);
        this.currentMoney = bf.getLong();
        return this.packBuffer(bf);
    }
}

