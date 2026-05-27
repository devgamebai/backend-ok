/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StartGameMsg
extends BaseMsgEx {
    public long referenceId;
    public long timeBet;

    public StartGameMsg() {
        super(29001);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.referenceId);
        buffer.putLong(this.timeBet);
        return this.packBuffer(buffer);
    }
}

