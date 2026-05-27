/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UserInfoCaoThapMsg
extends BaseMsgEx {
    public byte numA;
    public byte card;
    public long money1;
    public long money2;
    public long money3;
    public short time;
    public byte step;
    public long referenceId;
    public String cards;

    public UserInfoCaoThapMsg() {
        super(6009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.numA);
        bf.put(this.card);
        bf.putLong(this.money1);
        bf.putLong(this.money2);
        bf.putLong(this.money3);
        bf.putShort(this.time);
        bf.put(this.step);
        bf.putLong(this.referenceId);
        this.putStr(bf, this.cards);
        return this.packBuffer(bf);
    }
}

