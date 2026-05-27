/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BroadcastMessageMsg
extends BaseMsgEx {
    public String message;

    public BroadcastMessageMsg() {
        super(20100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.message);
        return this.packBuffer(bf);
    }
}

