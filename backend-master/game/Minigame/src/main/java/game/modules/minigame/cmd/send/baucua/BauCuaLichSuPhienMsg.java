/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BauCuaLichSuPhienMsg
extends BaseMsgEx {
    public String data;

    public BauCuaLichSuPhienMsg() {
        super(5010);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.data);
        return this.packBuffer(bf);
    }
}

