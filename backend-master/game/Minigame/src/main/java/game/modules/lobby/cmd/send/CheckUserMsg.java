/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class CheckUserMsg
extends BaseMsgEx {
    public byte type;
    public byte fee;

    public CheckUserMsg() {
        super(20018);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.type);
        bf.put(this.fee);
        return this.packBuffer(bf);
    }
}

