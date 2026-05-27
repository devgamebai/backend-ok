/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class LichSuPhienMsg
extends BaseMsgEx {
    public String data;

    public LichSuPhienMsg() {
        super(2116);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        this.putStr(buffer, this.data);
        return this.packBuffer(buffer);
    }
}

