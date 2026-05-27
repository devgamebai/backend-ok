/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StartPlayCaoThapMsg
extends BaseMsgEx {
    public long referenceId;
    public byte card;
    public long money1;
    public long money2;
    public long money3;
    public long currentMoney;

    public StartPlayCaoThapMsg() {
        super(6001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.referenceId);
        bf.put(this.card);
        bf.putLong(this.money1);
        bf.putLong(this.money2);
        bf.putLong(this.money3);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

