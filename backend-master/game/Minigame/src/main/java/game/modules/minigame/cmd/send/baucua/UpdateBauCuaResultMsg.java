/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateBauCuaResultMsg
extends BaseMsgEx {
    public byte dice1;
    public byte dice2;
    public byte dice3;
    public byte xPot;
    public byte xValue;

    public UpdateBauCuaResultMsg() {
        super(5008);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.dice1);
        bf.put(this.dice2);
        bf.put(this.dice3);
        bf.put(this.xPot);
        bf.put(this.xValue);
        return this.packBuffer(bf);
    }
}

