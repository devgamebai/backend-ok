/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateTimeCaoThapMsg
extends BaseMsgEx {
    public short time;

    public UpdateTimeCaoThapMsg() {
        super(6008);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort(this.time);
        return this.packBuffer(bf);
    }
}

