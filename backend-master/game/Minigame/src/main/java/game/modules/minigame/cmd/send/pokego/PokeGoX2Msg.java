/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.pokego;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class PokeGoX2Msg
extends BaseMsgEx {
    public String ngayX2;

    public PokeGoX2Msg() {
        super(7009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        return this.packBuffer(bf);
    }
}

