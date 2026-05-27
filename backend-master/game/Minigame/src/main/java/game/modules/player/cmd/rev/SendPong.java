/*
 * Decompiled with CFR 0.144.
 */
package game.modules.player.cmd.rev;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendPong
extends BaseMsgEx {
    public SendPong() {
        super(1001);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        return this.packBuffer(buffer);
    }
}

