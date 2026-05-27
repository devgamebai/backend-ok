/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.minipoker;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdatePotMiniPokerMsg
extends BaseMsgEx {
    public long value;
    public byte x2 = 0;

    public UpdatePotMiniPokerMsg() {
        super(4002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}

