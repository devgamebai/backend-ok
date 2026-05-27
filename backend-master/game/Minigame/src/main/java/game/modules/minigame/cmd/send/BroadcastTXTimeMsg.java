/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BroadcastTXTimeMsg
extends BaseMsgEx {
    public byte remainTime;
    public boolean betting;

    public BroadcastTXTimeMsg() {
        super(2124);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remainTime);
        this.putBoolean(bf, Boolean.valueOf(this.betting));
        return this.packBuffer(bf);
    }
}

