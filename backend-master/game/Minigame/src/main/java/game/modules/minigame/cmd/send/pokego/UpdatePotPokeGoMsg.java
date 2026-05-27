/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.pokego;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdatePotPokeGoMsg
extends BaseMsgEx {
    public long value;
    public byte x2 = 0;

    public UpdatePotPokeGoMsg() {
        super(7002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}

