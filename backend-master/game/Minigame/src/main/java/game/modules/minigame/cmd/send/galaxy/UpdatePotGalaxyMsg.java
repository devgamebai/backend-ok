/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.galaxy;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdatePotGalaxyMsg
extends BaseMsgEx {
    public long value;
    public byte x2 = 0;

    public UpdatePotGalaxyMsg() {
        super(8002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}

