/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultCaoThapMsg
extends BaseMsgEx {
    public byte card;
    public long money1;
    public long money2;
    public long money3;

    public ResultCaoThapMsg() {
        super(6002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.card);
        bf.putLong(this.money1);
        bf.putLong(this.money2);
        bf.putLong(this.money3);
        return this.packBuffer(bf);
    }
}

