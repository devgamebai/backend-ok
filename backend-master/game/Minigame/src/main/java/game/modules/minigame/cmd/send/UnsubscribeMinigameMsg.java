/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UnsubscribeMinigameMsg
extends BaseMsgEx {
    public short gameId;

    public UnsubscribeMinigameMsg() {
        super(2001);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.gameId);
        return this.packBuffer(buffer);
    }
}

