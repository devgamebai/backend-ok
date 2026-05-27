/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.galaxy;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GalaxyX2Msg
extends BaseMsgEx {
    public String ngayX2;

    public GalaxyX2Msg() {
        super(8009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        return this.packBuffer(bf);
    }
}

