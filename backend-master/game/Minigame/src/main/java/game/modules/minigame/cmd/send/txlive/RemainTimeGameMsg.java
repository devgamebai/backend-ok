/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class RemainTimeGameMsg
extends BaseMsgEx {
    public long remainTime;
    public long referenceId;

    public RemainTimeGameMsg() {
        super(29004);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.remainTime);
        buffer.putLong(this.referenceId);
        return this.packBuffer(buffer);
    }
}

