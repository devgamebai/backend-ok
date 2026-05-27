/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StartNewRoundRutLocMsg
extends BaseMsgEx {
    public int remainTime;

    public StartNewRoundRutLocMsg() {
        super(2121);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.remainTime);
        return this.packBuffer(bf);
    }
}

