/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ChangeRoomCaoThapMsg
extends BaseMsgEx {
    public byte status;

    public ChangeRoomCaoThapMsg() {
        super(6006);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.status);
        return this.packBuffer(bf);
    }
}

