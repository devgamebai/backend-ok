/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateRutLocMsg
extends BaseMsgEx {
    public int soLuotRut = 0;

    public UpdateRutLocMsg() {
        super(2122);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.soLuotRut);
        return this.packBuffer(bf);
    }
}

