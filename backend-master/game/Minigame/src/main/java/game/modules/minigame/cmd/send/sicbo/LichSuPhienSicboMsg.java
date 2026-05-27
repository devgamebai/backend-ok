/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.sicbo;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class LichSuPhienSicboMsg
extends BaseMsgEx {
    public String data;

    public LichSuPhienSicboMsg() {
        super(28116);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        this.putStr(buffer, this.data);
        return this.packBuffer(buffer);
    }
}

