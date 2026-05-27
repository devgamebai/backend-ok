/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultRutLocMsg
extends BaseMsgEx {
    public int prize = 0;
    public long currentMoney;

    public ResultRutLocMsg() {
        super(2119);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

