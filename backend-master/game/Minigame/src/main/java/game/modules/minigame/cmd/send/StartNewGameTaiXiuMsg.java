/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StartNewGameTaiXiuMsg
extends BaseMsgEx {
    public long referenceId;
    public short remainTimeRutLoc;
    public long jpTai;
    public long jpXiu;

    public StartNewGameTaiXiuMsg() {
        super(2115);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.referenceId);
        buffer.putShort(this.remainTimeRutLoc);
        buffer.putLong(this.jpTai);
        buffer.putLong(this.jpXiu);
        return this.packBuffer(buffer);
    }
}

