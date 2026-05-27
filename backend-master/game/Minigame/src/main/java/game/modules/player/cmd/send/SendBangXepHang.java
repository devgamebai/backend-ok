/*
 * Decompiled with CFR 0.144.
 */
package game.modules.player.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendBangXepHang
extends BaseMsgEx {
    public byte type;
    public String top = "[]";

    public SendBangXepHang() {
        super(1001);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.put(this.type);
        this.putStr(buffer, this.top);
        return this.packBuffer(buffer);
    }
}

